package de.radiolumen.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FeedbackFlowTest {
    @Test
    fun lowHostRatingIsUrgentAndDeliveredOnce() {
        val fixture = fixture()
        val result = fixture.coordinator.rateHost(
            showInstanceId = "2026-11-05-nachmittag",
            hostId = "host-mara",
            hostName = "Mara Schulz",
            score = 2,
            tags = listOf("Unruhig"),
            moderated = true,
        )
        val accepted = assertIs<SubmitResult.Accepted>(result)
        assertTrue(accepted.delivered)
        assertTrue(fixture.event(accepted).urgent)
        assertEquals(1, fixture.feedback.forwarded.size)
        assertEquals(ForwardResult.DUPLICATE, fixture.feedback.forward(accepted.event))
        assertEquals(1, fixture.feedback.forwarded.size)
    }

    @Test
    fun secondPlaylistRatingInTheSameHourIsRejected() {
        val fixture = fixture()
        assertIs<SubmitResult.Accepted>(
            fixture.coordinator.ratePlaylist("hour-1", 5, listOf("Stimmig"), ""),
        )
        assertIs<SubmitResult.Rejected>(
            fixture.coordinator.ratePlaylist("hour-1", 1, emptyList(), ""),
        )
        assertEquals(1, fixture.feedback.forwarded.size)
    }

    @Test
    fun threeMixedScoresEscalateWithoutAlarmingOnTheFirst() {
        val fixture = fixture()
        val first = fixture.coordinator.ratePlaylist("h1", 3, emptyList(), "")
        val second = fixture.coordinator.ratePlaylist("h2", 3, emptyList(), "")
        val third = fixture.coordinator.ratePlaylist("h3", 3, emptyList(), "")
        assertFalse(fixture.event(first).urgent)
        assertFalse(fixture.event(second).urgent)
        assertTrue(fixture.event(third).urgent)
        assertEquals("Gemischt", fixture.coordinator.snapshot().mood.label)
    }

    @Test
    fun aSingleLowPlaylistScoreIsUrgentImmediately() {
        val fixture = fixture()
        val result = fixture.coordinator.ratePlaylist("h1", 1, listOf("Zu gleichförmig"), "Bitte mehr lokal")
        assertTrue(fixture.event(result).urgent)
        assertEquals("Kritisch", fixture.coordinator.snapshot().mood.label)
    }

    @Test
    fun wishesAreImmediateButNotUrgentAndThenCoolDown() {
        val clock = Clock(1_000L)
        val fixture = fixture(clock)
        val track = DemoStation.tracks.first()
        val first = assertIs<SubmitResult.Accepted>(fixture.coordinator.wish(track, "für Lea"))
        assertFalse(fixture.event(first).urgent)
        assertIs<SubmitResult.Rejected>(fixture.coordinator.wish(track, ""))
        clock.value += 15_000L
        assertIs<SubmitResult.Accepted>(fixture.coordinator.wish(track, ""))
    }

    @Test
    fun unmoderatedShowCannotBeRated() {
        val fixture = fixture()
        val result = fixture.coordinator.rateHost(
            "night",
            "host-none",
            "Ohne Moderation",
            5,
            emptyList(),
            false,
        )
        assertIs<SubmitResult.Rejected>(result)
        assertTrue(fixture.feedback.forwarded.isEmpty())
    }

    @Test
    fun failedForwardStaysInTheOutboxAndFlushCompletesIt() {
        val outbox = MemoryOutbox()
        val flaky = FlakyPort()
        val coordinator = FeedbackCoordinator(
            ledger = MemoryLedger(),
            desk = StudioDesk(),
            feedback = flaky,
            outbox = outbox,
            clock = { 10L },
            ids = { "fixed-id" },
        )
        val accepted = assertIs<SubmitResult.Accepted>(
            coordinator.ratePlaylist("h1", 4, emptyList(), ""),
        )
        assertFalse(accepted.delivered)
        assertEquals(listOf("fixed-id"), outbox.pending().map { it.eventId })
        val delivered = OutboxFlusher.flush(outbox.pending(), flaky) { outbox.markForwarded(it) }
        assertEquals(1, delivered)
        assertTrue(outbox.pending().isEmpty())
        assertEquals(1, flaky.accepted)
    }

    @Test
    fun acknowledgeClearsTheOpenUrgentCount() {
        val fixture = fixture()
        val accepted = assertIs<SubmitResult.Accepted>(
            fixture.coordinator.ratePlaylist("h1", 1, emptyList(), ""),
        )
        assertEquals(1, fixture.coordinator.snapshot().openUrgent)
        fixture.coordinator.acknowledge(accepted.event.eventId)
        assertEquals(0, fixture.coordinator.snapshot().openUrgent)
    }

    @Test
    fun resolvingTheCurrentTitleStaysCheap() {
        val playout = DemoStation.playout()
        val start = System.nanoTime()
        var checksum = 0L
        repeat(50_000) { step ->
            checksum += playout.current(1_730_000_000_000L + step * 1_000L).version
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        println("PERF playout50k_ms=$elapsedMs checksum=$checksum")
        assertTrue(elapsedMs < 1_500L, "Auflösung zu langsam: ${elapsedMs}ms")
    }

    private fun Fixture.event(result: SubmitResult): StudioEvent {
        val accepted = assertIs<SubmitResult.Accepted>(result)
        return coordinator.snapshot().events.first { it.id == accepted.event.eventId }
    }

    private fun fixture(clock: Clock = Clock(5_000L)): Fixture {
        val feedback = StationFeedbackStub()
        return Fixture(
            coordinator = FeedbackCoordinator(
                ledger = MemoryLedger(),
                desk = StudioDesk(),
                feedback = feedback,
                outbox = MemoryOutbox(),
                clock = clock,
                ids = sequentialIds(),
            ),
            feedback = feedback,
        )
    }

    private class Fixture(
        val coordinator: FeedbackCoordinator,
        val feedback: StationFeedbackStub,
    )

    private class Clock(var value: Long) : () -> Long {
        override fun invoke(): Long = value
    }

    private class FlakyPort : FeedbackPort {
        var accepted: Int = 0
        private var calls = 0

        override fun forward(event: FeedbackEvent): ForwardResult {
            calls++
            if (calls == 1) error("Sender vorübergehend nicht erreichbar")
            accepted++
            return ForwardResult.ACCEPTED
        }
    }

    private fun sequentialIds(): () -> String {
        var n = 0
        return { "id-${++n}" }
    }
}

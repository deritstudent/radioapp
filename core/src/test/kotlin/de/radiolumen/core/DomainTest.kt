package de.radiolumen.core

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptedPlayoutTest {
    private val slots = listOf(
        ScriptedPlayout.Slot(Track("a", "A", "Eins", "Album"), 10),
        ScriptedPlayout.Slot(Track("b", "B", "Zwei", "Album"), 5),
    )
    private val playout = ScriptedPlayout(slots, ZoneId.of("Europe/Berlin"))

    @Test
    fun startsOnFirstTrack() {
        val now = playout.current(0L)
        assertEquals("Eins", now.track.title)
        assertEquals(0L, now.startedAtEpochMs)
        assertEquals(0f, now.elapsedFraction(0L))
    }

    @Test
    fun switchesOnExactBoundary() {
        val atBoundary = playout.current(10_000L)
        assertEquals("Zwei", atBoundary.track.title)
        assertEquals(10_000L, atBoundary.startedAtEpochMs)
        val justBefore = playout.current(9_999L)
        assertEquals("Eins", justBefore.track.title)
    }

    @Test
    fun versionIsStableInsideATrackAndChangesAfterwards() {
        val first = playout.current(1_000L).version
        val later = playout.current(4_000L).version
        val next = playout.current(10_000L).version
        assertEquals(first, later)
        assertTrue(next != first)
    }

    @Test
    fun nextBoundaryLandsOnTheTrackEnd() {
        assertEquals(10_000L, playout.nextBoundaryEpochMs(2_500L))
    }

    @Test
    fun afternoonShowIsModeratedAndNightIsNot() {
        val afternoon = playout.current(berlin(2026, 11, 5, 15, 30))
        assertEquals("Nachmittagsstudio", afternoon.show.name)
        assertEquals("Mara Schulz", afternoon.show.hostName)
        assertTrue(afternoon.show.moderated)
        assertEquals("2026-11-05-15", afternoon.playlistId)

        val night = playout.current(berlin(2026, 11, 5, 2, 15))
        assertEquals("Nachtstrecke", night.show.name)
        assertFalse(night.show.moderated)
    }

    @Test
    fun progressReachesOneAtTheEndOfTheSlot() {
        val now = playout.current(0L)
        assertEquals(1f, now.elapsedFraction(10_000L))
    }

    private fun berlin(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Berlin"))
            .toInstant()
            .toEpochMilli()
}

class CatalogTest {
    private val catalog = DemoStation.catalog()

    @Test
    fun blankQueryReturnsTheArchiveHead() {
        assertEquals(12, catalog.search("").size)
    }

    @Test
    fun searchIgnoresCaseAndDiacritics() {
        val hits = catalog.search("spatsonne")
        assertEquals(listOf("Spätsonne"), hits.map { it.title })
    }

    @Test
    fun searchMatchesArtist() {
        assertEquals("Kira Sol", catalog.search("kira").single().artist)
    }
}

class CodecTest {
    @Test
    fun playlistRoundtripKeepsNote() {
        val event = FeedbackEvent.Playlist(
            eventId = "e1",
            createdAtEpochMs = 42L,
            playlistId = "2026-11-05-15",
            score = 2,
            tags = listOf("Stimmig"),
            note = "Zu viel | Pause",
        )
        assertEquals(event, FeedbackCodec.decode(FeedbackCodec.encode(event)))
    }

    @Test
    fun wishRoundtrip() {
        val event = FeedbackEvent.Wish(
            eventId = "e2",
            createdAtEpochMs = 7L,
            trackId = "t",
            artist = "Kira Sol",
            title = "Nordlicht",
            dedication = "für die Spätschicht",
        )
        assertEquals(event, FeedbackCodec.decode(FeedbackCodec.encode(event)))
    }
}

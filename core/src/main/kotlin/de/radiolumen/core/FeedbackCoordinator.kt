package de.radiolumen.core

import java.util.UUID

class FeedbackCoordinator(
    private val ledger: SubmissionLedger,
    private val desk: StudioDesk,
    private val feedback: FeedbackPort,
    private val outbox: DurableOutbox,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ids: () -> String = { UUID.randomUUID().toString() },
    private val wishCooldownMs: Long = 15_000L,
) {
    private var lastWishAt: Long = Long.MIN_VALUE / 4

    fun ratePlaylist(
        playlistId: String,
        score: Int,
        tags: List<String>,
        note: String,
    ): SubmitResult = synchronized(this) {
        val key = "playlist:$playlistId"
        if (ledger.contains(key)) {
            return Rejected("Für diese Stunde liegt bereits eine Bewertung vor.")
        }
        if (score !in 1..5) return Rejected("Die Bewertung braucht eine Stufe von 1 bis 5.")
        val cleanTags = tags.distinct()
        if (cleanTags.size > 3 || cleanTags.any { it !in Vocabulary.playlistTags }) {
            return Rejected("Es sind höchstens drei Schlagworte aus der Liste zulässig.")
        }
        val cleanNote = sanitize(note, 240)
        val event = FeedbackEvent.Playlist(
            eventId = ids(),
            createdAtEpochMs = clock(),
            playlistId = playlistId,
            score = score,
            tags = cleanTags,
            note = cleanNote,
        )
        deliver(key, event) {
            desk.onPlaylist(event)
        }
    }

    fun rateHost(
        showInstanceId: String,
        hostId: String,
        hostName: String,
        score: Int,
        tags: List<String>,
        moderated: Boolean,
    ): SubmitResult = synchronized(this) {
        if (!moderated) return Rejected("Diese Strecke läuft ohne Moderation.")
        val key = "host:$showInstanceId"
        if (ledger.contains(key)) {
            return Rejected("Die Moderation dieser Strecke wurde bereits bewertet.")
        }
        if (score !in 1..5) return Rejected("Die Bewertung braucht eine Stufe von 1 bis 5.")
        val cleanTags = tags.distinct()
        if (cleanTags.size > 3 || cleanTags.any { it !in Vocabulary.hostTags }) {
            return Rejected("Es sind höchstens drei Schlagworte aus der Liste zulässig.")
        }
        val event = FeedbackEvent.Host(
            eventId = ids(),
            createdAtEpochMs = clock(),
            showInstanceId = showInstanceId,
            hostId = hostId,
            hostName = hostName,
            score = score,
            tags = cleanTags,
        )
        deliver(key, event) {
            desk.onHost(event)
        }
    }

    fun wish(track: CatalogTrack, dedication: String): SubmitResult = synchronized(this) {
        val now = clock()
        if (now - lastWishAt < wishCooldownMs) {
            return Rejected("Der nächste Wunsch ist in wenigen Sekunden möglich.")
        }
        val cleanDedication = sanitize(dedication, 140)
        val event = FeedbackEvent.Wish(
            eventId = ids(),
            createdAtEpochMs = now,
            trackId = track.id,
            artist = track.artist,
            title = track.title,
            dedication = cleanDedication,
        )
        lastWishAt = now
        deliver(null, event) {
            desk.onWish(event)
        }
    }

    fun acknowledge(id: String): StudioSnapshot {
        desk.acknowledge(id)
        return desk.snapshot()
    }

    fun snapshot(): StudioSnapshot = desk.snapshot()

    private fun deliver(
        key: String?,
        event: FeedbackEvent,
        publishLocal: () -> Unit,
    ): SubmitResult {
        outbox.stage(key, event)
        if (key != null) ledger.add(key)
        publishLocal()
        val delivered = try {
            feedback.forward(event)
            outbox.markForwarded(event.eventId)
            true
        } catch (_: Exception) {
            false
        }
        return SubmitResult.Accepted(event, key, delivered)
    }

    private fun sanitize(value: String, max: Int): String =
        value.replace('\n', ' ').replace('\t', ' ').trim().take(max)

    private fun Rejected(reason: String) = SubmitResult.Rejected(reason)
}

object OutboxFlusher {
    fun flush(
        pending: List<FeedbackEvent>,
        feedback: FeedbackPort,
        mark: (String) -> Unit,
    ): Int {
        var delivered = 0
        for (event in pending) {
            try {
                feedback.forward(event)
                mark(event.eventId)
                delivered++
            } catch (_: Exception) {
                break
            }
        }
        return delivered
    }
}

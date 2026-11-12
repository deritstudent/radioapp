package de.radiolumen.core

class AlertPolicy(
    private val singleThreshold: Int = 2,
    private val weakThreshold: Int = 3,
    private val escalationCount: Int = 3,
) {
    fun playlistUrgent(score: Int, weakInWindow: Int): Boolean =
        score <= singleThreshold || weakInWindow >= escalationCount

    fun hostUrgent(score: Int): Boolean = score <= singleThreshold

    fun countsAsWeak(score: Int): Boolean = score <= weakThreshold
}

class StudioDesk(
    private val policy: AlertPolicy = AlertPolicy(),
    private val capacity: Int = 80,
    private val windowMs: Long = 10 * 60 * 1000L,
) {
    private val lock = Any()
    private val events = ArrayDeque<StudioEvent>()
    private val playlistScores = ArrayDeque<Scored>()

    fun onPlaylist(event: FeedbackEvent.Playlist): StudioEvent = synchronized(lock) {
        playlistScores.addLast(Scored(event.createdAtEpochMs, event.score))
        while (playlistScores.size > 20) playlistScores.removeFirst()
        val urgent = policy.playlistUrgent(event.score, weakInWindow(event.createdAtEpochMs))
        return push(
            StudioEvent(
                id = event.eventId,
                kind = FeedbackKind.PLAYLIST,
                title = "Playlist ${event.score}/5",
                detail = detail(event.tags, event.note),
                score = event.score,
                urgent = urgent,
                createdAtEpochMs = event.createdAtEpochMs,
            ),
        )
    }

    fun onHost(event: FeedbackEvent.Host): StudioEvent = synchronized(lock) {
        val urgent = policy.hostUrgent(event.score)
        return push(
            StudioEvent(
                id = event.eventId,
                kind = FeedbackKind.HOST,
                title = "${event.hostName} ${event.score}/5",
                detail = detail(event.tags, ""),
                score = event.score,
                urgent = urgent,
                createdAtEpochMs = event.createdAtEpochMs,
            ),
        )
    }

    fun onWish(event: FeedbackEvent.Wish): StudioEvent = synchronized(lock) { push(
        StudioEvent(
            id = event.eventId,
            kind = FeedbackKind.WISH,
            title = event.title,
            detail = buildString {
                append(event.artist)
                if (event.dedication.isNotBlank()) {
                    append(" · ")
                    append(event.dedication)
                }
            },
            score = null,
            urgent = false,
            createdAtEpochMs = event.createdAtEpochMs,
        ),
        )
    }

    fun acknowledge(id: String): Boolean = synchronized(lock) {
        val index = events.indexOfFirst { it.id == id }
        if (index < 0) return false
        events[index] = events[index].copy(acknowledged = true)
        true
    }

    fun snapshot(): StudioSnapshot = synchronized(lock) {
        val ordered = events.sortedWith(
            compareByDescending<StudioEvent> { it.urgent && !it.acknowledged }
                .thenByDescending { it.createdAtEpochMs },
        )
        return StudioSnapshot(
            events = ordered,
            mood = mood(),
            openUrgent = ordered.count { it.urgent && !it.acknowledged },
        )
    }

    private fun push(event: StudioEvent): StudioEvent {
        events.addFirst(event)
        while (events.size > capacity) events.removeLast()
        return event
    }

    private fun weakInWindow(now: Long): Int {
        val start = now - windowMs
        return playlistScores.count { it.at >= start && policy.countsAsWeak(it.score) }
    }

    private fun mood(): Mood {
        if (playlistScores.isEmpty()) {
            return Mood(average = null, label = "Noch keine Rückmeldung", sampleSize = 0)
        }
        val average = playlistScores.map { it.score }.average()
        val label = when {
            average < 2.5 -> "Kritisch"
            average < 3.5 -> "Gemischt"
            average < 4.5 -> "Tragend"
            else -> "Sehr getragen"
        }
        return Mood(average = average, label = label, sampleSize = playlistScores.size)
    }

    private fun detail(tags: List<String>, note: String): String = buildString {
        append(tags.joinToString(", "))
        if (note.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(note)
        }
        if (isEmpty()) append("Ohne Kommentar")
    }

    private data class Scored(val at: Long, val score: Int)
}

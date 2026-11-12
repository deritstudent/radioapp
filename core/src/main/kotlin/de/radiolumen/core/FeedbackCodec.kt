package de.radiolumen.core

import java.util.Base64

object FeedbackCodec {
    fun encode(event: FeedbackEvent): String = when (event) {
        is FeedbackEvent.Playlist -> join(
            "PLAYLIST",
            event.eventId,
            event.createdAtEpochMs.toString(),
            event.playlistId,
            event.score.toString(),
            event.tags.joinToString(","),
            b64(event.note),
        )

        is FeedbackEvent.Host -> join(
            "HOST",
            event.eventId,
            event.createdAtEpochMs.toString(),
            event.showInstanceId,
            event.hostId,
            b64(event.hostName),
            event.score.toString(),
            event.tags.joinToString(","),
        )

        is FeedbackEvent.Wish -> join(
            "WISH",
            event.eventId,
            event.createdAtEpochMs.toString(),
            event.trackId,
            b64(event.artist),
            b64(event.title),
            b64(event.dedication),
        )
    }

    fun decode(payload: String): FeedbackEvent {
        val parts = payload.split('|')
        require(parts.firstOrNull() == "v1" && parts.size >= 4) { "Unbekanntes Outbox-Format" }
        return when (parts[1]) {
            "PLAYLIST" -> FeedbackEvent.Playlist(
                eventId = parts[2],
                createdAtEpochMs = parts[3].toLong(),
                playlistId = parts[4],
                score = parts[5].toInt(),
                tags = splitTags(parts[6]),
                note = unb64(parts[7]),
            )

            "HOST" -> FeedbackEvent.Host(
                eventId = parts[2],
                createdAtEpochMs = parts[3].toLong(),
                showInstanceId = parts[4],
                hostId = parts[5],
                hostName = unb64(parts[6]),
                score = parts[7].toInt(),
                tags = splitTags(parts[8]),
            )

            "WISH" -> FeedbackEvent.Wish(
                eventId = parts[2],
                createdAtEpochMs = parts[3].toLong(),
                trackId = parts[4],
                artist = unb64(parts[5]),
                title = unb64(parts[6]),
                dedication = unb64(parts[7]),
            )

            else -> error("Unbekannte Ereignisart")
        }
    }

    private fun join(vararg cells: String): String = buildString {
        append("v1")
        cells.forEach { cell ->
            append('|')
            append(cell)
        }
    }

    private fun b64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun unb64(value: String): String =
        String(Base64.getDecoder().decode(value), Charsets.UTF_8)

    private fun splitTags(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split(',')
}

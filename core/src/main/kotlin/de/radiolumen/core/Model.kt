package de.radiolumen.core

data class Track(
    val id: String,
    val artist: String,
    val title: String,
    val album: String,
)

data class Show(
    val id: String,
    val instanceId: String,
    val name: String,
    val hostId: String,
    val hostName: String,
    val moderated: Boolean,
)

data class NowPlaying(
    val track: Track,
    val show: Show,
    val playlistId: String,
    val playlistLabel: String,
    val startedAtEpochMs: Long,
    val durationSec: Int,
    val version: Long,
    val upNext: List<Track>,
) {
    fun elapsedFraction(nowEpochMs: Long): Float {
        val elapsed = (nowEpochMs - startedAtEpochMs).coerceAtLeast(0L)
        val total = durationSec * 1000L
        if (total <= 0L) return 0f
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}

data class CatalogTrack(
    val id: String,
    val artist: String,
    val title: String,
    val album: String,
    val year: Int,
)

enum class FeedbackKind { PLAYLIST, HOST, WISH }

sealed class FeedbackEvent {
    abstract val eventId: String
    abstract val createdAtEpochMs: Long
    abstract val kind: FeedbackKind

    data class Playlist(
        override val eventId: String,
        override val createdAtEpochMs: Long,
        val playlistId: String,
        val score: Int,
        val tags: List<String>,
        val note: String,
    ) : FeedbackEvent() {
        override val kind: FeedbackKind = FeedbackKind.PLAYLIST
    }

    data class Host(
        override val eventId: String,
        override val createdAtEpochMs: Long,
        val showInstanceId: String,
        val hostId: String,
        val hostName: String,
        val score: Int,
        val tags: List<String>,
    ) : FeedbackEvent() {
        override val kind: FeedbackKind = FeedbackKind.HOST
    }

    data class Wish(
        override val eventId: String,
        override val createdAtEpochMs: Long,
        val trackId: String,
        val artist: String,
        val title: String,
        val dedication: String,
    ) : FeedbackEvent() {
        override val kind: FeedbackKind = FeedbackKind.WISH
    }
}

enum class ForwardResult { ACCEPTED, DUPLICATE }

sealed class SubmitResult {
    data class Accepted(
        val event: FeedbackEvent,
        val dedupeKey: String?,
        val delivered: Boolean,
    ) : SubmitResult()

    data class Rejected(val reason: String) : SubmitResult()
}

data class StudioEvent(
    val id: String,
    val kind: FeedbackKind,
    val title: String,
    val detail: String,
    val score: Int?,
    val urgent: Boolean,
    val createdAtEpochMs: Long,
    val acknowledged: Boolean = false,
)

data class Mood(
    val average: Double?,
    val label: String,
    val sampleSize: Int,
)

data class StudioSnapshot(
    val events: List<StudioEvent>,
    val mood: Mood,
    val openUrgent: Int,
)

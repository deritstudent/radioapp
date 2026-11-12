package de.radiolumen.hoererfunk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "now_playing")
data class NowPlayingEntity(
    @PrimaryKey val singleton: Int = 1,
    val trackId: String,
    val artist: String,
    val title: String,
    val album: String,
    val showId: String,
    val showInstanceId: String,
    val showName: String,
    val hostId: String,
    val hostName: String,
    val moderated: Boolean,
    val playlistId: String,
    val playlistLabel: String,
    val startedAtEpochMs: Long,
    val durationSec: Int,
    val version: Long,
    val upNext: String,
    val cachedAtEpochMs: Long,
)

@Entity(tableName = "submission_key")
data class SubmissionKeyEntity(
    @PrimaryKey val keyName: String,
)

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val eventId: String,
    val payload: String,
    val createdAt: Long,
    val forwardedAt: Long? = null,
)

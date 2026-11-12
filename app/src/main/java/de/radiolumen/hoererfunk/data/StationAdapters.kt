package de.radiolumen.hoererfunk.data

import de.radiolumen.core.DurableOutbox
import de.radiolumen.core.FeedbackCodec
import de.radiolumen.core.FeedbackPort
import de.radiolumen.core.MemoryLedger
import de.radiolumen.core.NowPlaying
import de.radiolumen.core.OutboxFlusher
import de.radiolumen.core.PlayoutPort
import de.radiolumen.core.Show
import de.radiolumen.core.Track
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers

class RoomOutbox(private val dao: LumenDao) : DurableOutbox {
    override fun stage(key: String?, event: de.radiolumen.core.FeedbackEvent) {
        dao.stage(key, event.eventId, FeedbackCodec.encode(event), event.createdAtEpochMs)
    }

    override fun markForwarded(eventId: String) {
        dao.markForwarded(eventId, System.currentTimeMillis())
    }
}

class StationSync(
    private val dao: LumenDao,
    private val ledger: MemoryLedger,
    private val feedback: FeedbackPort,
) {
    fun warm() {
        ledger.hydrate(dao.allKeys())
        val pending = dao.pending().map { FeedbackCodec.decode(it.payload) }
        OutboxFlusher.flush(pending, feedback) { eventId ->
            dao.markForwarded(eventId, System.currentTimeMillis())
        }
    }
}

class NowPlayingSource(
    private val playout: PlayoutPort,
    private val dao: LumenDao,
) {
    fun stream(): Flow<NowPlaying> = flow {
        val cached = dao.readNow()?.toDomain()
        if (cached != null) emit(cached)
        var lastVersion = cached?.version
        while (currentCoroutineContext().isActive) {
            val nowMs = System.currentTimeMillis()
            val fresh = playout.current(nowMs)
            if (fresh.version != lastVersion) {
                dao.writeNow(fresh.toEntity(nowMs))
                lastVersion = fresh.version
                emit(fresh)
            }
            val boundary = playout.nextBoundaryEpochMs(nowMs)
            val wait = minOf(boundary - nowMs, HEARTBEAT_MS).coerceAtLeast(MIN_WAIT_MS)
            delay(wait)
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val HEARTBEAT_MS = 30_000L
        const val MIN_WAIT_MS = 400L
    }
}

private fun NowPlaying.toEntity(cachedAt: Long) = NowPlayingEntity(
    trackId = track.id,
    artist = track.artist,
    title = track.title,
    album = track.album,
    showId = show.id,
    showInstanceId = show.instanceId,
    showName = show.name,
    hostId = show.hostId,
    hostName = show.hostName,
    moderated = show.moderated,
    playlistId = playlistId,
    playlistLabel = playlistLabel,
    startedAtEpochMs = startedAtEpochMs,
    durationSec = durationSec,
    version = version,
    upNext = upNext.joinToString("\n") { "${it.id}\t${it.artist}\t${it.title}\t${it.album}" },
    cachedAtEpochMs = cachedAt,
)

private fun NowPlayingEntity.toDomain(): NowPlaying = NowPlaying(
    track = Track(trackId, artist, title, album),
    show = Show(showId, showInstanceId, showName, hostId, hostName, moderated),
    playlistId = playlistId,
    playlistLabel = playlistLabel,
    startedAtEpochMs = startedAtEpochMs,
    durationSec = durationSec,
    version = version,
    upNext = upNext.lineSequence().filter { it.isNotBlank() }.map { line ->
        val cells = line.split('\t')
        Track(cells[0], cells[1], cells[2], cells[3])
    }.toList(),
)

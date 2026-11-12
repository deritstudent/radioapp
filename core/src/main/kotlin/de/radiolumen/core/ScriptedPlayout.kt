package de.radiolumen.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScriptedPlayout(
    private val slots: List<Slot>,
    private val zone: ZoneId = ZoneId.of("Europe/Berlin"),
) : PlayoutPort {
    init {
        require(slots.isNotEmpty()) { "Die Sendeplanung braucht mindestens einen Titel." }
        require(slots.all { it.durationSec > 0 }) { "Titellängen müssen positiv sein." }
    }

    data class Slot(val track: Track, val durationSec: Int)

    override fun current(nowEpochMs: Long): NowPlaying {
        val safeNow = nowEpochMs.coerceAtLeast(0L)
        val cycle = cycleSec()
        val elapsedSec = safeNow / 1000L
        val cycleIndex = elapsedSec / cycle
        val pos = (elapsedSec % cycle).toInt()
        var acc = 0
        var slotIndex = 0
        var into = 0
        for ((index, slot) in slots.withIndex()) {
            val next = acc + slot.durationSec
            if (pos < next) {
                slotIndex = index
                into = pos - acc
                break
            }
            acc = next
        }
        val slot = slots[slotIndex]
        val zoned = Instant.ofEpochMilli(safeNow).atZone(zone)
        val daypart = daypartFor(zoned.hour)
        val date = zoned.toLocalDate().format(DATE)
        return NowPlaying(
            track = slot.track,
            show = Show(
                id = daypart.id,
                instanceId = "$date-${daypart.id}",
                name = daypart.nameOfShow,
                hostId = daypart.hostId,
                hostName = daypart.hostName,
                moderated = daypart.moderated,
            ),
            playlistId = "$date-${zoned.hour}",
            playlistLabel = "Playlist ${zoned.hour}:00–${zoned.hour}:59 Uhr",
            startedAtEpochMs = safeNow - into * 1000L,
            durationSec = slot.durationSec,
            version = cycleIndex * 1_000_000L + slotIndex * 1_000L + daypart.ordinal,
            upNext = List(3) { offset -> slots[(slotIndex + offset + 1) % slots.size].track },
        )
    }

    override fun nextBoundaryEpochMs(nowEpochMs: Long): Long {
        val safeNow = nowEpochMs.coerceAtLeast(0L)
        val elapsedSec = safeNow / 1000L
        val pos = (elapsedSec % cycleSec()).toInt()
        var acc = 0
        var remainSec = slots.first().durationSec
        for (slot in slots) {
            val next = acc + slot.durationSec
            if (pos < next) {
                remainSec = next - pos
                break
            }
            acc = next
        }
        val remainMs = remainSec * 1000L - (safeNow % 1000L)
        return safeNow + remainMs.coerceAtLeast(1L)
    }

    private fun cycleSec(): Long = slots.sumOf { it.durationSec }.toLong()

    private fun daypartFor(hour: Int): Daypart = when (hour) {
        in 6..9 -> Daypart.MORNING
        in 10..13 -> Daypart.MIDDAY
        in 14..17 -> Daypart.AFTERNOON
        in 18..21 -> Daypart.EVENING
        else -> Daypart.NIGHT
    }

    enum class Daypart(
        val id: String,
        val nameOfShow: String,
        val hostId: String,
        val hostName: String,
        val moderated: Boolean,
    ) {
        MORNING("morgen", "Lumen am Morgen", "host-aylin", "Aylin Demir", true),
        MIDDAY("mittag", "Mittagsfrequenz", "host-jonas", "Jonas Weber", true),
        AFTERNOON("nachmittag", "Nachmittagsstudio", "host-mara", "Mara Schulz", true),
        EVENING("abend", "Abendfrequenz", "host-levin", "Levin Kranz", true),
        NIGHT("nacht", "Nachtstrecke", "host-none", "Ohne Moderation", false),
    }

    private companion object {
        val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

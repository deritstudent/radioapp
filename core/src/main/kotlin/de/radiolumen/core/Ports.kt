package de.radiolumen.core

interface PlayoutPort {
    fun current(nowEpochMs: Long = System.currentTimeMillis()): NowPlaying

    fun nextBoundaryEpochMs(nowEpochMs: Long = System.currentTimeMillis()): Long
}

interface CatalogPort {
    fun search(query: String, limit: Int = 12): List<CatalogTrack>
}

/** Sink towards the station systems that own ratings, wishes and billing of feedback. */
interface FeedbackPort {
    fun forward(event: FeedbackEvent): ForwardResult
}

interface SubmissionLedger {
    fun contains(key: String): Boolean

    fun add(key: String)

    fun hydrate(keys: Collection<String>)
}

interface DurableOutbox {
    fun stage(key: String?, event: FeedbackEvent)

    fun markForwarded(eventId: String)
}

class MemoryLedger : SubmissionLedger {
    private val keys = LinkedHashSet<String>()

    override fun contains(key: String): Boolean = synchronized(keys) { key in keys }

    override fun add(key: String) {
        synchronized(keys) { keys.add(key) }
    }

    override fun hydrate(keys: Collection<String>) {
        synchronized(this.keys) { this.keys.addAll(keys) }
    }
}

class StationFeedbackStub : FeedbackPort {
    private val seen = HashSet<String>()
    val forwarded: MutableList<FeedbackEvent> = mutableListOf()

    override fun forward(event: FeedbackEvent): ForwardResult = synchronized(seen) {
        if (!seen.add(event.eventId)) return ForwardResult.DUPLICATE
        forwarded += event
        ForwardResult.ACCEPTED
    }
}

class MemoryOutbox : DurableOutbox {
    val staged = mutableListOf<FeedbackEvent>()
    val keys = mutableListOf<String?>()
    val forwardedIds = mutableSetOf<String>()

    override fun stage(key: String?, event: FeedbackEvent) {
        keys += key
        staged += event
    }

    override fun markForwarded(eventId: String) {
        forwardedIds += eventId
    }

    fun pending(): List<FeedbackEvent> = staged.filter { it.eventId !in forwardedIds }
}

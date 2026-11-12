package de.radiolumen.hoererfunk

import android.app.Application
import androidx.room.Room
import de.radiolumen.core.DemoStation
import de.radiolumen.core.FeedbackCoordinator
import de.radiolumen.core.MemoryLedger
import de.radiolumen.core.StationFeedbackStub
import de.radiolumen.core.StudioDesk
import de.radiolumen.hoererfunk.data.LumenDatabase
import de.radiolumen.hoererfunk.data.NowPlayingSource
import de.radiolumen.hoererfunk.data.RoomOutbox
import de.radiolumen.hoererfunk.data.StationSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LumenApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

class AppGraph(application: Application) {
    private val database = Room.databaseBuilder(
        application,
        LumenDatabase::class.java,
        "lumen.db",
    ).build()
    private val dao = database.dao()
    private val ledger = MemoryLedger()
    val feedback = StationFeedbackStub()
    val catalog = DemoStation.catalog()
    val coordinator = FeedbackCoordinator(
        ledger = ledger,
        desk = StudioDesk(),
        feedback = feedback,
        outbox = RoomOutbox(dao),
    )
    val nowPlaying = NowPlayingSource(DemoStation.playout(), dao)
    private val sync = StationSync(dao, ledger, feedback)

    suspend fun warm() = withContext(Dispatchers.IO) { sync.warm() }

    companion object {
        const val STUDIO_CODE = "lumen-regie"
    }
}

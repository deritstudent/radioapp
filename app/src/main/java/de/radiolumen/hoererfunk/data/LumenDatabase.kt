package de.radiolumen.hoererfunk.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NowPlayingEntity::class, SubmissionKeyEntity::class, OutboxEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LumenDatabase : RoomDatabase() {
    abstract fun dao(): LumenDao
}

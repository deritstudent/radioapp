package de.radiolumen.hoererfunk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LumenDao {
    @Query("SELECT * FROM now_playing WHERE singleton = 1")
    suspend fun readNow(): NowPlayingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun writeNow(entity: NowPlayingEntity)

    @Query("SELECT keyName FROM submission_key")
    fun allKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertKey(key: SubmissionKeyEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun enqueue(entry: OutboxEntity)

    @Query("SELECT * FROM outbox WHERE forwardedAt IS NULL ORDER BY createdAt ASC")
    fun pending(): List<OutboxEntity>

    @Query("UPDATE outbox SET forwardedAt = :at WHERE eventId = :eventId")
    fun markForwarded(eventId: String, at: Long)

    @Transaction
    fun stage(key: String?, eventId: String, payload: String, createdAt: Long) {
        if (key != null) insertKey(SubmissionKeyEntity(key))
        enqueue(OutboxEntity(eventId = eventId, payload = payload, createdAt = createdAt))
    }
}

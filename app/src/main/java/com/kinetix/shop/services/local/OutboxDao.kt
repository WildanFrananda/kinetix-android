package com.kinetix.shop.services.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(
        entry: OutboxEntryEntity,
    )

    @Query("SELECT * FROM outbox_entries WHERE dedupeKey = :dedupeKey LIMIT 1")
    suspend fun findByDedupeKey(
        dedupeKey: String,
    ): OutboxEntryEntity?

    @Query("SELECT * FROM outbox_entries WHERE entryId = :entryId LIMIT 1")
    suspend fun findById(
        entryId: String,
    ): OutboxEntryEntity?

    @Query(
        "SELECT * FROM outbox_entries " +
            "WHERE status = :pendingStatus AND nextAttemptAtEpochSeconds <= :nowEpochSeconds " +
            "ORDER BY createdAtEpochSeconds ASC"
    )
    suspend fun listReadyEntries(
        pendingStatus: String,
        nowEpochSeconds: Long,
    ): List<OutboxEntryEntity>

    @Query(
        "UPDATE outbox_entries " +
            "SET status = :pendingStatus " +
            "WHERE status = :inFlightStatus"
    )
    suspend fun resetInFlightToPending(
        inFlightStatus: String,
        pendingStatus: String,
    )

    @Query(
        "UPDATE outbox_entries " +
            "SET status = :status, updatedAtEpochSeconds = :updatedAtEpochSeconds " +
            "WHERE entryId = :entryId"
    )
    suspend fun updateStatus(
        entryId: String,
        status: String,
        updatedAtEpochSeconds: Long,
    )

    @Query(
        "UPDATE outbox_entries " +
            "SET status = :status, attemptCount = :attemptCount, " +
            "nextAttemptAtEpochSeconds = :nextAttemptAtEpochSeconds, " +
            "lastError = :lastError, updatedAtEpochSeconds = :updatedAtEpochSeconds " +
            "WHERE entryId = :entryId"
    )
    suspend fun updateRetryState(
        entryId: String,
        status: String,
        attemptCount: Int,
        nextAttemptAtEpochSeconds: Long,
        lastError: String?,
        updatedAtEpochSeconds: Long,
    )

    @Query("DELETE FROM outbox_entries WHERE entryId = :entryId")
    suspend fun deleteById(
        entryId: String,
    )

    @Query("SELECT COUNT(*) FROM outbox_entries WHERE status = :pendingStatus")
    suspend fun countByStatus(
        pendingStatus: String,
    ): Int
}

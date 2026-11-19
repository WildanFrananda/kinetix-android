package com.kinetix.shop.services.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbox_entries",
    indices = [
        Index(value = ["dedupeKey"], unique = true),
        Index(value = ["status", "nextAttemptAtEpochSeconds"]),
    ],
)
data class OutboxEntryEntity(
    @PrimaryKey
    val entryId: String,
    val principalId: String,
    val operationType: String,
    val dedupeKey: String,
    val payloadJson: String,
    val status: String,
    val attemptCount: Int,
    val nextAttemptAtEpochSeconds: Long,
    val lastError: String?,
    val createdAtEpochSeconds: Long,
    val updatedAtEpochSeconds: Long,
)

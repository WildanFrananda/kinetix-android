package com.kinetix.shop.services.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_transaction_states",
    indices = [
        Index(value = ["principalId"]),
    ],
)
data class PendingTransactionStateEntity(
    @PrimaryKey
    val stateKey: String,
    val principalId: String,
    val operationType: String,
    val idempotencyKey: String,
    val referenceNumber: String?,
    val payloadFingerprint: String,
    val createdAtEpochSeconds: Long,
    val updatedAtEpochSeconds: Long,
)

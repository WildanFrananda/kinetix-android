package com.kinetix.shop.models.outbox

import com.kinetix.shop.models.ids.PrincipalId

data class OutboxEntry(
    val entryId: String,
    val principalId: PrincipalId,
    val operationType: OutboxOperationType,
    val dedupeKey: String,
    val payloadJson: String,
    val status: OutboxStatus,
    val attemptCount: Int,
    val nextAttemptAtEpochSeconds: Long,
    val lastError: String?,
    val createdAtEpochSeconds: Long,
    val updatedAtEpochSeconds: Long,
)

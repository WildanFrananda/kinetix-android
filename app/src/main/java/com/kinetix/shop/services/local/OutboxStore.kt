package com.kinetix.shop.services.local

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.outbox.OutboxEntry
import com.kinetix.shop.models.outbox.OutboxOperationType

interface OutboxStore {
    suspend fun enqueue(
        principalId: PrincipalId,
        operationType: OutboxOperationType,
        dedupeKey: String,
        payloadJson: String,
    ): OutboxEntry

    suspend fun listReadyEntries(
        nowEpochSeconds: Long,
    ): List<OutboxEntry>

    suspend fun markInFlight(
        entryId: String,
    )

    suspend fun markRetryLater(
        entryId: String,
        attemptCount: Int,
        nextAttemptAtEpochSeconds: Long,
        lastError: String,
    )

    suspend fun markPermanentFailure(
        entryId: String,
        attemptCount: Int,
        lastError: String,
    )

    suspend fun remove(
        entryId: String,
    )

    suspend fun countPending(): Int
}

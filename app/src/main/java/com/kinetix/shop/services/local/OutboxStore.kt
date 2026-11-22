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

    suspend fun reconcileConfirmedFields(
        dedupeKey: String,
        confirmedPayloadJson: String,
    )

    suspend fun recoverInFlightEntries()

    suspend fun listReadyEntries(
        nowEpochSeconds: Long,
    ): List<OutboxEntry>

    suspend fun markInFlight(
        entryId: String,
        expectedUpdatedAtEpochSeconds: Long = -1L,
    ): Boolean

    suspend fun markRetryLater(
        entryId: String,
        attemptCount: Int,
        nextAttemptAtEpochSeconds: Long,
        lastError: String,
        expectedUpdatedAtEpochSeconds: Long = -1L,
    )

    suspend fun markPermanentFailure(
        entryId: String,
        attemptCount: Int,
        lastError: String,
        expectedUpdatedAtEpochSeconds: Long = -1L,
    ): Boolean

    suspend fun remove(
        entryId: String,
        expectedUpdatedAtEpochSeconds: Long = -1L,
        consumedPayloadJson: String? = null,
    )

    suspend fun countPending(): Int
}

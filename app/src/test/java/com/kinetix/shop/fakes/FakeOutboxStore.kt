package com.kinetix.shop.fakes

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.outbox.OutboxEntry
import com.kinetix.shop.models.outbox.OutboxOperationType
import com.kinetix.shop.models.outbox.OutboxStatus
import com.kinetix.shop.services.local.OutboxStore

class FakeOutboxStore(
    private val nowProvider: () -> Long = { 1_700_000_000L },
) : OutboxStore {
    private var nextId = 1
    private val entriesById = linkedMapOf<String, OutboxEntry>()

    fun allEntries(): List<OutboxEntry> = entriesById.values.toList()

    override suspend fun enqueue(
        principalId: PrincipalId,
        operationType: OutboxOperationType,
        dedupeKey: String,
        payloadJson: String,
    ): OutboxEntry {
        val now = nowProvider()
        val existing = entriesById.values.firstOrNull { it.dedupeKey == dedupeKey }
        val entryId = existing?.entryId ?: "outbox-${nextId++}"
        val entry = OutboxEntry(
            entryId = entryId,
            principalId = principalId,
            operationType = operationType,
            dedupeKey = dedupeKey,
            payloadJson = payloadJson,
            status = OutboxStatus.PENDING,
            attemptCount = 0,
            nextAttemptAtEpochSeconds = now,
            lastError = null,
            createdAtEpochSeconds = existing?.createdAtEpochSeconds ?: now,
            updatedAtEpochSeconds = now,
        )
        entriesById[entryId] = entry
        return entry
    }

    override suspend fun listReadyEntries(
        nowEpochSeconds: Long,
    ): List<OutboxEntry> {
        return entriesById.values.filter { entry ->
            entry.status == OutboxStatus.PENDING &&
                entry.nextAttemptAtEpochSeconds <= nowEpochSeconds
        }
    }

    override suspend fun markInFlight(
        entryId: String,
    ) {
        val existing = entriesById[entryId] ?: return
        entriesById[entryId] = existing.copy(
            status = OutboxStatus.IN_FLIGHT,
            updatedAtEpochSeconds = nowProvider(),
        )
    }

    override suspend fun markRetryLater(
        entryId: String,
        attemptCount: Int,
        nextAttemptAtEpochSeconds: Long,
        lastError: String,
    ) {
        val existing = entriesById[entryId] ?: return
        entriesById[entryId] = existing.copy(
            status = OutboxStatus.PENDING,
            attemptCount = attemptCount,
            nextAttemptAtEpochSeconds = nextAttemptAtEpochSeconds,
            lastError = lastError,
            updatedAtEpochSeconds = nowProvider(),
        )
    }

    override suspend fun markPermanentFailure(
        entryId: String,
        attemptCount: Int,
        lastError: String,
    ) {
        val existing = entriesById[entryId] ?: return
        entriesById[entryId] = existing.copy(
            status = OutboxStatus.FAILED_PERMANENT,
            attemptCount = attemptCount,
            nextAttemptAtEpochSeconds = Long.MAX_VALUE,
            lastError = lastError,
            updatedAtEpochSeconds = nowProvider(),
        )
    }

    override suspend fun remove(
        entryId: String,
    ) {
        entriesById.remove(entryId)
    }

    override suspend fun countPending(): Int {
        return entriesById.values.count { it.status == OutboxStatus.PENDING }
    }
}

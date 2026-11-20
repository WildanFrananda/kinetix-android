package com.kinetix.shop.services.local

import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.outbox.OutboxEntry
import com.kinetix.shop.models.outbox.OutboxOperationType
import com.kinetix.shop.models.outbox.OutboxStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomOutboxStore @Inject constructor(
    private val dao: OutboxDao,
    private val clock: Clock,
) : OutboxStore {
    override suspend fun enqueue(
        principalId: PrincipalId,
        operationType: OutboxOperationType,
        dedupeKey: String,
        payloadJson: String,
    ): OutboxEntry {
        val now = clock.nowEpochSeconds()
        val existing = dao.findByDedupeKey(dedupeKey)
        val entity = OutboxEntryEntity(
            entryId = existing?.entryId ?: UUID.randomUUID().toString(),
            principalId = principalId.value,
            operationType = operationType.name,
            dedupeKey = dedupeKey,
            payloadJson = payloadJson,
            status = OutboxStatus.PENDING.name,
            attemptCount = 0,
            nextAttemptAtEpochSeconds = now,
            lastError = null,
            createdAtEpochSeconds = existing?.createdAtEpochSeconds ?: now,
            updatedAtEpochSeconds = now,
        )
        dao.upsert(entity)
        return mapToDomain(entity)
    }

    override suspend fun listReadyEntries(
        nowEpochSeconds: Long,
    ): List<OutboxEntry> {
        return dao.listReadyEntries(
            pendingStatus = OutboxStatus.PENDING.name,
            nowEpochSeconds = nowEpochSeconds,
        ).map { entity ->
            mapToDomain(entity)
        }
    }

    override suspend fun markInFlight(
        entryId: String,
    ) {
        dao.updateStatus(
            entryId = entryId,
            status = OutboxStatus.IN_FLIGHT.name,
            updatedAtEpochSeconds = clock.nowEpochSeconds(),
        )
    }

    override suspend fun markRetryLater(
        entryId: String,
        attemptCount: Int,
        nextAttemptAtEpochSeconds: Long,
        lastError: String,
    ) {
        dao.updateRetryState(
            entryId = entryId,
            status = OutboxStatus.PENDING.name,
            attemptCount = attemptCount,
            nextAttemptAtEpochSeconds = nextAttemptAtEpochSeconds,
            lastError = lastError,
            updatedAtEpochSeconds = clock.nowEpochSeconds(),
        )
    }

    override suspend fun markPermanentFailure(
        entryId: String,
        attemptCount: Int,
        lastError: String,
    ) {
        dao.updateRetryState(
            entryId = entryId,
            status = OutboxStatus.FAILED_PERMANENT.name,
            attemptCount = attemptCount,
            nextAttemptAtEpochSeconds = Long.MAX_VALUE,
            lastError = lastError,
            updatedAtEpochSeconds = clock.nowEpochSeconds(),
        )
    }

    override suspend fun remove(
        entryId: String,
    ) {
        dao.deleteById(entryId)
    }

    override suspend fun countPending(): Int {
        return dao.countByStatus(OutboxStatus.PENDING.name)
    }

    private fun mapToDomain(
        entity: OutboxEntryEntity,
    ): OutboxEntry {
        val operationType = OutboxOperationType.entries.firstOrNull {
            it.name == entity.operationType
        } ?: OutboxOperationType.PROFILE_UPDATE
        val status = OutboxStatus.entries.firstOrNull {
            it.name == entity.status
        } ?: OutboxStatus.PENDING
        return OutboxEntry(
            entryId = entity.entryId,
            principalId = PrincipalId(entity.principalId),
            operationType = operationType,
            dedupeKey = entity.dedupeKey,
            payloadJson = entity.payloadJson,
            status = status,
            attemptCount = entity.attemptCount,
            nextAttemptAtEpochSeconds = entity.nextAttemptAtEpochSeconds,
            lastError = entity.lastError,
            createdAtEpochSeconds = entity.createdAtEpochSeconds,
            updatedAtEpochSeconds = entity.updatedAtEpochSeconds,
        )
    }
}

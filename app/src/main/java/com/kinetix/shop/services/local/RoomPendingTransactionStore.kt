package com.kinetix.shop.services.local

import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.models.ids.IdempotencyKey
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.TopUpReference
import com.kinetix.shop.models.transaction.PendingTransactionOperationType
import com.kinetix.shop.models.transaction.PendingTransactionState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPendingTransactionStore @Inject constructor(
    private val dao: PendingTransactionStateDao,
    private val clock: Clock,
) : PendingTransactionStore {
    override suspend fun getOrCreateKey(
        principalId: PrincipalId,
        operationType: PendingTransactionOperationType,
        payloadFingerprint: String,
    ): PendingTransactionState {
        val key = buildStateKey(principalId, operationType)
        val existing = dao.findByKey(key)
        if (existing != null && existing.payloadFingerprint == payloadFingerprint) {
            return mapToDomain(existing)
        }
        val now = clock.nowEpochSeconds()
        val created = PendingTransactionStateEntity(
            stateKey = key,
            principalId = principalId.value,
            operationType = operationType.name,
            idempotencyKey = UUID.randomUUID().toString(),
            referenceNumber = null,
            payloadFingerprint = payloadFingerprint,
            createdAtEpochSeconds = now,
            updatedAtEpochSeconds = now,
        )
        dao.upsert(created)
        return mapToDomain(created)
    }

    override suspend fun attachReferenceNumber(
        stateKey: String,
        referenceNumber: TopUpReference,
    ) {
        val existing = dao.findByKey(stateKey) ?: return
        dao.upsert(
            existing.copy(
                referenceNumber = referenceNumber.value,
                updatedAtEpochSeconds = clock.nowEpochSeconds(),
            )
        )
    }

    override suspend fun find(
        principalId: PrincipalId,
        operationType: PendingTransactionOperationType,
    ): PendingTransactionState? {
        val entity = dao.findByKey(buildStateKey(principalId, operationType)) ?: return null
        return mapToDomain(entity)
    }

    override suspend fun clear(
        principalId: PrincipalId,
        operationType: PendingTransactionOperationType,
    ) {
        dao.deleteByKey(buildStateKey(principalId, operationType))
    }

    override suspend fun clearAllForPrincipal(
        principalId: PrincipalId,
    ) {
        dao.deleteByPrincipalId(principalId.value)
    }

    private fun buildStateKey(
        principalId: PrincipalId,
        operationType: PendingTransactionOperationType,
    ): String {
        return "${operationType.name}:${principalId.value}"
    }

    private fun mapToDomain(
        entity: PendingTransactionStateEntity,
    ): PendingTransactionState {
        val operation = PendingTransactionOperationType.entries.firstOrNull {
            it.name == entity.operationType
        } ?: PendingTransactionOperationType.CHECKOUT
        return PendingTransactionState(
            stateKey = entity.stateKey,
            principalId = PrincipalId(entity.principalId),
            operationType = operation,
            idempotencyKey = IdempotencyKey(entity.idempotencyKey),
            referenceNumber = entity.referenceNumber?.let { TopUpReference(it) },
            payloadFingerprint = entity.payloadFingerprint,
            createdAtEpochSeconds = entity.createdAtEpochSeconds,
            updatedAtEpochSeconds = entity.updatedAtEpochSeconds,
        )
    }
}

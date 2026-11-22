package com.kinetix.shop.services.local

import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.outbox.OutboxEntry
import com.kinetix.shop.models.outbox.OutboxOperationType
import com.kinetix.shop.models.outbox.OutboxStatus
import com.kinetix.shop.services.telemetry.CrashReporter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Singleton
class RoomOutboxStore @Inject constructor(
    private val dao: OutboxDao,
    private val clock: Clock,
    private val json: Json,
    private val crashReporter: CrashReporter,
) : OutboxStore {
    private val mutex = Mutex()

    override suspend fun enqueue(
        principalId: PrincipalId,
        operationType: OutboxOperationType,
        dedupeKey: String,
        payloadJson: String,
    ): OutboxEntry {
        return mutex.withLock {
            val now = clock.nowEpochSeconds()
            val existing = dao.findByDedupeKey(dedupeKey)
            val mergedPayload = if (existing != null && operationType == OutboxOperationType.PROFILE_UPDATE) {
                mergeJsonPayloads(existing.payloadJson, payloadJson)
            } else {
                payloadJson
            }
            val nextRevision = if (existing != null) {
                maxOf(now, existing.updatedAtEpochSeconds + 1L)
            } else {
                now
            }
            val entity = OutboxEntryEntity(
                entryId = existing?.entryId ?: UUID.randomUUID().toString(),
                principalId = principalId.value,
                operationType = operationType.name,
                dedupeKey = dedupeKey,
                payloadJson = mergedPayload,
                status = OutboxStatus.PENDING.name,
                attemptCount = 0,
                nextAttemptAtEpochSeconds = now,
                lastError = null,
                createdAtEpochSeconds = existing?.createdAtEpochSeconds ?: now,
                updatedAtEpochSeconds = nextRevision,
            )
            dao.upsert(entity)
            OutboxEntry(
                entryId = entity.entryId,
                principalId = principalId,
                operationType = operationType,
                dedupeKey = entity.dedupeKey,
                payloadJson = entity.payloadJson,
                status = OutboxStatus.PENDING,
                attemptCount = entity.attemptCount,
                nextAttemptAtEpochSeconds = entity.nextAttemptAtEpochSeconds,
                lastError = entity.lastError,
                createdAtEpochSeconds = entity.createdAtEpochSeconds,
                updatedAtEpochSeconds = entity.updatedAtEpochSeconds,
            )
        }
    }

    override suspend fun reconcileConfirmedFields(
        dedupeKey: String,
        confirmedPayloadJson: String,
    ) {
        mutex.withLock {
            val existing = dao.findByDedupeKey(dedupeKey) ?: return@withLock
            val remainingJson = subtractConfirmedFields(existing.payloadJson, confirmedPayloadJson)
            if (remainingJson == null) {
                dao.deleteById(existing.entryId)
            } else if (remainingJson != existing.payloadJson) {
                val now = clock.nowEpochSeconds()
                dao.upsert(
                    existing.copy(
                        payloadJson = remainingJson,
                        status = OutboxStatus.PENDING.name,
                        updatedAtEpochSeconds = maxOf(now, existing.updatedAtEpochSeconds + 1L),
                    )
                )
            }
        }
    }

    override suspend fun recoverInFlightEntries() {
        mutex.withLock {
            dao.resetInFlightToPending(
                inFlightStatus = OutboxStatus.IN_FLIGHT.name,
                pendingStatus = OutboxStatus.PENDING.name,
            )
        }
    }

    override suspend fun listReadyEntries(
        nowEpochSeconds: Long,
    ): List<OutboxEntry> {
        return mutex.withLock {
            val entities = dao.listReadyEntries(
                pendingStatus = OutboxStatus.PENDING.name,
                nowEpochSeconds = nowEpochSeconds,
            )
            val validEntries = mutableListOf<OutboxEntry>()
            for (entity in entities) {
                val operationType = OutboxOperationType.entries.firstOrNull {
                    it.name == entity.operationType
                }
                val status = OutboxStatus.entries.firstOrNull {
                    it.name == entity.status
                }
                if (operationType == null || status == null) {
                    dao.updateRetryState(
                        entryId = entity.entryId,
                        status = OutboxStatus.FAILED_PERMANENT.name,
                        attemptCount = entity.attemptCount,
                        nextAttemptAtEpochSeconds = Long.MAX_VALUE,
                        lastError = "UNRECOGNIZED_OUTBOX_ENTRY:${entity.operationType}:${entity.status}",
                        updatedAtEpochSeconds = clock.nowEpochSeconds(),
                    )
                    crashReporter.recordNonFatal(
                        throwable = IllegalStateException("Unrecognized outbox entry ${entity.operationType}/${entity.status}"),
                        contextMap = mapOf(
                            "outbox_entry_id" to entity.entryId,
                            "outbox_operation" to entity.operationType,
                            "outbox_status" to entity.status,
                        ),
                    )
                    continue
                }

                validEntries.add(
                    OutboxEntry(
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
                )
            }
            validEntries
        }
    }

    override suspend fun markInFlight(
        entryId: String,
        expectedUpdatedAtEpochSeconds: Long,
    ): Boolean {
        return mutex.withLock {
            val existing = dao.findById(entryId) ?: return@withLock false
            if (expectedUpdatedAtEpochSeconds >= 0L && existing.updatedAtEpochSeconds != expectedUpdatedAtEpochSeconds) {
                return@withLock false
            }
            dao.updateStatus(
                entryId = entryId,
                status = OutboxStatus.IN_FLIGHT.name,
                updatedAtEpochSeconds = existing.updatedAtEpochSeconds,
            )
            true
        }
    }

    override suspend fun markRetryLater(
        entryId: String,
        attemptCount: Int,
        nextAttemptAtEpochSeconds: Long,
        lastError: String,
        expectedUpdatedAtEpochSeconds: Long,
    ) {
        mutex.withLock {
            val existing = dao.findById(entryId) ?: return@withLock
            if (expectedUpdatedAtEpochSeconds >= 0L && existing.updatedAtEpochSeconds != expectedUpdatedAtEpochSeconds) {
                return@withLock
            }
            dao.updateRetryState(
                entryId = entryId,
                status = OutboxStatus.PENDING.name,
                attemptCount = attemptCount,
                nextAttemptAtEpochSeconds = nextAttemptAtEpochSeconds,
                lastError = lastError,
                updatedAtEpochSeconds = maxOf(clock.nowEpochSeconds(), existing.updatedAtEpochSeconds + 1L),
            )
        }
    }

    override suspend fun markPermanentFailure(
        entryId: String,
        attemptCount: Int,
        lastError: String,
        expectedUpdatedAtEpochSeconds: Long,
    ): Boolean {
        return mutex.withLock {
            val existing = dao.findById(entryId) ?: return@withLock false
            if (expectedUpdatedAtEpochSeconds >= 0L && existing.updatedAtEpochSeconds != expectedUpdatedAtEpochSeconds) {
                return@withLock false
            }
            dao.updateRetryState(
                entryId = entryId,
                status = OutboxStatus.FAILED_PERMANENT.name,
                attemptCount = attemptCount,
                nextAttemptAtEpochSeconds = Long.MAX_VALUE,
                lastError = lastError,
                updatedAtEpochSeconds = maxOf(clock.nowEpochSeconds(), existing.updatedAtEpochSeconds + 1L),
            )
            true
        }
    }

    override suspend fun remove(
        entryId: String,
        expectedUpdatedAtEpochSeconds: Long,
        consumedPayloadJson: String?,
    ) {
        mutex.withLock {
            val existing = dao.findById(entryId) ?: return@withLock
            if (expectedUpdatedAtEpochSeconds < 0L || existing.updatedAtEpochSeconds == expectedUpdatedAtEpochSeconds) {
                dao.deleteById(entryId)
                return@withLock
            }

            if (consumedPayloadJson != null) {
                val remaining = subtractConfirmedFields(existing.payloadJson, consumedPayloadJson)
                if (remaining == null) {
                    dao.deleteById(entryId)
                } else {
                    dao.upsert(
                        existing.copy(
                            payloadJson = remaining,
                            status = OutboxStatus.PENDING.name,
                            updatedAtEpochSeconds = maxOf(clock.nowEpochSeconds(), existing.updatedAtEpochSeconds + 1L),
                        )
                    )
                }
            }
        }
    }

    override suspend fun countPending(): Int {
        return mutex.withLock {
            dao.countByStatus(OutboxStatus.PENDING.name)
        }
    }

    private fun mergeJsonPayloads(
        existingJson: String,
        incomingJson: String,
    ): String {
        return try {
            val existingObj = json.parseToJsonElement(existingJson).jsonObject
            val incomingObj = json.parseToJsonElement(incomingJson).jsonObject
            val merged = linkedMapOf<String, kotlinx.serialization.json.JsonElement>()
            merged.putAll(existingObj)
            merged.putAll(incomingObj)
            JsonObject(merged).toString()
        } catch (_: Exception) {
            incomingJson
        }
    }

    private fun subtractConfirmedFields(
        pendingJson: String,
        confirmedJson: String,
    ): String? {
        return try {
            val pendingObj = json.parseToJsonElement(pendingJson).jsonObject
            val confirmedObj = json.parseToJsonElement(confirmedJson).jsonObject
            val remaining = linkedMapOf<String, kotlinx.serialization.json.JsonElement>()
            for ((key, pendingValue) in pendingObj) {
                if (!confirmedObj.containsKey(key) || confirmedObj[key] != pendingValue) {
                    remaining[key] = pendingValue
                }
            }
            if (remaining.isEmpty()) {
                null
            } else {
                JsonObject(remaining).toString()
            }
        } catch (_: Exception) {
            pendingJson
        }
    }
}

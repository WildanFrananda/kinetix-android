package com.kinetix.shop.fakes

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.outbox.OutboxEntry
import com.kinetix.shop.models.outbox.OutboxOperationType
import com.kinetix.shop.models.outbox.OutboxStatus
import com.kinetix.shop.services.local.OutboxStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class FakeOutboxStore(
    private val nowProvider: () -> Long = { 1_700_000_000L },
) : OutboxStore {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
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
        val mergedPayloadJson = if (existing == null || existing.principalId != principalId) {
            payloadJson
        } else {
            mergePayloadObjects(existing.payloadJson, payloadJson)
        }
        val entry = OutboxEntry(
            entryId = entryId,
            principalId = principalId,
            operationType = operationType,
            dedupeKey = dedupeKey,
            payloadJson = mergedPayloadJson,
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

    override suspend fun reconcileConfirmedFields(
        dedupeKey: String,
        confirmedPayloadJson: String,
    ) {
        val existing = entriesById.values.firstOrNull { it.dedupeKey == dedupeKey } ?: return
        val existingObj = try {
            json.parseToJsonElement(existing.payloadJson).jsonObject
        } catch (_: Exception) {
            entriesById.remove(existing.entryId)
            return
        }
        val confirmedObj = try {
            json.parseToJsonElement(confirmedPayloadJson).jsonObject
        } catch (_: Exception) {
            return
        }
        val remaining = existingObj.filterKeys { key -> !confirmedObj.containsKey(key) }
        if (remaining.isEmpty()) {
            entriesById.remove(existing.entryId)
        } else {
            entriesById[existing.entryId] = existing.copy(
                payloadJson = JsonObject(remaining).toString(),
                status = OutboxStatus.PENDING,
                updatedAtEpochSeconds = nowProvider(),
            )
        }
    }

    override suspend fun recoverInFlightEntries() {
        val now = nowProvider()
        for ((id, entry) in entriesById.entries.toList()) {
            if (entry.status == OutboxStatus.IN_FLIGHT) {
                entriesById[id] = entry.copy(
                    status = OutboxStatus.PENDING,
                    updatedAtEpochSeconds = now,
                )
            }
        }
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
        expectedUpdatedAtEpochSeconds: Long,
    ): Boolean {
        val existing = entriesById[entryId] ?: return false
        if (expectedUpdatedAtEpochSeconds >= 0L && existing.updatedAtEpochSeconds != expectedUpdatedAtEpochSeconds) {
            return false
        }
        entriesById[entryId] = existing.copy(
            status = OutboxStatus.IN_FLIGHT,
        )
        return true
    }

    override suspend fun markRetryLater(
        entryId: String,
        attemptCount: Int,
        nextAttemptAtEpochSeconds: Long,
        lastError: String,
        expectedUpdatedAtEpochSeconds: Long,
    ) {
        val existing = entriesById[entryId] ?: return
        if (expectedUpdatedAtEpochSeconds >= 0L && existing.updatedAtEpochSeconds != expectedUpdatedAtEpochSeconds) {
            return
        }
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
        expectedUpdatedAtEpochSeconds: Long,
    ): Boolean {
        val existing = entriesById[entryId] ?: return false
        if (expectedUpdatedAtEpochSeconds >= 0L && existing.updatedAtEpochSeconds != expectedUpdatedAtEpochSeconds) {
            return false
        }
        entriesById[entryId] = existing.copy(
            status = OutboxStatus.FAILED_PERMANENT,
            attemptCount = attemptCount,
            nextAttemptAtEpochSeconds = Long.MAX_VALUE,
            lastError = lastError,
            updatedAtEpochSeconds = nowProvider(),
        )
        return true
    }

    override suspend fun remove(
        entryId: String,
        expectedUpdatedAtEpochSeconds: Long,
        consumedPayloadJson: String?,
    ) {
        val existing = entriesById[entryId] ?: return
        if (expectedUpdatedAtEpochSeconds < 0L || existing.updatedAtEpochSeconds == expectedUpdatedAtEpochSeconds) {
            entriesById.remove(entryId)
        }
    }

    override suspend fun countPending(): Int {
        return entriesById.values.count { it.status == OutboxStatus.PENDING }
    }

    private fun mergePayloadObjects(
        existingPayloadJson: String,
        incomingPayloadJson: String,
    ): String {
        return try {
            val existingObj = json.parseToJsonElement(existingPayloadJson).jsonObject
            val incomingObj = json.parseToJsonElement(incomingPayloadJson).jsonObject
            val merged = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>()
            merged.putAll(existingObj)
            merged.putAll(incomingObj)
            JsonObject(merged).toString()
        } catch (_: Exception) {
            incomingPayloadJson
        }
    }
}

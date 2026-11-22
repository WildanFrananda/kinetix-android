package com.kinetix.shop.services.outbox

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.RateLimitedFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.outbox.OutboxEntry
import com.kinetix.shop.models.outbox.OutboxFlushOutcome
import com.kinetix.shop.models.outbox.OutboxOperationType
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.services.config.FeatureFlagProvider
import com.kinetix.shop.services.local.OutboxStore
import com.kinetix.shop.services.local.ProfileCacheStore
import com.kinetix.shop.services.remote.IdentityApiService
import com.kinetix.shop.services.session.SessionExecutionGateway
import com.kinetix.shop.services.telemetry.AnalyticsTracker
import com.kinetix.shop.services.telemetry.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Singleton
class OutboxProcessor @Inject constructor(
    private val outboxStore: OutboxStore,
    private val sessionGateway: SessionExecutionGateway,
    private val identityApiService: IdentityApiService,
    private val safeApiCaller: SafeApiCaller,
    private val profileCacheStore: ProfileCacheStore,
    private val outboxSyncScheduler: OutboxSyncScheduler,
    private val featureFlagProvider: FeatureFlagProvider,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
    private val json: Json,
    private val clock: Clock,
) {
    suspend fun flushPendingEntries(): OutboxFlushOutcome {
        if (!featureFlagProvider.isOutboxSyncEnabled()) {
            return OutboxFlushOutcome.COMPLETED
        }
        outboxStore.recoverInFlightEntries()

        val activeSession = sessionGateway.currentSession.value ?: when (val restored = sessionGateway.restoreSession()) {
            is Ok -> restored.value ?: return OutboxFlushOutcome.COMPLETED
            is Err -> return OutboxFlushOutcome.RETRY_LATER
        }

        val now = clock.nowEpochSeconds()
        val entries = outboxStore.listReadyEntries(now)
        var shouldRetry = false

        for (entry in entries) {
            if (entry.principalId != activeSession.principalId) {
                continue
            }
            val expectedRevision = entry.updatedAtEpochSeconds
            val claimed = outboxStore.markInFlight(
                entryId = entry.entryId,
                expectedUpdatedAtEpochSeconds = expectedRevision,
            )
            if (!claimed) {
                continue
            }
            when (val result = executeEntry(entry)) {
                is Ok -> {
                    outboxStore.remove(
                        entryId = entry.entryId,
                        expectedUpdatedAtEpochSeconds = expectedRevision,
                    )
                    analyticsTracker.logEvent(
                        name = "outbox_entry_synced",
                        params = mapOf("operation" to entry.operationType.name),
                    )
                }

                is Err -> {
                    val nextAttempt = entry.attemptCount + 1
                    val errorCode = result.failure::class.simpleName ?: "Failure"
                    if (isRetryable(result.failure)) {
                        val backoffSeconds = computeBackoffSeconds(nextAttempt, result.failure)
                        outboxStore.markRetryLater(
                            entryId = entry.entryId,
                            expectedUpdatedAtEpochSeconds = expectedRevision,
                            attemptCount = nextAttempt,
                            nextAttemptAtEpochSeconds = clock.nowEpochSeconds() + backoffSeconds,
                            lastError = errorCode,
                        )
                        shouldRetry = true
                    } else {
                        val markedPermanent = outboxStore.markPermanentFailure(
                            entryId = entry.entryId,
                            expectedUpdatedAtEpochSeconds = expectedRevision,
                            attemptCount = nextAttempt,
                            lastError = errorCode,
                        )
                        if (markedPermanent) {
                            try {
                                profileCacheStore.withdrawOptimisticProfile(activeSession.accountId)
                            } catch (_: Exception) {
                            }
                            crashReporter.recordNonFatal(
                                throwable = IllegalStateException("Outbox permanent failure for ${entry.operationType.name}: $errorCode"),
                                contextMap = mapOf(
                                    "outbox_entry_id" to entry.entryId,
                                    "outbox_operation" to entry.operationType.name,
                                ),
                            )
                        }
                    }
                }
            }
        }

        if (shouldRetry || outboxStore.countPending() > 0) {
            outboxSyncScheduler.scheduleSync()
        }

        return if (shouldRetry) {
            OutboxFlushOutcome.RETRY_LATER
        } else {
            OutboxFlushOutcome.COMPLETED
        }
    }

    private suspend fun executeEntry(
        entry: OutboxEntry,
    ): Result<UnitSentinel> {
        return when (entry.operationType) {
            OutboxOperationType.PROFILE_UPDATE -> executeProfileUpdate(entry)
        }
    }

    private suspend fun executeProfileUpdate(
        entry: OutboxEntry,
    ): Result<UnitSentinel> {
        val payloadObject = try {
            json.parseToJsonElement(entry.payloadJson).jsonObject
        } catch (_: Exception) {
            return Err(UnreadableAnswerFailure())
        }

        return sessionGateway.withValidSession { session ->
            if (session.principalId != entry.principalId) {
                return@withValidSession Err(
                    UnauthorizedFailure(
                        code = "PRINCIPAL_MISMATCH",
                    )
                )
            }
            safeApiCaller.execute(
                call = {
                    identityApiService.updateProfile(
                        accountId = session.accountId.value,
                        bearerToken = "Bearer ${session.accessToken}",
                        body = payloadObject,
                    )
                },
                transform = { dto ->
                    try {
                        profileCacheStore.saveProfile(
                            DeliveryProfile(
                                id = dto.id,
                                accountId = AccountId(dto.userId),
                                fullName = dto.fullName,
                                phoneNumber = dto.phoneNumber,
                                streetAddress = dto.streetAddress,
                                city = dto.city,
                                postalCode = dto.postalCode,
                                avatarUrl = dto.avatarUrl,
                                latitude = dto.latitude,
                                longitude = dto.longitude,
                                geocodedAt = dto.geocodedAt,
                            )
                        )
                    } catch (_: Exception) {
                    }
                    Ok(UnitSentinel)
                },
            )
        }
    }

    private fun isRetryable(
        failure: Failure,
    ): Boolean {
        return when (failure) {
            is NetworkFailure,
            is TimeoutFailure,
            is UnavailableFailure,
            is UnknownOutcomeFailure,
            is RateLimitedFailure,
            is SignInDelayedFailure,
            is UnauthorizedFailure -> true

            else -> false
        }
    }

    private fun computeBackoffSeconds(
        attemptCount: Int,
        failure: Failure,
    ): Long {
        val retryAfter = when (failure) {
            is RateLimitedFailure -> failure.retryAfterSeconds?.toLong()
            is SignInDelayedFailure -> failure.retryAfterSeconds.toLong()
            else -> null
        }
        val shifted = 1L shl (attemptCount - 1).coerceIn(0, 6)
        val exponential = (BASE_BACKOFF_SECONDS * shifted).coerceAtMost(MAX_BACKOFF_SECONDS)
        return maxOf(exponential, retryAfter ?: 0L)
    }

    companion object {
        const val BASE_BACKOFF_SECONDS = 15L
        const val MAX_BACKOFF_SECONDS = 300L
    }
}

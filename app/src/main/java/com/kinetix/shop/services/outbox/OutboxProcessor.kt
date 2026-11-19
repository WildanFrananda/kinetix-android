package com.kinetix.shop.services.outbox

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.RateLimitedFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
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
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.services.config.FeatureFlagProvider
import com.kinetix.shop.services.local.OutboxStore
import com.kinetix.shop.services.local.ProfileCacheStore
import com.kinetix.shop.services.remote.IdentityApiService
import com.kinetix.shop.services.telemetry.AnalyticsTracker
import com.kinetix.shop.services.telemetry.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Singleton
class OutboxProcessor @Inject constructor(
    private val outboxStore: OutboxStore,
    private val sessionRepository: SessionRepository,
    private val identityApiService: IdentityApiService,
    private val safeApiCaller: SafeApiCaller,
    private val profileCacheStore: ProfileCacheStore,
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
        val activeSession = sessionRepository.currentSession.value ?: return OutboxFlushOutcome.COMPLETED
        val now = clock.nowEpochSeconds()
        val entries = outboxStore.listReadyEntries(now)
        var shouldRetry = false

        for (entry in entries) {
            if (entry.principalId != activeSession.principalId) {
                continue
            }
            outboxStore.markInFlight(entry.entryId)
            when (val result = executeEntry(entry)) {
                is Ok -> {
                    outboxStore.remove(entry.entryId)
                    analyticsTracker.logEvent(
                        name = "outbox_entry_synced",
                        params = mapOf("operation" to entry.operationType.name),
                    )
                }

                is Err -> {
                    val nextAttempt = entry.attemptCount + 1
                    val errorCode = result.failure::class.simpleName ?: "Failure"
                    if (isRetryable(result.failure) && nextAttempt <= MAX_ATTEMPTS) {
                        val backoffSeconds = computeBackoffSeconds(nextAttempt, result.failure)
                        outboxStore.markRetryLater(
                            entryId = entry.entryId,
                            attemptCount = nextAttempt,
                            nextAttemptAtEpochSeconds = clock.nowEpochSeconds() + backoffSeconds,
                            lastError = errorCode,
                        )
                        shouldRetry = true
                    } else {
                        outboxStore.markPermanentFailure(
                            entryId = entry.entryId,
                            attemptCount = nextAttempt,
                            lastError = errorCode,
                        )
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
            OutboxOperationType.PRODUCT_REVIEW_SUBMIT,
            OutboxOperationType.DRIVER_REVIEW_SUBMIT,
            OutboxOperationType.CART_ITEM_UPSERT,
            OutboxOperationType.CART_ITEM_REMOVE,
            OutboxOperationType.CART_VOUCHER_APPLY -> Err(UnreadableAnswerFailure(""))
        }
    }

    private suspend fun executeProfileUpdate(
        entry: OutboxEntry,
    ): Result<UnitSentinel> {
        val payloadObject = try {
            json.parseToJsonElement(entry.payloadJson).jsonObject
        } catch (_: Exception) {
            return Err(UnreadableAnswerFailure(""))
        }

        return sessionRepository.withValidSession { session ->
            safeApiCaller.execute(
                call = {
                    identityApiService.updateProfile(
                        accountId = session.accountId.value,
                        bearerToken = "Bearer ${session.accessToken}",
                        body = payloadObject,
                    )
                },
                transform = { dto ->
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
        const val MAX_ATTEMPTS = 5
        const val BASE_BACKOFF_SECONDS = 15L
        const val MAX_BACKOFF_SECONDS = 300L
    }
}

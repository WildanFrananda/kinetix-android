package com.kinetix.shop.services.outbox

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.profile.UserProfileResponseDto
import com.kinetix.shop.fakes.FakeAnalyticsTracker
import com.kinetix.shop.fakes.FakeClock
import com.kinetix.shop.fakes.FakeCrashReporter
import com.kinetix.shop.fakes.FakeFeatureFlagProvider
import com.kinetix.shop.fakes.FakeIdentityApiService
import com.kinetix.shop.fakes.FakeOutboxStore
import com.kinetix.shop.fakes.FakeOutboxSyncScheduler
import com.kinetix.shop.fakes.FakeProfileCacheStore
import com.kinetix.shop.fakes.FakeSessionRepository
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.outbox.OutboxFlushOutcome
import com.kinetix.shop.models.outbox.OutboxOperationType
import com.kinetix.shop.models.outbox.OutboxStatus
import com.kinetix.shop.models.profile.DeliveryProfile
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OutboxProcessorTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(ApiErrorParser(json)))
    private val activeSession = StoredSession(
        accessToken = "acc-1",
        refreshToken = "ref-1",
        expiresAtEpochSeconds = 1_700_000_900L,
        accountId = AccountId(22L),
        principalId = PrincipalId("usr-22"),
        email = "buyer@kinetix.test",
    )

    @Test
    fun flushesPendingProfileUpdateAndRemovesOutboxEntryOnSuccess() = runTest {
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val outboxStore = FakeOutboxStore(nowProvider = { clock.nowEpochSeconds() })
        outboxStore.enqueue(
            principalId = activeSession.principalId,
            operationType = OutboxOperationType.PROFILE_UPDATE,
            dedupeKey = "profile:22",
            payloadJson = """{"streetAddress":"Jl. Merdeka No. 1"}""",
        )
        val profileCacheStore = FakeProfileCacheStore()
        val analyticsTracker = FakeAnalyticsTracker()
        val apiService = FakeIdentityApiService().apply {
            updateProfileHandler = { _, _, _ ->
                Response.success(
                    UserProfileResponseDto(
                        id = 1L,
                        userId = 22L,
                        fullName = "Budi Santoso",
                        phoneNumber = "+628123456789",
                        streetAddress = "Jl. Merdeka No. 1",
                        city = "Jakarta",
                        postalCode = "10110",
                        avatarUrl = null,
                        latitude = -6.17,
                        longitude = 106.82,
                        geocodedAt = "2026-11-19T07:00:00Z",
                    )
                )
            }
        }
        val processor = OutboxProcessor(
            outboxStore = outboxStore,
            sessionGateway = FakeSessionRepository(initialSession = activeSession),
            identityApiService = apiService,
            safeApiCaller = safeApiCaller,
            profileCacheStore = profileCacheStore,
            outboxSyncScheduler = FakeOutboxSyncScheduler(),
            featureFlagProvider = FakeFeatureFlagProvider(),
            crashReporter = FakeCrashReporter(),
            analyticsTracker = analyticsTracker,
            json = json,
            clock = clock,
        )

        val outcome = processor.flushPendingEntries()

        assertEquals(OutboxFlushOutcome.COMPLETED, outcome)
        assertTrue(outboxStore.allEntries().isEmpty())
        assertEquals("Jl. Merdeka No. 1", profileCacheStore.getProfile(AccountId(22L))?.streetAddress)
        assertEquals("outbox_entry_synced", analyticsTracker.events.single().first)
    }

    @Test
    fun schedulesExponentialBackoffOnTransientNetworkFailureWithoutAttemptCapAndWithdrawsOptimisticProfileOn400() = runTest {
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val outboxStore = FakeOutboxStore(nowProvider = { clock.nowEpochSeconds() })
        outboxStore.enqueue(
            principalId = activeSession.principalId,
            operationType = OutboxOperationType.PROFILE_UPDATE,
            dedupeKey = "profile:22",
            payloadJson = """{"streetAddress":"Jl. Merdeka No. 1"}""",
        )
        val profileCacheStore = FakeProfileCacheStore()
        val confirmedProfile = DeliveryProfile(
            id = 1L,
            accountId = AccountId(22L),
            fullName = "Budi Santoso",
            phoneNumber = "+628123456789",
            streetAddress = "Jl. Lama No. 10",
            city = "Jakarta",
            postalCode = "10110",
            avatarUrl = null,
            latitude = -6.17,
            longitude = 106.82,
            geocodedAt = "2026-11-19T07:00:00Z",
        )
        profileCacheStore.saveProfile(confirmedProfile)
        profileCacheStore.saveOptimisticProfile(
            confirmedProfile.copy(
                streetAddress = "Jl. Merdeka No. 1",
                isConfirmed = false,
            )
        )
        var return400 = false
        val apiService = FakeIdentityApiService().apply {
            updateProfileHandler = { _, _, _ ->
                if (!return400) {
                    throw IOException("Connection reset")
                }
                Response.error(
                    400,
                    """{"message":"Invalid postal code"}""".toResponseBody("application/json".toMediaType()),
                )
            }
        }
        val crashReporter = FakeCrashReporter()
        val syncScheduler = FakeOutboxSyncScheduler()
        val processor = OutboxProcessor(
            outboxStore = outboxStore,
            sessionGateway = FakeSessionRepository(initialSession = activeSession),
            identityApiService = apiService,
            safeApiCaller = safeApiCaller,
            profileCacheStore = profileCacheStore,
            outboxSyncScheduler = syncScheduler,
            featureFlagProvider = FakeFeatureFlagProvider(),
            crashReporter = crashReporter,
            analyticsTracker = FakeAnalyticsTracker(),
            json = json,
            clock = clock,
        )

        repeat(6) { attemptIndex ->
            val outcome = processor.flushPendingEntries()
            assertEquals(OutboxFlushOutcome.RETRY_LATER, outcome)
            val retriedEntry = outboxStore.allEntries().single()
            assertEquals(OutboxStatus.PENDING, retriedEntry.status)
            assertEquals(attemptIndex + 1, retriedEntry.attemptCount)
            clock.currentEpochSeconds = retriedEntry.nextAttemptAtEpochSeconds
        }
        assertTrue(syncScheduler.scheduleCount >= 6)

        return400 = true
        val permanentOutcome = processor.flushPendingEntries()
        assertEquals(OutboxFlushOutcome.COMPLETED, permanentOutcome)
        val failedEntry = outboxStore.allEntries().single()
        assertEquals(OutboxStatus.FAILED_PERMANENT, failedEntry.status)
        assertEquals(1, crashReporter.nonFatalExceptions.size)
        assertEquals("Jl. Lama No. 10", profileCacheStore.getProfile(AccountId(22L))?.streetAddress)
        assertEquals(true, profileCacheStore.getProfile(AccountId(22L))?.isConfirmed)
    }

    @Test
    fun recoversInFlightEntriesAndRestoresSessionBeforeFlushingInBackground() = runTest {
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val outboxStore = FakeOutboxStore(nowProvider = { clock.nowEpochSeconds() })
        val entry = outboxStore.enqueue(
            principalId = activeSession.principalId,
            operationType = OutboxOperationType.PROFILE_UPDATE,
            dedupeKey = "profile:22",
            payloadJson = """{"city":"Surabaya"}""",
        )
        outboxStore.markInFlight(entry.entryId, entry.updatedAtEpochSeconds)

        val sessionRepository = FakeSessionRepository(initialSession = null).apply {
            restoreResult = Ok(activeSession)
        }
        val apiService = FakeIdentityApiService().apply {
            updateProfileHandler = { _, _, _ ->
                Response.success(
                    UserProfileResponseDto(
                        id = 1L,
                        userId = 22L,
                        fullName = "Budi Santoso",
                        phoneNumber = "+628123456789",
                        streetAddress = "Jl. Merdeka No. 1",
                        city = "Surabaya",
                        postalCode = "60111",
                        avatarUrl = null,
                        latitude = null,
                        longitude = null,
                        geocodedAt = null,
                    )
                )
            }
        }
        val processor = OutboxProcessor(
            outboxStore = outboxStore,
            sessionGateway = sessionRepository,
            identityApiService = apiService,
            safeApiCaller = safeApiCaller,
            profileCacheStore = FakeProfileCacheStore(),
            outboxSyncScheduler = FakeOutboxSyncScheduler(),
            featureFlagProvider = FakeFeatureFlagProvider(),
            crashReporter = FakeCrashReporter(),
            analyticsTracker = FakeAnalyticsTracker(),
            json = json,
            clock = clock,
        )

        val outcome = processor.flushPendingEntries()

        assertEquals(OutboxFlushOutcome.COMPLETED, outcome)
        assertTrue(outboxStore.allEntries().isEmpty())
    }
}

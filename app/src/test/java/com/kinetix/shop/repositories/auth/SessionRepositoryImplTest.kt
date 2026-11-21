package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.failure.StorageFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.auth.CurrentSessionResponseDto
import com.kinetix.shop.fakes.FakeClock
import com.kinetix.shop.fakes.FakeCrashReporter
import com.kinetix.shop.fakes.FakeIdentityApiService
import com.kinetix.shop.fakes.FakeSessionRenewer
import com.kinetix.shop.fakes.FakeTokenStore
import com.kinetix.shop.models.auth.SessionRestoring
import com.kinetix.shop.models.auth.SessionSignedIn
import com.kinetix.shop.models.auth.SessionSignedOut
import com.kinetix.shop.models.auth.SessionUnreadable
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val errorParser = ApiErrorParser(json)
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(errorParser))

    @Test
    fun restoreSessionVerifiesMeUpdatesRoleAndSetsSignedInState() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val initialSession = StoredSession(
            accessToken = "acc-valid",
            refreshToken = "ref-valid",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(10L),
            principalId = PrincipalId("usr-10"),
            email = "buyer@kinetix.test",
            role = "customer",
        )
        val tokenStore = FakeTokenStore(current = initialSession)
        val apiService = FakeIdentityApiService().apply {
            meHandler = {
                Response.success(
                    CurrentSessionResponseDto(
                        principalId = "usr-10",
                        userId = 10L,
                        email = "buyer@kinetix.test",
                        role = "seller",
                        expiresAt = "2023-11-14T22:28:20Z",
                    )
                )
            }
        }
        val repository = SessionRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            tokenStore = tokenStore,
            sessionRenewer = FakeSessionRenewer(),
            clock = clock,
            crashReporter = FakeCrashReporter(),
            applicationScope = appScope,
        )

        assertEquals(SessionRestoring, repository.sessionState.value)

        val result = repository.restoreSession()
        assertTrue(result is Ok)
        assertTrue(repository.sessionState.value is SessionSignedIn)
        assertEquals("seller", repository.currentSession.value?.role)
        appScope.cancel()
    }

    @Test
    fun restoreSessionSetsUnreadableWithoutClearingStore() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val tokenStore = FakeTokenStore(
            readFailure = StorageFailure("Keystore invalidated"),
        )
        val repository = SessionRepositoryImpl(
            apiService = FakeIdentityApiService(),
            safeApiCaller = safeApiCaller,
            tokenStore = tokenStore,
            sessionRenewer = FakeSessionRenewer(),
            clock = FakeClock(),
            crashReporter = FakeCrashReporter(),
            applicationScope = appScope,
        )

        val result = repository.restoreSession()

        assertTrue(result is Err)
        assertEquals(SessionUnreadable, repository.sessionState.value)
        assertEquals(0, tokenStore.clearCount)
        appScope.cancel()
    }

    @Test
    fun restoreSessionKeepsRenewedSessionWhenSecondMeFailsWithNetworkError() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val initialSession = StoredSession(
            accessToken = "acc-expired",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(10L),
            principalId = PrincipalId("usr-10"),
            email = "buyer@kinetix.test",
        )
        val renewedSession = initialSession.copy(
            accessToken = "acc-fresh",
            refreshToken = "ref-2",
        )
        val tokenStore = FakeTokenStore(current = initialSession)
        var meCalls = 0
        val apiService = FakeIdentityApiService().apply {
            meHandler = {
                meCalls += 1
                if (meCalls == 1) {
                    Response.error(
                        401,
                        """{"message":"Unauthorized"}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                } else {
                    throw IOException("network lost after renew")
                }
            }
        }
        val renewer = FakeSessionRenewer(nextResult = Ok(renewedSession))
        val repository = SessionRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            tokenStore = tokenStore,
            sessionRenewer = renewer,
            clock = clock,
            crashReporter = FakeCrashReporter(),
            applicationScope = appScope,
        )

        val result = repository.restoreSession()

        assertTrue(result is Ok)
        assertEquals(renewedSession, (result as Ok).value)
        assertEquals(SessionSignedIn(renewedSession), repository.sessionState.value)
        assertEquals(0, tokenStore.clearCount)
        appScope.cancel()
    }

    @Test
    fun restoreSessionClearsSessionOnlyOnSecondUnauthorized() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val initialSession = StoredSession(
            accessToken = "acc-expired",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(10L),
            principalId = PrincipalId("usr-10"),
            email = "buyer@kinetix.test",
        )
        val renewedSession = initialSession.copy(
            accessToken = "acc-fresh",
            refreshToken = "ref-2",
        )
        val tokenStore = FakeTokenStore(current = initialSession)
        val apiService = FakeIdentityApiService().apply {
            meHandler = {
                Response.error(
                    401,
                    """{"message":"Unauthorized"}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            }
        }
        val repository = SessionRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            tokenStore = tokenStore,
            sessionRenewer = FakeSessionRenewer(nextResult = Ok(renewedSession)),
            clock = clock,
            crashReporter = FakeCrashReporter(),
            applicationScope = appScope,
        )

        val result = repository.restoreSession()

        assertTrue(result is Ok)
        assertNull((result as Ok).value)
        assertEquals(SessionSignedOut, repository.sessionState.value)
        assertEquals(1, tokenStore.clearCount)
        appScope.cancel()
    }

    @Test
    fun restoreSessionOnlyShortensExpiryFromMeAndNeverExtendsIt() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val initialSession = StoredSession(
            accessToken = "acc-valid",
            refreshToken = "ref-valid",
            expiresAtEpochSeconds = 1_700_000_500L,
            accountId = AccountId(10L),
            principalId = PrincipalId("usr-10"),
            email = "buyer@kinetix.test",
        )
        val tokenStore = FakeTokenStore(current = initialSession)
        val apiService = FakeIdentityApiService().apply {
            meHandler = {
                Response.success(
                    CurrentSessionResponseDto(
                        principalId = "usr-10",
                        userId = 10L,
                        email = "buyer@kinetix.test",
                        role = "customer",
                        expiresAt = "2023-11-14T22:40:00Z",
                    )
                )
            }
        }
        val repository = SessionRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            tokenStore = tokenStore,
            sessionRenewer = FakeSessionRenewer(),
            clock = clock,
            crashReporter = FakeCrashReporter(),
            applicationScope = appScope,
        )

        val result = repository.restoreSession()

        assertTrue(result is Ok)
        assertNotNull((result as Ok).value)
        assertEquals(1_700_000_500L, result.value!!.expiresAtEpochSeconds)
        appScope.cancel()
    }

    @Test
    fun signOutRenewsNearExpiryRetriesGateway401AndRecordsBreadcrumbWhenUnconfirmed() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(currentEpochSeconds = 1_700_000_850L)
        val initialSession = StoredSession(
            accessToken = "acc-expiring",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(10L),
            principalId = PrincipalId("usr-10"),
            email = "buyer@kinetix.test",
        )
        val renewedSession = initialSession.copy(
            accessToken = "acc-renewed",
            refreshToken = "ref-2",
            expiresAtEpochSeconds = 1_700_001_800L,
        )
        val tokenStore = FakeTokenStore(current = initialSession)
        val crashReporter = FakeCrashReporter()
        var logoutCalls = 0
        val apiService = FakeIdentityApiService().apply {
            logoutHandler = { _, _ ->
                logoutCalls += 1
                if (logoutCalls == 1) {
                    Response.error(
                        401,
                        """{"message":"Unauthorized"}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                } else {
                    Response.error(
                        503,
                        "gateway down".toResponseBody("text/plain".toMediaType()),
                    )
                }
            }
        }
        val renewer = FakeSessionRenewer(nextResult = Ok(renewedSession))
        val repository = SessionRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            tokenStore = tokenStore,
            sessionRenewer = renewer,
            clock = clock,
            crashReporter = crashReporter,
            applicationScope = appScope,
        )
        repository.updateSession(initialSession)

        repository.signOut()

        assertEquals(2, renewer.renewCalls)
        assertEquals(2, logoutCalls)
        assertEquals(SessionSignedOut, repository.sessionState.value)
        assertNull(tokenStore.current)
        assertEquals(1, crashReporter.breadcrumbs.size)
        appScope.cancel()
    }

    @Test
    fun withValidSessionRenewsAndRetriesOnceOnUnauthorized() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val initialSession = StoredSession(
            accessToken = "acc-expired",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(10L),
            principalId = PrincipalId("usr-10"),
            email = "buyer@kinetix.test",
        )
        val renewedSession = initialSession.copy(
            accessToken = "acc-fresh",
            refreshToken = "ref-2",
        )
        val renewer = FakeSessionRenewer(nextResult = Ok(renewedSession))
        val repository = SessionRepositoryImpl(
            apiService = FakeIdentityApiService(),
            safeApiCaller = safeApiCaller,
            tokenStore = FakeTokenStore(current = initialSession),
            sessionRenewer = renewer,
            clock = clock,
            crashReporter = FakeCrashReporter(),
            applicationScope = appScope,
        )
        repository.updateSession(initialSession)

        val seenTokens = mutableListOf<String>()
        val result = repository.withValidSession { session ->
            seenTokens.add(session.accessToken)
            if (session.accessToken == "acc-expired") {
                Err(UnauthorizedFailure("EXPIRED", "Expired"))
            } else {
                Ok("ok-after-renew")
            }
        }

        assertTrue(result is Ok)
        assertEquals("ok-after-renew", (result as Ok).value)
        assertEquals(listOf("acc-expired", "acc-fresh"), seenTokens)
        assertEquals(1, renewer.renewCalls)
        assertEquals(renewedSession, repository.currentSession.value)
        appScope.cancel()
    }
}

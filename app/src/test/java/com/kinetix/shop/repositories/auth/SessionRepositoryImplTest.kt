package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.auth.CurrentSessionResponseDto
import com.kinetix.shop.fakes.FakeClock
import com.kinetix.shop.fakes.FakeIdentityApiService
import com.kinetix.shop.fakes.FakeSessionRenewer
import com.kinetix.shop.fakes.FakeTokenStore
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SessionRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val errorParser = ApiErrorParser(json)
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(errorParser))

    @Test
    fun restoreSessionVerifiesMeAndUpdatesStoredSession() = runTest {
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val initialSession = StoredSession(
            accessToken = "acc-valid",
            refreshToken = "ref-valid",
            expiresAtEpochSeconds = 1_700_000_900L,
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
        )

        val result = repository.restoreSession()
        assertTrue(result is Ok)
        assertEquals("buyer@kinetix.test", repository.currentSession.value?.email)
    }

    @Test
    fun signOutClearsTokenStoreAndCurrentSession() = runTest {
        val initialSession = StoredSession(
            accessToken = "acc-valid",
            refreshToken = "ref-valid",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(10L),
            principalId = PrincipalId("usr-10"),
            email = "buyer@kinetix.test",
        )
        val tokenStore = FakeTokenStore(current = initialSession)
        val repository = SessionRepositoryImpl(
            apiService = FakeIdentityApiService(),
            safeApiCaller = safeApiCaller,
            tokenStore = tokenStore,
            sessionRenewer = FakeSessionRenewer(),
            clock = FakeClock(),
        )
        repository.updateSession(initialSession)

        repository.signOut()
        assertNull(repository.currentSession.value)
        assertNull(tokenStore.current)
    }

    @Test
    fun withValidSessionRenewsAndRetriesOnceOnUnauthorized() = runTest {
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
        )
        repository.updateSession(initialSession)

        val seenTokens = mutableListOf<String>()
        val result = repository.withValidSession { session ->
            seenTokens.add(session.accessToken)
            if (session.accessToken == "acc-expired") {
                com.kinetix.shop.core.result.Err(
                    com.kinetix.shop.core.failure.UnauthorizedFailure("EXPIRED", "Expired")
                )
            } else {
                Ok("ok-after-renew")
            }
        }

        assertTrue(result is Ok)
        assertEquals("ok-after-renew", (result as Ok).value)
        assertEquals(listOf("acc-expired", "acc-fresh"), seenTokens)
        assertEquals(1, renewer.renewCalls)
        assertEquals(renewedSession, repository.currentSession.value)
    }
}

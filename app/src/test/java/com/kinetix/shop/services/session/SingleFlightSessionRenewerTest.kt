package com.kinetix.shop.services.session

import com.kinetix.shop.core.failure.StorageFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeTokenRefreshTransport
import com.kinetix.shop.fakes.FakeTokenStore
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightSessionRenewerTest {
    private val initialSession = StoredSession(
        accessToken = "access-old",
        refreshToken = "refresh-old",
        expiresAtEpochSeconds = 1_700_000_010L,
        accountId = AccountId(7L),
        principalId = PrincipalId("principal-7"),
        email = "buyer@kinetix.test",
        role = "customer",
    )

    private val rotatedSession = initialSession.copy(
        accessToken = "access-new",
        refreshToken = "refresh-new",
        expiresAtEpochSeconds = 1_700_000_900L,
    )

    @Test
    fun coalescesConcurrentRefreshCallsIntoSingleNetworkCall() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val store = FakeTokenStore(initialSession)
        val transport = FakeTokenRefreshTransport(
            nextOutcome = RefreshTransportRotated(rotatedSession),
            simulatedDelayMillis = 50L,
        )
        val renewer = SingleFlightSessionRenewer(
            tokenStore = store,
            transport = transport,
            applicationScope = appScope,
        )

        val first = async { renewer.renew("access-old") }
        val second = async { renewer.renew("access-old") }

        val res1 = first.await()
        val res2 = second.await()

        assertTrue(res1 is Ok)
        assertTrue(res2 is Ok)
        assertEquals(rotatedSession, (res1 as Ok).value)
        assertEquals(rotatedSession, (res2 as Ok).value)
        assertEquals(1, transport.callCount)
        assertEquals(rotatedSession, store.current)
        appScope.cancel()
    }

    @Test
    fun cancellingCallerDoesNotCancelInFlightRefreshOrTokenWrite() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val store = FakeTokenStore(initialSession)
        val transport = FakeTokenRefreshTransport(
            nextOutcome = RefreshTransportRotated(rotatedSession),
            simulatedDelayMillis = 100L,
        )
        val renewer = SingleFlightSessionRenewer(
            tokenStore = store,
            transport = transport,
            applicationScope = appScope,
        )

        val callerJob = launch {
            renewer.renew("access-old")
        }
        advanceTimeBy(20L)
        callerJob.cancel()
        advanceUntilIdle()

        assertEquals(1, transport.callCount)
        assertEquals(rotatedSession, store.current)
        appScope.cancel()
    }

    @Test
    fun wipesTokenStoreWhenRefreshTokenIsRefused() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val store = FakeTokenStore(initialSession)
        val transport = FakeTokenRefreshTransport(
            nextOutcome = RefreshTransportRefused(
                statusCode = 401,
                errorCode = "TOKEN_REVOKED",
                message = "Refresh token revoked",
            ),
        )
        val renewer = SingleFlightSessionRenewer(
            tokenStore = store,
            transport = transport,
            applicationScope = appScope,
        )

        val result = renewer.renew("access-old")
        assertTrue(result is Err)
        assertTrue((result as Err).failure is UnauthorizedFailure)
        assertNull(store.current)
        assertEquals(1, store.clearCount)
        appScope.cancel()
    }

    @Test
    fun wipesTokenStoreOnAmbiguousDropBecauseRotationIsSingleUse() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val store = FakeTokenStore(initialSession)
        val transport = FakeTokenRefreshTransport(
            nextOutcome = RefreshTransportAmbiguousDrop("Connection reset after write"),
        )
        val renewer = SingleFlightSessionRenewer(
            tokenStore = store,
            transport = transport,
            applicationScope = appScope,
        )

        val result = renewer.renew("access-old")
        assertTrue(result is Err)
        assertNull(store.current)
        assertEquals(1, store.clearCount)
        appScope.cancel()
    }

    @Test
    fun clearsSessionWhenTokenWriteFailsAfter200Refresh() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val store = FakeTokenStore(
            current = initialSession,
            writeFailure = StorageFailure("Disk full"),
        )
        val transport = FakeTokenRefreshTransport(
            nextOutcome = RefreshTransportRotated(rotatedSession),
        )
        val renewer = SingleFlightSessionRenewer(
            tokenStore = store,
            transport = transport,
            applicationScope = appScope,
        )

        val result = renewer.renew("access-old")

        assertTrue(result is Err)
        assertEquals(1, store.clearCount)
        appScope.cancel()
    }
}

package com.kinetix.shop.viewmodels.auth

import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeAuthRepository
import com.kinetix.shop.models.auth.SignInAuthenticated
import com.kinetix.shop.models.auth.SignInMfaChallenge
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.auth.InMemoryMfaChallengeStore
import com.kinetix.shop.state.common.FormAuthenticatedUiState
import com.kinetix.shop.state.common.FormDelayedUiState
import com.kinetix.shop.state.common.FormIdleUiState
import com.kinetix.shop.state.common.FormMfaChallengeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emitsDelayedCountdownOn429AndReturnsToIdleWhenFinished() = runTest(testDispatcher) {
        val repository = FakeAuthRepository().apply {
            signInResult = Err(SignInDelayedFailure(retryAfterSeconds = 3))
        }
        val viewModel = SignInViewModel(repository, InMemoryMfaChallengeStore())

        viewModel.signIn(
            email = "buyer@kinetix.test",
            password = "password123",
        )

        advanceTimeBy(100.milliseconds)
        assertEquals(FormDelayedUiState(3), viewModel.uiState.value)

        advanceTimeBy(1_000.milliseconds)
        assertEquals(FormDelayedUiState(2), viewModel.uiState.value)

        advanceTimeBy(2_500.milliseconds)
        assertEquals(FormIdleUiState, viewModel.uiState.value)
    }

    @Test
    fun storesChallengeTokenInMemoryAndEmitsMfaChallengeStateWithoutLambda() = runTest(testDispatcher) {
        val challengeStore = InMemoryMfaChallengeStore()
        val repository = FakeAuthRepository().apply {
            signInResult = Ok(
                SignInMfaChallenge(
                    challengeToken = "chal-xyz",
                    expiresInSeconds = 300L,
                    passkeysAvailable = false,
                )
            )
        }
        val viewModel = SignInViewModel(repository, challengeStore)

        viewModel.signIn(
            email = "buyer@kinetix.test",
            password = "password123",
        )
        advanceUntilIdle()

        assertEquals(FormMfaChallengeUiState, viewModel.uiState.value)
        assertEquals("chal-xyz", challengeStore.peek())

        viewModel.consumeNavigationEvent()
        assertEquals(FormIdleUiState, viewModel.uiState.value)
    }

    @Test
    fun emitsAuthenticatedStateAndClearsStaleChallengeOnDirectSignIn() = runTest(testDispatcher) {
        val challengeStore = InMemoryMfaChallengeStore().apply {
            store("stale-challenge")
        }
        val session = StoredSession(
            accessToken = "acc-1",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(5L),
            principalId = PrincipalId("usr-5"),
            email = "buyer@kinetix.test",
        )
        val repository = FakeAuthRepository().apply {
            signInResult = Ok(SignInAuthenticated(session))
        }
        val viewModel = SignInViewModel(repository, challengeStore)

        viewModel.signIn(
            email = "buyer@kinetix.test",
            password = "password123",
        )
        advanceUntilIdle()

        assertEquals(FormAuthenticatedUiState, viewModel.uiState.value)
        assertNull(challengeStore.peek())
    }
}

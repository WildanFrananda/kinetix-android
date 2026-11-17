package com.kinetix.shop.viewmodels.auth

import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeAuthRepository
import com.kinetix.shop.models.auth.SignInMfaChallenge
import com.kinetix.shop.state.common.FormDelayedUiState
import com.kinetix.shop.state.common.FormIdleUiState
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
import org.junit.Assert.assertTrue
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
        val viewModel = SignInViewModel(repository)

        viewModel.signIn(
            email = "buyer@kinetix.test",
            password = "password123",
            onAuthenticated = {},
            onMfaChallenge = {},
        )

        advanceTimeBy(100.milliseconds)
        assertEquals(FormDelayedUiState(3), viewModel.uiState.value)

        advanceTimeBy(1_000.milliseconds)
        assertEquals(FormDelayedUiState(2), viewModel.uiState.value)

        advanceTimeBy(2_500.milliseconds)
        assertEquals(FormIdleUiState, viewModel.uiState.value)
    }

    @Test
    fun invokesOnMfaChallengeWhenRepositoryRequiresSecondFactor() = runTest(testDispatcher) {
        val repository = FakeAuthRepository().apply {
            signInResult = Ok(
                SignInMfaChallenge(
                    challengeToken = "chal-xyz",
                    expiresInSeconds = 300L,
                    passkeysAvailable = false,
                )
            )
        }
        val viewModel = SignInViewModel(repository)
        var capturedChallenge: String? = null

        viewModel.signIn(
            email = "buyer@kinetix.test",
            password = "password123",
            onAuthenticated = {},
            onMfaChallenge = { capturedChallenge = it },
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is FormIdleUiState)
        assertEquals("chal-xyz", capturedChallenge)
    }
}

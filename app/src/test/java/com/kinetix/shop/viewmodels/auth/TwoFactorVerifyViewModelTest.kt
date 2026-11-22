package com.kinetix.shop.viewmodels.auth

import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.fakes.FakeAuthRepository
import com.kinetix.shop.state.common.FormErrorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TwoFactorVerifyViewModelTest {
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
    fun invokesOnChallengeExpiredWhenChallengeTokenIsSpent() = runTest(testDispatcher) {
        val failure = ChallengeSpentFailure(
            code = "CHALLENGE_SPENT",
        )
        val repository = FakeAuthRepository().apply {
            verifyTwoFactorResult = Err(failure)
        }
        val viewModel = TwoFactorVerifyViewModel(repository)
        var challengeExpiredCalled = false

        viewModel.verify(
            challengeToken = "chal-spent",
            code = "123456",
            onVerified = {},
            onChallengeExpired = { challengeExpiredCalled = true },
        )
        advanceUntilIdle()

        assertTrue(challengeExpiredCalled)
        assertEquals(FormErrorUiState(failure), viewModel.uiState.value)
    }
}

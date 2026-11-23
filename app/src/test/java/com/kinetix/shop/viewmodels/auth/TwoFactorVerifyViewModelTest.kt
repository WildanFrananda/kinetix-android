package com.kinetix.shop.viewmodels.auth

import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.fakes.FakeAuthRepository
import com.kinetix.shop.services.auth.InMemoryMfaChallengeStore
import com.kinetix.shop.state.common.FormChallengeExpiredUiState
import com.kinetix.shop.state.common.FormIdleUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun emitsChallengeExpiredAndClearsStoreWhenChallengeTokenIsSpent() = runTest(testDispatcher) {
        val failure = ChallengeSpentFailure(
            code = "CHALLENGE_SPENT",
        )
        val repository = FakeAuthRepository().apply {
            verifyTwoFactorResult = Err(failure)
        }
        val challengeStore = InMemoryMfaChallengeStore().apply {
            store("chal-spent")
        }
        val viewModel = TwoFactorVerifyViewModel(repository, challengeStore)

        viewModel.verify(code = "123456")
        advanceUntilIdle()

        assertEquals(FormChallengeExpiredUiState, viewModel.uiState.value)
        assertNull(challengeStore.peek())

        viewModel.consumeNavigationEvent()
        assertEquals(FormIdleUiState, viewModel.uiState.value)
    }

    @Test
    fun startsInChallengeExpiredStateWhenInMemoryTokenIsMissingAfterProcessDeath() = runTest(testDispatcher) {
        val viewModel = TwoFactorVerifyViewModel(
            authRepository = FakeAuthRepository(),
            mfaChallengeStore = InMemoryMfaChallengeStore(),
        )

        assertEquals(FormChallengeExpiredUiState, viewModel.uiState.value)
    }
}

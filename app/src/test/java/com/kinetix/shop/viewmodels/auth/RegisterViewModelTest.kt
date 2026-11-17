package com.kinetix.shop.viewmodels.auth

import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeAuthRepository
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.state.common.FormErrorUiState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
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
    fun invokesOnRegisteredWhenRegistrationSucceeds() = runTest(testDispatcher) {
        val session = StoredSession(
            accessToken = "acc-1",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(5L),
            principalId = PrincipalId("usr-5"),
            email = "new@kinetix.test",
        )
        val repository = FakeAuthRepository().apply {
            registerResult = Ok(session)
        }
        val viewModel = RegisterViewModel(repository)
        var registeredCalled = false

        viewModel.register(
            email = "new@kinetix.test",
            password = "password12345",
            onRegistered = { registeredCalled = true },
        )
        advanceUntilIdle()

        assertTrue(registeredCalled)
        assertEquals(FormIdleUiState, viewModel.uiState.value)
    }

    @Test
    fun exposesConflictFailureWhenEmailAlreadyExists() = runTest(testDispatcher) {
        val conflict = ConflictFailure(
            code = "EMAIL_ALREADY_EXISTS",
            detail = "Email already registered",
        )
        val repository = FakeAuthRepository().apply {
            registerResult = Err(conflict)
        }
        val viewModel = RegisterViewModel(repository)

        viewModel.register(
            email = "dup@kinetix.test",
            password = "password12345",
            onRegistered = {},
        )
        advanceUntilIdle()

        assertEquals(FormErrorUiState(conflict), viewModel.uiState.value)
    }
}

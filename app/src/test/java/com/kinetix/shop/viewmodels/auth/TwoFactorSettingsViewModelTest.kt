package com.kinetix.shop.viewmodels.auth

import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeTwoFactorRepository
import com.kinetix.shop.models.auth.TwoFactorSetupSecret
import com.kinetix.shop.state.auth.TwoFactorSettingsCompletedUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsSecretReadyUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TwoFactorSettingsViewModelTest {
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
    fun transitionsFromSecretReadyToCompletedWhenEnabling2FA() = runTest(testDispatcher) {
        val secret = TwoFactorSetupSecret(
            secret = "JBSWY3DPEHPK3PXP",
            qrCodeUrl = "otpauth://totp/Kinetix",
        )
        val repository = FakeTwoFactorRepository().apply {
            setupResult = Ok(secret)
            enableResult = Ok(true)
        }
        val viewModel = TwoFactorSettingsViewModel(repository)

        viewModel.requestSetupSecret("password12345")
        advanceUntilIdle()
        assertEquals(TwoFactorSettingsSecretReadyUiState(secret), viewModel.uiState.value)

        viewModel.enableTwoFactor("123456", "password12345")
        advanceUntilIdle()
        assertEquals(TwoFactorSettingsCompletedUiState(enabled = true), viewModel.uiState.value)
    }

    @Test
    fun reportsPasswordRequiredSeparatelyFromInvalidTotpCode() = runTest(testDispatcher) {
        val viewModel = TwoFactorSettingsViewModel(FakeTwoFactorRepository())

        viewModel.enableTwoFactor("123456", "   ")
        assertEquals(
            com.kinetix.shop.state.auth.TwoFactorSettingsErrorUiState(
                com.kinetix.shop.core.failure.ClientValidationFailure(
                    com.kinetix.shop.core.failure.ClientValidationReason.PASSWORD_REQUIRED
                )
            ),
            viewModel.uiState.value,
        )

        viewModel.disableTwoFactor("123456", "")
        assertEquals(
            com.kinetix.shop.state.auth.TwoFactorSettingsErrorUiState(
                com.kinetix.shop.core.failure.ClientValidationFailure(
                    com.kinetix.shop.core.failure.ClientValidationReason.PASSWORD_REQUIRED
                )
            ),
            viewModel.uiState.value,
        )
    }
}

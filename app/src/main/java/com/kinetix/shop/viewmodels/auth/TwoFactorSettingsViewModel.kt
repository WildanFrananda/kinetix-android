package com.kinetix.shop.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.ClientValidationFailure
import com.kinetix.shop.core.failure.ClientValidationReason
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.time.DelayCountdownTimer
import com.kinetix.shop.repositories.auth.TwoFactorRepository
import com.kinetix.shop.state.auth.TwoFactorSettingsCompletedUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsDelayedUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsErrorUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsIdleUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsSecretReadyUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsSubmittingUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TwoFactorSettingsViewModel @Inject constructor(
    private val twoFactorRepository: TwoFactorRepository,
) : ViewModel() {
    private val countdownTimer = DelayCountdownTimer(viewModelScope)
    private val mutableState = MutableStateFlow<TwoFactorSettingsUiState>(TwoFactorSettingsIdleUiState)
    val uiState: StateFlow<TwoFactorSettingsUiState> = mutableState.asStateFlow()

    fun requestSetupSecret(
        password: String,
    ) {
        val current = mutableState.value
        if (current is TwoFactorSettingsSubmittingUiState || current is TwoFactorSettingsDelayedUiState) {
            return
        }
        if (password.isBlank()) {
            mutableState.value = TwoFactorSettingsErrorUiState(
                ClientValidationFailure(ClientValidationReason.PASSWORD_REQUIRED)
            )
            return
        }

        mutableState.value = TwoFactorSettingsSubmittingUiState
        viewModelScope.launch {
            when (val result = twoFactorRepository.setupTwoFactor(password)) {
                is Ok -> {
                    mutableState.value = TwoFactorSettingsSecretReadyUiState(result.value)
                }
                is Err -> {
                    val failure = result.failure
                    if (failure is SignInDelayedFailure) {
                        startCountdown(failure.retryAfterSeconds)
                    } else {
                        mutableState.value = TwoFactorSettingsErrorUiState(failure)
                    }
                }
            }
        }
    }

    fun enableTwoFactor(
        code: String,
        password: String,
    ) {
        val current = mutableState.value
        if (current is TwoFactorSettingsSubmittingUiState || current is TwoFactorSettingsDelayedUiState) {
            return
        }
        if (password.isBlank()) {
            mutableState.value = TwoFactorSettingsErrorUiState(
                ClientValidationFailure(ClientValidationReason.PASSWORD_REQUIRED)
            )
            return
        }
        val cleanCode = code.trim()
        if (cleanCode.length != NetworkPolicy.TOTP_CODE_LENGTH || cleanCode.any { !it.isDigit() }) {
            mutableState.value = TwoFactorSettingsErrorUiState(
                ClientValidationFailure(ClientValidationReason.INVALID_TOTP_CODE)
            )
            return
        }

        mutableState.value = TwoFactorSettingsSubmittingUiState
        viewModelScope.launch {
            when (val result = twoFactorRepository.enableTwoFactor(cleanCode, password)) {
                is Ok -> {
                    mutableState.value = TwoFactorSettingsCompletedUiState(enabled = result.value)
                }
                is Err -> {
                    val failure = result.failure
                    if (failure is SignInDelayedFailure) {
                        startCountdown(failure.retryAfterSeconds)
                    } else {
                        mutableState.value = TwoFactorSettingsErrorUiState(failure)
                    }
                }
            }
        }
    }

    fun disableTwoFactor(
        code: String,
        password: String,
    ) {
        val current = mutableState.value
        if (current is TwoFactorSettingsSubmittingUiState || current is TwoFactorSettingsDelayedUiState) {
            return
        }
        if (password.isBlank()) {
            mutableState.value = TwoFactorSettingsErrorUiState(
                ClientValidationFailure(ClientValidationReason.PASSWORD_REQUIRED)
            )
            return
        }
        val cleanCode = code.trim()
        if (cleanCode.length != NetworkPolicy.TOTP_CODE_LENGTH || cleanCode.any { !it.isDigit() }) {
            mutableState.value = TwoFactorSettingsErrorUiState(
                ClientValidationFailure(ClientValidationReason.INVALID_TOTP_CODE)
            )
            return
        }

        mutableState.value = TwoFactorSettingsSubmittingUiState
        viewModelScope.launch {
            when (val result = twoFactorRepository.disableTwoFactor(cleanCode, password)) {
                is Ok -> {
                    mutableState.value = TwoFactorSettingsCompletedUiState(enabled = result.value)
                }
                is Err -> {
                    val failure = result.failure
                    if (failure is SignInDelayedFailure) {
                        startCountdown(failure.retryAfterSeconds)
                    } else {
                        mutableState.value = TwoFactorSettingsErrorUiState(failure)
                    }
                }
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownTimer.start(
            seconds = seconds,
            onTick = { remaining ->
                mutableState.value = TwoFactorSettingsDelayedUiState(remaining)
            },
            onFinished = {
                mutableState.value = TwoFactorSettingsIdleUiState
            },
        )
    }

    override fun onCleared() {
        countdownTimer.cancel()
        super.onCleared()
    }
}

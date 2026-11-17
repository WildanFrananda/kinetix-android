package com.kinetix.shop.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.ValidationFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.time.DelayCountdownTimer
import com.kinetix.shop.repositories.auth.AuthRepository
import com.kinetix.shop.state.common.AsyncFormUiState
import com.kinetix.shop.state.common.FormDelayedUiState
import com.kinetix.shop.state.common.FormErrorUiState
import com.kinetix.shop.state.common.FormIdleUiState
import com.kinetix.shop.state.common.FormSubmittingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TwoFactorVerifyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val countdownTimer = DelayCountdownTimer(viewModelScope)
    private val mutableState = MutableStateFlow<AsyncFormUiState>(FormIdleUiState)
    val uiState: StateFlow<AsyncFormUiState> = mutableState.asStateFlow()

    fun verify(
        challengeToken: String,
        code: String,
        onVerified: () -> Unit,
        onChallengeExpired: () -> Unit,
    ) {
        val current = mutableState.value
        if (current is FormSubmittingUiState || current is FormDelayedUiState) {
            return
        }
        val sanitized = code.trim()
        if (sanitized.length != NetworkPolicy.TOTP_CODE_LENGTH || sanitized.any { !it.isDigit() }) {
            mutableState.value = FormErrorUiState(
                ValidationFailure(
                    code = "INVALID_TOTP_CODE",
                    detail = "",
                    field = "code",
                )
            )
            return
        }

        mutableState.value = FormSubmittingUiState
        viewModelScope.launch {
            when (val result = authRepository.verifyTwoFactor(challengeToken, sanitized)) {
                is Ok -> {
                    mutableState.value = FormIdleUiState
                    onVerified()
                }
                is Err -> {
                    when (val failure = result.failure) {
                        is SignInDelayedFailure -> {
                            countdownTimer.start(
                                seconds = failure.retryAfterSeconds,
                                onTick = { remaining ->
                                    mutableState.value = FormDelayedUiState(remaining)
                                },
                                onFinished = {
                                    mutableState.value = FormIdleUiState
                                },
                            )
                        }
                        is ChallengeSpentFailure -> {
                            mutableState.value = FormErrorUiState(failure)
                            onChallengeExpired()
                        }
                        else -> {
                            mutableState.value = FormErrorUiState(failure)
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        countdownTimer.cancel()
        super.onCleared()
    }
}

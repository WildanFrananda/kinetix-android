package com.kinetix.shop.viewmodels.auth

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.failure.ClientValidationFailure
import com.kinetix.shop.core.failure.ClientValidationReason
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.time.DelayCountdownTimer
import com.kinetix.shop.repositories.auth.AuthRepository
import com.kinetix.shop.services.auth.MfaChallengeStore
import com.kinetix.shop.state.common.AsyncFormUiState
import com.kinetix.shop.state.common.FormAuthenticatedUiState
import com.kinetix.shop.state.common.FormChallengeExpiredUiState
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
    private val mfaChallengeStore: MfaChallengeStore,
) : ViewModel() {
    private val countdownTimer = DelayCountdownTimer(viewModelScope)
    private val mutableState = MutableStateFlow(
        if (mfaChallengeStore.peek() == null) {
            FormChallengeExpiredUiState
        } else {
            FormIdleUiState
        }
    )
    val uiState: StateFlow<AsyncFormUiState> = mutableState.asStateFlow()

    fun verify(
        code: String,
    ) {
        val current = mutableState.value
        if (current is FormSubmittingUiState || current is FormDelayedUiState) {
            return
        }
        val challengeToken = mfaChallengeStore.peek()
        if (challengeToken == null) {
            mutableState.value = FormChallengeExpiredUiState
            return
        }
        val sanitized = code.trim()
        if (sanitized.length != NetworkPolicy.TOTP_CODE_LENGTH || sanitized.any { !it.isDigit() }) {
            mutableState.value = FormErrorUiState(
                ClientValidationFailure(ClientValidationReason.INVALID_TOTP_CODE)
            )
            return
        }

        mutableState.value = FormSubmittingUiState
        viewModelScope.launch {
            when (val result = authRepository.verifyTwoFactor(challengeToken, sanitized)) {
                is Ok -> {
                    mfaChallengeStore.clear()
                    mutableState.value = FormAuthenticatedUiState
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
                            mfaChallengeStore.clear()
                            mutableState.value = FormChallengeExpiredUiState
                        }
                        else -> {
                            mutableState.value = FormErrorUiState(failure)
                        }
                    }
                }
            }
        }
    }

    fun consumeNavigationEvent() {
        val current = mutableState.value
        if (current is FormAuthenticatedUiState || current is FormChallengeExpiredUiState) {
            mutableState.value = FormIdleUiState
        }
    }

    fun cancelChallenge() {
        mfaChallengeStore.clear()
        mutableState.value = FormChallengeExpiredUiState
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        countdownTimer.cancel()
    }
}

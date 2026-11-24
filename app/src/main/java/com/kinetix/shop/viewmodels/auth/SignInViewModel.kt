package com.kinetix.shop.viewmodels.auth

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.failure.ClientValidationFailure
import com.kinetix.shop.core.failure.ClientValidationReason
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.time.DelayCountdownTimer
import com.kinetix.shop.models.auth.SignInAuthenticated
import com.kinetix.shop.models.auth.SignInMfaChallenge
import com.kinetix.shop.repositories.auth.AuthRepository
import com.kinetix.shop.services.auth.MfaChallengeStore
import com.kinetix.shop.state.common.AsyncFormUiState
import com.kinetix.shop.state.common.FormAuthenticatedUiState
import com.kinetix.shop.state.common.FormDelayedUiState
import com.kinetix.shop.state.common.FormErrorUiState
import com.kinetix.shop.state.common.FormIdleUiState
import com.kinetix.shop.state.common.FormMfaChallengeUiState
import com.kinetix.shop.state.common.FormSubmittingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val mfaChallengeStore: MfaChallengeStore,
) : ViewModel() {
    private val countdownTimer = DelayCountdownTimer(viewModelScope)
    private val mutableState = MutableStateFlow<AsyncFormUiState>(FormIdleUiState)
    val uiState: StateFlow<AsyncFormUiState> = mutableState.asStateFlow()

    fun signIn(
        email: String,
        password: String,
    ) {
        val current = mutableState.value
        if (current is FormSubmittingUiState || current is FormDelayedUiState) {
            return
        }
        if (email.isBlank() || password.isBlank()) {
            mutableState.value = FormErrorUiState(
                ClientValidationFailure(ClientValidationReason.CREDENTIALS_REQUIRED)
            )
            return
        }

        mutableState.value = FormSubmittingUiState
        viewModelScope.launch {
            when (val result = authRepository.signIn(email.trim(), password)) {
                is Ok -> {
                    when (val outcome = result.value) {
                        is SignInAuthenticated -> {
                            mfaChallengeStore.clear()
                            mutableState.value = FormAuthenticatedUiState
                        }
                        is SignInMfaChallenge -> {
                            mfaChallengeStore.store(outcome.challengeToken)
                            mutableState.value = FormMfaChallengeUiState
                        }
                    }
                }
                is Err -> {
                    val failure = result.failure
                    if (failure is SignInDelayedFailure) {
                        countdownTimer.start(
                            seconds = failure.retryAfterSeconds,
                            onTick = { remaining ->
                                mutableState.value = FormDelayedUiState(remaining)
                            },
                            onFinished = {
                                mutableState.value = FormIdleUiState
                            },
                        )
                    } else {
                        mutableState.value = FormErrorUiState(failure)
                    }
                }
            }
        }
    }

    fun consumeNavigationEvent() {
        val current = mutableState.value
        if (current is FormAuthenticatedUiState || current is FormMfaChallengeUiState) {
            mutableState.value = FormIdleUiState
        }
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        countdownTimer.cancel()
    }
}

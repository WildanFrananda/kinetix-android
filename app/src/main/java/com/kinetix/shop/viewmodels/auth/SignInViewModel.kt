package com.kinetix.shop.viewmodels.auth

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
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val countdownTimer = DelayCountdownTimer(viewModelScope)
    private val mutableState = MutableStateFlow<AsyncFormUiState>(FormIdleUiState)
    val uiState: StateFlow<AsyncFormUiState> = mutableState.asStateFlow()

    fun signIn(
        email: String,
        password: String,
        onAuthenticated: () -> Unit,
        onMfaChallenge: (String) -> Unit,
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
                            mutableState.value = FormIdleUiState
                            onAuthenticated()
                        }
                        is SignInMfaChallenge -> {
                            mutableState.value = FormIdleUiState
                            onMfaChallenge(outcome.challengeToken)
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

    override fun onCleared() {
        countdownTimer.cancel()
        super.onCleared()
    }
}

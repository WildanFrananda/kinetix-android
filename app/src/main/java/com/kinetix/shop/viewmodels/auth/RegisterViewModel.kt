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
import com.kinetix.shop.repositories.auth.AuthRepository
import com.kinetix.shop.state.common.AsyncFormUiState
import com.kinetix.shop.state.common.FormAuthenticatedUiState
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val countdownTimer = DelayCountdownTimer(viewModelScope)
    private val mutableState = MutableStateFlow<AsyncFormUiState>(FormIdleUiState)
    val uiState: StateFlow<AsyncFormUiState> = mutableState.asStateFlow()

    fun register(
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
        if (password.length < NetworkPolicy.MIN_PASSWORD_LENGTH) {
            mutableState.value = FormErrorUiState(
                ClientValidationFailure(ClientValidationReason.PASSWORD_TOO_SHORT)
            )
            return
        }

        mutableState.value = FormSubmittingUiState
        viewModelScope.launch {
            when (val result = authRepository.register(email.trim(), password)) {
                is Ok -> {
                    mutableState.value = FormAuthenticatedUiState
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
        if (mutableState.value is FormAuthenticatedUiState) {
            mutableState.value = FormIdleUiState
        }
    }

    override fun onCleared() {
        countdownTimer.cancel()
        super.onCleared()
    }
}

package com.kinetix.shop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kinetix.shop.R
import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.AdminAccountNotSupportedFailure
import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.failure.ClientValidationFailure
import com.kinetix.shop.core.failure.ClientValidationReason
import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.failure.ForbiddenFailure
import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.failure.RateLimitedFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.StorageFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.failure.ValidationFailure
import com.kinetix.shop.core.failure.WrongPasswordFailure
import com.kinetix.shop.core.failure.WrongTwoFactorCodeFailure

object FailureMessageMapper {
    @Composable
    fun resolve(
        failure: Failure,
    ): String {
        return when (failure) {
            is AdminAccountNotSupportedFailure -> stringResource(R.string.error_admin_not_supported)
            is ChallengeSpentFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_challenge_expired) }
            }
            is WrongTwoFactorCodeFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_wrong_totp_code) }
            }
            is WrongPasswordFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_wrong_password) }
            }
            is UnauthorizedFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_unauthorized) }
            }
            is ForbiddenFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_forbidden) }
            }
            is NotFoundFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_not_found) }
            }
            is ConflictFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_conflict) }
            }
            is RateLimitedFailure -> stringResource(
                R.string.error_rate_limited,
                failure.retryAfterSeconds ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS,
            )
            is SignInDelayedFailure -> stringResource(
                R.string.error_rate_limited,
                failure.retryAfterSeconds,
            )
            is ClientValidationFailure -> {
                when (failure.reason) {
                    ClientValidationReason.CREDENTIALS_REQUIRED -> {
                        stringResource(R.string.error_credentials_required)
                    }
                    ClientValidationReason.PASSWORD_TOO_SHORT -> {
                        stringResource(R.string.error_password_too_short)
                    }
                    ClientValidationReason.INVALID_TOTP_CODE -> {
                        stringResource(R.string.error_invalid_totp_code)
                    }
                    ClientValidationReason.PASSWORD_REQUIRED -> {
                        stringResource(R.string.error_password_required)
                    }
                    ClientValidationReason.PROFILE_FIELDS_REQUIRED -> {
                        stringResource(R.string.error_profile_fields_required)
                    }
                }
            }
            is ValidationFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_validation_generic) }
            }
            is TimeoutFailure -> stringResource(R.string.error_timeout)
            is UnavailableFailure -> stringResource(R.string.error_unavailable)
            is NetworkFailure -> failure.detail.ifBlank { stringResource(R.string.error_network) }
            is UnreadableAnswerFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_unreadable_response) }
            }
            is StorageFailure -> failure.detail.ifBlank { stringResource(R.string.error_storage) }
            is UnknownOutcomeFailure -> {
                failure.detail.ifBlank { stringResource(R.string.error_unknown_outcome) }
            }
        }
    }
}

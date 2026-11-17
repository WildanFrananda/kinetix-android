package com.kinetix.shop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kinetix.shop.R
import com.kinetix.shop.core.failure.AdminAccountNotSupportedFailure
import com.kinetix.shop.core.failure.ChallengeSpentFailure
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
            is ConflictFailure -> failure.detail
            is RateLimitedFailure -> stringResource(
                R.string.error_rate_limited,
                failure.retryAfterSeconds ?: 60,
            )
            is SignInDelayedFailure -> stringResource(
                R.string.error_rate_limited,
                failure.retryAfterSeconds,
            )
            is ValidationFailure -> {
                when (failure.code) {
                    "CREDENTIALS_REQUIRED" -> stringResource(R.string.error_credentials_required)
                    "PASSWORD_TOO_SHORT" -> stringResource(R.string.error_password_too_short)
                    "INVALID_TOTP_CODE" -> stringResource(R.string.error_invalid_totp_code)
                    "PASSWORD_REQUIRED" -> stringResource(R.string.error_password_required)
                    "PROFILE_FIELDS_REQUIRED" -> stringResource(R.string.error_profile_fields_required)
                    else -> failure.detail.ifBlank { stringResource(R.string.error_validation_generic) }
                }
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

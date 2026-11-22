package com.kinetix.shop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
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
    fun resolveSpec(
        failure: Failure,
    ): FailureTextSpec {
        return when (failure) {
            is AdminAccountNotSupportedFailure -> FailureTextSpec(R.string.error_admin_not_supported)
            is ChallengeSpentFailure -> FailureTextSpec(R.string.error_challenge_expired)
            is WrongTwoFactorCodeFailure -> FailureTextSpec(R.string.error_wrong_totp_code)
            is WrongPasswordFailure -> FailureTextSpec(R.string.error_wrong_password)
            is UnauthorizedFailure -> {
                val resId = when (failure.code) {
                    "NOT_SIGNED_IN",
                    "NO_SESSION",
                    "SESSION_AMBIGUOUS_DROP",
                    "SESSION_EXPIRED",
                    "EXPIRED",
                    "PRINCIPAL_MISMATCH" -> R.string.error_session_expired

                    else -> if (failure.code?.startsWith("SESSION_REFUSED_") == true) {
                        R.string.error_session_expired
                    } else {
                        R.string.error_unauthorized
                    }
                }
                FailureTextSpec(resId)
            }
            is ForbiddenFailure -> FailureTextSpec(R.string.error_forbidden)
            is NotFoundFailure -> FailureTextSpec(R.string.error_not_found)
            is ConflictFailure -> {
                val resId = when (failure.code) {
                    "EMAIL_ALREADY_EXISTS" -> R.string.error_email_already_exists
                    else -> R.string.error_conflict
                }
                FailureTextSpec(resId)
            }
            is RateLimitedFailure -> {
                val waitSeconds = (failure.retryAfterSeconds ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS)
                    .coerceAtLeast(1)
                FailureTextSpec(
                    resId = R.plurals.error_rate_limited,
                    quantity = waitSeconds,
                    formatArgs = listOf(waitSeconds),
                )
            }
            is SignInDelayedFailure -> {
                val waitSeconds = failure.retryAfterSeconds.coerceAtLeast(1)
                FailureTextSpec(
                    resId = R.plurals.error_rate_limited,
                    quantity = waitSeconds,
                    formatArgs = listOf(waitSeconds),
                )
            }
            is ClientValidationFailure -> {
                val resId = when (failure.reason) {
                    ClientValidationReason.CREDENTIALS_REQUIRED -> R.string.error_credentials_required
                    ClientValidationReason.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
                    ClientValidationReason.INVALID_TOTP_CODE -> R.string.error_invalid_totp_code
                    ClientValidationReason.PASSWORD_REQUIRED -> R.string.error_password_required
                    ClientValidationReason.PROFILE_FIELDS_REQUIRED -> R.string.error_profile_fields_required
                }
                FailureTextSpec(resId)
            }
            is ValidationFailure -> {
                val resId = when (failure.code) {
                    "malformed_query" -> R.string.error_search_malformed_query
                    "PASSWORD_REQUIRED" -> R.string.error_password_required
                    else -> R.string.error_validation_generic
                }
                FailureTextSpec(resId)
            }
            is TimeoutFailure -> FailureTextSpec(R.string.error_timeout)
            is UnavailableFailure -> {
                val resId = when (failure.code) {
                    "index_unavailable",
                    "dependency_unavailable",
                    "stale" -> R.string.error_search_index_unavailable

                    else -> R.string.error_unavailable
                }
                FailureTextSpec(resId)
            }
            is NetworkFailure -> FailureTextSpec(R.string.error_network)
            is UnreadableAnswerFailure -> FailureTextSpec(R.string.error_unreadable_response)
            is StorageFailure -> FailureTextSpec(R.string.error_storage)
            is UnknownOutcomeFailure -> {
                val resId = when (failure.code) {
                    "corrupt_record" -> R.string.error_search_corrupt_record
                    else -> R.string.error_unknown_outcome
                }
                FailureTextSpec(resId)
            }
        }
    }

    @Composable
    fun resolve(
        failure: Failure,
    ): String {
        val spec = resolveSpec(failure)
        val args = spec.formatArgs.toTypedArray()
        return if (spec.quantity != null) {
            pluralStringResource(spec.resId, spec.quantity, *args)
        } else {
            stringResource(spec.resId, *args)
        }
    }
}

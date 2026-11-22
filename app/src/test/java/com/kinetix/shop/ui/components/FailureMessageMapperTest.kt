package com.kinetix.shop.ui.components

import com.kinetix.shop.R
import com.kinetix.shop.core.failure.AdminAccountNotSupportedFailure
import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.failure.ClientValidationFailure
import com.kinetix.shop.core.failure.ClientValidationReason
import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.ForbiddenFailure
import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.failure.RateLimitedFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.StorageFailure
import com.kinetix.shop.core.failure.StorageFailureReason
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.failure.ValidationFailure
import com.kinetix.shop.core.failure.WrongPasswordFailure
import com.kinetix.shop.core.failure.WrongTwoFactorCodeFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class FailureMessageMapperTest {
    @Test
    fun mapsEveryFailureTypeAndMachineCodeToExpectedResourceSpec() {
        assertEquals(
            FailureTextSpec(R.string.error_admin_not_supported),
            FailureMessageMapper.resolveSpec(AdminAccountNotSupportedFailure),
        )
        assertEquals(
            FailureTextSpec(R.string.error_challenge_expired),
            FailureMessageMapper.resolveSpec(ChallengeSpentFailure("CHALLENGE_SPENT")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_wrong_totp_code),
            FailureMessageMapper.resolveSpec(WrongTwoFactorCodeFailure()),
        )
        assertEquals(
            FailureTextSpec(R.string.error_wrong_password),
            FailureMessageMapper.resolveSpec(WrongPasswordFailure()),
        )
        assertEquals(
            FailureTextSpec(R.string.error_unauthorized),
            FailureMessageMapper.resolveSpec(UnauthorizedFailure("INVALID_CREDENTIALS")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_session_expired),
            FailureMessageMapper.resolveSpec(UnauthorizedFailure("SESSION_EXPIRED")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_session_expired),
            FailureMessageMapper.resolveSpec(UnauthorizedFailure("SESSION_REFUSED_401")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_forbidden),
            FailureMessageMapper.resolveSpec(ForbiddenFailure("FORBIDDEN")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_not_found),
            FailureMessageMapper.resolveSpec(NotFoundFailure("HTTP_404")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_email_already_exists),
            FailureMessageMapper.resolveSpec(ConflictFailure("EMAIL_ALREADY_EXISTS")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_conflict),
            FailureMessageMapper.resolveSpec(ConflictFailure("CONFLICT")),
        )
        assertEquals(
            FailureTextSpec(
                resId = R.plurals.error_rate_limited,
                quantity = 12,
                formatArgs = listOf(12),
            ),
            FailureMessageMapper.resolveSpec(RateLimitedFailure(12)),
        )
        assertEquals(
            FailureTextSpec(
                resId = R.plurals.error_rate_limited,
                quantity = 60,
                formatArgs = listOf(60),
            ),
            FailureMessageMapper.resolveSpec(RateLimitedFailure(null)),
        )
        assertEquals(
            FailureTextSpec(
                resId = R.plurals.error_rate_limited,
                quantity = 5,
                formatArgs = listOf(5),
            ),
            FailureMessageMapper.resolveSpec(SignInDelayedFailure(5)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_credentials_required),
            FailureMessageMapper.resolveSpec(ClientValidationFailure(ClientValidationReason.CREDENTIALS_REQUIRED)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_password_too_short),
            FailureMessageMapper.resolveSpec(ClientValidationFailure(ClientValidationReason.PASSWORD_TOO_SHORT)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_invalid_totp_code),
            FailureMessageMapper.resolveSpec(ClientValidationFailure(ClientValidationReason.INVALID_TOTP_CODE)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_password_required),
            FailureMessageMapper.resolveSpec(ClientValidationFailure(ClientValidationReason.PASSWORD_REQUIRED)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_profile_fields_required),
            FailureMessageMapper.resolveSpec(ClientValidationFailure(ClientValidationReason.PROFILE_FIELDS_REQUIRED)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_search_malformed_query),
            FailureMessageMapper.resolveSpec(ValidationFailure("malformed_query")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_password_required),
            FailureMessageMapper.resolveSpec(ValidationFailure("PASSWORD_REQUIRED")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_validation_generic),
            FailureMessageMapper.resolveSpec(ValidationFailure("HTTP_400")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_timeout),
            FailureMessageMapper.resolveSpec(TimeoutFailure),
        )
        assertEquals(
            FailureTextSpec(R.string.error_search_index_unavailable),
            FailureMessageMapper.resolveSpec(UnavailableFailure("index_unavailable", null)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_unavailable),
            FailureMessageMapper.resolveSpec(UnavailableFailure("GATEWAY_503", null)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_network),
            FailureMessageMapper.resolveSpec(NetworkFailure()),
        )
        assertEquals(
            FailureTextSpec(R.string.error_unreadable_response),
            FailureMessageMapper.resolveSpec(UnreadableAnswerFailure()),
        )
        assertEquals(
            FailureTextSpec(R.string.error_storage),
            FailureMessageMapper.resolveSpec(StorageFailure(StorageFailureReason.READ_FAILED)),
        )
        assertEquals(
            FailureTextSpec(R.string.error_search_corrupt_record),
            FailureMessageMapper.resolveSpec(UnknownOutcomeFailure("corrupt_record")),
        )
        assertEquals(
            FailureTextSpec(R.string.error_unknown_outcome),
            FailureMessageMapper.resolveSpec(UnknownOutcomeFailure("GATEWAY_504")),
        )
    }
}

package com.kinetix.shop.core.network

import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.failure.ForbiddenFailure
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.failure.RateLimitedFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.ValidationFailure
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpFailureMapper @Inject constructor(
    private val errorParser: ApiErrorParser,
) {
    fun map(
        statusCode: Int,
        rawErrorBody: String?,
        retryAfterHeaderSeconds: Int?,
        requestId: String? = null,
        isWrite: Boolean = false,
    ): Failure {
        val parsed = errorParser.parse(rawErrorBody)
        val isServiceJson = errorParser.isServiceJsonError(parsed)
        val machineCode = errorParser.extractErrorCode(parsed)
        val code = machineCode ?: if (!isServiceJson && statusCode in 502..504) {
            "GATEWAY_$statusCode"
        } else {
            "HTTP_$statusCode"
        }
        val detail = errorParser.extractMessage(parsed) ?: "HTTP $statusCode"
        val resolvedRequestId = parsed?.traceId?.takeIf { it.isNotBlank() }
            ?: parsed?.requestId?.takeIf { it.isNotBlank() }
            ?: requestId

        return when (statusCode) {
            400, 422 -> ValidationFailure(
                code = code,
                detail = detail,
                field = parsed?.field,
                requestId = resolvedRequestId,
            )

            401 -> UnauthorizedFailure(
                code = code,
                detail = detail,
                requestId = resolvedRequestId,
            )

            403 -> ForbiddenFailure(
                code = code,
                detail = detail,
                requestId = resolvedRequestId,
            )

            404 -> NotFoundFailure(
                code = code,
                detail = detail,
                requestId = resolvedRequestId,
            )

            409 -> ConflictFailure(
                code = code,
                detail = detail,
                requestId = resolvedRequestId,
            )

            429 -> {
                val delaySeconds = parsed?.retryAfterSeconds
                    ?: retryAfterHeaderSeconds
                    ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS
                if (code == "SIGN_IN_DELAYED") {
                    SignInDelayedFailure(
                        retryAfterSeconds = delaySeconds,
                        requestId = resolvedRequestId,
                    )
                } else {
                    RateLimitedFailure(
                        retryAfterSeconds = delaySeconds,
                        requestId = resolvedRequestId,
                    )
                }
            }

            502, 503, 504 -> {
                if (!isServiceJson && isWrite) {
                    UnknownOutcomeFailure(
                        code = code,
                        detail = detail,
                        requestId = resolvedRequestId,
                    )
                } else {
                    UnavailableFailure(
                        code = code,
                        retryAfterSeconds = parsed?.retryAfterSeconds ?: retryAfterHeaderSeconds,
                        requestId = resolvedRequestId,
                    )
                }
            }

            else -> UnknownOutcomeFailure(
                code = code,
                detail = detail,
                requestId = resolvedRequestId,
            )
        }
    }
}

package com.kinetix.shop.services.session

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.dto.auth.SessionTokensResponseDto
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.telemetry.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Singleton
class RetrofitTokenRefreshTransport @Inject constructor(
    private val wireExecutor: RefreshWireExecutor,
    private val errorParser: ApiErrorParser,
    private val json: Json,
    private val clock: Clock,
    private val crashReporter: CrashReporter,
) : TokenRefreshTransport {
    override suspend fun refresh(
        refreshToken: String,
    ): RefreshTransportOutcome {
        return when (val wireOutcome = wireExecutor.postRefresh(refreshToken)) {
            is RefreshWireFailure -> {
                if (!wireOutcome.leftDevice) {
                    RefreshTransportTransientError(
                        detail = wireOutcome.throwable.message ?: "Refresh did not leave device",
                        retryAfterSeconds = null,
                    )
                } else {
                    RefreshTransportAmbiguousDrop(
                        detail = wireOutcome.throwable.message ?: "Connection lost after request headers started",
                    )
                }
            }

            is RefreshWireResponse -> classifyResponse(wireOutcome)
        }
    }

    private fun classifyResponse(
        response: RefreshWireResponse,
    ): RefreshTransportOutcome {
        val status = response.statusCode
        val rawBody = response.rawBody
        val retryAfterHeader = response.retryAfterHeaderSeconds

        if (status == 200) {
            if (rawBody.isNullOrBlank()) {
                return RefreshTransportAmbiguousDrop("Empty 200 refresh response")
            }

            return try {
                val dto = json.decodeFromString(
                    SessionTokensResponseDto.serializer(),
                    rawBody,
                )
                RefreshTransportRotated(
                    StoredSession(
                        accessToken = dto.accessToken,
                        refreshToken = dto.refreshToken,
                        expiresAtEpochSeconds = clock.nowEpochSeconds() + dto.expiresIn,
                        accountId = AccountId(dto.user.id),
                        principalId = PrincipalId(dto.user.principalId),
                        email = dto.user.email,
                        role = dto.user.role,
                    )
                )
            } catch (exception: SerializationException) {
                RefreshTransportAmbiguousDrop(
                    exception.message ?: "Unreadable 200 refresh response",
                )
            }
        }

        val parsed = errorParser.parse(rawBody)
        val errorCode = parsed?.error
        val message = errorParser.extractMessage(parsed) ?: "Session refresh failed"

        return when (status) {
            400 -> {
                if (errorCode == "UNKNOWN_CLIENT" || errorCode == "CLIENT_REQUIRED") {
                    reportClientProofFailure(status, errorCode, response.requestId)
                    RefreshTransportTransientError(
                        detail = errorCode,
                        retryAfterSeconds = retryAfterHeader,
                    )
                } else {
                    RefreshTransportRefused(
                        statusCode = status,
                        errorCode = errorCode,
                        message = message,
                    )
                }
            }

            401 -> {
                if (errorCode == "CLIENT_PROOF_REQUIRED" || errorCode == "CLIENT_PROOF_INVALID") {
                    reportClientProofFailure(status, errorCode, response.requestId)
                    RefreshTransportTransientError(
                        detail = errorCode,
                        retryAfterSeconds = retryAfterHeader,
                    )
                } else {
                    RefreshTransportRefused(
                        statusCode = status,
                        errorCode = errorCode,
                        message = message,
                    )
                }
            }

            429 -> RefreshTransportTransientError(
                detail = errorCode ?: "HTTP_429",
                retryAfterSeconds = parsed?.retryAfterSeconds ?: retryAfterHeader,
            )

            503 -> {
                when (errorCode) {
                    "CLIENT_PROOF_UNAVAILABLE" -> {
                        reportClientProofFailure(status, errorCode, response.requestId)
                        RefreshTransportTransientError(
                            detail = errorCode,
                            retryAfterSeconds = retryAfterHeader,
                        )
                    }

                    "DATABASE_UNAVAILABLE" -> RefreshTransportTransientError(
                        detail = errorCode,
                        retryAfterSeconds = retryAfterHeader,
                    )

                    null -> RefreshTransportTransientError(
                        detail = "GATEWAY_503",
                        retryAfterSeconds = retryAfterHeader,
                    )

                    else -> RefreshTransportAmbiguousDrop("HTTP 503 $errorCode on token refresh")
                }
            }

            502 -> {
                if (errorCode == null) {
                    RefreshTransportTransientError(
                        detail = "GATEWAY_502",
                        retryAfterSeconds = retryAfterHeader,
                    )
                } else {
                    RefreshTransportAmbiguousDrop("HTTP 502 $errorCode on token refresh")
                }
            }

            else -> RefreshTransportAmbiguousDrop("HTTP $status on token refresh")
        }
    }

    private fun reportClientProofFailure(
        status: Int,
        errorCode: String,
        requestId: String?,
    ) {
        val context = buildMap {
            put("refresh_status", status.toString())
            put("refresh_error_code", errorCode)
            if (requestId != null) {
                put("request_id", requestId)
            }
        }
        crashReporter.recordNonFatal(
            throwable = IllegalStateException("Client proof refusal on refresh: $errorCode"),
            contextMap = context,
        )
    }
}

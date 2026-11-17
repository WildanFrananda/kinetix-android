package com.kinetix.shop.services.session

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.dto.auth.RefreshTokenRequestDto
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.remote.IdentityApiService
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException
import kotlinx.serialization.SerializationException

@Singleton
class RetrofitTokenRefreshTransport @Inject constructor(
    private val identityApiService: IdentityApiService,
    private val errorParser: ApiErrorParser,
    private val clock: Clock,
) : TokenRefreshTransport {
    override suspend fun refresh(refreshToken: String): RefreshTransportOutcome {
        return try {
            val response = identityApiService.refresh(
                RefreshTokenRequestDto(refreshToken = refreshToken)
            )
            val status = response.code()
            val retryAfterHeader = response.headers()["Retry-After"]?.toIntOrNull()

            when (status) {
                200 -> {
                    val dto = response.body()
                        ?: return RefreshTransportAmbiguousDrop("Empty 200 refresh response")

                    RefreshTransportRotated(
                        StoredSession(
                            accessToken = dto.accessToken,
                            refreshToken = dto.refreshToken,
                            expiresAtEpochSeconds = clock.nowEpochSeconds() + dto.expiresIn,
                            accountId = AccountId(dto.user.id),
                            principalId = PrincipalId(dto.user.principalId),
                            email = dto.user.email,
                        )
                    )
                }
                400, 401, 403 -> {
                    val rawError = response.errorBody()?.string()
                    val parsed = errorParser.parse(rawError)

                    RefreshTransportRefused(
                        statusCode = status,
                        errorCode = parsed?.error,
                        message = errorParser.extractMessage(parsed) ?: "Session refresh refused",
                    )
                }
                429, 502, 503 -> {
                    val rawError = response.errorBody()?.string()
                    val parsed = errorParser.parse(rawError)

                    RefreshTransportTransientError(
                        detail = parsed?.error ?: "HTTP $status",
                        retryAfterSeconds = parsed?.retryAfterSeconds ?: retryAfterHeader,
                    )
                }
                else -> {
                    RefreshTransportAmbiguousDrop("HTTP $status on token refresh")
                }
            }
        } catch (exception: ConnectException) {
            RefreshTransportTransientError(
                detail = exception.message ?: "Connect failed",
                retryAfterSeconds = null,
            )
        } catch (exception: UnknownHostException) {
            RefreshTransportTransientError(
                detail = exception.message ?: "DNS lookup failed",
                retryAfterSeconds = null,
            )
        } catch (exception: SSLException) {
            RefreshTransportTransientError(
                detail = exception.message ?: "TLS handshake failed",
                retryAfterSeconds = null,
            )
        } catch (exception: SerializationException) {
            RefreshTransportAmbiguousDrop(
                exception.message ?: "Unreadable 200 refresh response"
            )
        } catch (exception: IOException) {
            RefreshTransportAmbiguousDrop(
                exception.message ?: "Connection dropped after sending refresh token"
            )
        }
    }
}

package com.kinetix.shop.services.session

import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.services.storage.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SingleFlightSessionRenewer @Inject constructor(
    private val tokenStore: TokenStore,
    private val transport: TokenRefreshTransport,
) : SessionRenewer {
    private val mutex = Mutex()

    override suspend fun renew(failedAccessToken: String?): Result<StoredSession> {
        return mutex.withLock {
            val currentSession = when (val currentSessionResult = tokenStore.read()) {
                is Ok -> currentSessionResult.value
                is Err -> {
                    return@withLock Err(currentSessionResult.failure)
                }
            }

            if (currentSession == null) {
                return@withLock Err(
                    UnauthorizedFailure(
                        code = "NO_SESSION",
                        detail = "No stored session is available to renew",
                    )
                )
            }

            if (failedAccessToken != null && currentSession.accessToken != failedAccessToken) {
                return@withLock Ok(currentSession)
            }

            when (val outcome = transport.refresh(currentSession.refreshToken)) {
                is RefreshTransportRotated -> {
                    when (val writeResult = tokenStore.write(outcome.session)) {
                        is Ok -> Ok(outcome.session)
                        is Err -> Err(writeResult.failure)
                    }
                }

                is RefreshTransportRefused -> {
                    tokenStore.clear()

                    Err(
                        UnauthorizedFailure(
                            code = outcome.errorCode ?: "SESSION_REFUSED_${outcome.statusCode}",
                            detail = outcome.message,
                        )
                    )
                }

                is RefreshTransportAmbiguousDrop -> {
                    tokenStore.clear()

                    Err(
                        UnauthorizedFailure(
                            code = "SESSION_AMBIGUOUS_DROP",
                            detail = outcome.detail,
                        )
                    )
                }

                is RefreshTransportTransientError -> {
                    Err(
                        UnavailableFailure(
                            code = "REFRESH_UNAVAILABLE",
                            retryAfterSeconds = outcome.retryAfterSeconds,
                        )
                    )
                }
            }
        }
    }
}

package com.kinetix.shop.services.session

import com.kinetix.shop.core.concurrency.ApplicationScope
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.services.storage.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SingleFlightSessionRenewer @Inject constructor(
    private val tokenStore: TokenStore,
    private val transport: TokenRefreshTransport,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : SessionRenewer {
    private val mutex = Mutex()
    private var inFlightRefresh: Deferred<Result<StoredSession>>? = null

    override suspend fun renew(
        failedAccessToken: String?,
    ): Result<StoredSession> {
        val deferred = mutex.withLock {
            val active = inFlightRefresh
            if (active != null && active.isActive) {
                active
            } else {
                val created = applicationScope.async {
                    try {
                        performRefresh(failedAccessToken)
                    } finally {
                        mutex.withLock {
                            inFlightRefresh = null
                        }
                    }
                }
                inFlightRefresh = created
                created
            }
        }

        return deferred.await()
    }

    private suspend fun performRefresh(
        failedAccessToken: String?,
    ): Result<StoredSession> {
        val currentSession = when (val currentSessionResult = tokenStore.read()) {
            is Ok -> currentSessionResult.value
            is Err -> {
                return Err(currentSessionResult.failure)
            }
        }

        if (currentSession == null) {
            return Err(
                UnauthorizedFailure(
                    code = "NO_SESSION",
                )
            )
        }

        if (failedAccessToken != null && currentSession.accessToken != failedAccessToken) {
            return Ok(currentSession)
        }

        return when (val outcome = transport.refresh(currentSession.refreshToken)) {
            is RefreshTransportRotated -> {
                when (val writeResult = tokenStore.write(outcome.session)) {
                    is Ok -> Ok(outcome.session)
                    is Err -> {
                        tokenStore.clear()
                        Err(writeResult.failure)
                    }
                }
            }

            is RefreshTransportRefused -> {
                tokenStore.clear()

                Err(
                    UnauthorizedFailure(
                        code = outcome.errorCode ?: "SESSION_REFUSED_${outcome.statusCode}",
                    )
                )
            }

            is RefreshTransportAmbiguousDrop -> {
                tokenStore.clear()

                Err(
                    UnauthorizedFailure(
                        code = "SESSION_AMBIGUOUS_DROP",
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

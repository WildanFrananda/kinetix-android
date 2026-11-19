package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.dto.auth.LogoutRequestDto
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.remote.IdentityApiService
import com.kinetix.shop.services.session.SessionRenewer
import com.kinetix.shop.services.storage.TokenStore
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val apiService: IdentityApiService,
    private val safeApiCaller: SafeApiCaller,
    private val tokenStore: TokenStore,
    private val sessionRenewer: SessionRenewer,
    private val clock: Clock,
) : SessionRepository {
    private val sessionFlow = MutableStateFlow<StoredSession?>(null)

    override val currentSession: StateFlow<StoredSession?> = sessionFlow.asStateFlow()

    override fun updateSession(session: StoredSession?) {
        sessionFlow.value = session
    }

    override suspend fun restoreSession(): Result<StoredSession?> {
        val loaded = when (val readResult = tokenStore.read()) {
            is Ok -> readResult.value
            is Err -> {
                sessionFlow.value = null
                return Err(readResult.failure)
            }
        }
        if (loaded == null) {
            sessionFlow.value = null
            return Ok(null)
        }

        val activeSession = if (needsProactiveRefresh(loaded)) {
            when (val renewed = sessionRenewer.renew(loaded.accessToken)) {
                is Ok -> renewed.value
                is Err -> {
                    val remaining = tokenStore.read()
                    if (remaining is Ok && remaining.value == null) {
                        sessionFlow.value = null
                        return Ok(null)
                    }
                    sessionFlow.value = loaded
                    return Ok(loaded)
                }
            }
        } else {
            loaded
        }

        val verified = verifyMe(activeSession)
        if (verified is Ok) {
            sessionFlow.value = verified.value
            return Ok(verified.value)
        }
        if (verified is Err && verified.failure is UnauthorizedFailure) {
            when (val renewed = sessionRenewer.renew(activeSession.accessToken)) {
                is Ok -> {
                    val rechecked = verifyMe(renewed.value)
                    if (rechecked is Ok) {
                        sessionFlow.value = rechecked.value
                        return Ok(rechecked.value)
                    }
                    tokenStore.clear()
                    sessionFlow.value = null
                    return Ok(null)
                }
                is Err -> {
                    val remaining = tokenStore.read()
                    if (remaining is Ok && remaining.value == null) {
                        sessionFlow.value = null
                        return Ok(null)
                    }
                }
            }
        }

        sessionFlow.value = activeSession
        return Ok(activeSession)
    }

    override suspend fun signOut(): Result<UnitSentinel> {
        val current = sessionFlow.value ?: when (val read = tokenStore.read()) {
            is Ok -> read.value
            is Err -> null
        }

        if (current != null) {
            try {
                withTimeoutOrNull(NetworkPolicy.LOGOUT_TIMEOUT_MILLIS.milliseconds) {
                    apiService.logout(
                        bearerToken = "Bearer ${current.accessToken}",
                        request = LogoutRequestDto(
                            refreshToken = current.refreshToken,
                        ),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
            }
        }

        tokenStore.clear()
        sessionFlow.value = null
        return Ok(UnitSentinel)
    }

    override suspend fun <T> withValidSession(
        action: suspend (StoredSession) -> Result<T>,
    ): Result<T> {
        val baseSession = sessionFlow.value ?: when (val read = tokenStore.read()) {
            is Ok -> read.value
            is Err -> return Err(read.failure)
        } ?: return Err(
            UnauthorizedFailure(
                code = "NOT_SIGNED_IN",
                detail = "",
            )
        )

        val readySession = if (needsProactiveRefresh(baseSession)) {
            when (val renewed = sessionRenewer.renew(baseSession.accessToken)) {
                is Ok -> {
                    sessionFlow.value = renewed.value
                    renewed.value
                }
                is Err -> {
                    val remaining = tokenStore.read()
                    if (remaining is Ok && remaining.value == null) {
                        sessionFlow.value = null
                    }
                    return Err(renewed.failure)
                }
            }
        } else {
            baseSession
        }

        val firstAttempt = action(readySession)
        if (firstAttempt is Err && firstAttempt.failure is UnauthorizedFailure) {
            return when (val renewed = sessionRenewer.renew(readySession.accessToken)) {
                is Ok -> {
                    sessionFlow.value = renewed.value
                    action(renewed.value)
                }
                is Err -> {
                    val remaining = tokenStore.read()
                    if (remaining is Ok && remaining.value == null) {
                        sessionFlow.value = null
                    }
                    Err(renewed.failure)
                }
            }
        }

        return firstAttempt
    }

    private fun needsProactiveRefresh(
        session: StoredSession,
    ): Boolean {
        return (session.expiresAtEpochSeconds - clock.nowEpochSeconds()) <=
            NetworkPolicy.PROACTIVE_REFRESH_MARGIN_SECONDS
    }

    private suspend fun verifyMe(
        session: StoredSession,
    ): Result<StoredSession> {
        return safeApiCaller.execute(
            call = {
                apiService.me(
                    bearerToken = "Bearer ${session.accessToken}",
                )
            },
            transform = { dto ->
                val parsedExpirySeconds = try {
                    Instant.parse(dto.expiresAt).epochSecond
                } catch (_: DateTimeParseException) {
                    session.expiresAtEpochSeconds
                }
                val updated = session.copy(
                    accountId = AccountId(dto.userId),
                    principalId = PrincipalId(dto.principalId),
                    email = dto.email,
                    expiresAtEpochSeconds = parsedExpirySeconds,
                )
                tokenStore.write(updated)
                Ok(updated)
            },
        )
    }
}

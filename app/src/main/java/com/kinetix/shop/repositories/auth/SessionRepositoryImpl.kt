package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.concurrency.ApplicationScope
import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.dto.auth.LogoutRequestDto
import com.kinetix.shop.models.auth.SessionRestoring
import com.kinetix.shop.models.auth.SessionSignedIn
import com.kinetix.shop.models.auth.SessionSignedOut
import com.kinetix.shop.models.auth.SessionState
import com.kinetix.shop.models.auth.SessionUnreadable
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.local.ProfileCacheStore
import com.kinetix.shop.services.remote.IdentityApiService
import com.kinetix.shop.services.session.SessionRenewer
import com.kinetix.shop.services.storage.TokenStore
import com.kinetix.shop.services.telemetry.CrashReporter
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val apiService: IdentityApiService,
    private val safeApiCaller: SafeApiCaller,
    private val tokenStore: TokenStore,
    private val sessionRenewer: SessionRenewer,
    private val profileCacheStore: ProfileCacheStore,
    private val clock: Clock,
    private val crashReporter: CrashReporter,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : SessionRepository {
    private val sessionStateFlow = MutableStateFlow<SessionState>(SessionRestoring)
    private val sessionFlow = MutableStateFlow<StoredSession?>(null)
    private val restoreMutex = Mutex()
    private var inFlightRestore: Deferred<Result<StoredSession?>>? = null

    override val sessionState: StateFlow<SessionState> = sessionStateFlow.asStateFlow()

    override val currentSession: StateFlow<StoredSession?> = sessionFlow.asStateFlow()

    override fun updateSession(session: StoredSession?) {
        if (session == null) {
            setSignedOut()
        } else {
            setSignedIn(session)
        }
    }

    override suspend fun restoreSession(): Result<StoredSession?> {
        val deferred = restoreMutex.withLock {
            val active = inFlightRestore
            if (active != null && active.isActive) {
                active
            } else {
                val created = applicationScope.async {
                    try {
                        performRestore()
                    } finally {
                        restoreMutex.withLock {
                            inFlightRestore = null
                        }
                    }
                }
                inFlightRestore = created
                created
            }
        }

        return deferred.await()
    }

    override suspend fun signOut(): Result<UnitSentinel> {
        return applicationScope.async {
            performSignOut()
        }.await()
    }

    override suspend fun <T> withValidSession(
        action: suspend (StoredSession) -> Result<T>,
    ): Result<T> {
        val baseSession = sessionFlow.value ?: when (val read = tokenStore.read()) {
            is Ok -> read.value
            is Err -> {
                setUnreadable()
                return Err(read.failure)
            }
        } ?: return Err(
            UnauthorizedFailure(
                code = "NOT_SIGNED_IN",
                detail = "",
            )
        )

        val readySession = if (needsProactiveRefresh(baseSession)) {
            when (val renewed = sessionRenewer.renew(baseSession.accessToken)) {
                is Ok -> {
                    setSignedIn(renewed.value)
                    renewed.value
                }

                is Err -> {
                    syncAfterRenewFailure(baseSession.accountId)
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
                    setSignedIn(renewed.value)
                    action(renewed.value)
                }

                is Err -> {
                    syncAfterRenewFailure(readySession.accountId)
                    Err(renewed.failure)
                }
            }
        }

        return firstAttempt
    }

    private suspend fun performRestore(): Result<StoredSession?> {
        val loaded = when (val readResult = tokenStore.read()) {
            is Ok -> readResult.value
            is Err -> {
                setUnreadable()
                return Err(readResult.failure)
            }
        }
        if (loaded == null) {
            setSignedOut()
            return Ok(null)
        }

        val activeSession = if (needsProactiveRefresh(loaded)) {
            when (val renewed = sessionRenewer.renew(loaded.accessToken)) {
                is Ok -> renewed.value
                is Err -> {
                    return when (val remaining = tokenStore.read()) {
                        is Ok -> {
                            val persisted = remaining.value
                            if (persisted == null) {
                                clearCachedProfile(loaded.accountId)
                                setSignedOut()
                                Ok(null)
                            } else {
                                setSignedIn(persisted)
                                Ok(persisted)
                            }
                        }

                        is Err -> {
                            setUnreadable()
                            Err(remaining.failure)
                        }
                    }
                }
            }
        } else {
            loaded
        }

        val verified = verifyMe(activeSession)
        if (verified is Ok) {
            setSignedIn(verified.value)
            return Ok(verified.value)
        }

        if (verified is Err && verified.failure is UnauthorizedFailure) {
            when (val renewed = sessionRenewer.renew(activeSession.accessToken)) {
                is Ok -> {
                    val rechecked = verifyMe(renewed.value)
                    if (rechecked is Ok) {
                        setSignedIn(rechecked.value)
                        return Ok(rechecked.value)
                    }
                    if (rechecked is Err && rechecked.failure is UnauthorizedFailure) {
                        tokenStore.clear()
                        clearCachedProfile(activeSession.accountId)
                        setSignedOut()
                        return Ok(null)
                    }
                    setSignedIn(renewed.value)
                    return Ok(renewed.value)
                }

                is Err -> {
                    return when (val remaining = tokenStore.read()) {
                        is Ok -> {
                            val persisted = remaining.value
                            if (persisted == null) {
                                clearCachedProfile(activeSession.accountId)
                                setSignedOut()
                                Ok(null)
                            } else {
                                setSignedIn(persisted)
                                Ok(persisted)
                            }
                        }

                        is Err -> {
                            setUnreadable()
                            Err(remaining.failure)
                        }
                    }
                }
            }
        }

        setSignedIn(activeSession)
        return Ok(activeSession)
    }

    private suspend fun performSignOut(): Result<UnitSentinel> {
        if (sessionStateFlow.value is SessionUnreadable) {
            return Ok(UnitSentinel)
        }

        val initial = sessionFlow.value ?: when (val read = tokenStore.read()) {
            is Ok -> read.value
            is Err -> {
                setUnreadable()
                return Ok(UnitSentinel)
            }
        }

        if (initial != null) {
            val sessionForLogout = if (needsProactiveRefresh(initial)) {
                when (val renewed = sessionRenewer.renew(initial.accessToken)) {
                    is Ok -> renewed.value
                    is Err -> {
                        if (renewed.failure is UnauthorizedFailure) {
                            clearCachedProfile(initial.accountId)
                            setSignedOut()
                            return Ok(UnitSentinel)
                        }
                        initial
                    }
                }
            } else {
                initial
            }

            val logoutResult = withTimeoutOrNull(NetworkPolicy.LOGOUT_TIMEOUT_MILLIS.milliseconds) {
                executeLogoutWithSingleGatewayRetry(sessionForLogout)
            }
            if (logoutResult == null || logoutResult is Err) {
                crashReporter.recordBreadcrumb(
                    category = "auth",
                    message = "logout_unconfirmed",
                )
            }
            clearCachedProfile(initial.accountId)
        }

        tokenStore.clear()
        setSignedOut()
        return Ok(UnitSentinel)
    }

    private suspend fun executeLogoutWithSingleGatewayRetry(
        session: StoredSession,
    ): Result<UnitSentinel> {
        val firstAttempt = callLogout(session)
        if (firstAttempt is Err) {
            val failure = firstAttempt.failure
            if (failure is UnauthorizedFailure && failure.code == "HTTP_401") {
                val renewed = sessionRenewer.renew(session.accessToken)
                if (renewed is Ok) {
                    return callLogout(renewed.value)
                }
            }
        }

        return firstAttempt
    }

    private suspend fun callLogout(
        session: StoredSession,
    ): Result<UnitSentinel> {
        return safeApiCaller.executeNoContent(
            call = {
                apiService.logout(
                    bearerToken = "Bearer ${session.accessToken}",
                    request = LogoutRequestDto(
                        refreshToken = session.refreshToken,
                    ),
                )
            },
        )
    }

    private suspend fun syncAfterRenewFailure(
        accountId: AccountId,
    ) {
        when (val remaining = tokenStore.read()) {
            is Ok -> {
                if (remaining.value == null) {
                    clearCachedProfile(accountId)
                    setSignedOut()
                }
            }

            is Err -> setUnreadable()
        }
    }

    private suspend fun clearCachedProfile(
        accountId: AccountId,
    ) {
        try {
            profileCacheStore.clearProfile(accountId)
        } catch (_: Exception) {
        }
    }

    private fun setSignedIn(
        session: StoredSession,
    ) {
        sessionFlow.value = session
        sessionStateFlow.value = SessionSignedIn(session)
    }

    private fun setSignedOut() {
        sessionFlow.value = null
        sessionStateFlow.value = SessionSignedOut
    }

    private fun setUnreadable() {
        sessionFlow.value = null
        sessionStateFlow.value = SessionUnreadable
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
                val shortenedExpirySeconds = minOf(
                    session.expiresAtEpochSeconds,
                    parsedExpirySeconds,
                )
                val updated = session.copy(
                    accountId = AccountId(dto.userId),
                    principalId = PrincipalId(dto.principalId),
                    email = dto.email,
                    role = dto.role,
                    expiresAtEpochSeconds = shortenedExpirySeconds,
                )
                tokenStore.write(updated)
                Ok(updated)
            },
        )
    }
}

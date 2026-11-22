package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.auth.SessionSignedIn
import com.kinetix.shop.models.auth.SessionSignedOut
import com.kinetix.shop.models.auth.SessionState
import com.kinetix.shop.models.auth.SessionUnreadable
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.repositories.auth.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSessionRepository(
    initialSession: StoredSession? = null,
) : SessionRepository {
    private val sessionStateFlow = MutableStateFlow<SessionState>(
        if (initialSession != null) {
            SessionSignedIn(initialSession)
        } else {
            SessionSignedOut
        }
    )
    private val sessionFlow = MutableStateFlow(initialSession)

    override val sessionState: StateFlow<SessionState> = sessionStateFlow.asStateFlow()

    override val currentSession: StateFlow<StoredSession?> = sessionFlow.asStateFlow()

    var restoreResult: Result<StoredSession?> = Ok(initialSession)
    var signOutCalls: Int = 0

    override fun updateSession(session: StoredSession?) {
        sessionFlow.value = session
        sessionStateFlow.value = if (session != null) {
            SessionSignedIn(session)
        } else {
            SessionSignedOut
        }
    }

    override suspend fun restoreSession(): Result<StoredSession?> {
        when (val result = restoreResult) {
            is Ok -> updateSession(result.value)
            is Err -> {
                sessionFlow.value = null
                sessionStateFlow.value = SessionUnreadable
            }
        }
        return restoreResult
    }

    override suspend fun signOut(): Result<UnitSentinel> {
        signOutCalls += 1
        updateSession(null)
        return Ok(UnitSentinel)
    }

    override suspend fun <T> withValidSession(
        action: suspend (StoredSession) -> Result<T>,
    ): Result<T> {
        val current = sessionFlow.value ?: return Err(
            UnauthorizedFailure("NOT_SIGNED_IN")
        )
        return action(current)
    }
}

package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.repositories.auth.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSessionRepository(
    initialSession: StoredSession? = null,
) : SessionRepository {
    private val sessionFlow = MutableStateFlow(initialSession)
    override val currentSession: StateFlow<StoredSession?> = sessionFlow.asStateFlow()

    var restoreResult: Result<StoredSession?> = Ok(initialSession)
    var signOutCalls: Int = 0

    override fun updateSession(session: StoredSession?) {
        sessionFlow.value = session
    }

    override suspend fun restoreSession(): Result<StoredSession?> {
        if (restoreResult is Ok) {
            sessionFlow.value = (restoreResult as Ok<StoredSession?>).value
        }
        return restoreResult
    }

    override suspend fun signOut(): Result<UnitSentinel> {
        signOutCalls += 1
        sessionFlow.value = null
        return Ok(UnitSentinel)
    }

    override suspend fun <T> withValidSession(
        action: suspend (StoredSession) -> Result<T>,
    ): Result<T> {
        val current = sessionFlow.value ?: return Err(
            UnauthorizedFailure("NOT_SIGNED_IN", "Not signed in")
        )
        return action(current)
    }
}

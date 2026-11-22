package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.auth.SessionState
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.services.session.SessionExecutionGateway
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository : SessionExecutionGateway {
    override val sessionState: StateFlow<SessionState>

    override val currentSession: StateFlow<StoredSession?>

    fun updateSession(session: StoredSession?)

    override suspend fun restoreSession(): Result<StoredSession?>

    suspend fun signOut(): Result<UnitSentinel>

    override suspend fun <T> withValidSession(
        action: suspend (StoredSession) -> Result<T>,
    ): Result<T>
}

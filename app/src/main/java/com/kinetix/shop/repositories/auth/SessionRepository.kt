package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.auth.SessionState
import com.kinetix.shop.models.auth.StoredSession
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    val currentSession: StateFlow<StoredSession?>

    fun updateSession(session: StoredSession?)

    suspend fun restoreSession(): Result<StoredSession?>

    suspend fun signOut(): Result<UnitSentinel>

    suspend fun <T> withValidSession(
        action: suspend (StoredSession) -> Result<T>,
    ): Result<T>
}

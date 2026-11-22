package com.kinetix.shop.services.session

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.SessionState
import com.kinetix.shop.models.auth.StoredSession
import kotlinx.coroutines.flow.StateFlow

interface SessionExecutionGateway {
    val sessionState: StateFlow<SessionState>

    val currentSession: StateFlow<StoredSession?>

    suspend fun restoreSession(): Result<StoredSession?>

    suspend fun <T> withValidSession(
        action: suspend (StoredSession) -> Result<T>,
    ): Result<T>
}

package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.SignInOutcome
import com.kinetix.shop.models.auth.StoredSession

interface AuthRepository {
    suspend fun register(
        email: String,
        password: String,
    ): Result<StoredSession>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<SignInOutcome>

    suspend fun verifyTwoFactor(
        challengeToken: String,
        code: String,
    ): Result<StoredSession>
}

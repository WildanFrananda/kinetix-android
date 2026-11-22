package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.SignInOutcome
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.repositories.auth.AuthRepository

class FakeAuthRepository : AuthRepository {
    var registerResult: Result<StoredSession> = Err(
        UnauthorizedFailure("NOT_CONFIGURED")
    )
    var signInResult: Result<SignInOutcome> = Err(
        UnauthorizedFailure("NOT_CONFIGURED")
    )
    var verifyTwoFactorResult: Result<StoredSession> = Err(
        UnauthorizedFailure("NOT_CONFIGURED")
    )

    var signInCallCount: Int = 0
    var registerCallCount: Int = 0
    var verifyTwoFactorCallCount: Int = 0

    override suspend fun register(
        email: String,
        password: String,
    ): Result<StoredSession> {
        registerCallCount += 1
        return registerResult
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<SignInOutcome> {
        signInCallCount += 1
        return signInResult
    }

    override suspend fun verifyTwoFactor(
        challengeToken: String,
        code: String,
    ): Result<StoredSession> {
        verifyTwoFactorCallCount += 1
        return verifyTwoFactorResult
    }
}

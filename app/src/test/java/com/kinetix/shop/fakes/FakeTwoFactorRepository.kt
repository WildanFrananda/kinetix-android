package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.TwoFactorSetupSecret
import com.kinetix.shop.repositories.auth.TwoFactorRepository

class FakeTwoFactorRepository : TwoFactorRepository {
    var setupResult: Result<TwoFactorSetupSecret> = Err(
        UnauthorizedFailure("NOT_CONFIGURED")
    )
    var enableResult: Result<Boolean> = Err(
        UnauthorizedFailure("NOT_CONFIGURED")
    )
    var disableResult: Result<Boolean> = Err(
        UnauthorizedFailure("NOT_CONFIGURED")
    )

    var setupCallCount: Int = 0
    var enableCallCount: Int = 0
    var disableCallCount: Int = 0

    override suspend fun setupTwoFactor(
        password: String,
    ): Result<TwoFactorSetupSecret> {
        setupCallCount += 1
        return setupResult
    }

    override suspend fun enableTwoFactor(
        code: String,
        password: String,
    ): Result<Boolean> {
        enableCallCount += 1
        return enableResult
    }

    override suspend fun disableTwoFactor(
        code: String,
        password: String,
    ): Result<Boolean> {
        disableCallCount += 1
        return disableResult
    }
}

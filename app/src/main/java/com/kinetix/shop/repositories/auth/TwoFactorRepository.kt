package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.TwoFactorSetupSecret

interface TwoFactorRepository {
    suspend fun setupTwoFactor(
        password: String,
    ): Result<TwoFactorSetupSecret>

    suspend fun enableTwoFactor(
        code: String,
        password: String,
    ): Result<Boolean>

    suspend fun disableTwoFactor(
        code: String,
        password: String,
    ): Result<Boolean>
}

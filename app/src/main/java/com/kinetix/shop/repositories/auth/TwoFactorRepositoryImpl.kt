package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.WrongPasswordFailure
import com.kinetix.shop.core.failure.WrongTwoFactorCodeFailure
import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.dto.auth.TwoFactorCodePasswordRequestDto
import com.kinetix.shop.dto.auth.TwoFactorPasswordRequestDto
import com.kinetix.shop.models.auth.TwoFactorSetupSecret
import com.kinetix.shop.services.remote.IdentityApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TwoFactorRepositoryImpl @Inject constructor(
    private val apiService: IdentityApiService,
    private val safeApiCaller: SafeApiCaller,
    private val errorParser: ApiErrorParser,
    private val sessionRepository: SessionRepository,
) : TwoFactorRepository {
    override suspend fun setupTwoFactor(
        password: String,
    ): Result<TwoFactorSetupSecret> {
        return sessionRepository.withValidSession { session ->
            safeApiCaller.execute(
                call = {
                    apiService.setupTwoFactor(
                        accountId = session.accountId.value,
                        bearerToken = "Bearer ${session.accessToken}",
                        request = TwoFactorPasswordRequestDto(
                            password = password,
                        ),
                    )
                },
                onErrorOverride = { statusCode, rawErrorBody, retryAfterSeconds ->
                    val parsed = errorParser.parse(rawErrorBody)
                    val errorCode = errorParser.extractErrorCode(parsed)
                    val message = errorParser.extractMessage(parsed).orEmpty()
                    when (statusCode) {
                        400 -> {
                            if (errorCode == "PASSWORD_REQUIRED" || message.contains("password", ignoreCase = true)) {
                                WrongPasswordFailure()
                            } else {
                                null
                            }
                        }
                        401 -> {
                            if (errorParser.isServiceJsonError(parsed)) {
                                WrongPasswordFailure()
                            } else {
                                null
                            }
                        }
                        429 -> SignInDelayedFailure(
                            parsed?.retryAfterSeconds
                                ?: retryAfterSeconds
                                ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS,
                        )
                        else -> null
                    }
                },
                transform = { dto ->
                    Ok(
                        TwoFactorSetupSecret(
                            secret = dto.twoFactorSecret,
                            qrCodeUrl = dto.qrCodeUrl,
                        )
                    )
                },
            )
        }
    }

    override suspend fun enableTwoFactor(
        code: String,
        password: String,
    ): Result<Boolean> {
        return sessionRepository.withValidSession { session ->
            safeApiCaller.execute(
                call = {
                    apiService.enableTwoFactor(
                        accountId = session.accountId.value,
                        bearerToken = "Bearer ${session.accessToken}",
                        request = TwoFactorCodePasswordRequestDto(
                            code = code,
                            password = password,
                        ),
                    )
                },
                onErrorOverride = { statusCode, rawErrorBody, retryAfterSeconds ->
                    val parsed = errorParser.parse(rawErrorBody)
                    val errorCode = errorParser.extractErrorCode(parsed)
                    val message = errorParser.extractMessage(parsed).orEmpty()
                    when (statusCode) {
                        400 -> {
                            if (errorCode == "PASSWORD_REQUIRED" || message.contains("password", ignoreCase = true)) {
                                WrongPasswordFailure()
                            } else {
                                null
                            }
                        }
                        401 -> {
                            if (errorParser.isServiceJsonError(parsed)) {
                                WrongPasswordFailure()
                            } else {
                                null
                            }
                        }
                        422 -> WrongTwoFactorCodeFailure()
                        429 -> SignInDelayedFailure(
                            parsed?.retryAfterSeconds
                                ?: retryAfterSeconds
                                ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS,
                        )
                        else -> null
                    }
                },
                transform = { dto ->
                    Ok(dto.enabled)
                },
            )
        }
    }

    override suspend fun disableTwoFactor(
        code: String,
        password: String,
    ): Result<Boolean> {
        return sessionRepository.withValidSession { session ->
            safeApiCaller.execute(
                call = {
                    apiService.disableTwoFactor(
                        accountId = session.accountId.value,
                        bearerToken = "Bearer ${session.accessToken}",
                        request = TwoFactorCodePasswordRequestDto(
                            code = code,
                            password = password,
                        ),
                    )
                },
                onErrorOverride = { statusCode, rawErrorBody, retryAfterSeconds ->
                    val parsed = errorParser.parse(rawErrorBody)
                    val errorCode = errorParser.extractErrorCode(parsed)
                    val message = errorParser.extractMessage(parsed).orEmpty()
                    when (statusCode) {
                        400 -> {
                            if (errorCode == "PASSWORD_REQUIRED" || message.contains("password", ignoreCase = true)) {
                                WrongPasswordFailure()
                            } else {
                                null
                            }
                        }
                        401 -> {
                            if (errorParser.isServiceJsonError(parsed)) {
                                WrongPasswordFailure()
                            } else {
                                null
                            }
                        }
                        422 -> WrongTwoFactorCodeFailure()
                        429 -> SignInDelayedFailure(
                            parsed?.retryAfterSeconds
                                ?: retryAfterSeconds
                                ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS,
                        )
                        else -> null
                    }
                },
                transform = { dto ->
                    Ok(dto.enabled)
                },
            )
        }
    }
}

package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.failure.AdminAccountNotSupportedFailure
import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.failure.WrongTwoFactorCodeFailure
import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.dto.auth.CredentialsRequestDto
import com.kinetix.shop.dto.auth.SessionTokensResponseDto
import com.kinetix.shop.dto.auth.VerifyTwoFactorRequestDto
import com.kinetix.shop.models.auth.SignInAuthenticated
import com.kinetix.shop.models.auth.SignInMfaChallenge
import com.kinetix.shop.models.auth.SignInOutcome
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.remote.IdentityApiService
import com.kinetix.shop.services.storage.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: IdentityApiService,
    private val safeApiCaller: SafeApiCaller,
    private val errorParser: ApiErrorParser,
    private val tokenStore: TokenStore,
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
) : AuthRepository {
    override suspend fun register(
        email: String,
        password: String,
    ): Result<StoredSession> {
        return safeApiCaller.execute(
            call = {
                apiService.register(
                    request = CredentialsRequestDto(
                        email = email,
                        password = password,
                    )
                )
            },
            onErrorOverride = { statusCode, rawErrorBody, retryAfterSeconds ->
                val parsed = errorParser.parse(rawErrorBody)
                when (statusCode) {
                    409 -> ConflictFailure(
                        code = errorParser.extractErrorCode(parsed) ?: "EMAIL_ALREADY_EXISTS",
                    )
                    429 -> SignInDelayedFailure(
                        parsed?.retryAfterSeconds
                            ?: retryAfterSeconds
                            ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS,
                    )
                    else -> null
                }
            },
            transform = { dto ->
                persistTokens(dto)
            },
        )
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<SignInOutcome> {
        return safeApiCaller.execute(
            call = {
                apiService.login(
                    request = CredentialsRequestDto(
                        email = email,
                        password = password,
                    )
                )
            },
            onErrorOverride = { statusCode, rawErrorBody, retryAfterSeconds ->
                val parsed = errorParser.parse(rawErrorBody)
                if (statusCode == 429) {
                    SignInDelayedFailure(
                        parsed?.retryAfterSeconds
                            ?: retryAfterSeconds
                            ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS,
                    )
                } else {
                    null
                }
            },
            transform = { dto ->
                if (dto.mfaEnrolmentRequired == true) {
                    return@execute Err(AdminAccountNotSupportedFailure)
                }
                if (dto.mfaRequired == true) {
                    val token = dto.challengeToken
                    val expiresIn = dto.expiresIn
                    if (token.isNullOrBlank() || expiresIn == null) {
                        return@execute Err(UnreadableAnswerFailure())
                    }
                    return@execute Ok(
                        SignInMfaChallenge(
                            challengeToken = token,
                            expiresInSeconds = expiresIn,
                            passkeysAvailable = dto.passkeys == true,
                        )
                    )
                }
                val accessToken = dto.accessToken
                val refreshToken = dto.refreshToken
                val expiresIn = dto.expiresIn
                val user = dto.user
                if (
                    accessToken.isNullOrBlank() ||
                    refreshToken.isNullOrBlank() ||
                    expiresIn == null ||
                    user == null
                ) {
                    return@execute Err(UnreadableAnswerFailure())
                }
                val session = StoredSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAtEpochSeconds = clock.nowEpochSeconds() + expiresIn,
                    accountId = AccountId(user.id),
                    principalId = PrincipalId(user.principalId),
                    email = user.email,
                    role = user.role,
                )
                when (val saveResult = tokenStore.write(session)) {
                    is Ok -> {
                        sessionRepository.updateSession(session)
                        Ok(SignInAuthenticated(session))
                    }
                    is Err -> Err(saveResult.failure)
                }
            },
        )
    }

    override suspend fun verifyTwoFactor(
        challengeToken: String,
        code: String,
    ): Result<StoredSession> {
        return safeApiCaller.execute(
            call = {
                apiService.verifyTwoFactor(
                    request = VerifyTwoFactorRequestDto(
                        challengeToken = challengeToken,
                        code = code,
                    )
                )
            },
            onErrorOverride = { statusCode, rawErrorBody, retryAfterSeconds ->
                val parsed = errorParser.parse(rawErrorBody)
                val errorCode = errorParser.extractErrorCode(parsed).orEmpty()
                val message = errorParser.extractMessage(parsed).orEmpty()
                when (statusCode) {
                    429 -> SignInDelayedFailure(
                        parsed?.retryAfterSeconds
                            ?: retryAfterSeconds
                            ?: NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS,
                    )
                    401 -> {
                        if (
                            errorCode == "CHALLENGE_EXHAUSTED" ||
                            errorCode == "CHALLENGE_SPENT" ||
                            errorCode == "CHALLENGE_INVALID" ||
                            message.contains("challenge", ignoreCase = true) ||
                            message.contains("expired", ignoreCase = true) ||
                            message.contains("invalid token", ignoreCase = true)
                        ) {
                            ChallengeSpentFailure(
                                code = errorCode.ifEmpty { "CHALLENGE_SPENT" },
                            )
                        } else {
                            WrongTwoFactorCodeFailure()
                        }
                    }
                    else -> null
                }
            },
            transform = { dto ->
                persistTokens(dto)
            },
        )
    }

    private suspend fun persistTokens(
        dto: SessionTokensResponseDto,
    ): Result<StoredSession> {
        val session = StoredSession(
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
            expiresAtEpochSeconds = clock.nowEpochSeconds() + dto.expiresIn,
            accountId = AccountId(dto.user.id),
            principalId = PrincipalId(dto.user.principalId),
            email = dto.user.email,
            role = dto.user.role,
        )
        return when (val writeResult = tokenStore.write(session)) {
            is Ok -> {
                sessionRepository.updateSession(session)
                Ok(session)
            }
            is Err -> Err(writeResult.failure)
        }
    }
}

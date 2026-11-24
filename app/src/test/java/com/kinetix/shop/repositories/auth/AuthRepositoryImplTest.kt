package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.failure.AdminAccountNotSupportedFailure
import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.WrongTwoFactorCodeFailure
import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.auth.AuthenticatedUserDto
import com.kinetix.shop.dto.auth.LoginResponseDto
import com.kinetix.shop.dto.auth.SessionTokensResponseDto
import com.kinetix.shop.fakes.FakeClock
import com.kinetix.shop.fakes.FakeIdentityApiService
import com.kinetix.shop.fakes.FakeSessionRepository
import com.kinetix.shop.fakes.FakeTokenStore
import com.kinetix.shop.models.auth.SignInAuthenticated
import com.kinetix.shop.models.auth.SignInMfaChallenge
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val errorParser = ApiErrorParser(json)
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(errorParser))

    @Test
    fun signInReturnsMfaChallengeWhenMfaRequiredIsTrue() = runTest {
        val apiService = FakeIdentityApiService().apply {
            loginHandler = {
                Response.success(
                    LoginResponseDto(
                        mfaRequired = true,
                        challengeToken = "chal-token-99",
                        expiresIn = 300L,
                        passkeys = false,
                    )
                )
            }
        }
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            tokenStore = FakeTokenStore(),
            sessionRepository = FakeSessionRepository(),
            clock = FakeClock(),
        )

        val result = repository.signIn("buyer@kinetix.test", "secret")
        assertTrue(result is Ok)
        val outcome = (result as Ok).value
        assertTrue(outcome is SignInMfaChallenge)
        assertEquals("chal-token-99", (outcome as SignInMfaChallenge).challengeToken)
    }

    @Test
    fun signInRejectsAdminMfaEnrolmentWithAdminAccountNotSupportedFailure() = runTest {
        val apiService = FakeIdentityApiService().apply {
            loginHandler = {
                Response.success(
                    LoginResponseDto(
                        mfaEnrolmentRequired = true,
                        enrolmentToken = "enrol-1",
                        userId = 1L,
                    )
                )
            }
        }
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            tokenStore = FakeTokenStore(),
            sessionRepository = FakeSessionRepository(),
            clock = FakeClock(),
        )

        val result = repository.signIn("admin@kinetix.test", "secret")
        assertTrue(result is Err)
        assertEquals(AdminAccountNotSupportedFailure, (result as Err).failure)
    }

    @Test
    fun verifyTwoFactorMapsExpiredChallenge401ToChallengeSpentFailure() = runTest {
        val apiService = FakeIdentityApiService().apply {
            verifyTwoFactorHandler = {
                Response.error(
                    401,
                    """{"error":"CHALLENGE_EXHAUSTED","message":"MFA challenge expired"}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            }
        }
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            tokenStore = FakeTokenStore(),
            sessionRepository = FakeSessionRepository(),
            clock = FakeClock(),
        )

        val result = repository.verifyTwoFactor("chal-1", "123456")
        assertTrue(result is Err)
        assertTrue((result as Err).failure is ChallengeSpentFailure)
    }

    @Test
    fun signInDirectSessionPersistsTokensWithRoleAndReceiptTimeExpiry() = runTest {
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val tokenStore = FakeTokenStore()
        val sessionRepository = FakeSessionRepository()
        val apiService = FakeIdentityApiService().apply {
            loginHandler = {
                Response.success(
                    LoginResponseDto(
                        accessToken = "acc-direct",
                        refreshToken = "ref-direct",
                        expiresIn = 900L,
                        tokenType = "Bearer",
                        user = AuthenticatedUserDto(
                            id = 42L,
                            email = "buyer@kinetix.test",
                            role = "customer",
                            principalId = "usr-42",
                        ),
                    )
                )
            }
        }
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            tokenStore = tokenStore,
            sessionRepository = sessionRepository,
            clock = clock,
        )

        val result = repository.signIn("buyer@kinetix.test", "secret123")

        assertTrue(result is Ok)
        val outcome = (result as Ok).value
        assertTrue(outcome is SignInAuthenticated)
        val session = (outcome as SignInAuthenticated).session
        assertEquals("acc-direct", session.accessToken)
        assertEquals("customer", session.role)
        assertEquals(AccountId(42L), session.accountId)
        assertEquals(PrincipalId("usr-42"), session.principalId)
        assertEquals(1_700_000_900L, session.expiresAtEpochSeconds)
        assertEquals(session, tokenStore.current)
        assertEquals(session, sessionRepository.currentSession.value)
    }

    @Test
    fun signInMaps401WrongPasswordToUnauthorizedAnd429ToSignInDelayed() = runTest {
        val apiService = FakeIdentityApiService().apply {
            loginHandler = {
                Response.error(
                    401,
                    """{"error":"INVALID_CREDENTIALS","message":"Invalid email or password"}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            }
        }
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            tokenStore = FakeTokenStore(),
            sessionRepository = FakeSessionRepository(),
            clock = FakeClock(),
        )

        val unauthorized = repository.signIn("buyer@kinetix.test", "wrong")
        assertTrue(unauthorized is Err)
        assertTrue((unauthorized as Err).failure is UnauthorizedFailure)

        apiService.loginHandler = {
            Response.error(
                429,
                """{"error":"SIGN_IN_DELAYED","retryAfterSeconds":12}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val delayed = repository.signIn("buyer@kinetix.test", "wrong")
        assertTrue(delayed is Err)
        val failure = (delayed as Err).failure
        assertTrue(failure is SignInDelayedFailure)
        assertEquals(12, (failure as SignInDelayedFailure).retryAfterSeconds)
    }

    @Test
    fun verifyTwoFactorMapsWrongCodeToWrongTwoFactorCodeFailureAnd200ToSession() = runTest {
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val tokenStore = FakeTokenStore()
        val sessionRepository = FakeSessionRepository()
        val apiService = FakeIdentityApiService().apply {
            verifyTwoFactorHandler = {
                Response.error(
                    401,
                    """{"error":"WRONG_CODE","message":"Incorrect verification code"}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            }
        }
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            tokenStore = tokenStore,
            sessionRepository = sessionRepository,
            clock = clock,
        )

        val wrongCodeResult = repository.verifyTwoFactor("chal-open", "000000")
        assertTrue(wrongCodeResult is Err)
        assertTrue((wrongCodeResult as Err).failure is WrongTwoFactorCodeFailure)

        apiService.verifyTwoFactorHandler = {
            Response.success(
                SessionTokensResponseDto(
                    accessToken = "acc-2fa",
                    refreshToken = "ref-2fa",
                    expiresIn = 900L,
                    tokenType = "Bearer",
                    user = AuthenticatedUserDto(
                        id = 42L,
                        email = "buyer@kinetix.test",
                        role = "customer",
                        principalId = "usr-42",
                    ),
                )
            )
        }
        val verified = repository.verifyTwoFactor("chal-open", "654321")
        assertTrue(verified is Ok)
        assertEquals("acc-2fa", (verified as Ok).value.accessToken)
        assertEquals(verified.value, tokenStore.current)
        assertEquals(verified.value, sessionRepository.currentSession.value)
    }

    @Test
    fun registerPersistsCreatedSessionAndMaps409Conflict() = runTest {
        val clock = FakeClock(currentEpochSeconds = 1_700_000_000L)
        val tokenStore = FakeTokenStore()
        val sessionRepository = FakeSessionRepository()
        val apiService = FakeIdentityApiService().apply {
            registerHandler = {
                Response.success(
                    201,
                    SessionTokensResponseDto(
                        accessToken = "acc-reg",
                        refreshToken = "ref-reg",
                        expiresIn = 900L,
                        tokenType = "Bearer",
                        user = AuthenticatedUserDto(
                            id = 99L,
                            email = "new@kinetix.test",
                            role = "customer",
                            principalId = "usr-99",
                        ),
                    ),
                )
            }
        }
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            tokenStore = tokenStore,
            sessionRepository = sessionRepository,
            clock = clock,
        )

        val created = repository.register("new@kinetix.test", "StrongPass123!")
        assertTrue(created is Ok)
        val session = (created as Ok).value
        assertEquals(AccountId(99L), session.accountId)
        assertEquals(PrincipalId("usr-99"), session.principalId)
        assertEquals("customer", session.role)
        assertEquals(session, tokenStore.current)
        assertEquals(session, sessionRepository.currentSession.value)

        apiService.registerHandler = {
            Response.error(
                409,
                """{"error":"EMAIL_ALREADY_EXISTS","message":"Email already registered"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val conflict = repository.register("new@kinetix.test", "StrongPass123!")
        assertTrue(conflict is Err)
        val failure = (conflict as Err).failure
        assertTrue(failure is ConflictFailure)
        assertEquals("EMAIL_ALREADY_EXISTS", (failure as ConflictFailure).code)
    }
}

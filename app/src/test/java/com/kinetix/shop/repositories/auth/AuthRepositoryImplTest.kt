package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.failure.AdminAccountNotSupportedFailure
import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.auth.LoginResponseDto
import com.kinetix.shop.fakes.FakeClock
import com.kinetix.shop.fakes.FakeIdentityApiService
import com.kinetix.shop.fakes.FakeSessionRepository
import com.kinetix.shop.fakes.FakeTokenStore
import com.kinetix.shop.models.auth.SignInMfaChallenge
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
}

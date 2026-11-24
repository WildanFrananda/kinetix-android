package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.WrongPasswordFailure
import com.kinetix.shop.core.failure.WrongTwoFactorCodeFailure
import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.auth.TwoFactorSetupResponseDto
import com.kinetix.shop.dto.auth.TwoFactorStatusResponseDto
import com.kinetix.shop.fakes.FakeClock
import com.kinetix.shop.fakes.FakeCrashReporter
import com.kinetix.shop.fakes.FakeIdentityApiService
import com.kinetix.shop.fakes.FakeProfileCacheStore
import com.kinetix.shop.fakes.FakeSessionRenewer
import com.kinetix.shop.fakes.FakeSessionRepository
import com.kinetix.shop.fakes.FakeTokenStore
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TwoFactorRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val errorParser = ApiErrorParser(json)
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(errorParser))

    @Test
    fun setupTwoFactorReturnsSecretMapsIdentity401ToWrongPasswordAndRenewsOnGateway401() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val initialSession = StoredSession(
            accessToken = "acc-expired",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(15L),
            principalId = PrincipalId("usr-15"),
            email = "buyer@kinetix.test",
        )
        val renewedSession = initialSession.copy(
            accessToken = "acc-fresh",
            refreshToken = "ref-2",
        )
        val renewer = FakeSessionRenewer(nextResult = Ok(renewedSession))
        val sessionRepository = SessionRepositoryImpl(
            apiService = FakeIdentityApiService(),
            safeApiCaller = safeApiCaller,
            tokenStore = FakeTokenStore(current = initialSession),
            sessionRenewer = renewer,
            profileCacheStore = FakeProfileCacheStore(),
            clock = FakeClock(currentEpochSeconds = 1_700_000_000L),
            crashReporter = FakeCrashReporter(),
            applicationScope = appScope,
        )
        sessionRepository.updateSession(initialSession)

        val apiService = FakeIdentityApiService().apply {
            setupTwoFactorHandler = { _, bearer, _ ->
                if (bearer == "Bearer acc-expired") {
                    Response.error(
                        401,
                        "Jwt is expired".toResponseBody("text/plain".toMediaType()),
                    )
                } else {
                    Response.success(
                        TwoFactorSetupResponseDto(
                            twoFactorSecret = "JBSWY3DPEHPK3PXP",
                            qrCodeUrl = "otpauth://totp/Kinetix:buyer@kinetix.test?secret=JBSWY3DPEHPK3PXP",
                            enabled = false,
                        )
                    )
                }
            }
        }
        val repository = TwoFactorRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            sessionRepository = sessionRepository,
        )

        val gatewayRenewResult = repository.setupTwoFactor("password123")
        assertTrue(gatewayRenewResult is Ok)
        assertEquals("JBSWY3DPEHPK3PXP", (gatewayRenewResult as Ok).value.secret)
        assertEquals(1, renewer.renewCalls)

        apiService.setupTwoFactorHandler = { _, _, _ ->
            Response.error(
                401,
                """{"statusCode":401,"message":"Invalid password"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val wrongPasswordResult = repository.setupTwoFactor("bad-password")
        assertTrue(wrongPasswordResult is Err)
        assertTrue((wrongPasswordResult as Err).failure is WrongPasswordFailure)
        assertEquals(1, renewer.renewCalls)
        assertEquals(renewedSession, sessionRepository.currentSession.value)

        apiService.setupTwoFactorHandler = { _, _, _ ->
            Response.error(
                409,
                """{"statusCode":409,"message":"2FA is already enabled"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val conflictResult = repository.setupTwoFactor("password123")
        assertTrue(conflictResult is Err)
        assertTrue((conflictResult as Err).failure is ConflictFailure)
        appScope.cancel()
    }

    @Test
    fun enableTwoFactorReturnsEnabledTrueAndDistinguishes422From400PasswordRequiredAnd429() = runTest {
        val initialSession = StoredSession(
            accessToken = "acc-1",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(15L),
            principalId = PrincipalId("usr-15"),
            email = "buyer@kinetix.test",
        )
        val sessionRepository = FakeSessionRepository(initialSession = initialSession)
        val apiService = FakeIdentityApiService().apply {
            enableTwoFactorHandler = { _, _, _ ->
                Response.success(TwoFactorStatusResponseDto(enabled = true))
            }
        }
        val repository = TwoFactorRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            sessionRepository = sessionRepository,
        )

        val result = repository.enableTwoFactor("123456", "password123")
        assertTrue(result is Ok)
        assertEquals(true, (result as Ok).value)

        apiService.enableTwoFactorHandler = { _, _, _ ->
            Response.error(
                400,
                """{"error":"PASSWORD_REQUIRED","message":"Password is required"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val passwordRequired = repository.enableTwoFactor("123456", "")
        assertTrue(passwordRequired is Err)
        assertTrue((passwordRequired as Err).failure is WrongPasswordFailure)

        apiService.enableTwoFactorHandler = { _, _, _ ->
            Response.error(
                422,
                """{"statusCode":422,"message":"Invalid verification code"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val wrongCode = repository.enableTwoFactor("000000", "password123")
        assertTrue(wrongCode is Err)
        assertTrue((wrongCode as Err).failure is WrongTwoFactorCodeFailure)

        apiService.enableTwoFactorHandler = { _, _, _ ->
            Response.error(
                429,
                """{"error":"SIGN_IN_DELAYED","retryAfterSeconds":9}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val delayed = repository.enableTwoFactor("000000", "password123")
        assertTrue(delayed is Err)
        val delayedFailure = (delayed as Err).failure
        assertTrue(delayedFailure is SignInDelayedFailure)
        assertEquals(9, (delayedFailure as SignInDelayedFailure).retryAfterSeconds)
    }

    @Test
    fun disableTwoFactorRenewsOnGateway401KeepsSessionOnIdentity401AndMaps422And409() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val initialSession = StoredSession(
            accessToken = "acc-old",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(15L),
            principalId = PrincipalId("usr-15"),
            email = "buyer@kinetix.test",
        )
        val renewedSession = initialSession.copy(
            accessToken = "acc-new",
            refreshToken = "ref-2",
        )
        val renewer = FakeSessionRenewer(nextResult = Ok(renewedSession))
        val sessionRepository = SessionRepositoryImpl(
            apiService = FakeIdentityApiService(),
            safeApiCaller = safeApiCaller,
            tokenStore = FakeTokenStore(current = initialSession),
            sessionRenewer = renewer,
            profileCacheStore = FakeProfileCacheStore(),
            clock = FakeClock(currentEpochSeconds = 1_700_000_000L),
            crashReporter = FakeCrashReporter(),
            applicationScope = appScope,
        )
        sessionRepository.updateSession(initialSession)

        val apiService = FakeIdentityApiService().apply {
            disableTwoFactorHandler = { _, bearer, _ ->
                if (bearer == "Bearer acc-old") {
                    Response.error(
                        401,
                        "".toResponseBody("text/plain".toMediaType()),
                    )
                } else {
                    Response.success(TwoFactorStatusResponseDto(enabled = false))
                }
            }
        }
        val repository = TwoFactorRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            errorParser = errorParser,
            sessionRepository = sessionRepository,
        )

        val disabled = repository.disableTwoFactor("123456", "password123")
        assertTrue(disabled is Ok)
        assertEquals(false, (disabled as Ok).value)
        assertEquals(1, renewer.renewCalls)

        apiService.disableTwoFactorHandler = { _, _, _ ->
            Response.error(
                401,
                """{"statusCode":401,"message":"Invalid password"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val identityWrongPassword = repository.disableTwoFactor("123456", "wrong")
        assertTrue(identityWrongPassword is Err)
        assertTrue((identityWrongPassword as Err).failure is WrongPasswordFailure)
        assertEquals(1, renewer.renewCalls)

        apiService.disableTwoFactorHandler = { _, _, _ ->
            Response.error(
                422,
                """{"statusCode":422,"message":"Invalid code"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val wrongCode = repository.disableTwoFactor("000000", "password123")
        assertTrue(wrongCode is Err)
        assertTrue((wrongCode as Err).failure is WrongTwoFactorCodeFailure)

        apiService.disableTwoFactorHandler = { _, _, _ ->
            Response.error(
                409,
                """{"statusCode":409,"message":"2FA is not enabled"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val conflict = repository.disableTwoFactor("123456", "password123")
        assertTrue(conflict is Err)
        assertTrue((conflict as Err).failure is ConflictFailure)
        appScope.cancel()
    }
}

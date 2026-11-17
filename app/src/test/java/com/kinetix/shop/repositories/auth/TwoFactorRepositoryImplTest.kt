package com.kinetix.shop.repositories.auth

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.auth.TwoFactorStatusResponseDto
import com.kinetix.shop.fakes.FakeIdentityApiService
import com.kinetix.shop.fakes.FakeSessionRepository
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class TwoFactorRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val errorParser = ApiErrorParser(json)
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(errorParser))

    @Test
    fun enableTwoFactorReturnsEnabledTrueWhenBackendConfirms() = runTest {
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
    }
}

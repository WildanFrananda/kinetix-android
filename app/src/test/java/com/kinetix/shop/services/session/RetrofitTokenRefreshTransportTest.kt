package com.kinetix.shop.services.session

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.dto.auth.AuthenticatedUserDto
import com.kinetix.shop.dto.auth.SessionTokensResponseDto
import com.kinetix.shop.fakes.FakeClock
import com.kinetix.shop.fakes.FakeIdentityApiService
import java.io.IOException
import java.net.ConnectException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RetrofitTokenRefreshTransportTest {
    private val errorParser = ApiErrorParser(
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    )

    @Test
    fun maps200ToRotatedSessionAndComputesExpiry() = runTest {
        val apiService = FakeIdentityApiService().apply {
            refreshHandler = {
                Response.success(
                    SessionTokensResponseDto(
                        accessToken = "new-access",
                        refreshToken = "new-refresh",
                        expiresIn = 900L,
                        tokenType = "Bearer",
                        user = AuthenticatedUserDto(
                            id = 7L,
                            email = "buyer@kinetix.test",
                            role = "customer",
                            principalId = "usr-7",
                        ),
                    )
                )
            }
        }
        val transport = RetrofitTokenRefreshTransport(
            identityApiService = apiService,
            errorParser = errorParser,
            clock = FakeClock(currentEpochSeconds = 1_700_000_000L),
        )

        val outcome = transport.refresh("old-refresh")
        assertTrue(outcome is RefreshTransportRotated)
        val rotated = (outcome as RefreshTransportRotated).session
        assertEquals("new-access", rotated.accessToken)
        assertEquals(1_700_000_900L, rotated.expiresAtEpochSeconds)
    }

    @Test
    fun distinguishes401Refused503TransientAndIoDropAmbiguous() = runTest {
        val apiService = FakeIdentityApiService()
        val transport = RetrofitTokenRefreshTransport(
            identityApiService = apiService,
            errorParser = errorParser,
            clock = FakeClock(),
        )

        apiService.refreshHandler = {
            Response.error(
                401,
                """{"error":"TOKEN_REVOKED","message":"Session revoked"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        assertTrue(transport.refresh("r1") is RefreshTransportRefused)

        apiService.refreshHandler = {
            Response.error(
                503,
                """{"error":"UNAVAILABLE"}""".toResponseBody("application/json".toMediaType()),
            )
        }
        assertTrue(transport.refresh("r2") is RefreshTransportTransientError)

        apiService.refreshHandler = { throw ConnectException("refused before transmit") }
        assertTrue(transport.refresh("r3") is RefreshTransportTransientError)

        apiService.refreshHandler = { throw IOException("reset after transmit") }
        assertTrue(transport.refresh("r4") is RefreshTransportAmbiguousDrop)
    }
}

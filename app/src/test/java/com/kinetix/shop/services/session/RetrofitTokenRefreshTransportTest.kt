package com.kinetix.shop.services.session

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.fakes.FakeClock
import com.kinetix.shop.fakes.FakeCrashReporter
import com.kinetix.shop.fakes.FakeRefreshWireExecutor
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrofitTokenRefreshTransportTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val errorParser = ApiErrorParser(json)

    @Test
    fun maps200ToRotatedSessionWithRoleAndReceiptTimeExpiry() = runTest {
        val executor = FakeRefreshWireExecutor {
            RefreshWireResponse(
                statusCode = 200,
                rawBody = """{"accessToken":"new-access","refreshToken":"new-refresh","expiresIn":900,"tokenType":"Bearer","user":{"id":7,"email":"buyer@kinetix.test","role":"customer","principalId":"usr-7"}}""",
                retryAfterHeaderSeconds = null,
                requestId = "req-1",
                leftDevice = true,
            )
        }
        val transport = RetrofitTokenRefreshTransport(
            wireExecutor = executor,
            errorParser = errorParser,
            json = json,
            clock = FakeClock(currentEpochSeconds = 1_700_000_000L),
            crashReporter = FakeCrashReporter(),
        )

        val outcome = transport.refresh("old-refresh")
        assertTrue(outcome is RefreshTransportRotated)
        val rotated = (outcome as RefreshTransportRotated).session
        assertEquals("new-access", rotated.accessToken)
        assertEquals("customer", rotated.role)
        assertEquals(1_700_000_900L, rotated.expiresAtEpochSeconds)
    }

    @Test
    fun classifiesRefreshOutcomesStrictlyByErrorCodeAndWireEvents() = runTest {
        val crashReporter = FakeCrashReporter()
        val executor = FakeRefreshWireExecutor {
            RefreshWireResponse(401, """{"error":"TOKEN_REVOKED","message":"Revoked"}""", null, "r1", true)
        }
        val transport = RetrofitTokenRefreshTransport(
            wireExecutor = executor,
            errorParser = errorParser,
            json = json,
            clock = FakeClock(),
            crashReporter = crashReporter,
        )

        assertTrue(transport.refresh("t1") is RefreshTransportRefused)

        executor.handler = {
            RefreshWireResponse(401, """{"error":"CLIENT_MISMATCH","message":"Mismatch"}""", null, "r2", true)
        }
        assertTrue(transport.refresh("t2") is RefreshTransportRefused)

        executor.handler = {
            RefreshWireResponse(400, """{"error":"INVALID_REQUEST","message":"Bad refresh token"}""", null, "r2b", true)
        }
        assertTrue(transport.refresh("t2b") is RefreshTransportRefused)

        val clientProofResponses = listOf(
            400 to "UNKNOWN_CLIENT",
            400 to "CLIENT_REQUIRED",
            401 to "CLIENT_PROOF_REQUIRED",
            401 to "CLIENT_PROOF_INVALID",
            503 to "CLIENT_PROOF_UNAVAILABLE",
        )
        for ((status, code) in clientProofResponses) {
            executor.handler = {
                RefreshWireResponse(status, """{"error":"$code","message":"Proof"}""", null, "req-$code", true)
            }
            assertTrue(
                "Expected transient for $status $code",
                transport.refresh("tp") is RefreshTransportTransientError,
            )
        }
        assertEquals(5, crashReporter.nonFatalExceptions.size)

        executor.handler = {
            RefreshWireResponse(503, """{"error":"DATABASE_UNAVAILABLE"}""", null, "r3", true)
        }
        assertTrue(transport.refresh("t3") is RefreshTransportTransientError)

        executor.handler = {
            RefreshWireResponse(429, """{"error":"REQUEST_REFUSED"}""", 15, "r4", true)
        }
        val rateLimited = transport.refresh("t4")
        assertTrue(rateLimited is RefreshTransportTransientError)
        assertEquals(15, (rateLimited as RefreshTransportTransientError).retryAfterSeconds)

        executor.handler = {
            RefreshWireResponse(503, "upstream connect error", null, "r5", true)
        }
        assertTrue(transport.refresh("t5") is RefreshTransportTransientError)

        for (unknownStatus in listOf(403, 500, 504)) {
            executor.handler = {
                RefreshWireResponse(unknownStatus, """{"error":"OTHER"}""", null, "ru", true)
            }
            assertTrue(
                "Expected ambiguous for $unknownStatus",
                transport.refresh("tu") is RefreshTransportAmbiguousDrop,
            )
        }

        executor.handler = {
            RefreshWireFailure(SocketTimeoutException("connect timeout"), leftDevice = false)
        }
        assertTrue(transport.refresh("t6") is RefreshTransportTransientError)

        executor.handler = {
            RefreshWireFailure(IOException("reset after write"), leftDevice = true)
        }
        assertTrue(transport.refresh("t7") is RefreshTransportAmbiguousDrop)
    }
}

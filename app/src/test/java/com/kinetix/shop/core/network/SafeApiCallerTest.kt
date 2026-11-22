package com.kinetix.shop.core.network

import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.failure.WrongPasswordFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeCrashReporter
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SafeApiCallerTest {
    private val caller = SafeApiCaller(
        HttpFailureMapper(
            ApiErrorParser(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        )
    )

    @Test
    fun mapsSocketTimeoutSerializationAndIoExceptions() = runTest {
        val timeoutResult = caller.execute<String, String>(
            call = { throw SocketTimeoutException("read timed out") },
            transform = { Ok(it) },
        )
        assertEquals(Err(TimeoutFailure), timeoutResult)

        val serializationResult = caller.execute<String, String>(
            call = { throw SerializationException("bad json") },
            transform = { Ok(it) },
        )
        assertTrue((serializationResult as Err).failure is UnreadableAnswerFailure)

        val networkResult = caller.execute<String, String>(
            call = { throw IOException("no route to host") },
            transform = { Ok(it) },
        )
        assertTrue((networkResult as Err).failure is NetworkFailure)
    }

    @Test
    fun executeNullableInvokesOnNullSuccessFor200WithNullBody() = runTest {
        val result = caller.executeNullable<String, String?>(
            call = { Response.success(null) },
            onNullSuccess = { Ok(null) },
            transform = { Ok(it) },
        )
        assertEquals(Ok(null), result)
    }

    @Test
    fun attachesTraceIdOrRequestIdHeaderToMappedFailure() = runTest {
        val bodyTraceResult = caller.execute<String, String>(
            call = {
                Response.error<String>(
                    404,
                    """{"error":"NOT_FOUND","message":"Missing","traceId":"body-trace-1"}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            },
            transform = { Ok(it) },
        )
        assertEquals("body-trace-1", (bodyTraceResult as Err).failure.requestId)

        val headerFallbackResult = caller.execute<String, String>(
            call = {
                val rawResponse = okhttp3.Response.Builder()
                    .request(okhttp3.Request.Builder().url("https://api.kinetix.invalid/test").build())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(503)
                    .message("Service Unavailable")
                    .headers(Headers.headersOf("X-Request-Id", "hdr-req-2"))
                    .build()
                Response.error<String>(
                    "upstream unavailable".toResponseBody("text/plain".toMediaType()),
                    rawResponse,
                )
            },
            transform = { Ok(it) },
        )
        assertEquals("hdr-req-2", (headerFallbackResult as Err).failure.requestId)

        val blankTraceIdResult = caller.execute<String, String>(
            call = {
                val rawResponse = okhttp3.Response.Builder()
                    .request(okhttp3.Request.Builder().url("https://api.kinetix.invalid/test").build())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(400)
                    .message("Bad Request")
                    .headers(Headers.headersOf("X-Request-Id", "hdr-req-3"))
                    .build()
                Response.error<String>(
                    """{"error":"BAD_REQUEST","message":"Invalid","traceId":""}"""
                        .toResponseBody("application/json".toMediaType()),
                    rawResponse,
                )
            },
            transform = { Ok(it) },
        )
        assertEquals("hdr-req-3", (blankTraceIdResult as Err).failure.requestId)

        val overrideResult = caller.execute<String, String>(
            call = {
                val rawResponse = okhttp3.Response.Builder()
                    .request(okhttp3.Request.Builder().url("https://api.kinetix.invalid/test").build())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .headers(Headers.headersOf("X-Request-Id", "hdr-req-4"))
                    .build()
                Response.error<String>(
                    """{"error":"WRONG_PASSWORD","message":"Invalid"}"""
                        .toResponseBody("application/json".toMediaType()),
                    rawResponse,
                )
            },
            onErrorOverride = { _, _, _ -> WrongPasswordFailure() },
            transform = { Ok(it) },
        )
        assertEquals("hdr-req-4", (overrideResult as Err).failure.requestId)
    }

    @Test
    fun attachesLatestRequestIdWhenTransportThrowsIoException() = runTest {
        val crashReporter = FakeCrashReporter()
        crashReporter.recordRequest(
            method = "GET",
            routeTemplate = "api/v1/products",
            requestId = "req-io-99",
        )
        val trackedCaller = SafeApiCaller(
            failureMapper = HttpFailureMapper(
                ApiErrorParser(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }
                )
            ),
            correlationRecorder = crashReporter,
        )

        val result = trackedCaller.execute<String, String>(
            call = { throw IOException("connection reset") },
            transform = { Ok(it) },
        )

        assertEquals("req-io-99", (result as Err).failure.requestId)
    }
}

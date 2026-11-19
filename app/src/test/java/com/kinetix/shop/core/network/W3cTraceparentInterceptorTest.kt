package com.kinetix.shop.core.network

import com.kinetix.shop.fakes.FakeCrashReporter
import com.kinetix.shop.fakes.FakeTraceIdGenerator
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class W3cTraceparentInterceptorTest {
    @Test
    fun injectsValidW3cTraceparentHeaderAndRecordsCrashBreadcrumb() {
        val crashReporter = FakeCrashReporter()
        val interceptor = W3cTraceparentInterceptor(
            traceIdGenerator = FakeTraceIdGenerator(
                traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
                spanId = "00f067aa0ba902b7",
            ),
            crashReporter = crashReporter,
        )
        var capturedRequest: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                val req = chain.request()
                capturedRequest = req
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()

        client.newCall(
            Request.Builder()
                .url("https://api.kinetix.invalid/api/v1/catalog/products")
                .build()
        ).execute().close()

        assertEquals(
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
            capturedRequest?.header("traceparent"),
        )
        assertEquals(
            "4bf92f3577b34da6a3ce929d0e0e4736",
            crashReporter.customKeys[W3cTraceparentInterceptor.KEY_LAST_TRACE_ID],
        )
        assertEquals(1, crashReporter.breadcrumbs.size)
        assertEquals("http", crashReporter.breadcrumbs.first().first)
    }

    @Test
    fun secureRandomTraceIdGeneratorProducesValidNonZeroHexIds() {
        val generator = SecureRandomTraceIdGenerator()
        val traceId = generator.newTraceIdHex()
        val spanId = generator.newSpanIdHex()

        assertEquals(32, traceId.length)
        assertEquals(16, spanId.length)
        assertTrue(traceId.matches(Regex("^[0-9a-f]{32}$")))
        assertTrue(spanId.matches(Regex("^[0-9a-f]{16}$")))
        assertTrue(traceId.any { it != '0' })
        assertTrue(spanId.any { it != '0' })
    }
}

package com.kinetix.shop.core.network

import com.kinetix.shop.fakes.FakeCrashReporter
import com.kinetix.shop.fakes.FakeRequestIdGenerator
import com.kinetix.shop.services.remote.CatalogApiService
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Invocation

class RequestCorrelationInterceptorTest {
    @Test
    fun injectsRequestIdAndAcceptHeadersAndRecordsRouteTemplateWithoutConcretePathIds() {
        val crashReporter = FakeCrashReporter()
        val expectedRequestId = "11111111-2222-4333-8444-555555555555"
        val interceptor = RequestCorrelationInterceptor(
            requestIdGenerator = FakeRequestIdGenerator(expectedRequestId),
            correlationRecorder = crashReporter,
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

        val method = CatalogApiService::class.java.getMethod(
            "getProduct",
            String::class.java,
            kotlin.coroutines.Continuation::class.java,
        )
        val service = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.kinetix.invalid/")
            .build()
            .create(CatalogApiService::class.java)
        val invocation = Invocation.of(
            CatalogApiService::class.java,
            service,
            method,
            listOf("SKU-SECRET-999"),
        )

        client.newCall(
            Request.Builder()
                .url("https://api.kinetix.invalid/api/v1/products/SKU-SECRET-999")
                .tag(Invocation::class.java, invocation)
                .build()
        ).execute().close()

        assertEquals(expectedRequestId, capturedRequest?.header("X-Request-Id"))
        assertEquals("application/json", capturedRequest?.header("Accept"))
        assertNull(capturedRequest?.header("traceparent"))
        assertEquals(
            expectedRequestId,
            crashReporter.customKeys[RequestCorrelationInterceptor.KEY_LAST_REQUEST_ID],
        )
        assertEquals(1, crashReporter.breadcrumbs.size)
        val breadcrumbMessage = crashReporter.breadcrumbs.first().second
        assertTrue(breadcrumbMessage.contains("api/v1/products/{sku}"))
        assertFalse(breadcrumbMessage.contains("SKU-SECRET-999"))
    }

    @Test
    fun recordsUntaggedMarkerWhenRetrofitInvocationIsAbsent() {
        val crashReporter = FakeCrashReporter()
        val interceptor = RequestCorrelationInterceptor(
            requestIdGenerator = FakeRequestIdGenerator(),
            correlationRecorder = crashReporter,
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()

        client.newCall(
            Request.Builder()
                .url("https://api.kinetix.invalid/api/v1/users/42/profile")
                .build()
        ).execute().close()

        val breadcrumbMessage = crashReporter.breadcrumbs.first().second
        assertTrue(breadcrumbMessage.contains(RequestCorrelationInterceptor.UNTAGGED_ROUTE))
        assertFalse(breadcrumbMessage.contains("/42/"))
    }

    @Test
    fun uuidRequestIdGeneratorProducesValidUuidStrings() {
        val generator = UuidRequestIdGenerator()
        val first = generator.newRequestId()
        val second = generator.newRequestId()

        assertEquals(first, UUID.fromString(first).toString())
        assertTrue(first != second)
    }
}

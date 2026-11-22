package com.kinetix.shop.core.network

import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.ForbiddenFailure
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.failure.RateLimitedFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.ValidationFailure
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpFailureMapperTest {
    private val mapper = HttpFailureMapper(
        ApiErrorParser(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        )
    )

    @Test
    fun mapsHttpStatusCodesToExpectedSealedFailures() {
        assertTrue(mapper.map(400, """{"error":"BAD_REQ","message":"Bad"}""", null) is ValidationFailure)
        assertTrue(mapper.map(422, """{"error":"INVALID","message":"Invalid","field":"email"}""", null) is ValidationFailure)
        assertTrue(mapper.map(401, null, null) is UnauthorizedFailure)
        assertTrue(mapper.map(403, null, null) is ForbiddenFailure)
        assertTrue(mapper.map(404, null, null) is NotFoundFailure)
        assertTrue(mapper.map(409, null, null) is ConflictFailure)
        assertTrue(mapper.map(502, null, 10) is UnavailableFailure)
        assertTrue(mapper.map(503, null, null) is UnavailableFailure)
        assertTrue(mapper.map(504, null, null) is UnavailableFailure)
        assertTrue(mapper.map(500, null, null) is UnknownOutcomeFailure)
    }

    @Test
    fun distinguishesSignInDelayedFromGenericRateLimitOn429() {
        val delayed = mapper.map(
            429,
            """{"error":"SIGN_IN_DELAYED","retryAfterSeconds":12}""",
            30,
        )
        assertEquals(SignInDelayedFailure(12), delayed)

        val generic = mapper.map(429, null, 45)
        assertEquals(RateLimitedFailure(45), generic)

        val defaultWait = mapper.map(429, "Too Many Requests", null)
        assertEquals(RateLimitedFailure(60), defaultWait)
    }

    @Test
    fun classifiesByStatusCodeAndBodyOriginForGatewayAndPayment() {
        val gatewayWrite503 = mapper.map(
            statusCode = 503,
            rawErrorBody = "upstream connect error",
            retryAfterHeaderSeconds = null,
            isWrite = true,
        )
        assertTrue(gatewayWrite503 is UnknownOutcomeFailure)
        assertEquals("GATEWAY_503", (gatewayWrite503 as UnknownOutcomeFailure).code)

        val gatewayRead503 = mapper.map(
            statusCode = 503,
            rawErrorBody = "upstream connect error",
            retryAfterHeaderSeconds = null,
            isWrite = false,
        )
        assertTrue(gatewayRead503 is UnavailableFailure)
        assertEquals("GATEWAY_503", (gatewayRead503 as UnavailableFailure).code)

        val service503 = mapper.map(
            statusCode = 503,
            rawErrorBody = """{"error":"DATABASE_UNAVAILABLE","message":"DB down"}""",
            retryAfterHeaderSeconds = null,
            isWrite = true,
        )
        assertTrue(service503 is UnavailableFailure)
        assertEquals("DATABASE_UNAVAILABLE", (service503 as UnavailableFailure).code)

        val payment422 = mapper.map(
            statusCode = 422,
            rawErrorBody = """{"error":"Limit exceeded","code":"TOP_UP_AMOUNT_ABOVE_LIMIT","limit":"2000000"}""",
            retryAfterHeaderSeconds = null,
        )
        assertTrue(payment422 is ValidationFailure)
        assertEquals("TOP_UP_AMOUNT_ABOVE_LIMIT", (payment422 as ValidationFailure).code)
    }
}

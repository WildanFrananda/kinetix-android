package com.kinetix.shop.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorParserTest {
    private val parser = ApiErrorParser(
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    )

    @Test
    fun parsesStringAndArrayMessagesAndRetryAfterSeconds() {
        val stringDto = parser.parse(
            """{"error":"SIGN_IN_DELAYED","message":"Too many attempts","retryAfterSeconds":15}"""
        )
        assertEquals("SIGN_IN_DELAYED", stringDto?.error)
        assertEquals("Too many attempts", parser.extractMessage(stringDto))
        assertEquals(15, stringDto?.retryAfterSeconds)

        val arrayDto = parser.parse(
            """{"error":"VALIDATION","message":["email invalid","password too short"]}"""
        )
        assertEquals("email invalid; password too short", parser.extractMessage(arrayDto))
    }

    @Test
    fun returnsNullOnMalformedOrBlankBody() {
        assertNull(parser.parse(null))
        assertNull(parser.parse("   "))
        assertNull(parser.parse("not-json"))
    }

    @Test
    fun parsesAllServiceEnvelopesAndExtractsMachineErrorCodes() {
        val paymentDto = parser.parse(
            """{"error":"Top-up exceeds maximum wallet balance","code":"WALLET_BALANCE_LIMIT_EXCEEDED","limit":"2000000.00"}"""
        )
        assertEquals("WALLET_BALANCE_LIMIT_EXCEEDED", parser.extractErrorCode(paymentDto))
        assertTrue(parser.isServiceJsonError(paymentDto))

        val frameworkDto = parser.parse(
            """{"message":["password is required"],"error":"Bad Request","statusCode":400,"traceId":"tr-1"}"""
        )
        assertNull(parser.extractErrorCode(frameworkDto))
        assertTrue(parser.isServiceJsonError(frameworkDto))

        val searchDto = parser.parse("""{"error":"malformed_query"}""")
        assertEquals("malformed_query", parser.extractErrorCode(searchDto))
        assertNull(parser.extractMessage(searchDto))
        assertTrue(parser.isServiceJsonError(searchDto))

        val problemJsonDto = parser.parse(
            """{"type":"https://tools.ietf.org/html/rfc9110","title":"One or more validation errors occurred.","status":400,"traceId":"tr-2"}"""
        )
        assertTrue(parser.isServiceJsonError(problemJsonDto))

        val gateway401Dto = parser.parse("""{"message":"Unauthorized"}""")
        assertFalse(parser.isServiceJsonError(gateway401Dto))
        assertFalse(parser.isServiceJsonError(parser.parse("upstream connect error")))
    }
}

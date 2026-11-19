package com.kinetix.shop.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}

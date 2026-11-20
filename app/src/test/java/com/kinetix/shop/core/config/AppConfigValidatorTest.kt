package com.kinetix.shop.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigValidatorTest {
    private val validator = AppConfigValidator()

    @Test
    fun rejectsBlankUrls() {
        val result = validator.validate(
            httpBase = "",
            wsBase = "   ",
        )
        assertTrue(result is AppConfigInvalid)
        val problems = (result as AppConfigInvalid).problems
        assertEquals(2, problems.size)
    }

    @Test
    fun rejectsCleartextHttpAndWsSchemes() {
        val result = validator.validate(
            httpBase = "http://10.0.2.2:8080",
            wsBase = "ws://10.0.2.2:8080",
        )
        assertTrue(result is AppConfigInvalid)
        val problems = (result as AppConfigInvalid).problems
        assertEquals(2, problems.size)
    }

    @Test
    fun rejectsUrlsWithPathsOrQueries() {
        val result = validator.validate(
            httpBase = "https://api.wildanfrananda.my.id/v1",
            wsBase = "wss://api.wildanfrananda.my.id?foo=bar",
        )
        assertTrue(result is AppConfigInvalid)
        val problems = (result as AppConfigInvalid).problems
        assertEquals(2, problems.size)
    }

    @Test
    fun rejectsMalformedUriSyntaxWithoutThrowing() {
        val result = validator.validate(
            httpBase = "https://bad host",
            wsBase = "wss://bad host",
        )
        assertTrue(result is AppConfigInvalid)
        val problems = (result as AppConfigInvalid).problems
        assertEquals(2, problems.size)
    }

    @Test
    fun acceptsHttpsAndWssOriginsAndStripsTrailingSlash() {
        val result = validator.validate(
            httpBase = "https://api.wildanfrananda.my.id/",
            wsBase = "wss://api.wildanfrananda.my.id/",
        )
        assertTrue(result is AppConfigValid)
        val valid = result as AppConfigValid
        assertEquals("https://api.wildanfrananda.my.id", valid.config.httpBase)
        assertEquals("wss://api.wildanfrananda.my.id", valid.config.wsBase)
    }
}

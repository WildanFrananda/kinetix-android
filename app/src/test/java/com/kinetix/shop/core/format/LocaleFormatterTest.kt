package com.kinetix.shop.core.format

import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleFormatterTest {
    @Test
    fun formatsIdrAndUsdCurrencyAmountsAccordingToLocale() {
        val indonesianLocale = Locale.forLanguageTag("id-ID")
        val idrFormatted = LocaleFormatter.formatMoney(
            amountString = "89000.00",
            currencyCode = "IDR",
            locale = indonesianLocale,
        )
        assertTrue("Expected formatted IDR to contain 89.000, got $idrFormatted", idrFormatted.contains("89.000"))

        val minorFormatted = LocaleFormatter.formatMinorMoney(
            amountMinor = 8_900_000L,
            currencyCode = "IDR",
            locale = indonesianLocale,
        )
        assertEquals(idrFormatted, minorFormatted)

        val usdFormatted = LocaleFormatter.formatMoney(
            amountString = "149.99",
            currencyCode = "USD",
            locale = Locale.US,
        )
        assertEquals("$149.99", usdFormatted)
    }

    @Test
    fun formatsValidIsoTimestampAndPreservesUnparseableStrings() {
        val formatted = LocaleFormatter.formatIsoTimestamp(
            isoTimestamp = "2026-11-22T09:30:00Z",
            locale = Locale.US,
            zoneId = ZoneOffset.UTC,
        )
        assertNotEquals("2026-11-22T09:30:00Z", formatted)
        assertTrue(formatted.contains("2026"))

        val rawFallback = LocaleFormatter.formatIsoTimestamp(
            isoTimestamp = "2026-10-23 10:15:00",
            locale = Locale.US,
            zoneId = ZoneOffset.UTC,
        )
        assertEquals("2026-10-23 10:15:00", rawFallback)
    }
}

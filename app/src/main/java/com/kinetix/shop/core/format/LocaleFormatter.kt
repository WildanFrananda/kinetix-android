package com.kinetix.shop.core.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

object LocaleFormatter {
    private val minorDivisor = BigDecimal("100")

    fun formatMoney(
        amount: BigDecimal,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val normalizedCode = currencyCode.trim().uppercase(Locale.ROOT)
        return try {
            val currency = Currency.getInstance(normalizedCode)
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.currency = currency
            if (normalizedCode == "IDR" && amount.stripTrailingZeros().scale() <= 0) {
                formatter.minimumFractionDigits = 0
                formatter.maximumFractionDigits = 0
            } else {
                formatter.minimumFractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
                formatter.maximumFractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
            }
            formatter.format(amount)
        } catch (_: IllegalArgumentException) {
            val numberFormatter = NumberFormat.getNumberInstance(locale)
            numberFormatter.minimumFractionDigits = 0
            numberFormatter.maximumFractionDigits = 2
            "$normalizedCode ${numberFormatter.format(amount)}"
        }
    }

    fun formatMoney(
        amountString: String,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val parsed = amountString.trim().toBigDecimalOrNull() ?: return "$currencyCode $amountString"
        return formatMoney(
            amount = parsed,
            currencyCode = currencyCode,
            locale = locale,
        )
    }

    fun formatMinorMoney(
        amountMinor: Long,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val majorAmount = BigDecimal.valueOf(amountMinor)
            .divide(minorDivisor, 2, RoundingMode.HALF_UP)
        return formatMoney(
            amount = majorAmount,
            currencyCode = currencyCode,
            locale = locale,
        )
    }

    fun formatIsoTimestamp(
        isoTimestamp: String,
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        return try {
            val instant = Instant.parse(isoTimestamp.trim())
            val formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(zoneId)
            formatter.format(instant)
        } catch (_: DateTimeParseException) {
            isoTimestamp
        }
    }
}

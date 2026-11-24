package com.kinetix.shop.core.config

import java.net.URI
import java.net.URISyntaxException

object ImageUrlSanitizer {
    private val httpsSchemePrefixRegex = Regex("^[hH][tT][tT][pP][sS]://")

    fun sanitizeHttpsUrl(
        rawUrl: String?,
    ): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }

        val uri = try {
            URI(trimmed)
        } catch (_: URISyntaxException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }

        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return null
        }

        if (uri.host.isNullOrBlank()) {
            return null
        }

        return trimmed.replaceFirst(httpsSchemePrefixRegex, "https://")
    }
}

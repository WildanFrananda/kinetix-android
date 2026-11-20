package com.kinetix.shop.core.config

import java.net.URI
import java.net.URISyntaxException

class AppConfigValidator {
    fun validate(
        httpBase: String,
        wsBase: String,
    ): AppConfigCheckResult {
        val problems = mutableListOf<String>()
        val trimmedHttp = httpBase.trim()
        val trimmedWs = wsBase.trim()

        if (trimmedHttp.isEmpty()) {
            problems.add("KINETIX_HTTP_BASE is missing")
        } else {
            validateOrigin(
                rawUrl = trimmedHttp,
                expectedScheme = "https",
                keyName = "KINETIX_HTTP_BASE",
                problems = problems,
            )
        }

        if (trimmedWs.isEmpty()) {
            problems.add("KINETIX_WS_BASE is missing")
        } else {
            validateOrigin(
                rawUrl = trimmedWs,
                expectedScheme = "wss",
                keyName = "KINETIX_WS_BASE",
                problems = problems,
            )
        }

        if (problems.isNotEmpty()) {
            return AppConfigInvalid(problems.toList())
        }

        return AppConfigValid(
            AppConfig(
                httpBase = trimmedHttp.removeSuffix("/"),
                wsBase = trimmedWs.removeSuffix("/"),
            )
        )
    }

    private fun validateOrigin(
        rawUrl: String,
        expectedScheme: String,
        keyName: String,
        problems: MutableList<String>,
    ) {
        val uri = try {
            URI(rawUrl)
        } catch (_: URISyntaxException) {
            problems.add("$keyName is not a valid URI")
            return
        } catch (_: IllegalArgumentException) {
            problems.add("$keyName is not a valid URI")
            return
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != expectedScheme) {
            problems.add("$keyName must use $expectedScheme://")
        }

        if (uri.host.isNullOrBlank()) {
            problems.add("$keyName must include a host")
        }

        val path = uri.rawPath.orEmpty()
        if (path.isNotEmpty() && path != "/") {
            problems.add("$keyName must not contain a path")
        }

        if (uri.rawQuery != null || uri.rawFragment != null) {
            problems.add("$keyName must not contain query or fragment parameters")
        }
    }
}

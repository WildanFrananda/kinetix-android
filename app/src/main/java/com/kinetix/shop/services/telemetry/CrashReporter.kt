package com.kinetix.shop.services.telemetry

import com.kinetix.shop.models.ids.PrincipalId

interface CrashReporter {
    fun setUserPrincipal(
        principalId: PrincipalId?,
    )

    fun setCustomKey(
        key: String,
        value: String,
    )

    fun recordBreadcrumb(
        category: String,
        message: String,
    )

    fun recordNonFatal(
        throwable: Throwable,
        contextMap: Map<String, String> = emptyMap(),
    )
}

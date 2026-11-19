package com.kinetix.shop.services.telemetry

import com.kinetix.shop.models.ids.PrincipalId

interface AnalyticsTracker {
    fun setUserPrincipal(
        principalId: PrincipalId?,
    )

    fun logEvent(
        name: String,
        params: Map<String, String> = emptyMap(),
    )
}

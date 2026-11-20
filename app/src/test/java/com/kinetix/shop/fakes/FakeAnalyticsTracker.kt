package com.kinetix.shop.fakes

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.telemetry.AnalyticsTracker

class FakeAnalyticsTracker : AnalyticsTracker {
    var currentPrincipal: PrincipalId? = null
        private set
    val events = mutableListOf<Pair<String, Map<String, String>>>()

    override fun setUserPrincipal(
        principalId: PrincipalId?,
    ) {
        currentPrincipal = principalId
    }

    override fun logEvent(
        name: String,
        params: Map<String, String>,
    ) {
        events.add(name to params)
    }
}

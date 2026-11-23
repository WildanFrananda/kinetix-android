package com.kinetix.shop.services.telemetry

import android.util.Log
import com.kinetix.shop.models.ids.PrincipalId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun setUserPrincipal(
        principalId: PrincipalId?,
    ) {}

    override fun logEvent(
        name: String,
        params: Map<String, String>,
    ) {
        try {
            Log.i(LOG_TAG, "event=$name params=$params")
        } catch (_: RuntimeException) {}
    }

    companion object {
        private const val LOG_TAG = "KinetixAnalytics"
    }
}

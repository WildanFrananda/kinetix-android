package com.kinetix.shop.services.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.kinetix.shop.models.ids.PrincipalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnalyticsTracker {
    override fun setUserPrincipal(
        principalId: PrincipalId?,
    ) {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseAnalytics.getInstance(context).setUserId(principalId?.value)
        }
    }

    override fun logEvent(
        name: String,
        params: Map<String, String>,
    ) {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            val bundle = Bundle()
            params.forEach { (key, value) ->
                bundle.putString(key, value)
            }
            FirebaseAnalytics.getInstance(context).logEvent(name, bundle)
        }
    }
}

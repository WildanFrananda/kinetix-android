package com.kinetix.shop.services.telemetry

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kinetix.shop.core.network.RequestCorrelationInterceptor
import com.kinetix.shop.core.network.RequestCorrelationRecorder
import com.kinetix.shop.models.ids.PrincipalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : CrashReporter, RequestCorrelationRecorder {
    @Volatile
    private var lastRecordedRequestId: String? = null

    override fun recordRequest(
        method: String,
        routeTemplate: String,
        requestId: String,
    ) {
        lastRecordedRequestId = requestId
        setCustomKey(RequestCorrelationInterceptor.KEY_LAST_REQUEST_ID, requestId)
        recordBreadcrumb(
            category = "http",
            message = "$method $routeTemplate requestId=$requestId",
        )
    }

    override fun latestRequestId(): String? {
        return lastRecordedRequestId
    }
    override fun setUserPrincipal(
        principalId: PrincipalId?,
    ) {
        withCrashlytics { crashlytics ->
            crashlytics.setUserId(principalId?.value.orEmpty())
        }
    }

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        withCrashlytics { crashlytics ->
            crashlytics.setCustomKey(key, value)
        }
    }

    override fun recordBreadcrumb(
        category: String,
        message: String,
    ) {
        withCrashlytics { crashlytics ->
            crashlytics.log("[$category] $message")
        }
    }

    override fun recordNonFatal(
        throwable: Throwable,
        contextMap: Map<String, String>,
    ) {
        withCrashlytics { crashlytics ->
            contextMap.forEach { (key, value) ->
                crashlytics.setCustomKey(key, value)
            }
            crashlytics.recordException(throwable)
        }
    }

    private inline fun withCrashlytics(
        block: (FirebaseCrashlytics) -> Unit,
    ) {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            block(FirebaseCrashlytics.getInstance())
        }
    }
}

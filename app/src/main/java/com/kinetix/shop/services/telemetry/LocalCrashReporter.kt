package com.kinetix.shop.services.telemetry

import android.util.Log
import com.kinetix.shop.core.network.RequestCorrelationInterceptor
import com.kinetix.shop.core.network.RequestCorrelationRecorder
import com.kinetix.shop.models.ids.PrincipalId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCrashReporter @Inject constructor() : CrashReporter, RequestCorrelationRecorder {
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
    ) {}

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        safeLog { Log.d(LOG_TAG, "key $key=$value") }
    }

    override fun recordBreadcrumb(
        category: String,
        message: String,
    ) {
        safeLog { Log.i(LOG_TAG, "[$category] $message") }
    }

    override fun recordNonFatal(
        throwable: Throwable,
        contextMap: Map<String, String>,
    ) {
        safeLog { Log.w(LOG_TAG, "Non-fatal exception context=$contextMap", throwable) }
    }

    private inline fun safeLog(
        action: () -> Unit,
    ) {
        try {
            action()
        } catch (_: RuntimeException) {}
    }

    companion object {
        private const val LOG_TAG = "KinetixTelemetry"
    }
}

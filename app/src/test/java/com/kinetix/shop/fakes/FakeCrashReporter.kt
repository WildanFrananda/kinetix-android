package com.kinetix.shop.fakes

import com.kinetix.shop.core.network.RequestCorrelationInterceptor
import com.kinetix.shop.core.network.RequestCorrelationRecorder
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.telemetry.CrashReporter

class FakeCrashReporter : CrashReporter, RequestCorrelationRecorder {
    var currentPrincipal: PrincipalId? = null
        private set
    val customKeys = linkedMapOf<String, String>()
    val breadcrumbs = mutableListOf<Pair<String, String>>()
    val nonFatalExceptions = mutableListOf<Throwable>()

    override fun recordRequest(
        method: String,
        routeTemplate: String,
        requestId: String,
    ) {
        setCustomKey(RequestCorrelationInterceptor.KEY_LAST_REQUEST_ID, requestId)
        recordBreadcrumb(
            category = "http",
            message = "$method $routeTemplate requestId=$requestId",
        )
    }

    override fun setUserPrincipal(
        principalId: PrincipalId?,
    ) {
        currentPrincipal = principalId
    }

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        customKeys[key] = value
    }

    override fun recordBreadcrumb(
        category: String,
        message: String,
    ) {
        breadcrumbs.add(category to message)
    }

    override fun recordNonFatal(
        throwable: Throwable,
        contextMap: Map<String, String>,
    ) {
        customKeys.putAll(contextMap)
        nonFatalExceptions.add(throwable)
    }
}

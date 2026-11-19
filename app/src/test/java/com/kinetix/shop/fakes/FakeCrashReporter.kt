package com.kinetix.shop.fakes

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.services.telemetry.CrashReporter

class FakeCrashReporter : CrashReporter {
    var currentPrincipal: PrincipalId? = null
        private set
    val customKeys = linkedMapOf<String, String>()
    val breadcrumbs = mutableListOf<Pair<String, String>>()
    val nonFatalExceptions = mutableListOf<Throwable>()

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

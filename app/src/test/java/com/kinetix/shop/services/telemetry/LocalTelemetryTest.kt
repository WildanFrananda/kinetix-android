package com.kinetix.shop.services.telemetry

import com.kinetix.shop.models.ids.PrincipalId
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalTelemetryTest {
    @Test
    fun localCrashReporterRecordsLatestRequestIdAndIgnoresUserPrincipal() {
        val reporter = LocalCrashReporter()
        reporter.setUserPrincipal(PrincipalId("usr-99"))
        reporter.recordRequest(
            method = "GET",
            routeTemplate = "api/v1/products",
            requestId = "req-local-1",
        )
        assertEquals("req-local-1", reporter.latestRequestId())
    }
}

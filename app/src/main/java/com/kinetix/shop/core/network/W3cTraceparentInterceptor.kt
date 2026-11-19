package com.kinetix.shop.core.network

import com.kinetix.shop.services.telemetry.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class W3cTraceparentInterceptor @Inject constructor(
    private val traceIdGenerator: TraceIdGenerator,
    private val crashReporter: CrashReporter,
) : Interceptor {
    override fun intercept(
        chain: Interceptor.Chain,
    ): Response {
        val originalRequest = chain.request()
        val existingTraceparent = originalRequest.header(HEADER_TRACEPARENT)
        val traceId: String
        val traceparentValue: String
        if (existingTraceparent.isNullOrBlank()) {
            traceId = traceIdGenerator.newTraceIdHex()
            val spanId = traceIdGenerator.newSpanIdHex()
            traceparentValue = "00-$traceId-$spanId-01"
        } else {
            traceparentValue = existingTraceparent
            traceId = extractTraceId(existingTraceparent)
        }

        crashReporter.setCustomKey(KEY_LAST_TRACE_ID, traceId)
        crashReporter.recordBreadcrumb(
            category = "http",
            message = "${originalRequest.method} ${originalRequest.url.encodedPath} trace=$traceId",
        )

        val tracedRequest = if (existingTraceparent.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header(HEADER_TRACEPARENT, traceparentValue)
                .build()
        } else {
            originalRequest
        }
        return chain.proceed(tracedRequest)
    }

    private fun extractTraceId(
        traceparent: String,
    ): String {
        val parts = traceparent.split('-')
        return if (parts.size >= 2 && parts[1].isNotBlank()) {
            parts[1]
        } else {
            traceparent
        }
    }

    companion object {
        const val HEADER_TRACEPARENT = "traceparent"
        const val KEY_LAST_TRACE_ID = "last_trace_id"
    }
}

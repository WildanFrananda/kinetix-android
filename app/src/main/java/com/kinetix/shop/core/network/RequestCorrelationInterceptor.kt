package com.kinetix.shop.core.network

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HTTP
import retrofit2.http.OPTIONS
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

@Singleton
class RequestCorrelationInterceptor @Inject constructor(
    private val requestIdGenerator: RequestIdGenerator,
    private val correlationRecorder: RequestCorrelationRecorder,
) : Interceptor {
    override fun intercept(
        chain: Interceptor.Chain,
    ): Response {
        val originalRequest = chain.request()
        val existingRequestId = originalRequest.header(HEADER_REQUEST_ID)
        val requestId = if (existingRequestId.isNullOrBlank()) {
            requestIdGenerator.newRequestId()
        } else {
            existingRequestId
        }
        val routeTemplate = resolveRouteTemplate(originalRequest)

        correlationRecorder.recordRequest(
            method = originalRequest.method,
            routeTemplate = routeTemplate,
            requestId = requestId,
        )

        val requestBuilder = originalRequest.newBuilder()
        if (existingRequestId.isNullOrBlank()) {
            requestBuilder.header(HEADER_REQUEST_ID, requestId)
        }
        if (originalRequest.header(HEADER_ACCEPT).isNullOrBlank()) {
            requestBuilder.header(HEADER_ACCEPT, MEDIA_TYPE_JSON)
        }

        return chain.proceed(requestBuilder.build())
    }

    private fun resolveRouteTemplate(
        request: Request,
    ): String {
        val invocation = request.tag(Invocation::class.java) ?: return UNTAGGED_ROUTE
        val method = invocation.method()
        for (annotation in method.annotations) {
            val template = when (annotation) {
                is GET -> annotation.value
                is POST -> annotation.value
                is PUT -> annotation.value
                is DELETE -> annotation.value
                is PATCH -> annotation.value
                is HEAD -> annotation.value
                is OPTIONS -> annotation.value
                is HTTP -> annotation.path
                else -> null
            }
            if (!template.isNullOrBlank()) {
                return template
            }
        }

        return "${invocation.service().simpleName}.${method.name}"
    }

    companion object {
        const val HEADER_REQUEST_ID = "X-Request-Id"
        const val HEADER_ACCEPT = "Accept"
        const val MEDIA_TYPE_JSON = "application/json"
        const val KEY_LAST_REQUEST_ID = "last_request_id"
        const val UNTAGGED_ROUTE = "<untagged>"
    }
}

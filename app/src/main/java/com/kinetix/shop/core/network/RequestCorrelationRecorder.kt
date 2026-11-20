package com.kinetix.shop.core.network

interface RequestCorrelationRecorder {
    fun recordRequest(
        method: String,
        routeTemplate: String,
        requestId: String,
    )
}

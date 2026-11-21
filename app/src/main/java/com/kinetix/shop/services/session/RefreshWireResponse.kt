package com.kinetix.shop.services.session

data class RefreshWireResponse(
    val statusCode: Int,
    val rawBody: String?,
    val retryAfterHeaderSeconds: Int?,
    val requestId: String?,
    val leftDevice: Boolean,
) : RefreshWireOutcome

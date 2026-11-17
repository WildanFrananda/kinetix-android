package com.kinetix.shop.services.session

data class RefreshTransportTransientError(
    val detail: String,
    val retryAfterSeconds: Int?,
) : RefreshTransportOutcome

package com.kinetix.shop.core.failure

data class RateLimitedFailure(
    val retryAfterSeconds: Int?,
    override val requestId: String? = null,
) : Failure

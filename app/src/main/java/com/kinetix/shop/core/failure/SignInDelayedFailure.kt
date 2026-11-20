package com.kinetix.shop.core.failure

data class SignInDelayedFailure(
    val retryAfterSeconds: Int,
    override val requestId: String? = null,
) : Failure

package com.kinetix.shop.core.failure

data class UnavailableFailure(
    val code: String,
    val retryAfterSeconds: Int?,
) : Failure

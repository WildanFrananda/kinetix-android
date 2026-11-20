package com.kinetix.shop.core.failure

data class UnauthorizedFailure(
    val code: String,
    val detail: String,
    override val requestId: String? = null,
) : Failure

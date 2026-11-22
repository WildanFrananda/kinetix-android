package com.kinetix.shop.core.failure

data class UnauthorizedFailure(
    val code: String? = null,
    override val requestId: String? = null,
) : Failure

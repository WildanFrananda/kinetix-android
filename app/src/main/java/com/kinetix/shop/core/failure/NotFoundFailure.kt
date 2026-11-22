package com.kinetix.shop.core.failure

data class NotFoundFailure(
    val code: String? = null,
    override val requestId: String? = null,
) : Failure

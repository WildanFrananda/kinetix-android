package com.kinetix.shop.core.failure

data class NotFoundFailure(
    val code: String,
    val detail: String,
    override val requestId: String? = null,
) : Failure

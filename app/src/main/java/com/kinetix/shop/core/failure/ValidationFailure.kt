package com.kinetix.shop.core.failure

data class ValidationFailure(
    val code: String,
    val detail: String,
    val field: String?,
    override val requestId: String? = null,
) : Failure

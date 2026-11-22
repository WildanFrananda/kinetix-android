package com.kinetix.shop.core.failure

data class ValidationFailure(
    val code: String? = null,
    val field: String? = null,
    override val requestId: String? = null,
) : Failure

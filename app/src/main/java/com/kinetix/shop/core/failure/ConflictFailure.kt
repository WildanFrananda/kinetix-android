package com.kinetix.shop.core.failure

data class ConflictFailure(
    val code: String? = null,
    override val requestId: String? = null,
) : Failure

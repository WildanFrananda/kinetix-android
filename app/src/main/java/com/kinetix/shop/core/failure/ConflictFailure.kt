package com.kinetix.shop.core.failure

data class ConflictFailure(
    val code: String,
    val detail: String,
    override val requestId: String? = null,
) : Failure

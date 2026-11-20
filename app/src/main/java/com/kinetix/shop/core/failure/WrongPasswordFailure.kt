package com.kinetix.shop.core.failure

data class WrongPasswordFailure(
    val detail: String,
    override val requestId: String? = null,
) : Failure

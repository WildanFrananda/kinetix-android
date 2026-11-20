package com.kinetix.shop.core.failure

data class WrongTwoFactorCodeFailure(
    val detail: String,
    override val requestId: String? = null,
) : Failure

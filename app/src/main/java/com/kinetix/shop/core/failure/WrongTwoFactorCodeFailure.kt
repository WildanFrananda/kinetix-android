package com.kinetix.shop.core.failure

data class WrongTwoFactorCodeFailure(
    override val requestId: String? = null,
) : Failure

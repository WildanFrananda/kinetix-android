package com.kinetix.shop.core.failure

data class WrongPasswordFailure(
    override val requestId: String? = null,
) : Failure

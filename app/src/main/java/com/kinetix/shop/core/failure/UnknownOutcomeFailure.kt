package com.kinetix.shop.core.failure

data class UnknownOutcomeFailure(
    val code: String? = null,
    override val requestId: String? = null,
) : Failure

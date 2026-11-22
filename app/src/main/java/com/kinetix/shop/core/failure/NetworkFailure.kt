package com.kinetix.shop.core.failure

data class NetworkFailure(
    override val requestId: String? = null,
) : Failure

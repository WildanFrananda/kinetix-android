package com.kinetix.shop.core.failure

data class ForbiddenFailure(
    val code: String? = null,
    override val requestId: String? = null,
) : Failure

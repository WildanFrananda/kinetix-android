package com.kinetix.shop.core.failure

data class ForbiddenFailure(
    val code: String,
    val detail: String,
) : Failure

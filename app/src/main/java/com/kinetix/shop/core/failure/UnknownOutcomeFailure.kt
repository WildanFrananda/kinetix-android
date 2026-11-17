package com.kinetix.shop.core.failure

data class UnknownOutcomeFailure(
    val code: String,
    val detail: String,
) : Failure

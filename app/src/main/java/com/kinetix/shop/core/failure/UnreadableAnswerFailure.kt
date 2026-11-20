package com.kinetix.shop.core.failure

data class UnreadableAnswerFailure(
    val detail: String,
    override val requestId: String? = null,
) : Failure

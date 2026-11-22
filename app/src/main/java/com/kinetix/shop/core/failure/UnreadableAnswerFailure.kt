package com.kinetix.shop.core.failure

data class UnreadableAnswerFailure(
    override val requestId: String? = null,
) : Failure

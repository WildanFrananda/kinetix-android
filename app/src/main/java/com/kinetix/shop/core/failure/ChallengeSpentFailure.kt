package com.kinetix.shop.core.failure

data class ChallengeSpentFailure(
    val code: String? = null,
    override val requestId: String? = null,
) : Failure

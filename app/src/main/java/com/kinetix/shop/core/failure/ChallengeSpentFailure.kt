package com.kinetix.shop.core.failure

data class ChallengeSpentFailure(
    val code: String,
    val detail: String,
) : Failure

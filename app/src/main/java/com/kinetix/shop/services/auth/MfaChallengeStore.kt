package com.kinetix.shop.services.auth

interface MfaChallengeStore {
    fun store(
        challengeToken: String,
    )

    fun peek(): String?

    fun clear()
}

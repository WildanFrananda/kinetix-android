package com.kinetix.shop.services.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryMfaChallengeStore @Inject constructor() : MfaChallengeStore {
    @Volatile
    private var currentToken: String? = null

    override fun store(
        challengeToken: String,
    ) {
        currentToken = challengeToken.takeIf { it.isNotBlank() }
    }

    override fun peek(): String? {
        return currentToken
    }

    override fun clear() {
        currentToken = null
    }
}

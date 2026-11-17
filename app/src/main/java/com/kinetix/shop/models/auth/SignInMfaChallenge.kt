package com.kinetix.shop.models.auth

data class SignInMfaChallenge(
    val challengeToken: String,
    val expiresInSeconds: Long,
    val passkeysAvailable: Boolean,
) : SignInOutcome

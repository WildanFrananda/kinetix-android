package com.kinetix.shop.models.notification

data class DeviceVerificationChallenge(
    val nonce: String,
    val expiresAt: String,
)

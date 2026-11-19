package com.kinetix.shop.models.notification

data class DeviceVerificationChallenge(
    val challengeId: String,
    val challengeToken: String,
)

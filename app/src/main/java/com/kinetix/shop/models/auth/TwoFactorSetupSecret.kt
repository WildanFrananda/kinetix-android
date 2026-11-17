package com.kinetix.shop.models.auth

data class TwoFactorSetupSecret(
    val secret: String,
    val qrCodeUrl: String,
)

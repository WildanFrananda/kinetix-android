package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorSetupResponseDto(
    val twoFactorSecret: String,
    val qrCodeUrl: String,
    val enabled: Boolean,
)

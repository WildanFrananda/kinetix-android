package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorStatusResponseDto(
    val enabled: Boolean,
)

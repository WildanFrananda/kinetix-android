package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorPasswordRequestDto(
    val password: String,
)

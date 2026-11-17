package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorCodePasswordRequestDto(
    val code: String,
    val password: String,
)

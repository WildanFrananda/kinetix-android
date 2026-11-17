package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class VerifyTwoFactorRequestDto(
    val challengeToken: String,
    val code: String,
)

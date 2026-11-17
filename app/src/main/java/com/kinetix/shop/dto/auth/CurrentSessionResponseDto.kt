package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class CurrentSessionResponseDto(
    val principalId: String,
    val userId: Long,
    val email: String,
    val role: String,
    val expiresAt: String,
)

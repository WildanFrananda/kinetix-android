package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class SessionTokensResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String,
    val user: AuthenticatedUserDto,
)

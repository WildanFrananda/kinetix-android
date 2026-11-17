package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val tokenType: String? = null,
    val user: AuthenticatedUserDto? = null,
    val mfaRequired: Boolean? = null,
    val challengeToken: String? = null,
    val passkeys: Boolean? = null,
    val mfaEnrolmentRequired: Boolean? = null,
    val enrolmentToken: String? = null,
    val userId: Long? = null,
)

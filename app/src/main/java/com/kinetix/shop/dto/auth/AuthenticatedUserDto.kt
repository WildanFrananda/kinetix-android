package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticatedUserDto(
    val id: Long,
    val email: String,
    val role: String,
    val principalId: String,
)

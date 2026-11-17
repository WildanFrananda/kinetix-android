package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class CredentialsRequestDto(
    val email: String,
    val password: String,
)

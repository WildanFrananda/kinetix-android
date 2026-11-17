package com.kinetix.shop.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class StoredSessionPayloadDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val accountId: Long,
    val principalId: String,
    val email: String,
)

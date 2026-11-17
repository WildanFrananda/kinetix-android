package com.kinetix.shop.dto.profile

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponseDto(
    val id: Long,
    val userId: Long,
    val fullName: String,
    val phoneNumber: String,
    val streetAddress: String,
    val city: String,
    val postalCode: String,
    val avatarUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geocodedAt: String? = null,
)

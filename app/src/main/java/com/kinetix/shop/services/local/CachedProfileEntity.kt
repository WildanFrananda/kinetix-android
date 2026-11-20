package com.kinetix.shop.services.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_profiles")
data class CachedProfileEntity(
    @PrimaryKey
    val accountId: Long,
    val id: Long,
    val fullName: String,
    val phoneNumber: String,
    val streetAddress: String,
    val city: String,
    val postalCode: String,
    val avatarUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val geocodedAt: String?,
    val cachedAtEpochSeconds: Long,
)

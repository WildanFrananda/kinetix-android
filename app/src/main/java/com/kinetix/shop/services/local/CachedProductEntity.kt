package com.kinetix.shop.services.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_products",
    indices = [
        Index(value = ["categorySlug"]),
    ],
)
data class CachedProductEntity(
    @PrimaryKey
    val sku: String,
    val id: Long,
    val title: String,
    val description: String?,
    val categoryName: String,
    val categorySlug: String,
    val price: String,
    val currency: String,
    val imageUrl: String,
    val merchantPrincipalId: String,
    val weightGrams: Int?,
    val availableQuantity: Int?,
    val reservedQuantity: Int?,
    val binLocation: String?,
    val stockStatus: String,
    val cachedAtEpochSeconds: Long,
)

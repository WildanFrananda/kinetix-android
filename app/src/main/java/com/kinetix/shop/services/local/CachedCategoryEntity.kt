package com.kinetix.shop.services.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_categories")
data class CachedCategoryEntity(
    @PrimaryKey
    val slug: String,
    val id: Long,
    val name: String,
    val cachedAtEpochSeconds: Long,
)

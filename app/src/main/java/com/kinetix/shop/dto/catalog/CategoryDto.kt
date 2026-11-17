package com.kinetix.shop.dto.catalog

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val slug: String,
)

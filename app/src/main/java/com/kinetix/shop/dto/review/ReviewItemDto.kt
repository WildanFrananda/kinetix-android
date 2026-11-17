package com.kinetix.shop.dto.review

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewItemDto(
    val id: String,
    @SerialName("product_id")
    val productId: String,
    val rating: Int,
    val comment: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("merchant_principal_id")
    val merchantPrincipalId: String,
)

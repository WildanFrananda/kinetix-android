package com.kinetix.shop.dto.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductSummaryDto(
    val id: Long,
    val sku: String,
    val title: String,
    val category: String,
    @SerialName("category_slug")
    val categorySlug: String,
    val price: String,
    val currency: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("merchant_principal_id")
    val merchantPrincipalId: String,
    @SerialName("weight_grams")
    val weightGrams: Int? = null,
    @SerialName("available_stock")
    val availableStock: Int? = null,
    @SerialName("is_in_stock")
    val isInStock: Boolean? = null,
    @SerialName("stock_status")
    val stockStatus: String,
)

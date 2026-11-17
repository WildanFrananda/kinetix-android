package com.kinetix.shop.dto.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDetailResponseDto(
    val id: Long,
    val sku: String,
    val title: String,
    val description: String,
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
    @SerialName("warehouse_stock")
    val warehouseStock: WarehouseStockDto,
)

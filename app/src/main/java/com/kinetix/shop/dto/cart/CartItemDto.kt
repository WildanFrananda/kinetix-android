package com.kinetix.shop.dto.cart

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class CartItemDto(
    val productId: String,
    val productTitle: String,
    val unitPrice: JsonPrimitive,
    val quantity: Int,
    val categoryId: String? = null,
    val merchantPrincipalId: String? = null,
    val lineTotal: JsonPrimitive,
)

package com.kinetix.shop.dto.cart

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class CartResponseDto(
    val customerPrincipalId: String,
    val items: List<CartItemDto>,
    val appliedVoucherCode: String? = null,
    val subtotal: JsonPrimitive,
    val updatedAt: String,
)

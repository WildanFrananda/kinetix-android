package com.kinetix.shop.dto.cart

import kotlinx.serialization.Serializable

@Serializable
data class AddCartItemRequestDto(
    val productId: String,
    val quantity: Int,
)

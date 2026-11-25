package com.kinetix.shop.dto.cart

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCartItemRequestDto(
    val quantity: Int,
)

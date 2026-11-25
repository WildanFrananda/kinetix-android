package com.kinetix.shop.dto.pricing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApplyVoucherRequestDto(
    val code: String,
    @SerialName("cart_subtotal")
    val cartSubtotal: String,
    @SerialName("merchant_principal_id")
    val merchantPrincipalId: String? = null,
)

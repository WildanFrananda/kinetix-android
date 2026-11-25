package com.kinetix.shop.dto.cart

import kotlinx.serialization.Serializable

@Serializable
data class ApplyCartVoucherRequestDto(
    val voucherCode: String,
)

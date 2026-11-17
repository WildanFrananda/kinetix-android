package com.kinetix.shop.dto.pricing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlashSaleDto(
    val id: String,
    val title: String,
    @SerialName("product_id")
    val productId: String,
    @SerialName("flash_price")
    val flashPrice: String,
    @SerialName("stock_limit")
    val stockLimit: Int,
    @SerialName("stock_sold")
    val stockSold: Int,
    val active: Boolean,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("merchant_principal_id")
    val merchantPrincipalId: String,
)

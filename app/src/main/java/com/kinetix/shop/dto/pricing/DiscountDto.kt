package com.kinetix.shop.dto.pricing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscountDto(
    val id: String,
    val title: String,
    @SerialName("discount_type")
    val discountType: String,
    val value: String,
    @SerialName("target_product_id")
    val targetProductId: String? = null,
    @SerialName("target_category_id")
    val targetCategoryId: String? = null,
    val active: Boolean,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("merchant_principal_id")
    val merchantPrincipalId: String? = null,
)

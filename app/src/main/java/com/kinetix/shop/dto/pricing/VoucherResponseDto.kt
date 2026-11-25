package com.kinetix.shop.dto.pricing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoucherResponseDto(
    val id: String? = null,
    val code: String,
    val title: String,
    @SerialName("discount_type")
    val discountType: String,
    val value: String,
    @SerialName("min_spend")
    val minSpend: String,
    @SerialName("max_discount")
    val maxDiscount: String? = null,
    val quota: Int? = null,
    @SerialName("used_count")
    val usedCount: Int? = null,
    val active: Boolean? = null,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("merchant_principal_id")
    val merchantPrincipalId: String? = null,
)

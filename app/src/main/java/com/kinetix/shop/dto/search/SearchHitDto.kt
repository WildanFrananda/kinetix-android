package com.kinetix.shop.dto.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchHitDto(
    val id: String,
    @SerialName("merchant_id")
    val merchantId: String,
    val title: String,
    val description: String? = null,
    val categories: List<String> = emptyList(),
    @SerialName("price_minor")
    val priceMinor: Long,
    val currency: String,
    @SerialName("updated_at_ms")
    val updatedAtMs: Long? = null,
)

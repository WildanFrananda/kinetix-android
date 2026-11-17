package com.kinetix.shop.dto.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductListResponseDto(
    val count: Int,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val results: List<ProductSummaryDto>,
)

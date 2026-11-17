package com.kinetix.shop.dto.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchFacetsDto(
    val categories: List<SearchFacetEntryDto> = emptyList(),
    val currency: List<SearchFacetEntryDto> = emptyList(),
    @SerialName("merchant_id")
    val merchantId: List<SearchFacetEntryDto> = emptyList(),
)

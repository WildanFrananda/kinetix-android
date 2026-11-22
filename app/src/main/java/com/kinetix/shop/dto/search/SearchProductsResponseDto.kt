package com.kinetix.shop.dto.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchProductsResponseDto(
    val total: Int,
    val hits: List<SearchHitDto>,
    val facets: SearchFacetsDto? = null,
    val index: SearchIndexDto,
)

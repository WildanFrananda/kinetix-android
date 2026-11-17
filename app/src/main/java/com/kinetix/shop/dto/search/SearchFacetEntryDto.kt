package com.kinetix.shop.dto.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchFacetEntryDto(
    val value: String,
    val count: Int,
)

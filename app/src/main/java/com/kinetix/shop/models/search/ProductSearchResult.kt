package com.kinetix.shop.models.search

data class ProductSearchResult(
    val total: Int,
    val hits: List<SearchHit>,
    val categoryFacets: List<SearchFacetCount>,
    val indexState: SearchIndexState,
)

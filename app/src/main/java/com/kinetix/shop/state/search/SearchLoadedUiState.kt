package com.kinetix.shop.state.search

import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.search.ProductSearchResult

data class SearchLoadedUiState(
    val query: String,
    val selectedCategorySlug: CategorySlug?,
    val selectedSort: String?,
    val suggestions: List<String>,
    val searchResult: ProductSearchResult,
    val currentOffset: Int,
    val pageLimit: Int,
) : SearchUiState

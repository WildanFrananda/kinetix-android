package com.kinetix.shop.fakes

import com.kinetix.shop.dto.search.SearchProductsResponseDto
import com.kinetix.shop.dto.search.SearchSuggestionsResponseDto
import com.kinetix.shop.services.remote.SearchApiService
import retrofit2.Response

class FakeSearchApiService : SearchApiService {
    var suggestHandler: (String, Int) -> Response<SearchSuggestionsResponseDto> = { _, _ ->
        Response.success(SearchSuggestionsResponseDto(emptyList()))
    }
    var searchHandler: (String?, String?, String?, Int, Int) -> Response<SearchProductsResponseDto> = { _, _, _, _, _ ->
        throw UnsupportedOperationException("searchHandler not configured")
    }

    override suspend fun suggestProducts(
        query: String,
        limit: Int,
    ): Response<SearchSuggestionsResponseDto> {
        return suggestHandler(query, limit)
    }

    override suspend fun searchProducts(
        query: String?,
        categorySlug: String?,
        sort: String?,
        limit: Int,
        offset: Int,
    ): Response<SearchProductsResponseDto> {
        return searchHandler(query, categorySlug, sort, limit, offset)
    }
}

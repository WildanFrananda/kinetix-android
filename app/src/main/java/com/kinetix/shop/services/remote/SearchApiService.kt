package com.kinetix.shop.services.remote

import com.kinetix.shop.dto.search.SearchProductsResponseDto
import com.kinetix.shop.dto.search.SearchSuggestionsResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApiService {
    @GET("api/v1/products/suggest")
    suspend fun suggestProducts(
        @Query("q") query: String,
        @Query("limit") limit: Int,
    ): Response<SearchSuggestionsResponseDto>

    @GET("api/v1/products/search")
    suspend fun searchProducts(
        @Query("q") query: String? = null,
        @Query("category") categorySlug: String? = null,
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Response<SearchProductsResponseDto>
}

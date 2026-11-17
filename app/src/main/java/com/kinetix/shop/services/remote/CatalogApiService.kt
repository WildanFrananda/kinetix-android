package com.kinetix.shop.services.remote

import com.kinetix.shop.dto.catalog.CategoryDto
import com.kinetix.shop.dto.catalog.ProductDetailResponseDto
import com.kinetix.shop.dto.catalog.ProductListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApiService {
    @GET("api/v1/categories")
    suspend fun listCategories(): Response<List<CategoryDto>>

    @GET("api/v1/products")
    suspend fun listProducts(
        @Query("category") categorySlug: String? = null,
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): Response<ProductListResponseDto>

    @GET("api/v1/products/{sku}")
    suspend fun getProduct(
        @Path("sku") sku: String,
    ): Response<ProductDetailResponseDto>
}

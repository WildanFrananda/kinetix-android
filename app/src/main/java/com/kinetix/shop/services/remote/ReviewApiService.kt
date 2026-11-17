package com.kinetix.shop.services.remote

import com.kinetix.shop.dto.review.MerchantReviewsResponseDto
import com.kinetix.shop.dto.review.ProductReviewsResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApiService {
    @GET("api/v1/reviews/products/{productId}")
    suspend fun listProductReviews(
        @Path("productId") productSku: String,
        @Query("page") page: Int = 1,
    ): Response<ProductReviewsResponseDto>

    @GET("api/v1/reviews/merchants/{merchantPrincipalId}")
    suspend fun listMerchantReviews(
        @Path("merchantPrincipalId") merchantPrincipalId: String,
        @Query("page") page: Int = 1,
    ): Response<MerchantReviewsResponseDto>
}

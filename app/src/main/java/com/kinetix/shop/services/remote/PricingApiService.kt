package com.kinetix.shop.services.remote

import com.kinetix.shop.dto.pricing.DiscountDto
import com.kinetix.shop.dto.pricing.FlashSaleDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PricingApiService {
    @GET("api/v1/pricing/flash-sales/{productId}")
    suspend fun getFlashSale(
        @Path("productId") productSku: String,
    ): Response<FlashSaleDto?>

    @GET("api/v1/pricing/discounts")
    suspend fun listDiscounts(): Response<List<DiscountDto>>
}

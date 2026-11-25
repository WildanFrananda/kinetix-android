package com.kinetix.shop.services.remote

import com.kinetix.shop.dto.pricing.ApplyVoucherRequestDto
import com.kinetix.shop.dto.pricing.DiscountDto
import com.kinetix.shop.dto.pricing.VoucherResponseDto
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PricingApiService {
    @GET("api/v1/pricing/flash-sales/{productId}")
    suspend fun getFlashSale(
        @Path("productId") productSku: String,
    ): Response<JsonElement>

    @GET("api/v1/pricing/discounts")
    suspend fun listDiscounts(): Response<List<DiscountDto>>

    @GET("api/v1/pricing/vouchers/{code}")
    suspend fun getVoucher(
        @Path("code") code: String,
    ): Response<VoucherResponseDto>

    @POST("api/v1/pricing/vouchers/apply")
    suspend fun applyVoucherPreview(
        @Body request: ApplyVoucherRequestDto,
    ): Response<JsonElement>
}

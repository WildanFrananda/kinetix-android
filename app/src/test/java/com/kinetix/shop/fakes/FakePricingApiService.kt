package com.kinetix.shop.fakes

import com.kinetix.shop.dto.pricing.ApplyVoucherRequestDto
import com.kinetix.shop.dto.pricing.DiscountDto
import com.kinetix.shop.dto.pricing.VoucherResponseDto
import com.kinetix.shop.services.remote.PricingApiService
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakePricingApiService : PricingApiService {
    var getFlashSaleHandler: (String) -> Response<JsonElement> = {
        Response.success(JsonNull)
    }
    var listDiscountsHandler: () -> Response<List<DiscountDto>> = {
        Response.success(emptyList())
    }
    var getVoucherHandler: (String) -> Response<VoucherResponseDto> = {
        Response.error(404, """{"error":"NOT_FOUND","message":"Not found"}""".toResponseBody("application/json".toMediaType()))
    }
    var applyVoucherPreviewHandler: (ApplyVoucherRequestDto) -> Response<JsonElement> = {
        Response.success(JsonNull)
    }

    override suspend fun getFlashSale(
        productSku: String,
    ): Response<JsonElement> {
        return getFlashSaleHandler(productSku)
    }

    override suspend fun listDiscounts(): Response<List<DiscountDto>> {
        return listDiscountsHandler()
    }

    override suspend fun getVoucher(
        code: String,
    ): Response<VoucherResponseDto> {
        return getVoucherHandler(code)
    }

    override suspend fun applyVoucherPreview(
        request: ApplyVoucherRequestDto,
    ): Response<JsonElement> {
        return applyVoucherPreviewHandler(request)
    }
}

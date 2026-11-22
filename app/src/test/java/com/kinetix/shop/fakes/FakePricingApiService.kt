package com.kinetix.shop.fakes

import com.kinetix.shop.dto.pricing.DiscountDto
import com.kinetix.shop.services.remote.PricingApiService
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import retrofit2.Response

class FakePricingApiService : PricingApiService {
    var getFlashSaleHandler: (String) -> Response<JsonElement> = {
        Response.success(JsonNull)
    }
    var listDiscountsHandler: () -> Response<List<DiscountDto>> = {
        Response.success(emptyList())
    }

    override suspend fun getFlashSale(
        productSku: String,
    ): Response<JsonElement> {
        return getFlashSaleHandler(productSku)
    }

    override suspend fun listDiscounts(): Response<List<DiscountDto>> {
        return listDiscountsHandler()
    }
}

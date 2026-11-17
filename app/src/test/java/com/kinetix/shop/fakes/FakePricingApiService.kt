package com.kinetix.shop.fakes

import com.kinetix.shop.dto.pricing.DiscountDto
import com.kinetix.shop.dto.pricing.FlashSaleDto
import com.kinetix.shop.services.remote.PricingApiService
import retrofit2.Response

class FakePricingApiService : PricingApiService {
    var getFlashSaleHandler: (String) -> Response<FlashSaleDto?> = {
        Response.success(null)
    }
    var listDiscountsHandler: () -> Response<List<DiscountDto>> = {
        Response.success(emptyList())
    }

    override suspend fun getFlashSale(
        productSku: String,
    ): Response<FlashSaleDto?> {
        return getFlashSaleHandler(productSku)
    }

    override suspend fun listDiscounts(): Response<List<DiscountDto>> {
        return listDiscountsHandler()
    }
}

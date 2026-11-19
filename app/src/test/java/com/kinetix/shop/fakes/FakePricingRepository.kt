package com.kinetix.shop.fakes

import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.pricing.FlashSale
import com.kinetix.shop.models.pricing.ProductPriceIndication
import com.kinetix.shop.models.pricing.PromotionDiscount
import com.kinetix.shop.repositories.pricing.PricingRepository

class FakePricingRepository : PricingRepository {
    var flashSaleResult: Result<FlashSale?> = Ok(null)
    var discountsResult: Result<List<PromotionDiscount>> = Ok(emptyList())
    var priceIndicationOverride: Result<ProductPriceIndication>? = null

    override suspend fun getFlashSale(
        sku: ProductSku,
    ): Result<FlashSale?> {
        return flashSaleResult
    }

    override suspend fun listActiveDiscounts(): Result<List<PromotionDiscount>> {
        return discountsResult
    }

    override suspend fun computePriceIndication(
        sku: ProductSku,
        categorySlug: CategorySlug,
        merchantPrincipalId: PrincipalId,
        catalogPrice: String,
        currency: String,
    ): Result<ProductPriceIndication> {
        return priceIndicationOverride ?: Ok(
            ProductPriceIndication(
                catalogPrice = catalogPrice,
                effectivePrice = catalogPrice,
                currency = currency,
                appliedFlashSale = null,
                appliedDiscount = null,
                applicableDiscounts = emptyList(),
            )
        )
    }
}

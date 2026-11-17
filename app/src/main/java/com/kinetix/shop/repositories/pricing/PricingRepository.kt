package com.kinetix.shop.repositories.pricing

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.pricing.FlashSale
import com.kinetix.shop.models.pricing.ProductPriceIndication
import com.kinetix.shop.models.pricing.PromotionDiscount

interface PricingRepository {
    suspend fun getFlashSale(
        sku: ProductSku,
    ): Result<FlashSale?>

    suspend fun listActiveDiscounts(): Result<List<PromotionDiscount>>

    suspend fun computePriceIndication(
        sku: ProductSku,
        categorySlug: CategorySlug,
        merchantPrincipalId: PrincipalId,
        catalogPrice: String,
        currency: String,
    ): Result<ProductPriceIndication>
}

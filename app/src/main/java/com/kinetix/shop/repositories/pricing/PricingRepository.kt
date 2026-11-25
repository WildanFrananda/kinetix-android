package com.kinetix.shop.repositories.pricing

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.VoucherCode
import com.kinetix.shop.models.pricing.FlashSale
import com.kinetix.shop.models.pricing.ProductPriceIndication
import com.kinetix.shop.models.pricing.PromotionDiscount
import com.kinetix.shop.models.pricing.VoucherTerms
import java.math.BigDecimal

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

    suspend fun getVoucherTerms(
        code: VoucherCode,
    ): Result<VoucherTerms>

    suspend fun previewVoucher(
        code: VoucherCode,
        cartSubtotal: BigDecimal,
        merchantPrincipalId: PrincipalId?,
    ): Result<VoucherTerms?>
}

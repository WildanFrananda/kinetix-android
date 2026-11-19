package com.kinetix.shop.repositories.pricing

import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.pricing.DiscountType
import com.kinetix.shop.models.pricing.FlashSale
import com.kinetix.shop.models.pricing.ProductPriceIndication
import com.kinetix.shop.models.pricing.PromotionDiscount
import com.kinetix.shop.services.remote.PricingApiService
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PricingRepositoryImpl @Inject constructor(
    private val apiService: PricingApiService,
    private val safeApiCaller: SafeApiCaller,
) : PricingRepository {
    override suspend fun getFlashSale(
        sku: ProductSku,
    ): Result<FlashSale?> {
        return safeApiCaller.executeNullable(
            call = {
                apiService.getFlashSale(
                    productSku = sku.value,
                )
            },
            onNullSuccess = {
                Ok(null)
            },
            transform = { dto ->
                if (dto.flashPrice.toBigDecimalOrNull() == null) {
                    return@executeNullable Err(
                        UnreadableAnswerFailure("Invalid flash sale price '${dto.flashPrice}'")
                    )
                }

                Ok(
                    FlashSale(
                        id = dto.id,
                        title = dto.title,
                        productSku = ProductSku(dto.productId),
                        flashPrice = dto.flashPrice,
                        stockLimit = dto.stockLimit,
                        stockSold = dto.stockSold,
                        active = dto.active,
                        startTime = dto.startTime,
                        endTime = dto.endTime,
                        merchantPrincipalId = PrincipalId(dto.merchantPrincipalId),
                    )
                )
            },
        )
    }

    override suspend fun listActiveDiscounts(): Result<List<PromotionDiscount>> {
        return safeApiCaller.execute(
            call = {
                apiService.listDiscounts()
            },
            transform = { dtos ->
                val result = mutableListOf<PromotionDiscount>()
                for (dto in dtos) {
                    if (dto.value.toBigDecimalOrNull() == null) {
                        return@execute Err(
                            UnreadableAnswerFailure("Invalid discount value '${dto.value}'")
                        )
                    }
                    val type = if (dto.discountType.equals("PERCENTAGE", ignoreCase = true)) {
                        DiscountType.PERCENTAGE
                    } else {
                        DiscountType.FIXED
                    }
                    result.add(
                        PromotionDiscount(
                            id = dto.id,
                            title = dto.title,
                            discountType = type,
                            value = dto.value,
                            targetProductSku = dto.targetProductId?.let { ProductSku(it) },
                            targetCategorySlug = dto.targetCategoryId?.let { CategorySlug(it) },
                            active = dto.active,
                            startTime = dto.startTime,
                            endTime = dto.endTime,
                            merchantPrincipalId = dto.merchantPrincipalId?.let { PrincipalId(it) },
                        )
                    )
                }
                Ok(result)
            },
        )
    }

    override suspend fun computePriceIndication(
        sku: ProductSku,
        categorySlug: CategorySlug,
        merchantPrincipalId: PrincipalId,
        catalogPrice: String,
        currency: String,
    ): Result<ProductPriceIndication> {
        val baseDecimal = catalogPrice.toBigDecimalOrNull()
            ?: return Err(UnreadableAnswerFailure("Invalid catalog price '$catalogPrice'"))

        val flashSale = when (val flashResult = getFlashSale(sku)) {
            is Ok -> flashResult.value
            is Err -> null
        }

        if (flashSale != null && flashSale.active && flashSale.remainingQuota > 0) {
            val flashDecimal = flashSale.flashPrice.toBigDecimalOrNull()
            if (flashDecimal != null && flashDecimal < baseDecimal) {
                return Ok(
                    ProductPriceIndication(
                        catalogPrice = catalogPrice,
                        effectivePrice = flashDecimal.toPlainString(),
                        currency = currency,
                        appliedFlashSale = flashSale,
                        appliedDiscount = null,
                        applicableDiscounts = emptyList(),
                    )
                )
            }
        }

        val discounts = when (val discountResult = listActiveDiscounts()) {
            is Ok -> discountResult.value
            is Err -> emptyList()
        }

        val matchingDiscounts = discounts.filter { discount ->
            val targetMatches =
                discount.targetProductSku == sku ||
                    discount.targetCategorySlug == categorySlug ||
                    (discount.targetProductSku == null && discount.targetCategorySlug == null)
            val merchantMatches =
                discount.merchantPrincipalId == null ||
                    discount.merchantPrincipalId == merchantPrincipalId
            discount.active && targetMatches && merchantMatches
        }

        var bestDecimal = baseDecimal
        var bestDiscount: PromotionDiscount? = null
        for (discount in matchingDiscounts) {
            val discountVal = discount.value.toBigDecimalOrNull() ?: continue
            val discountedDecimal = when (discount.discountType) {
                DiscountType.PERCENTAGE -> {
                    val reduction = baseDecimal
                        .multiply(discountVal)
                        .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    (baseDecimal - reduction).max(BigDecimal.ZERO)
                }
                DiscountType.FIXED -> {
                    (baseDecimal - discountVal).max(BigDecimal.ZERO)
                }
            }
            if (discountedDecimal < bestDecimal) {
                bestDecimal = discountedDecimal
                bestDiscount = discount
            }
        }

        return Ok(
            ProductPriceIndication(
                catalogPrice = catalogPrice,
                effectivePrice = bestDecimal.toPlainString(),
                currency = currency,
                appliedFlashSale = null,
                appliedDiscount = bestDiscount,
                applicableDiscounts = matchingDiscounts,
            )
        )
    }
}

package com.kinetix.shop.models.pricing

data class ProductPriceIndication(
    val catalogPrice: String,
    val effectivePrice: String,
    val currency: String,
    val appliedFlashSale: FlashSale?,
    val applicableDiscounts: List<PromotionDiscount>,
)

package com.kinetix.shop.models.pricing

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.VoucherCode
import java.math.BigDecimal

data class VoucherTerms(
    val code: VoucherCode,
    val title: String,
    val discountType: DiscountType,
    val value: BigDecimal,
    val minSpend: BigDecimal,
    val maxDiscount: BigDecimal?,
    val expiresAt: String,
    val merchantPrincipalId: PrincipalId?,
)

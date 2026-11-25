package com.kinetix.shop.models.cart

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.VoucherCode
import java.math.BigDecimal

data class Cart(
    val customerPrincipalId: PrincipalId,
    val items: List<CartItem>,
    val appliedVoucherCode: VoucherCode?,
    val subtotal: BigDecimal,
    val updatedAt: String,
) {
    fun hasMultipleMerchants(): Boolean {
        if (items.size <= 1) {
            return false
        }
        return items.map { it.merchantPrincipalId?.value }.distinct().size > 1
    }

    fun singleMerchantPrincipalId(): PrincipalId? {
        val merchants = items.mapNotNull { it.merchantPrincipalId?.value }.distinct()
        if (merchants.size != 1 || items.any { it.merchantPrincipalId == null }) {
            return null
        }
        return PrincipalId(merchants.first())
    }
}

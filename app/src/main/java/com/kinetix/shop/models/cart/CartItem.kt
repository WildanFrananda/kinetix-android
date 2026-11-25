package com.kinetix.shop.models.cart

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import java.math.BigDecimal

data class CartItem(
    val productId: ProductSku,
    val productTitle: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val categoryId: String?,
    val merchantPrincipalId: PrincipalId?,
    val lineTotal: BigDecimal,
)

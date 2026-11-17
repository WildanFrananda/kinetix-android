package com.kinetix.shop.routes

import com.kinetix.shop.models.ids.ProductSku

data class ProductDetailRoute(
    val sku: ProductSku,
) : AppRoute {
    override val requiresAuthentication: Boolean = false
}

package com.kinetix.shop.routes

import com.kinetix.shop.models.ids.ProductSku
import kotlinx.serialization.Serializable

@Serializable
data class ProductDetailRoute(
    val sku: String,
) : AppRoute {
    fun toProductSku(): ProductSku {
        return ProductSku(sku)
    }
}

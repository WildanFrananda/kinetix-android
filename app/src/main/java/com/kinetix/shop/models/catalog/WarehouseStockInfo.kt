package com.kinetix.shop.models.catalog

import com.kinetix.shop.models.ids.ProductSku

data class WarehouseStockInfo(
    val sku: ProductSku,
    val binLocation: String?,
    val availableQuantity: Int?,
    val reservedQuantity: Int?,
    val status: StockStatus,
)

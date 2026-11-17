package com.kinetix.shop.dto.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WarehouseStockDto(
    val sku: String,
    @SerialName("bin_location")
    val binLocation: String? = null,
    @SerialName("available_quantity")
    val availableQuantity: Int? = null,
    @SerialName("reserved_quantity")
    val reservedQuantity: Int? = null,
    @SerialName("stock_status")
    val stockStatus: String,
)

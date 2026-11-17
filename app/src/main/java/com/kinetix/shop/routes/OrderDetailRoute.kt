package com.kinetix.shop.routes

import com.kinetix.shop.models.ids.OrderId

data class OrderDetailRoute(
    val orderId: OrderId,
) : AppRoute {
    override val requiresAuthentication: Boolean = true
}

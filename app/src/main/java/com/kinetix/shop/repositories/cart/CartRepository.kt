package com.kinetix.shop.repositories.cart

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.cart.Cart
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.VoucherCode

interface CartRepository {
    suspend fun getCart(): Result<Cart>

    suspend fun addCartItem(
        sku: ProductSku,
        quantity: Int,
    ): Result<Cart>

    suspend fun updateCartItem(
        sku: ProductSku,
        quantity: Int,
    ): Result<Cart>

    suspend fun removeCartItem(
        sku: ProductSku,
    ): Result<Cart>

    suspend fun applyCartVoucher(
        code: VoucherCode,
    ): Result<Cart>

    suspend fun removeCartVoucher(): Result<Cart>

    suspend fun clearCart(): Result<UnitSentinel>
}

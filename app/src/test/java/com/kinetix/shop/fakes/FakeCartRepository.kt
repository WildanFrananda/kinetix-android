package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.cart.Cart
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.VoucherCode
import com.kinetix.shop.repositories.cart.CartRepository

class FakeCartRepository : CartRepository {
    var getCartHandler: suspend () -> Result<Cart> = {
        Err(UnauthorizedFailure("NOT_CONFIGURED"))
    }
    var addCartItemHandler: suspend (ProductSku, Int) -> Result<Cart> = { _, _ ->
        Err(UnauthorizedFailure("NOT_CONFIGURED"))
    }
    var updateCartItemHandler: suspend (ProductSku, Int) -> Result<Cart> = { _, _ ->
        Err(UnauthorizedFailure("NOT_CONFIGURED"))
    }
    var removeCartItemHandler: suspend (ProductSku) -> Result<Cart> = {
        Err(UnauthorizedFailure("NOT_CONFIGURED"))
    }
    var applyCartVoucherHandler: suspend (VoucherCode) -> Result<Cart> = {
        Err(UnauthorizedFailure("NOT_CONFIGURED"))
    }
    var removeCartVoucherHandler: suspend () -> Result<Cart> = {
        Err(UnauthorizedFailure("NOT_CONFIGURED"))
    }
    var clearCartHandler: suspend () -> Result<UnitSentinel> = {
        Ok(UnitSentinel)
    }

    override suspend fun getCart(): Result<Cart> {
        return getCartHandler()
    }

    override suspend fun addCartItem(
        sku: ProductSku,
        quantity: Int,
    ): Result<Cart> {
        return addCartItemHandler(sku, quantity)
    }

    override suspend fun updateCartItem(
        sku: ProductSku,
        quantity: Int,
    ): Result<Cart> {
        return updateCartItemHandler(sku, quantity)
    }

    override suspend fun removeCartItem(
        sku: ProductSku,
    ): Result<Cart> {
        return removeCartItemHandler(sku)
    }

    override suspend fun applyCartVoucher(
        code: VoucherCode,
    ): Result<Cart> {
        return applyCartVoucherHandler(code)
    }

    override suspend fun removeCartVoucher(): Result<Cart> {
        return removeCartVoucherHandler()
    }

    override suspend fun clearCart(): Result<UnitSentinel> {
        return clearCartHandler()
    }
}

package com.kinetix.shop.fakes

import com.kinetix.shop.dto.cart.AddCartItemRequestDto
import com.kinetix.shop.dto.cart.ApplyCartVoucherRequestDto
import com.kinetix.shop.dto.cart.CartResponseDto
import com.kinetix.shop.dto.cart.UpdateCartItemRequestDto
import com.kinetix.shop.services.remote.OrderApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeOrderApiService : OrderApiService {
    var getCartHandler: suspend (String) -> Response<CartResponseDto> = {
        Response.error(500, "not configured".toResponseBody("text/plain".toMediaType()))
    }
    var addCartItemHandler: suspend (String, AddCartItemRequestDto) -> Response<CartResponseDto> = { _, _ ->
        Response.error(500, "not configured".toResponseBody("text/plain".toMediaType()))
    }
    var updateCartItemHandler: suspend (String, String, UpdateCartItemRequestDto) -> Response<CartResponseDto> =
        { _, _, _ ->
            Response.error(500, "not configured".toResponseBody("text/plain".toMediaType()))
        }
    var removeCartItemHandler: suspend (String, String) -> Response<CartResponseDto> = { _, _ ->
        Response.error(500, "not configured".toResponseBody("text/plain".toMediaType()))
    }
    var applyCartVoucherHandler: suspend (String, ApplyCartVoucherRequestDto) -> Response<CartResponseDto> = { _, _ ->
        Response.error(500, "not configured".toResponseBody("text/plain".toMediaType()))
    }
    var removeCartVoucherHandler: suspend (String) -> Response<CartResponseDto> = {
        Response.error(500, "not configured".toResponseBody("text/plain".toMediaType()))
    }
    var clearCartHandler: suspend (String) -> Response<Unit> = {
        Response.success(204, Unit)
    }

    override suspend fun getCart(
        bearerToken: String,
    ): Response<CartResponseDto> {
        return getCartHandler(bearerToken)
    }

    override suspend fun addCartItem(
        bearerToken: String,
        request: AddCartItemRequestDto,
    ): Response<CartResponseDto> {
        return addCartItemHandler(bearerToken, request)
    }

    override suspend fun updateCartItem(
        bearerToken: String,
        productId: String,
        request: UpdateCartItemRequestDto,
    ): Response<CartResponseDto> {
        return updateCartItemHandler(bearerToken, productId, request)
    }

    override suspend fun removeCartItem(
        bearerToken: String,
        productId: String,
    ): Response<CartResponseDto> {
        return removeCartItemHandler(bearerToken, productId)
    }

    override suspend fun applyCartVoucher(
        bearerToken: String,
        request: ApplyCartVoucherRequestDto,
    ): Response<CartResponseDto> {
        return applyCartVoucherHandler(bearerToken, request)
    }

    override suspend fun removeCartVoucher(
        bearerToken: String,
    ): Response<CartResponseDto> {
        return removeCartVoucherHandler(bearerToken)
    }

    override suspend fun clearCart(
        bearerToken: String,
    ): Response<Unit> {
        return clearCartHandler(bearerToken)
    }
}

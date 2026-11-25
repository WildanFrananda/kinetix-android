package com.kinetix.shop.services.remote

import com.kinetix.shop.dto.cart.AddCartItemRequestDto
import com.kinetix.shop.dto.cart.ApplyCartVoucherRequestDto
import com.kinetix.shop.dto.cart.CartResponseDto
import com.kinetix.shop.dto.cart.UpdateCartItemRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface OrderApiService {
    @GET("api/v1/cart")
    suspend fun getCart(
        @Header("Authorization") bearerToken: String,
    ): Response<CartResponseDto>

    @POST("api/v1/cart/items")
    suspend fun addCartItem(
        @Header("Authorization") bearerToken: String,
        @Body request: AddCartItemRequestDto,
    ): Response<CartResponseDto>

    @PUT("api/v1/cart/items/{productId}")
    suspend fun updateCartItem(
        @Header("Authorization") bearerToken: String,
        @Path("productId") productId: String,
        @Body request: UpdateCartItemRequestDto,
    ): Response<CartResponseDto>

    @DELETE("api/v1/cart/items/{productId}")
    suspend fun removeCartItem(
        @Header("Authorization") bearerToken: String,
        @Path("productId") productId: String,
    ): Response<CartResponseDto>

    @POST("api/v1/cart/voucher")
    suspend fun applyCartVoucher(
        @Header("Authorization") bearerToken: String,
        @Body request: ApplyCartVoucherRequestDto,
    ): Response<CartResponseDto>

    @DELETE("api/v1/cart/voucher")
    suspend fun removeCartVoucher(
        @Header("Authorization") bearerToken: String,
    ): Response<CartResponseDto>

    @DELETE("api/v1/cart")
    suspend fun clearCart(
        @Header("Authorization") bearerToken: String,
    ): Response<Unit>
}

package com.kinetix.shop.repositories.cart

import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.dto.cart.AddCartItemRequestDto
import com.kinetix.shop.dto.cart.ApplyCartVoucherRequestDto
import com.kinetix.shop.dto.cart.CartResponseDto
import com.kinetix.shop.dto.cart.UpdateCartItemRequestDto
import com.kinetix.shop.models.cart.Cart
import com.kinetix.shop.models.cart.CartItem
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.VoucherCode
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.services.remote.OrderApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepositoryImpl @Inject constructor(
    private val apiService: OrderApiService,
    private val safeApiCaller: SafeApiCaller,
    private val sessionRepository: SessionRepository,
) : CartRepository {
    override suspend fun getCart(): Result<Cart> {
        return sessionRepository.withValidSession { session ->
            safeApiCaller.execute(
                call = {
                    apiService.getCart(
                        bearerToken = "Bearer ${session.accessToken}",
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
        }
    }

    override suspend fun addCartItem(
        sku: ProductSku,
        quantity: Int,
    ): Result<Cart> {
        return executeWrite { bearer ->
            safeApiCaller.execute(
                call = {
                    apiService.addCartItem(
                        bearerToken = bearer,
                        request = AddCartItemRequestDto(
                            productId = sku.value,
                            quantity = quantity,
                        ),
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
        }
    }

    override suspend fun updateCartItem(
        sku: ProductSku,
        quantity: Int,
    ): Result<Cart> {
        return executeWrite { bearer ->
            safeApiCaller.execute(
                call = {
                    apiService.updateCartItem(
                        bearerToken = bearer,
                        productId = sku.value,
                        request = UpdateCartItemRequestDto(
                            quantity = quantity,
                        ),
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
        }
    }

    override suspend fun removeCartItem(
        sku: ProductSku,
    ): Result<Cart> {
        return executeWrite { bearer ->
            safeApiCaller.execute(
                call = {
                    apiService.removeCartItem(
                        bearerToken = bearer,
                        productId = sku.value,
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
        }
    }

    override suspend fun applyCartVoucher(
        code: VoucherCode,
    ): Result<Cart> {
        return executeWrite { bearer ->
            safeApiCaller.execute(
                call = {
                    apiService.applyCartVoucher(
                        bearerToken = bearer,
                        request = ApplyCartVoucherRequestDto(
                            voucherCode = code.value,
                        ),
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
        }
    }

    override suspend fun removeCartVoucher(): Result<Cart> {
        return executeWrite { bearer ->
            safeApiCaller.execute(
                call = {
                    apiService.removeCartVoucher(
                        bearerToken = bearer,
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
        }
    }

    override suspend fun clearCart(): Result<UnitSentinel> {
        return executeWrite { bearer ->
            safeApiCaller.executeNoContent(
                call = {
                    apiService.clearCart(
                        bearerToken = bearer,
                    )
                },
            )
        }
    }

    private suspend fun <T> executeWrite(
        block: suspend (String) -> Result<T>,
    ): Result<T> {
        return sessionRepository.withValidSession { session ->
            val result = block("Bearer ${session.accessToken}")
            if (
                result is Err &&
                result.failure is UnavailableFailure &&
                result.failure.code == "CART_UNAVAILABLE"
            ) {
                Err(
                    UnknownOutcomeFailure(
                        code = "CART_UNAVAILABLE",
                        requestId = result.failure.requestId,
                    )
                )
            } else {
                result
            }
        }
    }

    private fun mapToDomain(
        dto: CartResponseDto,
    ): Cart {
        val subtotal = dto.subtotal.content.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
        return Cart(
            customerPrincipalId = PrincipalId(dto.customerPrincipalId),
            items = dto.items.map { item ->
                CartItem(
                    productId = ProductSku(item.productId),
                    productTitle = item.productTitle,
                    unitPrice = item.unitPrice.content.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    quantity = item.quantity,
                    categoryId = item.categoryId,
                    merchantPrincipalId = item.merchantPrincipalId?.let { PrincipalId(it) },
                    lineTotal = item.lineTotal.content.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                )
            },
            appliedVoucherCode = dto.appliedVoucherCode?.takeIf { it.isNotBlank() }?.let { VoucherCode(it) },
            subtotal = subtotal,
            updatedAt = dto.updatedAt,
        )
    }
}

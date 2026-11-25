package com.kinetix.shop.repositories.cart

import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.ValidationFailure
import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.cart.CartItemDto
import com.kinetix.shop.dto.cart.CartResponseDto
import com.kinetix.shop.fakes.FakeOrderApiService
import com.kinetix.shop.fakes.FakeSessionRepository
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.VoucherCode
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class CartRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(ApiErrorParser(json)))
    private val activeSession = StoredSession(
        accessToken = "acc-buyer",
        refreshToken = "ref-buyer",
        expiresAtEpochSeconds = 1_800_000_000L,
        accountId = AccountId(42L),
        principalId = PrincipalId("4c2e8a71-5b3d-4f6e-9a1c-7d8e2f3b4a50"),
        email = "buyer@kinetix.test",
        role = "customer",
    )

    @Test
    fun getCartMaps503CartUnavailableToUnavailableFailureWhileWritesMapItToUnknownOutcomeFailure() = runTest {
        val apiService = FakeOrderApiService().apply {
            val unavailableBody = """{"error":"CART_UNAVAILABLE","message":"your cart cannot be reached right now"}"""
            getCartHandler = {
                Response.error(503, unavailableBody.toResponseBody("application/json".toMediaType()))
            }
            addCartItemHandler = { _, _ ->
                Response.error(503, unavailableBody.toResponseBody("application/json".toMediaType()))
            }
        }
        val repository = CartRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            sessionRepository = FakeSessionRepository(initialSession = activeSession),
        )

        val readResult = repository.getCart()
        assertTrue(readResult is Err)
        assertTrue((readResult as Err).failure is UnavailableFailure)

        val writeResult = repository.addCartItem(ProductSku("KOPI-ARABIKA-250"), 1)
        assertTrue(writeResult is Err)
        val writeFailure = (writeResult as Err).failure
        assertTrue(writeFailure is UnknownOutcomeFailure)
        assertEquals("CART_UNAVAILABLE", (writeFailure as UnknownOutcomeFailure).code)
    }

    @Test
    fun addAndApplyVoucherMap409CartBusyAnd422And404RefusalsAccurately() = runTest {
        val apiService = FakeOrderApiService().apply {
            addCartItemHandler = { _, _ ->
                Response.error(
                    409,
                    """{"error":"CART_BUSY","message":"the cart changed under 10 attempts"}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            }
            applyCartVoucherHandler = { _, _ ->
                Response.error(
                    400,
                    """{"error":"VOUCHER_CODE_REQUIRED","message":"name the voucher"}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            }
        }
        val repository = CartRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            sessionRepository = FakeSessionRepository(initialSession = activeSession),
        )

        val busy = repository.addCartItem(ProductSku("SKU-1"), 2)
        assertTrue(busy is Err)
        assertTrue((busy as Err).failure is ConflictFailure)
        assertEquals("CART_BUSY", (busy.failure as ConflictFailure).code)

        val voucherRequired = repository.applyCartVoucher(VoucherCode(""))
        assertTrue(voucherRequired is Err)
        assertTrue((voucherRequired as Err).failure is ValidationFailure)
        assertEquals("VOUCHER_CODE_REQUIRED", (voucherRequired.failure as ValidationFailure).code)

        apiService.addCartItemHandler = { _, _ ->
            Response.error(
                404,
                """{"error":"PRODUCT_NOT_IN_CATALOG","productId":"MISSING","message":"not in catalog"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val missing = repository.addCartItem(ProductSku("MISSING"), 1)
        assertTrue(missing is Err)
        assertTrue((missing as Err).failure is NotFoundFailure)

        apiService.addCartItemHandler = { _, _ ->
            Response.success(
                CartResponseDto(
                    customerPrincipalId = activeSession.principalId.value,
                    items = listOf(
                        CartItemDto(
                            productId = "KOPI-ARABIKA-250",
                            productTitle = "Kopi Arabika",
                            unitPrice = JsonPrimitive(85000),
                            quantity = 2,
                            categoryId = "kopi",
                            merchantPrincipalId = "merch-1",
                            lineTotal = JsonPrimitive(170000),
                        )
                    ),
                    appliedVoucherCode = null,
                    subtotal = JsonPrimitive(170000),
                    updatedAt = "2026-10-22T03:15:07Z",
                )
            )
        }
        val added = repository.addCartItem(ProductSku("KOPI-ARABIKA-250"), 2)
        assertTrue(added is Ok)
        assertEquals(BigDecimal("170000"), (added as Ok).value.subtotal)
    }
}

package com.kinetix.shop.viewmodels.cart

import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeCartRepository
import com.kinetix.shop.fakes.FakePricingRepository
import com.kinetix.shop.models.cart.Cart
import com.kinetix.shop.models.cart.CartItem
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.VoucherCode
import com.kinetix.shop.models.pricing.DiscountType
import com.kinetix.shop.models.pricing.VoucherTerms
import com.kinetix.shop.state.cart.CartLoadedUiState
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshesCartAutomaticallyAfterUnknownOutcomeWriteFailureAndAppliesVoucherWithPreview() = runTest(testDispatcher) {
        val initialCart = Cart(
            customerPrincipalId = PrincipalId("buyer-1"),
            items = listOf(
                CartItem(
                    productId = ProductSku("SKU-1"),
                    productTitle = "Kopi Arabika",
                    unitPrice = BigDecimal("85000"),
                    quantity = 2,
                    categoryId = "kopi",
                    merchantPrincipalId = PrincipalId("merch-1"),
                    lineTotal = BigDecimal("170000"),
                )
            ),
            appliedVoucherCode = null,
            subtotal = BigDecimal("170000"),
            updatedAt = "2026-10-22T03:15:07Z",
        )
        val refreshedCart = initialCart.copy(
            items = listOf(initialCart.items.first().copy(quantity = 3, lineTotal = BigDecimal("255000"))),
            subtotal = BigDecimal("255000"),
        )
        var getCalls = 0
        val cartRepo = FakeCartRepository().apply {
            getCartHandler = {
                getCalls += 1
                if (getCalls == 1) Ok(initialCart) else Ok(refreshedCart)
            }
            updateCartItemHandler = { _, _ ->
                Err(UnknownOutcomeFailure("CART_UNAVAILABLE"))
            }
            applyCartVoucherHandler = { code ->
                Ok(refreshedCart.copy(appliedVoucherCode = code))
            }
            removeCartVoucherHandler = {
                Ok(refreshedCart.copy(appliedVoucherCode = null))
            }
        }
        val voucherTerms = VoucherTerms(
            code = VoucherCode("HEMAT10"),
            title = "Diskon 10%",
            discountType = DiscountType.PERCENTAGE,
            value = BigDecimal("10"),
            minSpend = BigDecimal("100000"),
            maxDiscount = null,
            expiresAt = "2026-12-31T23:59:59Z",
            merchantPrincipalId = PrincipalId("merch-1"),
        )
        val pricingRepo = FakePricingRepository().apply {
            voucherTermsResult = Ok(voucherTerms)
            previewVoucherResult = Ok(voucherTerms)
        }

        val viewModel = CartViewModel(
            cartRepository = cartRepo,
            pricingRepository = pricingRepo,
        )
        advanceUntilIdle()

        viewModel.updateItemQuantity(ProductSku("SKU-1"), 3)
        advanceUntilIdle()

        val afterUnknownWrite = viewModel.uiState.value as CartLoadedUiState
        assertEquals(2, getCalls)
        assertEquals(3, afterUnknownWrite.cart.items.single().quantity)
        assertTrue(afterUnknownWrite.actionFailure is UnknownOutcomeFailure)

        viewModel.applyVoucher("HEMAT10")
        advanceUntilIdle()

        val afterVoucher = viewModel.uiState.value as CartLoadedUiState
        assertEquals(VoucherCode("HEMAT10"), afterVoucher.cart.appliedVoucherCode)
        assertTrue(afterVoucher.voucherPreviewAccepted)
        assertFalse(afterVoucher.voucherPreviewRejected)

        viewModel.removeVoucher()
        advanceUntilIdle()
        val afterRemove = viewModel.uiState.value as CartLoadedUiState
        assertNull(afterRemove.cart.appliedVoucherCode)
    }

    @Test
    fun detectsMultipleMerchantsOnCart() {
        val multiMerchantCart = Cart(
            customerPrincipalId = PrincipalId("buyer-1"),
            items = listOf(
                CartItem(
                    productId = ProductSku("SKU-1"),
                    productTitle = "Item A",
                    unitPrice = BigDecimal("10000"),
                    quantity = 1,
                    categoryId = null,
                    merchantPrincipalId = PrincipalId("merch-1"),
                    lineTotal = BigDecimal("10000"),
                ),
                CartItem(
                    productId = ProductSku("SKU-2"),
                    productTitle = "Item B",
                    unitPrice = BigDecimal("20000"),
                    quantity = 1,
                    categoryId = null,
                    merchantPrincipalId = PrincipalId("merch-2"),
                    lineTotal = BigDecimal("20000"),
                ),
            ),
            appliedVoucherCode = null,
            subtotal = BigDecimal("30000"),
            updatedAt = "2026-10-22T03:15:07Z",
        )
        assertTrue(multiMerchantCart.hasMultipleMerchants())
        assertNull(multiMerchantCart.singleMerchantPrincipalId())
    }
}

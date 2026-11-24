package com.kinetix.shop.viewmodels.catalog

import androidx.lifecycle.SavedStateHandle
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeCatalogRepository
import com.kinetix.shop.fakes.FakePricingRepository
import com.kinetix.shop.fakes.FakeReviewRepository
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.catalog.WarehouseStockInfo
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.state.catalog.ProductDetailLoadedUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {
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
    fun loadsAutomaticallyFromSavedStateHandleAndDegradesGracefullyWhenPricingAndReviewsFail() = runTest(testDispatcher) {
        val unavailable = UnavailableFailure(
            code = "UPSTREAM_UNAVAILABLE",
            retryAfterSeconds = null,
        )
        val sku = ProductSku("SKU-500")
        val detail = ProductDetail(
            id = 500L,
            sku = sku,
            title = "Kinetix Pro Mouse",
            description = "Ultra-lightweight wireless mouse",
            categoryName = "Peripherals",
            categorySlug = CategorySlug("peripherals"),
            price = "89.00",
            currency = "USD",
            imageUrl = "https://cdn.kinetix.test/mouse.png",
            merchantPrincipalId = PrincipalId("merch-1"),
            weightGrams = 60,
            warehouseStock = WarehouseStockInfo(
                sku = sku,
                binLocation = "A-01",
                availableQuantity = 14,
                reservedQuantity = 0,
                status = StockStatus.IN_STOCK,
            ),
        )
        val catalogRepo = FakeCatalogRepository().apply {
            productDetailResult = Ok(detail)
        }
        val pricingRepo = FakePricingRepository().apply {
            priceIndicationOverride = Err(unavailable)
        }
        val reviewRepo = FakeReviewRepository().apply {
            productReviewsResult = Err(unavailable)
            merchantReviewsResult = Err(unavailable)
        }

        val viewModel = ProductDetailViewModel(
            catalogRepository = catalogRepo,
            pricingRepository = pricingRepo,
            reviewRepository = reviewRepo,
            savedStateHandle = SavedStateHandle(mapOf("sku" to sku.value)),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailLoadedUiState)
        val loaded = state as ProductDetailLoadedUiState
        assertEquals("89.00", loaded.priceIndication.effectivePrice)
        assertNull(loaded.productReviews)
        assertNull(loaded.merchantReviews)
    }
}

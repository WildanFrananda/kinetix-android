package com.kinetix.shop.repositories.catalog

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.catalog.CategoryDto
import com.kinetix.shop.dto.catalog.ProductDetailResponseDto
import com.kinetix.shop.dto.catalog.WarehouseStockDto
import com.kinetix.shop.fakes.FakeCatalogApiService
import com.kinetix.shop.fakes.FakeCatalogCacheStore
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.models.ids.ProductSku
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class CatalogRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(ApiErrorParser(json)))

    @Test
    fun getProductDetailMapsPriceAndMarksCachedFallbackWithUnknownStockAndCacheOrigin() = runTest {
        val cacheStore = FakeCatalogCacheStore()
        var isOffline = false
        val apiService = FakeCatalogApiService().apply {
            getProductHandler = { sku ->
                if (isOffline) {
                    throw IOException("Offline")
                }
                Response.success(
                    ProductDetailResponseDto(
                        id = 101L,
                        sku = sku,
                        title = "Mechanical Keyboard",
                        description = "Hot-swappable 75%",
                        category = "Peripherals",
                        categorySlug = "peripherals",
                        price = "149.99",
                        currency = "USD",
                        imageUrl = "https://cdn.kinetix.test/kb.png",
                        merchantPrincipalId = "merch-1",
                        weightGrams = 950,
                        warehouseStock = WarehouseStockDto(
                            sku = sku,
                            binLocation = "A1",
                            availableQuantity = 12,
                            reservedQuantity = 2,
                            stockStatus = "in_stock",
                        ),
                    )
                )
            }
        }
        val repository = CatalogRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            catalogCacheStore = cacheStore,
        )

        val liveResult = repository.getProductDetail(ProductSku("SKU-KB-75"))
        assertTrue(liveResult is Ok)
        val liveDetail = (liveResult as Ok).value
        assertEquals("149.99", liveDetail.price)
        assertEquals(StockStatus.IN_STOCK, liveDetail.warehouseStock.status)
        assertEquals(DataOrigin.LIVE, liveDetail.dataOrigin)

        isOffline = true
        val cachedResult = repository.getProductDetail(ProductSku("SKU-KB-75"))
        assertTrue(cachedResult is Ok)
        val cachedDetail = (cachedResult as Ok).value
        assertEquals(DataOrigin.CACHE, cachedDetail.dataOrigin)
        assertNotNull(cachedDetail.cachedAtEpochSeconds)
        assertEquals(StockStatus.UNKNOWN, cachedDetail.warehouseStock.status)
        assertNull(cachedDetail.warehouseStock.availableQuantity)
    }

    @Test
    fun listCategoriesFallsBackToLocalCacheWhenOfflineWithCacheOrigin() = runTest {
        val cacheStore = FakeCatalogCacheStore()
        var isOffline = false
        val apiService = FakeCatalogApiService().apply {
            listCategoriesHandler = {
                if (isOffline) {
                    throw IOException("Offline")
                }
                Response.success(
                    listOf(
                        CategoryDto(
                            id = 1L,
                            name = "Peripherals",
                            slug = "peripherals",
                        )
                    )
                )
            }
        }
        val repository = CatalogRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            catalogCacheStore = cacheStore,
        )

        val onlineResult = repository.listCategories()
        assertTrue(onlineResult is Ok)
        assertEquals(DataOrigin.LIVE, (onlineResult as Ok).value.first().dataOrigin)

        isOffline = true
        val offlineResult = repository.listCategories()
        assertTrue(offlineResult is Ok)
        val cachedCategory = (offlineResult as Ok).value.single()
        assertEquals("Peripherals", cachedCategory.name)
        assertEquals(DataOrigin.CACHE, cachedCategory.dataOrigin)
        assertNotNull(cachedCategory.cachedAtEpochSeconds)
    }
}

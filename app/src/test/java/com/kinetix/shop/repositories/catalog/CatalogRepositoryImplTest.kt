package com.kinetix.shop.repositories.catalog

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.catalog.ProductDetailResponseDto
import com.kinetix.shop.dto.catalog.WarehouseStockDto
import com.kinetix.shop.fakes.FakeCatalogApiService
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.ids.ProductSku
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
    fun getProductDetailMapsPriceAndDegradedWarehouseStockStatus() = runTest {
        val apiService = FakeCatalogApiService().apply {
            getProductHandler = { sku ->
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
                            binLocation = null,
                            availableQuantity = null,
                            reservedQuantity = null,
                            stockStatus = "unknown",
                        ),
                    )
                )
            }
        }
        val repository = CatalogRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
        )

        val result = repository.getProductDetail(ProductSku("SKU-KB-75"))
        assertTrue(result is Ok)
        val detail = (result as Ok).value
        assertEquals("149.99", detail.price)
        assertEquals(StockStatus.UNKNOWN, detail.warehouseStock.status)
    }
}

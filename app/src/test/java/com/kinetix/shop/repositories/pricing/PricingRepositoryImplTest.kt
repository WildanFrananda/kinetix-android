package com.kinetix.shop.repositories.pricing

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.pricing.DiscountDto
import com.kinetix.shop.dto.pricing.FlashSaleDto
import com.kinetix.shop.fakes.FakePricingApiService
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class PricingRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(ApiErrorParser(json)))

    @Test
    fun computePriceIndicationPrioritizesActiveFlashSaleOverDiscounts() = runTest {
        val apiService = FakePricingApiService().apply {
            getFlashSaleHandler = {
                Response.success(
                    json.encodeToJsonElement(
                        FlashSaleDto.serializer(),
                        FlashSaleDto(
                            id = "fs-1",
                            title = "Noon Flash",
                            productId = "SKU-1",
                            flashPrice = "75.00",
                            stockLimit = 50,
                            stockSold = 10,
                            active = true,
                            startTime = "2026-10-20T10:00:00Z",
                            endTime = "2026-10-20T14:00:00Z",
                            merchantPrincipalId = "merch-1",
                        ),
                    )
                )
            }
            listDiscountsHandler = {
                Response.success(
                    listOf(
                        DiscountDto(
                            id = "disc-1",
                            title = "10% Off",
                            discountType = "PERCENTAGE",
                            value = "10",
                            targetProductId = "SKU-1",
                            targetCategoryId = null,
                            active = true,
                            startTime = "2026-10-20T00:00:00Z",
                            endTime = "2026-10-21T00:00:00Z",
                            merchantPrincipalId = "merch-1",
                        )
                    )
                )
            }
        }
        val repository = PricingRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            json = json,
        )

        val result = repository.computePriceIndication(
            sku = ProductSku("SKU-1"),
            categorySlug = CategorySlug("audio"),
            merchantPrincipalId = PrincipalId("merch-1"),
            catalogPrice = "100.00",
            currency = "USD",
        )
        assertTrue(result is Ok)
        val indication = (result as Ok).value
        assertEquals("75.00", indication.effectivePrice)
        assertNotNull(indication.appliedFlashSale)
        assertTrue(indication.applicableDiscounts.isEmpty())
    }

    @Test
    fun computePriceIndicationAppliesPlatformFlashSaleWithNullMerchantPrincipalId() = runTest {
        val apiService = FakePricingApiService().apply {
            getFlashSaleHandler = {
                Response.success(
                    json.parseToJsonElement(
                        """{"id":"fs-platform","title":"Platform Flash","product_id":"SKU-1","flash_price":"60.00","stock_limit":20,"stock_sold":2,"active":true,"start_time":"2026-10-20T10:00:00Z","end_time":"2026-10-20T14:00:00Z","merchant_principal_id":null}"""
                    )
                )
            }
        }
        val repository = PricingRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            json = json,
        )

        val result = repository.computePriceIndication(
            sku = ProductSku("SKU-1"),
            categorySlug = CategorySlug("audio"),
            merchantPrincipalId = PrincipalId("merch-1"),
            catalogPrice = "100.00",
            currency = "IDR",
        )
        assertTrue(result is Ok)
        val indication = (result as Ok).value
        assertEquals("60.00", indication.effectivePrice)
        assertNotNull(indication.appliedFlashSale)
        assertNull(indication.appliedFlashSale?.merchantPrincipalId)
    }

    @Test
    fun computePriceIndicationFallsBackToMatchingMerchantDiscountWhenNoFlashSale() = runTest {
        val apiService = FakePricingApiService().apply {
            getFlashSaleHandler = { Response.success(JsonNull) }
            listDiscountsHandler = {
                Response.success(
                    listOf(
                        DiscountDto(
                            id = "disc-cat",
                            title = "20% Off Audio",
                            discountType = "PERCENTAGE",
                            value = "20",
                            targetProductId = null,
                            targetCategoryId = "audio",
                            active = true,
                            startTime = "2026-10-20T00:00:00Z",
                            endTime = "2026-10-21T00:00:00Z",
                            merchantPrincipalId = "merch-1",
                        )
                    )
                )
            }
        }
        val repository = PricingRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            json = json,
        )

        val result = repository.computePriceIndication(
            sku = ProductSku("SKU-2"),
            categorySlug = CategorySlug("audio"),
            merchantPrincipalId = PrincipalId("merch-1"),
            catalogPrice = "200.00",
            currency = "USD",
        )
        assertTrue(result is Ok)
        val indication = (result as Ok).value
        assertEquals("160.00", indication.effectivePrice)
        assertNull(indication.appliedFlashSale)
        assertEquals("disc-cat", indication.applicableDiscounts.first().id)
    }
}

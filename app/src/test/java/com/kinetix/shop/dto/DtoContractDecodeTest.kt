package com.kinetix.shop.dto

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kinetix.shop.core.serialization.BigDecimalSerializer
import com.kinetix.shop.di.CoreModule
import com.kinetix.shop.dto.auth.CurrentSessionResponseDto
import com.kinetix.shop.dto.auth.LoginResponseDto
import com.kinetix.shop.dto.auth.SessionTokensResponseDto
import com.kinetix.shop.dto.auth.TwoFactorSetupResponseDto
import com.kinetix.shop.dto.auth.TwoFactorStatusResponseDto
import com.kinetix.shop.dto.catalog.CategoryDto
import com.kinetix.shop.dto.catalog.ProductDetailResponseDto
import com.kinetix.shop.dto.catalog.ProductListResponseDto
import com.kinetix.shop.dto.pricing.DiscountDto
import com.kinetix.shop.dto.pricing.FlashSaleDto
import com.kinetix.shop.dto.profile.UserProfileResponseDto
import com.kinetix.shop.dto.review.MerchantReviewsResponseDto
import com.kinetix.shop.dto.review.ProductReviewsResponseDto
import com.kinetix.shop.dto.search.SearchProductsResponseDto
import com.kinetix.shop.dto.search.SearchSuggestionsResponseDto
import java.lang.reflect.Type
import java.math.BigDecimal
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class DtoContractDecodeTest {
    private val json = CoreModule.provideJson()
    private val converterFactory = json.asConverterFactory("application/json; charset=UTF-8".toMediaType())
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.kinetix.invalid/")
        .addConverterFactory(converterFactory)
        .build()

    private inline fun <reified T> decodeViaRetrofit(
        rawJson: String,
    ): T {
        val type: Type = T::class.java
        val converter = converterFactory.responseBodyConverter(type, emptyArray(), retrofit)
            ?: error("Missing converter for $type")
        @Suppress("UNCHECKED_CAST")
        return converter.convert(rawJson.toResponseBody("application/json".toMediaType())) as T
    }

    @Test
    fun decodesAuthAndProfileDtosThroughRetrofitConverter() {
        val tokens = decodeViaRetrofit<SessionTokensResponseDto>(
            """{"accessToken":"acc-1","refreshToken":"ref-1","expiresIn":900,"tokenType":"Bearer","user":{"id":12,"email":"buyer@kinetix.test","role":"customer","principalId":"11111111-2222-4333-8444-555555555555"}}"""
        )
        assertEquals("customer", tokens.user.role)
        assertEquals(12L, tokens.user.id)

        val loginMfa = decodeViaRetrofit<LoginResponseDto>(
            """{"mfaRequired":true,"challengeToken":"chal-1","expiresIn":300,"passkeys":false}"""
        )
        assertEquals(true, loginMfa.mfaRequired)
        assertNull(loginMfa.accessToken)

        val me = decodeViaRetrofit<CurrentSessionResponseDto>(
            """{"principalId":"11111111-2222-4333-8444-555555555555","userId":12,"email":"buyer@kinetix.test","role":"customer","expiresAt":"2026-11-22T12:00:00Z"}"""
        )
        assertEquals("customer", me.role)

        val setup = decodeViaRetrofit<TwoFactorSetupResponseDto>(
            """{"qrCodeUrl":"otpauth://totp/Kinetix:buyer@kinetix.test?secret=ABC","twoFactorSecret":"ABC","enabled":false}"""
        )
        assertEquals(false, setup.enabled)

        val status = decodeViaRetrofit<TwoFactorStatusResponseDto>(
            """{"enabled":true}"""
        )
        assertEquals(true, status.enabled)

        val profileWithNulls = decodeViaRetrofit<UserProfileResponseDto>(
            """{"id":5,"userId":12,"fullName":"Budi","phoneNumber":"+62812345678","streetAddress":"Jl. Sudirman 1","city":"Jakarta","postalCode":"10220","avatarUrl":null,"latitude":null,"longitude":null}"""
        )
        assertNull(profileWithNulls.avatarUrl)
        assertNull(profileWithNulls.geocodedAt)
    }

    @Test
    fun decodesCatalogAndPricingDtosIncludingPlatformNullsAndJsonNull() {
        val category = decodeViaRetrofit<CategoryDto>(
            """{"id":1,"name":"Audio","slug":"audio"}"""
        )
        assertEquals("audio", category.slug)

        val productList = decodeViaRetrofit<ProductListResponseDto>(
            """{"count":1,"page":1,"page_size":10,"results":[{"id":9,"sku":"SKU-9","title":"Headphones","category":"Audio","category_slug":"audio","price":"89000.00","currency":"IDR","image_url":"","merchant_principal_id":"m-1","weight_grams":null,"available_stock":null,"is_in_stock":null,"stock_status":"unknown"}]}"""
        )
        val summary = productList.results.single()
        assertNull(summary.availableStock)
        assertNull(summary.isInStock)
        assertEquals("unknown", summary.stockStatus)

        val detail = decodeViaRetrofit<ProductDetailResponseDto>(
            """{"id":9,"sku":"SKU-9","title":"Headphones","description":"Studio monitor","category":"Audio","category_slug":"audio","price":"89000.00","currency":"IDR","image_url":"https://img.kinetix.invalid/9.png","merchant_principal_id":"m-1","warehouse_stock":{"sku":"SKU-9","bin_location":null,"available_quantity":null,"reserved_quantity":null,"stock_status":"unknown"}}"""
        )
        assertNull(detail.warehouseStock.binLocation)
        assertEquals("unknown", detail.warehouseStock.stockStatus)

        val nullFlashSale = decodeViaRetrofit<JsonElement>("null")
        assertTrue(nullFlashSale is JsonNull)

        val platformFlashSale = decodeViaRetrofit<FlashSaleDto>(
            """{"id":"fs-1","title":"11.11 Flash","product_id":"SKU-9","flash_price":"75000.00","stock_limit":100,"stock_sold":15,"active":true,"start_time":"2026-11-11T00:00:00Z","end_time":"2026-11-11T23:59:59Z","merchant_principal_id":null}"""
        )
        assertNull(platformFlashSale.merchantPrincipalId)

        val platformDiscount = decodeViaRetrofit<DiscountDto>(
            """{"id":"d-1","title":"Platform 10%","discount_type":"PERCENTAGE","value":"10.00","active":true,"start_time":"2026-11-01T00:00:00Z","end_time":"2026-11-30T00:00:00Z"}"""
        )
        assertNull(platformDiscount.targetProductId)
        assertNull(platformDiscount.merchantPrincipalId)
    }

    @Test
    fun decodesSearchAndReviewDtosWithAbsentFacetsAndNullMerchantId() {
        val suggestions = decodeViaRetrofit<SearchSuggestionsResponseDto>(
            """{"suggestions":["headphones","headset"]}"""
        )
        assertEquals(2, suggestions.suggestions.size)

        val searchWithoutFacets = decodeViaRetrofit<SearchProductsResponseDto>(
            """{"total":1,"hits":[{"id":"SKU-9","merchant_id":"m-1","title":"Headphones","price_minor":8900000,"currency":"IDR"}],"index":{"indexed_through_ms":null,"age_seconds":null,"stale":true}}"""
        )
        assertNull(searchWithoutFacets.facets)
        assertEquals(8900000L, searchWithoutFacets.hits.single().priceMinor)
        assertTrue(searchWithoutFacets.hits.single().categories.isEmpty())

        val productReviews = decodeViaRetrofit<ProductReviewsResponseDto>(
            """{"product_id":"SKU-9","average_rating":4.5,"total_reviews":1,"data":[{"id":"rev-1","product_id":"SKU-9","rating":5,"comment":null,"created_at":"2026-11-10T10:00:00Z","merchant_principal_id":null}],"current_page":1,"last_page":1}"""
        )
        assertNull(productReviews.data.single().merchantPrincipalId)
        assertNull(productReviews.data.single().comment)

        val merchantReviews = decodeViaRetrofit<MerchantReviewsResponseDto>(
            """{"merchant_principal_id":"m-1","average_rating":5.0,"total_reviews":1,"data":[{"id":"rev-1","product_id":"SKU-9","rating":5,"comment":"Great","created_at":"2026-11-10T10:00:00Z","merchant_principal_id":"m-1"}],"current_page":1,"last_page":1}"""
        )
        assertEquals("m-1", merchantReviews.merchantPrincipalId)
    }

    @Test
    fun bigDecimalSerializerDecodesQuotedStringsAndUnquotedNumbersWithoutFloatPrecisionLoss() {
        val fromString = json.decodeFromString(BigDecimalSerializer, """"89000.00"""")
        val fromNumber = json.decodeFromString(BigDecimalSerializer, "202000.55")

        assertEquals(BigDecimal("89000.00"), fromString)
        assertEquals(BigDecimal("202000.55"), fromNumber)
        assertEquals(""""202000.55"""", json.encodeToString(BigDecimalSerializer, fromNumber))
    }
}

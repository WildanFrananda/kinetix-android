package com.kinetix.shop.repositories.catalog

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.catalog.ProductPage
import com.kinetix.shop.models.catalog.ProductSummary
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.catalog.WarehouseStockInfo
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.services.local.CatalogCacheStore
import com.kinetix.shop.services.remote.CatalogApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val apiService: CatalogApiService,
    private val safeApiCaller: SafeApiCaller,
    private val catalogCacheStore: CatalogCacheStore,
) : CatalogRepository {
    override suspend fun listCategories(): Result<List<Category>> {
        val networkResult = safeApiCaller.execute(
            call = {
                apiService.listCategories()
            },
            transform = { dtos ->
                Ok(
                    dtos.map { dto ->
                        Category(
                            id = dto.id,
                            name = dto.name,
                            slug = CategorySlug(dto.slug),
                        )
                    }
                )
            },
        )
        return when (networkResult) {
            is Ok -> {
                catalogCacheStore.saveCategories(networkResult.value)
                networkResult
            }

            is Err -> {
                if (canFallbackToCache(networkResult.failure)) {
                    val cached = catalogCacheStore.getCategories()
                    if (cached.isNotEmpty()) {
                        return Ok(cached)
                    }
                }
                networkResult
            }
        }
    }

    override suspend fun listProducts(
        categorySlug: CategorySlug?,
        query: String?,
        page: Int,
        pageSize: Int,
    ): Result<ProductPage> {
        val networkResult = safeApiCaller.execute(
            call = {
                apiService.listProducts(
                    categorySlug = categorySlug?.value,
                    query = query,
                    page = page,
                    pageSize = pageSize,
                )
            },
            transform = { dto ->
                val items = mutableListOf<ProductSummary>()
                for (item in dto.results) {
                    if (item.price.toBigDecimalOrNull() == null) {
                        return@execute Err(
                            UnreadableAnswerFailure("Invalid product price '${item.price}'")
                        )
                    }
                    items.add(
                        ProductSummary(
                            id = item.id,
                            sku = ProductSku(item.sku),
                            title = item.title,
                            categoryName = item.category,
                            categorySlug = CategorySlug(item.categorySlug),
                            price = item.price,
                            currency = item.currency,
                            imageUrl = item.imageUrl,
                            merchantPrincipalId = PrincipalId(item.merchantPrincipalId),
                            weightGrams = item.weightGrams,
                            availableStock = item.availableStock,
                            stockStatus = parseStockStatus(item.stockStatus),
                        )
                    )
                }
                Ok(
                    ProductPage(
                        totalCount = dto.count,
                        page = dto.page,
                        pageSize = dto.pageSize,
                        items = items,
                    )
                )
            },
        )
        return when (networkResult) {
            is Ok -> {
                catalogCacheStore.saveProductSummaries(networkResult.value.items)
                networkResult
            }

            is Err -> {
                if (page == 1 && canFallbackToCache(networkResult.failure)) {
                    val cached = catalogCacheStore.getProductSummaries(
                        categorySlug = categorySlug,
                        query = query,
                        limit = pageSize,
                    )
                    if (cached.isNotEmpty()) {
                        return Ok(
                            ProductPage(
                                totalCount = cached.size,
                                page = 1,
                                pageSize = pageSize,
                                items = cached,
                            )
                        )
                    }
                }
                networkResult
            }
        }
    }

    override suspend fun getProductDetail(
        sku: ProductSku,
    ): Result<ProductDetail> {
        val networkResult = safeApiCaller.execute(
            call = {
                apiService.getProduct(
                    sku = sku.value,
                )
            },
            transform = { dto ->
                if (dto.price.toBigDecimalOrNull() == null) {
                    return@execute Err(
                        UnreadableAnswerFailure("Invalid product price '${dto.price}'")
                    )
                }
                Ok(
                    ProductDetail(
                        id = dto.id,
                        sku = ProductSku(dto.sku),
                        title = dto.title,
                        description = dto.description,
                        categoryName = dto.category,
                        categorySlug = CategorySlug(dto.categorySlug),
                        price = dto.price,
                        currency = dto.currency,
                        imageUrl = dto.imageUrl,
                        merchantPrincipalId = PrincipalId(dto.merchantPrincipalId),
                        weightGrams = dto.weightGrams,
                        warehouseStock = WarehouseStockInfo(
                            sku = ProductSku(dto.warehouseStock.sku),
                            binLocation = dto.warehouseStock.binLocation,
                            availableQuantity = dto.warehouseStock.availableQuantity,
                            reservedQuantity = dto.warehouseStock.reservedQuantity,
                            status = parseStockStatus(dto.warehouseStock.stockStatus),
                        ),
                    )
                )
            },
        )
        return when (networkResult) {
            is Ok -> {
                catalogCacheStore.saveProductDetail(networkResult.value)
                networkResult
            }

            is Err -> {
                if (canFallbackToCache(networkResult.failure)) {
                    val cached = catalogCacheStore.getProductDetail(sku)
                    if (cached != null) {
                        return Ok(cached)
                    }
                }
                networkResult
            }
        }
    }

    private fun canFallbackToCache(
        failure: Failure,
    ): Boolean {
        return when (failure) {
            is NetworkFailure,
            is TimeoutFailure,
            is UnavailableFailure -> true

            else -> false
        }
    }

    private fun parseStockStatus(
        raw: String,
    ): StockStatus {
        return when (raw.lowercase()) {
            "in_stock", "available" -> StockStatus.IN_STOCK
            "out_of_stock" -> StockStatus.OUT_OF_STOCK
            else -> StockStatus.UNKNOWN
        }
    }
}

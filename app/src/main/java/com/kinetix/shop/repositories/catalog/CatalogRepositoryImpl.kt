package com.kinetix.shop.repositories.catalog

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
import com.kinetix.shop.services.remote.CatalogApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val apiService: CatalogApiService,
    private val safeApiCaller: SafeApiCaller,
) : CatalogRepository {
    override suspend fun listCategories(): Result<List<Category>> {
        return safeApiCaller.execute(
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
    }

    override suspend fun listProducts(
        categorySlug: CategorySlug?,
        query: String?,
        page: Int,
        pageSize: Int,
    ): Result<ProductPage> {
        return safeApiCaller.execute(
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
    }

    override suspend fun getProductDetail(
        sku: ProductSku,
    ): Result<ProductDetail> {
        return safeApiCaller.execute(
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

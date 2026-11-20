package com.kinetix.shop.services.local

import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.catalog.ProductSummary
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.catalog.WarehouseStockInfo
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCatalogCacheStore @Inject constructor(
    private val dao: CatalogCacheDao,
    private val clock: Clock,
) : CatalogCacheStore {
    override suspend fun saveCategories(
        categories: List<Category>,
    ) {
        val now = clock.nowEpochSeconds()
        dao.upsertCategories(
            categories.map { category ->
                CachedCategoryEntity(
                    slug = category.slug.value,
                    id = category.id,
                    name = category.name,
                    cachedAtEpochSeconds = now,
                )
            }
        )
    }

    override suspend fun getCategories(): List<Category> {
        return dao.listCategories().map { entity ->
            Category(
                id = entity.id,
                name = entity.name,
                slug = CategorySlug(entity.slug),
            )
        }
    }

    override suspend fun saveProductSummaries(
        products: List<ProductSummary>,
    ) {
        val now = clock.nowEpochSeconds()
        val entities = products.map { item ->
            val existing = dao.findProductBySku(item.sku.value)
            CachedProductEntity(
                sku = item.sku.value,
                id = item.id,
                title = item.title,
                description = existing?.description,
                categoryName = item.categoryName,
                categorySlug = item.categorySlug.value,
                price = item.price,
                currency = item.currency,
                imageUrl = item.imageUrl,
                merchantPrincipalId = item.merchantPrincipalId.value,
                weightGrams = item.weightGrams,
                availableQuantity = item.availableStock,
                reservedQuantity = existing?.reservedQuantity,
                binLocation = existing?.binLocation,
                stockStatus = item.stockStatus.name,
                cachedAtEpochSeconds = now,
            )
        }
        dao.upsertProducts(entities)
    }

    override suspend fun getProductSummaries(
        categorySlug: CategorySlug?,
        query: String?,
        limit: Int,
    ): List<ProductSummary> {
        return dao.listProducts(
            categorySlug = categorySlug?.value,
            query = query?.takeIf { it.isNotBlank() },
            limit = limit,
        ).map { entity ->
            ProductSummary(
                id = entity.id,
                sku = ProductSku(entity.sku),
                title = entity.title,
                categoryName = entity.categoryName,
                categorySlug = CategorySlug(entity.categorySlug),
                price = entity.price,
                currency = entity.currency,
                imageUrl = entity.imageUrl,
                merchantPrincipalId = PrincipalId(entity.merchantPrincipalId),
                weightGrams = entity.weightGrams,
                availableStock = entity.availableQuantity,
                stockStatus = parseStockStatus(entity.stockStatus),
            )
        }
    }

    override suspend fun saveProductDetail(
        detail: ProductDetail,
    ) {
        val now = clock.nowEpochSeconds()
        dao.upsertProduct(
            CachedProductEntity(
                sku = detail.sku.value,
                id = detail.id,
                title = detail.title,
                description = detail.description,
                categoryName = detail.categoryName,
                categorySlug = detail.categorySlug.value,
                price = detail.price,
                currency = detail.currency,
                imageUrl = detail.imageUrl,
                merchantPrincipalId = detail.merchantPrincipalId.value,
                weightGrams = detail.weightGrams,
                availableQuantity = detail.warehouseStock.availableQuantity,
                reservedQuantity = detail.warehouseStock.reservedQuantity,
                binLocation = detail.warehouseStock.binLocation,
                stockStatus = detail.warehouseStock.status.name,
                cachedAtEpochSeconds = now,
            )
        )
    }

    override suspend fun getProductDetail(
        sku: ProductSku,
    ): ProductDetail? {
        val entity = dao.findProductBySku(sku.value) ?: return null
        val description = entity.description ?: return null
        return ProductDetail(
            id = entity.id,
            sku = ProductSku(entity.sku),
            title = entity.title,
            description = description,
            categoryName = entity.categoryName,
            categorySlug = CategorySlug(entity.categorySlug),
            price = entity.price,
            currency = entity.currency,
            imageUrl = entity.imageUrl,
            merchantPrincipalId = PrincipalId(entity.merchantPrincipalId),
            weightGrams = entity.weightGrams,
            warehouseStock = WarehouseStockInfo(
                sku = ProductSku(entity.sku),
                binLocation = entity.binLocation,
                availableQuantity = entity.availableQuantity,
                reservedQuantity = entity.reservedQuantity,
                status = parseStockStatus(entity.stockStatus),
            ),
        )
    }

    private fun parseStockStatus(
        raw: String,
    ): StockStatus {
        return StockStatus.entries.firstOrNull { it.name == raw } ?: StockStatus.UNKNOWN
    }
}

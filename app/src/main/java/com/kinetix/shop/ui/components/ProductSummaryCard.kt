package com.kinetix.shop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinetix.shop.R
import com.kinetix.shop.core.config.ImageUrlSanitizer
import com.kinetix.shop.core.format.LocaleFormatter
import com.kinetix.shop.models.catalog.ProductSummary
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.ui.theme.KinetixPalette

@Composable
fun ProductSummaryCard(
    item: ProductSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = KinetixPalette.CardSlate,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            val safeImageUrl = ImageUrlSanitizer.sanitizeHttpsUrl(item.imageUrl)
            if (safeImageUrl != null) {
                ProductImageBox(
                    imageUrl = safeImageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
                Spacer(
                    modifier = Modifier.height(10.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.categoryName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixPalette.CyanPulse,
                )
                val stockLabel = when (item.stockStatus) {
                    StockStatus.IN_STOCK -> {
                        val count = item.availableStock
                        if (count != null) {
                            stringResource(R.string.stock_available_with_count, count)
                        } else {
                            stringResource(R.string.stock_available)
                        }
                    }
                    StockStatus.OUT_OF_STOCK -> stringResource(R.string.stock_out_of_stock)
                    StockStatus.UNKNOWN -> stringResource(R.string.stock_unavailable)
                }
                val stockColor = when (item.stockStatus) {
                    StockStatus.IN_STOCK -> KinetixPalette.SuccessEmerald
                    StockStatus.OUT_OF_STOCK -> KinetixPalette.DangerCoral
                    StockStatus.UNKNOWN -> KinetixPalette.WarningAmber
                }
                Text(
                    text = stockLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = stockColor,
                )
            }
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = KinetixPalette.TextPrimary,
            )
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            Text(
                text = LocaleFormatter.formatMoney(
                    amountString = item.price,
                    currencyCode = item.currency,
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = KinetixPalette.ElectricVolt,
            )
        }
    }
}

package com.kinetix.shop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.shop.R
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
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {
            ProductImageBox(
                imageUrl = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(
                modifier = Modifier.height(10.dp),
            )
            Text(
                text = item.categoryName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = KinetixPalette.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(
                modifier = Modifier.height(4.dp),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(
                modifier = Modifier.height(8.dp),
            )
            Text(
                text = LocaleFormatter.formatMoney(
                    amountString = item.price,
                    currencyCode = item.currency,
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(
                modifier = Modifier.height(6.dp),
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
                StockStatus.IN_STOCK -> MaterialTheme.colorScheme.tertiary
                StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                StockStatus.UNKNOWN -> KinetixPalette.WarningAmber
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(stockColor),
                )
                Text(
                    text = stockLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = KinetixPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

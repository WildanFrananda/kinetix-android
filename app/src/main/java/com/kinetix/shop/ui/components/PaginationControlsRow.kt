package com.kinetix.shop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kinetix.shop.R
import com.kinetix.shop.ui.theme.KinetixPalette

@Composable
fun PaginationControlsRow(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = currentPage > 1,
        ) {
            Text(
                text = stringResource(R.string.action_prev),
            )
        }
        Text(
            text = stringResource(
                R.string.pagination_page_of,
                currentPage,
                totalPages,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = KinetixPalette.TextSecondary,
        )
        OutlinedButton(
            onClick = onNext,
            enabled = currentPage < totalPages,
        ) {
            Text(
                text = stringResource(R.string.action_next),
            )
        }
    }
}

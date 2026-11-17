package com.kinetix.shop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinetix.shop.R
import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.ui.theme.KinetixPalette

@Composable
fun ErrorBannerBlock(
    modifier: Modifier = Modifier,
    failure: Failure,
    onRetry: (() -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = KinetixPalette.CardSlate,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = FailureMessageMapper.resolve(failure),
                style = MaterialTheme.typography.bodyMedium,
                color = KinetixPalette.DangerCoral,
            )
            if (onRetry != null) {
                Spacer(
                    modifier = Modifier.height(12.dp),
                )
                Button(
                    onClick = onRetry,
                ) {
                    Text(
                        text = stringResource(R.string.action_retry),
                    )
                }
            }
        }
    }
}

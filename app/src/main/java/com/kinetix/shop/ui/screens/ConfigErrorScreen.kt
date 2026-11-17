package com.kinetix.shop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinetix.shop.R
import com.kinetix.shop.ui.theme.KinetixPalette

@Composable
fun ConfigErrorScreen(
    reasons: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.config_error_title),
                style = MaterialTheme.typography.headlineMedium,
                color = KinetixPalette.DangerCoral,
            )
            Spacer(
                modifier = Modifier.height(12.dp),
            )
            Text(
                text = stringResource(R.string.config_error_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    reasons.forEach { reason ->
                        Text(
                            text = "• $reason",
                            style = MaterialTheme.typography.labelMedium,
                            color = KinetixPalette.WarningAmber,
                        )
                    }
                }
            }
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            Text(
                text = stringResource(R.string.config_error_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

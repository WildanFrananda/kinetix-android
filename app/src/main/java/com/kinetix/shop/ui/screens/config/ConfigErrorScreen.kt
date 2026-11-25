package com.kinetix.shop.ui.screens.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_kinetix_mark),
                contentDescription = stringResource(R.string.brand_wordmark),
                modifier = Modifier.size(36.dp),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            Text(
                text = stringResource(R.string.config_error_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error,
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
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    reasons.forEach { reason ->
                        val keyName = reason.substringBefore(" ")
                        val resolvedReason = when {
                            reason.endsWith(" is missing") ->
                                stringResource(R.string.config_error_reason_missing, keyName)
                            reason.endsWith(" is not a valid URI") ->
                                stringResource(R.string.config_error_reason_invalid_uri, keyName)
                            reason.endsWith(" must use https://") ->
                                stringResource(R.string.config_error_reason_scheme, keyName, "https")
                            reason.endsWith(" must use wss://") ->
                                stringResource(R.string.config_error_reason_scheme, keyName, "wss")
                            reason.endsWith(" must include a host") ->
                                stringResource(R.string.config_error_reason_missing_host, keyName)
                            reason.endsWith(" must not contain a path") ->
                                stringResource(R.string.config_error_reason_has_path, keyName)
                            reason.endsWith(" must not contain query or fragment parameters") ->
                                stringResource(R.string.config_error_reason_has_query_or_fragment, keyName)
                            else ->
                                stringResource(R.string.config_error_reason_generic)
                        }
                        Text(
                            text = stringResource(
                                R.string.config_error_reason_bullet,
                                resolvedReason,
                            ),
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

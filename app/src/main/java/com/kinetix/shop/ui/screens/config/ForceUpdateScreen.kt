package com.kinetix.shop.ui.screens.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
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
fun ForceUpdateScreen(
    onRecheck: () -> Unit,
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
            Text(
                text = stringResource(R.string.force_update_title),
                style = MaterialTheme.typography.headlineMedium,
                color = KinetixPalette.WarningAmber,
            )
            Spacer(
                modifier = Modifier.height(12.dp),
            )
            Text(
                text = stringResource(R.string.force_update_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(
                modifier = Modifier.height(20.dp),
            )
            Button(
                onClick = onRecheck,
            ) {
                Text(
                    text = stringResource(R.string.action_retry),
                )
            }
        }
    }
}

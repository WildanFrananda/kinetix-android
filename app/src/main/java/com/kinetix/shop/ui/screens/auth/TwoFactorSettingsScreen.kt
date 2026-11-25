package com.kinetix.shop.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetix.shop.R
import com.kinetix.shop.state.auth.TwoFactorSettingsCompletedUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsDelayedUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsErrorUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsIdleUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsSecretReadyUiState
import com.kinetix.shop.state.auth.TwoFactorSettingsSubmittingUiState
import com.kinetix.shop.ui.components.FailureMessageMapper
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.auth.TwoFactorSettingsViewModel

@Composable
fun TwoFactorSettingsScreen(
    viewModel: TwoFactorSettingsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }

    val controlsEnabled =
        state !is TwoFactorSettingsSubmittingUiState && state !is TwoFactorSettingsDelayedUiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.two_factor_settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.two_factor_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            enabled = controlsEnabled,
            label = { Text(text = stringResource(R.string.field_account_password)) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password },
        )

        OutlinedTextField(
            value = totpCode,
            onValueChange = { input ->
                totpCode = input.filter { it.isDigit() }.take(6)
            },
            enabled = controlsEnabled,
            label = { Text(text = stringResource(R.string.field_authenticator_code)) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.SmsOtpCode },
        )

        when (val current = state) {
            is TwoFactorSettingsErrorUiState -> {
                Text(
                    text = FailureMessageMapper.resolve(current.failure),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is TwoFactorSettingsDelayedUiState -> {
                Text(
                    text = stringResource(
                        R.string.two_factor_delayed_banner,
                        current.secondsRemaining,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixPalette.WarningAmber,
                )
            }
            is TwoFactorSettingsCompletedUiState -> {
                Text(
                    text = if (current.enabled) {
                        stringResource(R.string.two_factor_completed_enabled)
                    } else {
                        stringResource(R.string.two_factor_completed_disabled)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            is TwoFactorSettingsSecretReadyUiState -> {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.two_factor_secret_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = current.secret.secret,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(
                                R.string.two_factor_uri_label,
                                current.secret.qrCodeUrl,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = KinetixPalette.TextMuted,
                        )
                    }
                }
            }
            is TwoFactorSettingsIdleUiState,
            is TwoFactorSettingsSubmittingUiState -> Unit
        }

        Spacer(
            modifier = Modifier.height(4.dp),
        )

        OutlinedButton(
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            onClick = {
                viewModel.requestSetupSecret(
                    password = password,
                )
            },
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.two_factor_generate_secret_button),
            )
        }
        Button(
            shape = MaterialTheme.shapes.small,
            onClick = {
                viewModel.enableTwoFactor(
                    code = totpCode,
                    password = password,
                )
            },
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.two_factor_enable_button),
            )
        }
        OutlinedButton(
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            onClick = {
                viewModel.disableTwoFactor(
                    code = totpCode,
                    password = password,
                )
            },
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.two_factor_disable_button),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

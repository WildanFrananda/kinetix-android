package com.kinetix.shop.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    var password by rememberSaveable { mutableStateOf("") }
    var totpCode by rememberSaveable { mutableStateOf("") }

    val controlsEnabled =
        state !is TwoFactorSettingsSubmittingUiState && state !is TwoFactorSettingsDelayedUiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.two_factor_settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = KinetixPalette.ElectricVolt,
        )
        Text(
            text = stringResource(R.string.two_factor_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = KinetixPalette.TextSecondary,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            enabled = controlsEnabled,
            label = { Text(text = stringResource(R.string.field_account_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = totpCode,
            onValueChange = { input ->
                totpCode = input.filter { it.isDigit() }.take(6)
            },
            enabled = controlsEnabled,
            label = { Text(text = stringResource(R.string.field_authenticator_code)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        when (val current = state) {
            is TwoFactorSettingsErrorUiState -> {
                Text(
                    text = FailureMessageMapper.resolve(current.failure),
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixPalette.DangerCoral,
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
                    color = KinetixPalette.SuccessEmerald,
                )
            }
            is TwoFactorSettingsSecretReadyUiState -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = KinetixPalette.CardSlate,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.two_factor_secret_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = KinetixPalette.TextSecondary,
                        )
                        Text(
                            text = current.secret.secret,
                            style = MaterialTheme.typography.titleMedium,
                            color = KinetixPalette.ElectricVolt,
                        )
                        Text(
                            text = stringResource(
                                R.string.two_factor_uri_label,
                                current.secret.qrCodeUrl,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = KinetixPalette.TextSecondary,
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
                color = KinetixPalette.DangerCoral,
            )
        }
    }
}

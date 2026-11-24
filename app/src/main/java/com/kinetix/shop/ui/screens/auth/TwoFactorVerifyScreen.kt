package com.kinetix.shop.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetix.shop.R
import com.kinetix.shop.state.common.FormAuthenticatedUiState
import com.kinetix.shop.state.common.FormChallengeExpiredUiState
import com.kinetix.shop.state.common.FormDelayedUiState
import com.kinetix.shop.state.common.FormErrorUiState
import com.kinetix.shop.state.common.FormIdleUiState
import com.kinetix.shop.state.common.FormMfaChallengeUiState
import com.kinetix.shop.state.common.FormSubmittingUiState
import com.kinetix.shop.ui.components.FailureMessageMapper
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.auth.TwoFactorVerifyViewModel

@Composable
fun TwoFactorVerifyScreen(
    viewModel: TwoFactorVerifyViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }

    val controlsEnabled = state is FormIdleUiState || state is FormErrorUiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.two_factor_verify_title),
            style = MaterialTheme.typography.headlineMedium,
            color = KinetixPalette.ElectricVolt,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        Text(
            text = stringResource(R.string.two_factor_verify_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = KinetixPalette.TextSecondary,
        )
        Spacer(
            modifier = Modifier.height(20.dp),
        )

        OutlinedTextField(
            value = code,
            onValueChange = { input ->
                code = input.filter { it.isDigit() }.take(6)
            },
            enabled = controlsEnabled,
            label = { Text(text = stringResource(R.string.field_authenticator_code)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (controlsEnabled) {
                        viewModel.verify(code = code)
                    }
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.SmsOtpCode },
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )

        when (val current = state) {
            is FormErrorUiState -> {
                Text(
                    text = FailureMessageMapper.resolve(current.failure),
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixPalette.DangerCoral,
                )
                Spacer(
                    modifier = Modifier.height(12.dp),
                )
            }
            is FormDelayedUiState -> {
                Text(
                    text = stringResource(
                        R.string.two_factor_verify_delayed_banner,
                        current.secondsRemaining,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixPalette.WarningAmber,
                )
                Spacer(
                    modifier = Modifier.height(12.dp),
                )
            }
            is FormIdleUiState,
            is FormSubmittingUiState,
            is FormAuthenticatedUiState,
            is FormMfaChallengeUiState,
            is FormChallengeExpiredUiState -> Unit
        }

        Button(
            onClick = {
                viewModel.verify(code = code)
            },
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val buttonLabel = when (val current = state) {
                is FormSubmittingUiState -> stringResource(R.string.two_factor_verify_button_submitting)
                is FormDelayedUiState -> stringResource(
                    R.string.two_factor_verify_button_delayed,
                    current.secondsRemaining,
                )
                is FormIdleUiState,
                is FormErrorUiState,
                is FormAuthenticatedUiState,
                is FormMfaChallengeUiState,
                is FormChallengeExpiredUiState -> stringResource(R.string.two_factor_verify_button_idle)
            }
            Text(
                text = buttonLabel,
            )
        }
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        TextButton(
            onClick = { viewModel.cancelChallenge() },
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.two_factor_verify_back_to_sign_in),
                color = KinetixPalette.CyanPulse,
            )
        }
    }
}

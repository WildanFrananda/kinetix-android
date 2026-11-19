package com.kinetix.shop.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kinetix.shop.state.common.FormDelayedUiState
import com.kinetix.shop.state.common.FormErrorUiState
import com.kinetix.shop.state.common.FormIdleUiState
import com.kinetix.shop.state.common.FormSubmittingUiState
import com.kinetix.shop.ui.components.FailureMessageMapper
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.auth.SignInViewModel

@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    onAuthenticated: () -> Unit,
    onMfaChallenge: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val controlsEnabled = state is FormIdleUiState || state is FormErrorUiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.sign_in_title),
            style = MaterialTheme.typography.headlineMedium,
            color = KinetixPalette.ElectricVolt,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        Text(
            text = stringResource(R.string.sign_in_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = KinetixPalette.TextSecondary,
        )
        Spacer(
            modifier = Modifier.height(20.dp),
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            enabled = controlsEnabled,
            label = { Text(text = stringResource(R.string.field_email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            enabled = controlsEnabled,
            label = { Text(text = stringResource(R.string.field_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
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
                        R.string.sign_in_delayed_banner,
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
            is FormSubmittingUiState -> Unit
        }

        Button(
            onClick = {
                viewModel.signIn(
                    email = email,
                    password = password,
                    onAuthenticated = onAuthenticated,
                    onMfaChallenge = onMfaChallenge,
                )
            },
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val buttonLabel = when (val current = state) {
                is FormSubmittingUiState -> stringResource(R.string.sign_in_button_submitting)
                is FormDelayedUiState -> stringResource(
                    R.string.sign_in_button_delayed,
                    current.secondsRemaining,
                )
                is FormIdleUiState,
                is FormErrorUiState -> stringResource(R.string.sign_in_button_idle)
            }
            Text(
                text = buttonLabel,
            )
        }
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        TextButton(
            onClick = onNavigateToRegister,
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.sign_in_switch_to_register),
                color = KinetixPalette.CyanPulse,
            )
        }
    }
}

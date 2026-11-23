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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kinetix.shop.state.common.FormAuthenticatedUiState
import com.kinetix.shop.state.common.FormChallengeExpiredUiState
import com.kinetix.shop.state.common.FormDelayedUiState
import com.kinetix.shop.state.common.FormErrorUiState
import com.kinetix.shop.state.common.FormIdleUiState
import com.kinetix.shop.state.common.FormMfaChallengeUiState
import com.kinetix.shop.state.common.FormSubmittingUiState
import com.kinetix.shop.ui.components.FailureMessageMapper
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.auth.SignInViewModel

@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    onAuthenticated: () -> Unit,
    onMfaChallenge: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        when (state) {
            is FormAuthenticatedUiState -> {
                viewModel.consumeNavigationEvent()
                onAuthenticated()
            }
            is FormMfaChallengeUiState -> {
                viewModel.consumeNavigationEvent()
                onMfaChallenge()
            }
            else -> Unit
        }
    }

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
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress + ContentType.Username },
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
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (controlsEnabled) {
                        viewModel.signIn(
                            email = email,
                            password = password,
                        )
                    }
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password },
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
            is FormSubmittingUiState,
            is FormAuthenticatedUiState,
            is FormMfaChallengeUiState,
            is FormChallengeExpiredUiState -> Unit
        }

        Button(
            onClick = {
                viewModel.signIn(
                    email = email,
                    password = password,
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
                is FormErrorUiState,
                is FormAuthenticatedUiState,
                is FormMfaChallengeUiState,
                is FormChallengeExpiredUiState -> stringResource(R.string.sign_in_button_idle)
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

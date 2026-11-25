package com.kinetix.shop.ui.screens.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetix.shop.R
import com.kinetix.shop.core.format.LocaleFormatter
import com.kinetix.shop.models.pricing.DiscountType
import com.kinetix.shop.state.cart.CartErrorUiState
import com.kinetix.shop.state.cart.CartLoadedUiState
import com.kinetix.shop.state.cart.CartLoadingUiState
import com.kinetix.shop.ui.components.CartSkeleton
import com.kinetix.shop.ui.components.ErrorBannerBlock
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.cart.CartViewModel

@Composable
fun CartScreen(
    viewModel: CartViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var voucherInput by rememberSaveable { mutableStateOf("") }
    var voucherExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        val loaded = state as? CartLoadedUiState
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.cart_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (loaded != null && loaded.cart.items.isNotEmpty()) {
                    val totalQuantity = loaded.cart.items.sumOf { it.quantity }
                    Text(
                        text = stringResource(R.string.cart_item_count_format, totalQuantity),
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixPalette.TextMuted,
                    )
                }
            }

            if (loaded != null && loaded.cart.items.isNotEmpty()) {
                TextButton(
                    enabled = !loaded.isMutating,
                    onClick = { viewModel.clearCart() },
                ) {
                    Text(
                        text = stringResource(R.string.cart_clear_action),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val current = state) {
                is CartLoadingUiState -> {
                    CartSkeleton()
                }
                is CartErrorUiState -> {
                    ErrorBannerBlock(
                        failure = current.failure,
                        onRetry = { viewModel.refresh() },
                    )
                }
                is CartLoadedUiState -> {
                    val cart = current.cart
                    val controlsEnabled = !current.isMutating

                    val actionFailure = current.actionFailure
                    if (actionFailure != null) {
                        ErrorBannerBlock(
                            failure = actionFailure,
                            onRetry = { viewModel.refresh() },
                        )
                    }

                    if (cart.hasMultipleMerchants()) {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, KinetixPalette.WarningAmber),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.cart_single_merchant_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = KinetixPalette.WarningAmber,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    if (cart.items.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.size(64.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ShoppingCart,
                                        contentDescription = null,
                                        tint = KinetixPalette.TextMuted,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.cart_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = stringResource(R.string.cart_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = KinetixPalette.TextMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        cart.items.forEach { item ->
                            val monogram = item.productTitle
                                .trim()
                                .split(" ")
                                .filter { it.isNotEmpty() }
                                .take(2)
                                .joinToString("") { it.first().uppercaseChar().toString() }
                                .ifEmpty { "KX" }

                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.size(68.dp),
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = monogram,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = KinetixPalette.TextMuted,
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Text(
                                                text = item.productTitle,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 6.dp),
                                            )
                                            IconButton(
                                                enabled = controlsEnabled,
                                                onClick = { viewModel.removeItem(item.productId) },
                                                modifier = Modifier.size(32.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = stringResource(R.string.cart_remove_item),
                                                    tint = KinetixPalette.TextMuted,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }

                                        Text(
                                            text = stringResource(
                                                R.string.cart_item_unit_price_format,
                                                LocaleFormatter.formatMoney(
                                                    amountString = item.unitPrice.toPlainString(),
                                                    currencyCode = "IDR",
                                                ),
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = KinetixPalette.TextMuted,
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    IconButton(
                                                        enabled = controlsEnabled,
                                                        onClick = {
                                                            viewModel.updateItemQuantity(
                                                                sku = item.productId,
                                                                quantity = item.quantity - 1,
                                                            )
                                                        },
                                                        modifier = Modifier.size(34.dp),
                                                    ) {
                                                        Text(
                                                            text = "-",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                        )
                                                    }
                                                    Text(
                                                        text = item.quantity.toString(),
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 8.dp),
                                                    )
                                                    IconButton(
                                                        enabled = controlsEnabled && item.quantity < 999,
                                                        onClick = {
                                                            viewModel.updateItemQuantity(
                                                                sku = item.productId,
                                                                quantity = item.quantity + 1,
                                                            )
                                                        },
                                                        modifier = Modifier.size(34.dp),
                                                    ) {
                                                        Text(
                                                            text = "+",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = LocaleFormatter.formatMoney(
                                                    amountString = item.lineTotal.toPlainString(),
                                                    currencyCode = "IDR",
                                                ),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val appliedCode = cart.appliedVoucherCode
                        if (appliedCode != null) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.cart_voucher_applied_format,
                                                appliedCode.value,
                                            ),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        TextButton(
                                            enabled = controlsEnabled,
                                            onClick = { viewModel.removeVoucher() },
                                        ) {
                                            Text(
                                                text = stringResource(R.string.cart_voucher_remove),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }

                                    val terms = current.voucherTerms
                                    if (terms != null) {
                                        val benefitText = if (terms.discountType == DiscountType.PERCENTAGE) {
                                            stringResource(
                                                R.string.cart_voucher_terms_percentage,
                                                terms.value.stripTrailingZeros().toPlainString(),
                                                LocaleFormatter.formatMoney(
                                                    amountString = terms.minSpend.toPlainString(),
                                                    currencyCode = "IDR",
                                                ),
                                            )
                                        } else {
                                            stringResource(
                                                R.string.cart_voucher_terms_fixed,
                                                LocaleFormatter.formatMoney(
                                                    amountString = terms.value.toPlainString(),
                                                    currencyCode = "IDR",
                                                ),
                                                LocaleFormatter.formatMoney(
                                                    amountString = terms.minSpend.toPlainString(),
                                                    currencyCode = "IDR",
                                                ),
                                            )
                                        }
                                        Text(
                                            text = benefitText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }

                                    if (current.voucherPreviewRejected) {
                                        Text(
                                            text = stringResource(R.string.cart_voucher_preview_not_applicable),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KinetixPalette.WarningAmber,
                                        )
                                    } else if (current.voucherPreviewAccepted) {
                                        Text(
                                            text = stringResource(R.string.cart_voucher_preview_caveat),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KinetixPalette.TextMuted,
                                        )
                                    }
                                }
                            }
                        } else {
                            val showVoucherField = voucherExpanded || voucherInput.isNotEmpty() || current.voucherPreviewRejected
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { voucherExpanded = !showVoucherField }
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.cart_voucher_toggle),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Icon(
                                            imageVector = if (showVoucherField) {
                                                Icons.Filled.KeyboardArrowUp
                                            } else {
                                                Icons.Filled.KeyboardArrowDown
                                            },
                                            contentDescription = null,
                                            tint = KinetixPalette.TextMuted,
                                        )
                                    }

                                    AnimatedVisibility(visible = showVoucherField) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            OutlinedTextField(
                                                value = voucherInput,
                                                onValueChange = { voucherInput = it },
                                                enabled = controlsEnabled,
                                                label = { Text(text = stringResource(R.string.cart_voucher_field_label)) },
                                                singleLine = true,
                                                shape = MaterialTheme.shapes.small,
                                                keyboardOptions = KeyboardOptions(
                                                    capitalization = KeyboardCapitalization.Characters,
                                                    autoCorrectEnabled = false,
                                                    imeAction = ImeAction.Done,
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        if (controlsEnabled) {
                                                            viewModel.applyVoucher(voucherInput)
                                                        }
                                                    },
                                                ),
                                                modifier = Modifier.weight(1f),
                                            )
                                            Button(
                                                shape = MaterialTheme.shapes.small,
                                                enabled = controlsEnabled,
                                                onClick = { viewModel.applyVoucher(voucherInput) },
                                                modifier = Modifier.height(48.dp),
                                            ) {
                                                Text(text = stringResource(R.string.cart_voucher_apply))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (loaded != null && loaded.cart.items.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.cart_subtotal_indication_label),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.cart_subtotal_indication_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = KinetixPalette.TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = LocaleFormatter.formatMoney(
                                amountString = loaded.cart.subtotal.toPlainString(),
                                currencyCode = "IDR",
                            ),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

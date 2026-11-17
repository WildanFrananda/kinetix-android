package com.kinetix.shop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kinetix.shop.R
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.routes.AppRouterState
import com.kinetix.shop.routes.CartRoute
import com.kinetix.shop.routes.HomeRoute
import com.kinetix.shop.routes.OrderDetailRoute
import com.kinetix.shop.routes.OrdersRoute
import com.kinetix.shop.routes.ProductDetailRoute
import com.kinetix.shop.routes.ProfileRoute
import com.kinetix.shop.routes.RegisterRoute
import com.kinetix.shop.routes.SearchRoute
import com.kinetix.shop.routes.SignInRoute
import com.kinetix.shop.routes.TwoFactorSettingsRoute
import com.kinetix.shop.routes.TwoFactorVerifyRoute
import com.kinetix.shop.routes.WalletRoute
import com.kinetix.shop.ui.screens.auth.RegisterScreen
import com.kinetix.shop.ui.screens.auth.SignInScreen
import com.kinetix.shop.ui.screens.auth.TwoFactorSettingsScreen
import com.kinetix.shop.ui.screens.auth.TwoFactorVerifyScreen
import com.kinetix.shop.ui.screens.catalog.HomeScreen
import com.kinetix.shop.ui.screens.catalog.ProductDetailScreen
import com.kinetix.shop.ui.screens.profile.ProfileScreen
import com.kinetix.shop.ui.screens.search.SearchScreen
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.auth.RegisterViewModel
import com.kinetix.shop.viewmodels.auth.SignInViewModel
import com.kinetix.shop.viewmodels.auth.TwoFactorSettingsViewModel
import com.kinetix.shop.viewmodels.auth.TwoFactorVerifyViewModel
import com.kinetix.shop.viewmodels.catalog.HomeCatalogViewModel
import com.kinetix.shop.viewmodels.catalog.ProductDetailViewModel
import com.kinetix.shop.viewmodels.profile.ProfileViewModel
import com.kinetix.shop.viewmodels.search.SearchViewModel
import kotlinx.coroutines.launch

@Composable
fun AppRootScreen(
    sessionRepository: SessionRepository,
) {
    val router = remember { AppRouterState(initialRoute = HomeRoute) }
    val currentRoute by router.currentRoute.collectAsState()
    val currentSession by sessionRepository.currentSession.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isSignedIn = currentSession != null

    LaunchedEffect(Unit) {
        sessionRepository.restoreSession()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.brand_wordmark),
                    style = MaterialTheme.typography.headlineMedium,
                    color = KinetixPalette.ElectricVolt,
                )
                if (currentRoute !is HomeRoute) {
                    TextButton(
                        onClick = { router.navigateBack() },
                    ) {
                        Text(
                            text = stringResource(R.string.nav_back),
                            color = KinetixPalette.CyanPulse,
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = KinetixPalette.ElevatedSlate,
            ) {
                NavigationBarItem(
                    selected = currentRoute is HomeRoute,
                    onClick = { router.navigateTo(HomeRoute, isSignedIn) },
                    icon = {},
                    label = { Text(text = stringResource(R.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = currentRoute is SearchRoute,
                    onClick = { router.navigateTo(SearchRoute(), isSignedIn) },
                    icon = {},
                    label = { Text(text = stringResource(R.string.nav_search)) },
                )
                NavigationBarItem(
                    selected = currentRoute is ProfileRoute || currentRoute is SignInRoute,
                    onClick = { router.navigateTo(ProfileRoute, isSignedIn) },
                    icon = {},
                    label = {
                        Text(
                            text = if (isSignedIn) {
                                stringResource(R.string.nav_profile)
                            } else {
                                stringResource(R.string.nav_sign_in)
                            }
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val route = currentRoute) {
                is HomeRoute -> {
                    val viewModel: HomeCatalogViewModel = hiltViewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onSelectProduct = { sku ->
                            router.navigateTo(ProductDetailRoute(sku), isSignedIn)
                        },
                    )
                }
                is SearchRoute -> {
                    val viewModel: SearchViewModel = hiltViewModel()
                    SearchScreen(
                        viewModel = viewModel,
                        onSelectProduct = { sku ->
                            router.navigateTo(ProductDetailRoute(sku), isSignedIn)
                        },
                    )
                }
                is ProductDetailRoute -> {
                    val viewModel: ProductDetailViewModel = hiltViewModel()
                    ProductDetailScreen(
                        sku = route.sku,
                        viewModel = viewModel,
                    )
                }
                is SignInRoute -> {
                    val viewModel: SignInViewModel = hiltViewModel()
                    SignInScreen(
                        viewModel = viewModel,
                        onAuthenticated = { router.onAuthenticated() },
                        onMfaChallenge = { challengeToken ->
                            router.navigateTo(TwoFactorVerifyRoute(challengeToken), isSignedIn = false)
                        },
                        onNavigateToRegister = {
                            router.navigateTo(RegisterRoute, isSignedIn = false)
                        },
                    )
                }
                is RegisterRoute -> {
                    val viewModel: RegisterViewModel = hiltViewModel()
                    RegisterScreen(
                        viewModel = viewModel,
                        onRegistered = { router.onAuthenticated() },
                        onNavigateToSignIn = {
                            router.navigateTo(SignInRoute, isSignedIn = false)
                        },
                    )
                }
                is TwoFactorVerifyRoute -> {
                    val viewModel: TwoFactorVerifyViewModel = hiltViewModel()
                    TwoFactorVerifyScreen(
                        challengeToken = route.challengeToken,
                        viewModel = viewModel,
                        onVerified = { router.onAuthenticated() },
                        onChallengeExpired = {
                            router.navigateTo(SignInRoute, isSignedIn = false)
                        },
                    )
                }
                is TwoFactorSettingsRoute -> {
                    val viewModel: TwoFactorSettingsViewModel = hiltViewModel()
                    TwoFactorSettingsScreen(
                        viewModel = viewModel,
                    )
                }
                is ProfileRoute,
                is CartRoute,
                is WalletRoute,
                is OrdersRoute,
                is OrderDetailRoute -> {
                    val viewModel: ProfileViewModel = hiltViewModel()
                    ProfileScreen(
                        email = currentSession?.email.orEmpty(),
                        principalId = currentSession?.principalId?.value.orEmpty(),
                        viewModel = viewModel,
                        onNavigateToTwoFactorSettings = {
                            router.navigateTo(TwoFactorSettingsRoute, isSignedIn = true)
                        },
                        onSignOut = {
                            coroutineScope.launch {
                                sessionRepository.signOut()
                                router.onSignedOut()
                            }
                        },
                    )
                }
            }
        }
    }
}

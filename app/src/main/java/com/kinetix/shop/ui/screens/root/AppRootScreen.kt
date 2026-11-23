package com.kinetix.shop.ui.screens.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kinetix.shop.R
import com.kinetix.shop.models.auth.SessionRestoring
import com.kinetix.shop.models.auth.SessionSignedIn
import com.kinetix.shop.models.auth.SessionSignedOut
import com.kinetix.shop.models.auth.SessionUnreadable
import com.kinetix.shop.routes.HomeRoute
import com.kinetix.shop.routes.ProductDetailRoute
import com.kinetix.shop.routes.ProfileRoute
import com.kinetix.shop.routes.RegisterRoute
import com.kinetix.shop.routes.SearchRoute
import com.kinetix.shop.routes.SignInRoute
import com.kinetix.shop.routes.TwoFactorSettingsRoute
import com.kinetix.shop.routes.TwoFactorVerifyRoute
import com.kinetix.shop.state.common.FormAuthenticatedUiState
import com.kinetix.shop.state.common.FormChallengeExpiredUiState
import com.kinetix.shop.state.common.FormMfaChallengeUiState
import com.kinetix.shop.ui.screens.auth.RegisterScreen
import com.kinetix.shop.ui.screens.auth.SignInScreen
import com.kinetix.shop.ui.screens.auth.TwoFactorSettingsScreen
import com.kinetix.shop.ui.screens.auth.TwoFactorVerifyScreen
import com.kinetix.shop.ui.screens.catalog.HomeScreen
import com.kinetix.shop.ui.screens.catalog.ProductDetailScreen
import com.kinetix.shop.ui.screens.config.ForceUpdateScreen
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
import com.kinetix.shop.viewmodels.root.AppRootViewModel
import com.kinetix.shop.viewmodels.search.SearchViewModel

@Composable
fun AppRootScreen(
    viewModel: AppRootViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val forceUpdateRequired by viewModel.forceUpdateRequired.collectAsStateWithLifecycle()
    var pendingAuthTarget by rememberSaveable { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (forceUpdateRequired) {
        ForceUpdateScreen(
            onRecheck = { viewModel.onAppForegrounded() },
        )
        return
    }

    val isSignedIn = sessionState is SessionSignedIn
    val isOnHome = currentDestination?.hasRoute<HomeRoute>() == true
    val isOnSearch = currentDestination?.hasRoute<SearchRoute>() == true
    val isOnProfile = currentDestination?.hasRoute<ProfileRoute>() == true
    val isOnTwoFactorSettings = currentDestination?.hasRoute<TwoFactorSettingsRoute>() == true
    val isOnProfileOrAuth =
        isOnProfile ||
            currentDestination?.hasRoute<SignInRoute>() == true ||
            currentDestination?.hasRoute<RegisterRoute>() == true

    LaunchedEffect(sessionState, currentDestination) {
        if (sessionState !is SessionSignedOut) {
            return@LaunchedEffect
        }
        if (isOnProfile) {
            pendingAuthTarget = "profile"
            navController.navigate(SignInRoute) {
                popUpTo<ProfileRoute> { inclusive = true }
                launchSingleTop = true
            }
        } else if (isOnTwoFactorSettings) {
            pendingAuthTarget = "two_factor_settings"
            navController.navigate(SignInRoute) {
                popUpTo<TwoFactorSettingsRoute> { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun handleAuthenticated() {
        val target = pendingAuthTarget
        pendingAuthTarget = null
        navController.popBackStack<SignInRoute>(inclusive = true)
        when (target) {
            "profile" -> navController.navigate(ProfileRoute) { launchSingleTop = true }
            "two_factor_settings" -> navController.navigate(TwoFactorSettingsRoute) {
                launchSingleTop = true
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.brand_wordmark),
                    style = MaterialTheme.typography.headlineMedium,
                    color = KinetixPalette.ElectricVolt,
                )
                if (!isOnHome && navController.previousBackStackEntry != null) {
                    TextButton(
                        onClick = { navController.popBackStack() },
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
                    selected = isOnHome,
                    onClick = {
                        navController.navigate(HomeRoute) {
                            popUpTo<HomeRoute> { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = stringResource(R.string.nav_home),
                        )
                    },
                    label = { Text(text = stringResource(R.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = isOnSearch,
                    onClick = {
                        navController.navigate(SearchRoute()) {
                            launchSingleTop = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.nav_search),
                        )
                    },
                    label = { Text(text = stringResource(R.string.nav_search)) },
                )
                NavigationBarItem(
                    selected = isOnProfileOrAuth,
                    onClick = {
                        if (isSignedIn) {
                            navController.navigate(ProfileRoute) {
                                launchSingleTop = true
                            }
                        } else {
                            pendingAuthTarget = "profile"
                            navController.navigate(SignInRoute) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = if (isSignedIn) {
                                stringResource(R.string.nav_profile)
                            } else {
                                stringResource(R.string.nav_sign_in)
                            },
                        )
                    },
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
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
            ) {
                composable<HomeRoute> {
                    val homeViewModel: HomeCatalogViewModel = hiltViewModel()
                    HomeScreen(
                        viewModel = homeViewModel,
                        onSelectProduct = { sku ->
                            navController.navigate(ProductDetailRoute(sku = sku.value))
                        },
                    )
                }
                composable<SearchRoute> { entry ->
                    val route = entry.toRoute<SearchRoute>()
                    val searchViewModel: SearchViewModel = hiltViewModel()
                    SearchScreen(
                        initialQuery = route.initialQuery,
                        viewModel = searchViewModel,
                        onSelectProduct = { sku ->
                            navController.navigate(ProductDetailRoute(sku = sku.value))
                        },
                    )
                }
                composable<ProductDetailRoute> {
                    val productDetailViewModel: ProductDetailViewModel = hiltViewModel()
                    ProductDetailScreen(
                        viewModel = productDetailViewModel,
                    )
                }
                composable<SignInRoute> {
                    val signInViewModel: SignInViewModel = hiltViewModel()
                    val signInState by signInViewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(signInState) {
                        when (signInState) {
                            is FormAuthenticatedUiState -> {
                                handleAuthenticated()
                                signInViewModel.consumeNavigationEvent()
                            }
                            is FormMfaChallengeUiState -> {
                                navController.navigate(TwoFactorVerifyRoute) {
                                    launchSingleTop = true
                                }
                                signInViewModel.consumeNavigationEvent()
                            }
                            else -> Unit
                        }
                    }
                    SignInScreen(
                        viewModel = signInViewModel,
                        onNavigateToRegister = {
                            navController.navigate(RegisterRoute) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable<RegisterRoute> {
                    val registerViewModel: RegisterViewModel = hiltViewModel()
                    val registerState by registerViewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(registerState) {
                        if (registerState is FormAuthenticatedUiState) {
                            handleAuthenticated()
                            registerViewModel.consumeNavigationEvent()
                        }
                    }
                    RegisterScreen(
                        viewModel = registerViewModel,
                        onNavigateToSignIn = {
                            navController.popBackStack<SignInRoute>(inclusive = false)
                        },
                    )
                }
                composable<TwoFactorVerifyRoute> {
                    val twoFactorVerifyViewModel: TwoFactorVerifyViewModel = hiltViewModel()
                    val verifyState by twoFactorVerifyViewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(verifyState) {
                        when (verifyState) {
                            is FormAuthenticatedUiState -> {
                                handleAuthenticated()
                                twoFactorVerifyViewModel.consumeNavigationEvent()
                            }
                            is FormChallengeExpiredUiState -> {
                                navController.popBackStack<SignInRoute>(inclusive = false)
                                twoFactorVerifyViewModel.consumeNavigationEvent()
                            }
                            else -> Unit
                        }
                    }
                    TwoFactorVerifyScreen(
                        viewModel = twoFactorVerifyViewModel,
                    )
                }
                composable<TwoFactorSettingsRoute> {
                    when (sessionState) {
                        is SessionRestoring -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is SessionUnreadable -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.session_unreadable_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        pendingAuthTarget = "two_factor_settings"
                                        navController.navigate(SignInRoute) {
                                            launchSingleTop = true
                                        }
                                    },
                                ) {
                                    Text(text = stringResource(R.string.session_unreadable_sign_in_action))
                                }
                            }
                        }

                        is SessionSignedIn -> {
                            val settingsViewModel: TwoFactorSettingsViewModel = hiltViewModel()
                            TwoFactorSettingsScreen(
                                viewModel = settingsViewModel,
                            )
                        }

                        is SessionSignedOut -> Unit
                    }
                }
                composable<ProfileRoute> {
                    when (val state = sessionState) {
                        is SessionRestoring -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is SessionUnreadable -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.session_unreadable_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        pendingAuthTarget = "profile"
                                        navController.navigate(SignInRoute) {
                                            launchSingleTop = true
                                        }
                                    },
                                ) {
                                    Text(text = stringResource(R.string.session_unreadable_sign_in_action))
                                }
                            }
                        }

                        is SessionSignedIn -> {
                            val activeSession = state.session
                            val profileViewModel: ProfileViewModel = hiltViewModel()
                            ProfileScreen(
                                email = activeSession.email,
                                accountId = activeSession.accountId.value,
                                principalId = activeSession.principalId.value,
                                viewModel = profileViewModel,
                                onNavigateToTwoFactorSettings = {
                                    navController.navigate(TwoFactorSettingsRoute) {
                                        launchSingleTop = true
                                    }
                                },
                                onSignOut = {
                                    viewModel.signOut()
                                    pendingAuthTarget = null
                                    navController.navigate(HomeRoute) {
                                        popUpTo<HomeRoute> { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }

                        is SessionSignedOut -> Unit
                    }
                }
            }
        }
    }
}

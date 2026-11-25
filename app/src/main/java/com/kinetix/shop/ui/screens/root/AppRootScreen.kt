package com.kinetix.shop.ui.screens.root

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
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
import com.kinetix.shop.routes.CartRoute
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
import com.kinetix.shop.ui.components.CartSkeleton
import com.kinetix.shop.ui.components.ProfileSkeleton
import com.kinetix.shop.ui.screens.auth.RegisterScreen
import com.kinetix.shop.ui.screens.auth.SignInScreen
import com.kinetix.shop.ui.screens.auth.TwoFactorSettingsScreen
import com.kinetix.shop.ui.screens.auth.TwoFactorVerifyScreen
import com.kinetix.shop.ui.screens.cart.CartScreen
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
import com.kinetix.shop.viewmodels.cart.CartViewModel
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
    val isOnCart = currentDestination?.hasRoute<CartRoute>() == true
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
        if (isOnCart) {
            pendingAuthTarget = "cart"
            navController.navigate(SignInRoute) {
                popUpTo<CartRoute> { inclusive = true }
                launchSingleTop = true
            }
        } else if (isOnProfile) {
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
            "cart" -> navController.navigate(CartRoute) { launchSingleTop = true }
            "profile" -> navController.navigate(ProfileRoute) { launchSingleTop = true }
            "two_factor_settings" -> navController.navigate(TwoFactorSettingsRoute) {
                launchSingleTop = true
            }
            else -> Unit
        }
    }

    fun isTopLevelTab(destination: NavDestination?): Boolean {
        return destination != null && (destination.hasRoute<HomeRoute>() ||
                destination.hasRoute<SearchRoute>() ||
                destination.hasRoute<CartRoute>() ||
                destination.hasRoute<ProfileRoute>())
    }

    val easeOutCurve = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    fun AnimatedContentTransitionScope<NavBackStackEntry>.resolveEnterTransition(): EnterTransition {
        if (isTopLevelTab(initialState.destination) && isTopLevelTab(targetState.destination)) {
            return fadeIn(
                animationSpec = tween(
                    durationMillis = 120,
                    delayMillis = 40,
                    easing = easeOutCurve,
                ),
            )
        }
        return slideInHorizontally(
            animationSpec = tween(
                durationMillis = 220,
                easing = easeOutCurve,
            ),
            initialOffsetX = { fullWidth -> fullWidth / 6 },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 150,
                delayMillis = 30,
                easing = easeOutCurve,
            ),
        )
    }

    fun AnimatedContentTransitionScope<NavBackStackEntry>.resolveExitTransition(): ExitTransition {
        if (isTopLevelTab(initialState.destination) && isTopLevelTab(targetState.destination)) {
            return fadeOut(
                animationSpec = tween(
                    durationMillis = 60,
                    easing = easeOutCurve,
                ),
            )
        }
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = 200,
                easing = easeOutCurve,
            ),
            targetOffsetX = { fullWidth -> -fullWidth / 12 },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 80,
                easing = easeOutCurve,
            ),
        )
    }

    fun AnimatedContentTransitionScope<NavBackStackEntry>.resolvePopEnterTransition(): EnterTransition {
        if (isTopLevelTab(initialState.destination) && isTopLevelTab(targetState.destination)) {
            return fadeIn(
                animationSpec = tween(
                    durationMillis = 120,
                    delayMillis = 40,
                    easing = easeOutCurve,
                ),
            )
        }
        return slideInHorizontally(
            animationSpec = tween(
                durationMillis = 210,
                easing = easeOutCurve,
            ),
            initialOffsetX = { fullWidth -> -fullWidth / 8 },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 150,
                delayMillis = 30,
                easing = easeOutCurve,
            ),
        )
    }

    fun AnimatedContentTransitionScope<NavBackStackEntry>.resolvePopExitTransition(): ExitTransition {
        if (isTopLevelTab(initialState.destination) && isTopLevelTab(targetState.destination)) {
            return fadeOut(
                animationSpec = tween(
                    durationMillis = 60,
                    easing = easeOutCurve,
                ),
            )
        }
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = 190,
                easing = easeOutCurve,
            ),
            targetOffsetX = { fullWidth -> fullWidth / 6 },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 80,
                easing = easeOutCurve,
            ),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_kinetix_mark),
                                contentDescription = stringResource(R.string.brand_wordmark),
                                modifier = Modifier.size(26.dp),
                            )
                            Text(
                                text = stringResource(R.string.brand_wordmark),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    letterSpacing = 1.2.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (!isOnHome && navController.previousBackStackEntry != null) {
                            TextButton(
                                onClick = { navController.popBackStack() },
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    text = stringResource(R.string.nav_back),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
        bottomBar = {
            val navItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = KinetixPalette.TextMuted,
                unselectedTextColor = KinetixPalette.TextMuted,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = isOnHome,
                        colors = navItemColors,
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
                        colors = navItemColors,
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
                        selected = isOnCart,
                        colors = navItemColors,
                        onClick = {
                            if (isSignedIn) {
                                navController.navigate(CartRoute) {
                                    launchSingleTop = true
                                }
                            } else {
                                pendingAuthTarget = "cart"
                                navController.navigate(SignInRoute) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = stringResource(R.string.nav_cart),
                            )
                        },
                        label = { Text(text = stringResource(R.string.nav_cart)) },
                    )
                    NavigationBarItem(
                        selected = isOnProfileOrAuth,
                        colors = navItemColors,
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
                enterTransition = { resolveEnterTransition() },
                exitTransition = { resolveExitTransition() },
                popEnterTransition = { resolvePopEnterTransition() },
                popExitTransition = { resolvePopExitTransition() },
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
                        isSignedIn = isSignedIn,
                        onRequireSignIn = {
                            navController.navigate(SignInRoute) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable<CartRoute> {
                    when (sessionState) {
                        is SessionRestoring -> {
                            CartSkeleton(
                                modifier = Modifier.padding(16.dp),
                            )
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
                                    shape = MaterialTheme.shapes.small,
                                    onClick = {
                                        pendingAuthTarget = "cart"
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
                            val cartViewModel: CartViewModel = hiltViewModel()
                            val lifecycleOwner = LocalLifecycleOwner.current
                            var skippedInitialResume by rememberSaveable { mutableStateOf(false) }
                            DisposableEffect(lifecycleOwner, cartViewModel) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        if (skippedInitialResume) {
                                            cartViewModel.refresh()
                                        } else {
                                            skippedInitialResume = true
                                        }
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose {
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                }
                            }
                            CartScreen(
                                viewModel = cartViewModel,
                            )
                        }

                        is SessionSignedOut -> Unit
                    }
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
                            ProfileSkeleton(
                                modifier = Modifier.padding(20.dp),
                            )
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
                                    shape = MaterialTheme.shapes.small,
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
                            ProfileSkeleton(
                                modifier = Modifier.padding(20.dp),
                            )
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
                                    shape = MaterialTheme.shapes.small,
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

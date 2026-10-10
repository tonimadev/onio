package com.ipirangatech.onio.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ipirangatech.onio.core.ui.motion.OnioMotion
import com.ipirangatech.onio.core.ui.motion.popOn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import com.ipirangatech.onio.R
import com.ipirangatech.onio.feature.cart.bridge.CartKey
import com.ipirangatech.onio.feature.cart.impl.CartRoute
import com.ipirangatech.onio.feature.cart.impl.CartViewModel
import com.ipirangatech.onio.feature.checkout.bridge.CheckoutKey
import com.ipirangatech.onio.feature.checkout.impl.CheckoutRoute
import com.ipirangatech.onio.feature.checkout.impl.CheckoutViewModel
import com.ipirangatech.onio.feature.home.bridge.HomeKey
import com.ipirangatech.onio.feature.home.impl.HomeRoute
import com.ipirangatech.onio.feature.home.impl.HomeViewModel
import com.ipirangatech.onio.feature.orders.bridge.OrderDetailKey
import com.ipirangatech.onio.feature.orders.bridge.OrdersKey
import com.ipirangatech.onio.feature.orders.impl.OrderDetailRoute
import com.ipirangatech.onio.feature.orders.impl.OrderDetailViewModel
import com.ipirangatech.onio.feature.orders.impl.OrdersRoute
import com.ipirangatech.onio.feature.orders.impl.OrdersViewModel
import com.ipirangatech.onio.feature.product.bridge.ProductKey
import com.ipirangatech.onio.feature.product.impl.ProductRoute
import com.ipirangatech.onio.feature.product.impl.ProductViewModel
import com.ipirangatech.onio.feature.rewards.bridge.OrderPlacedKey
import com.ipirangatech.onio.feature.rewards.bridge.RewardsKey
import com.ipirangatech.onio.feature.rewards.bridge.WheelKey
import com.ipirangatech.onio.feature.rewards.impl.OrderPlacedRoute
import com.ipirangatech.onio.feature.rewards.impl.OrderPlacedViewModel
import com.ipirangatech.onio.feature.rewards.impl.RewardsRoute
import com.ipirangatech.onio.feature.rewards.impl.RewardsViewModel
import com.ipirangatech.onio.feature.rewards.impl.WheelRoute
import com.ipirangatech.onio.feature.rewards.impl.WheelViewModel
import com.ipirangatech.onio.feature.savings.bridge.SavingsKey
import com.ipirangatech.onio.feature.savings.impl.SavingsRoute
import com.ipirangatech.onio.feature.savings.impl.SavingsViewModel
import com.ipirangatech.onio.feature.search.bridge.SearchKey
import com.ipirangatech.onio.feature.search.impl.SearchRoute
import com.ipirangatech.onio.feature.search.impl.SearchViewModel
import com.ipirangatech.onio.feature.settings.bridge.SettingsKey
import com.ipirangatech.onio.feature.settings.impl.SettingsRoute
import com.ipirangatech.onio.feature.settings.impl.SettingsViewModel

private data class TopLevel(val key: NavKey, val icon: ImageVector, val label: Int)

private val TopLevels = listOf(
    TopLevel(HomeKey, Icons.Filled.Home, R.string.nav_home),
    TopLevel(SearchKey(), Icons.Filled.Search, R.string.nav_search),
    TopLevel(CartKey, Icons.Filled.ShoppingCart, R.string.nav_cart),
    TopLevel(OrdersKey, Icons.Filled.Receipt, R.string.nav_orders),
    TopLevel(SavingsKey, Icons.Filled.Savings, R.string.nav_savings),
)

private fun androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation3.scene.Scene<NavKey>>.isTabSwitch(top: Set<NavKey>): Boolean {
    val from = initialState.entries.lastOrNull()?.contentKey
    val to = targetState.entries.lastOrNull()?.contentKey
    return from != null && to != null && from in top.map { it.toString() } && to in top.map { it.toString() }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun OnioNavHost(cartCount: Int, modifier: Modifier = Modifier) {
    val topLevelKeys = remember { TopLevels.map(TopLevel::key) }
    val state = rememberNavigationState(topLevelKeys)
    val navigator = remember(state) { Navigator(state) }

    val listDetail = rememberListDetailSceneStrategy<NavKey>()

    val provider = entryProvider<NavKey> {
        entry<HomeKey> {
            HomeRoute(
                viewModel = hiltViewModel<HomeViewModel>(),
                onOpenProduct = { navigator.navigate(ProductKey(it)) },
                onOpenCategory = { navigator.navigate(SearchKey(it)) },
                onOpenSearch = { navigator.selectTopLevel(SearchKey()) },
                onOpenCart = { navigator.selectTopLevel(CartKey) },
                onOpenRewards = { navigator.navigate(RewardsKey) },
            )
        }
        entry<SearchKey> { key ->
            SearchRoute(
                viewModel = hiltViewModel<SearchViewModel, SearchViewModel.Factory>(
                    creationCallback = { it.create(key.categorySlug) },
                ),
                onOpenProduct = { navigator.navigate(ProductKey(it)) },
                onOpenCart = { navigator.selectTopLevel(CartKey) },
            )
        }
        entry<ProductKey> { key ->
            ProductRoute(
                viewModel = hiltViewModel<ProductViewModel, ProductViewModel.Factory>(
                    creationCallback = { it.create(key.productId) },
                ),
                onBack = { navigator.goBack() },
                onOpenCart = { navigator.selectTopLevel(CartKey) },
            )
        }
        entry<CartKey> {
            CartRoute(
                viewModel = hiltViewModel<CartViewModel>(),
                onCheckout = { navigator.navigate(CheckoutKey) },
                onOpenProduct = { navigator.navigate(ProductKey(it)) },
            )
        }
        entry<CheckoutKey> {
            CheckoutRoute(
                viewModel = hiltViewModel<CheckoutViewModel>(),
                onBack = { navigator.goBack() },
                onOrderPlaced = { orderId, coins, spins ->
                    navigator.goBack() // leave checkout so back never reopens an empty form
                    navigator.navigate(OrderPlacedKey(orderId, coins, spins))
                },
            )
        }
        entry<OrdersKey>(metadata = ListDetailSceneStrategy.listPane()) {
            OrdersRoute(
                viewModel = hiltViewModel<OrdersViewModel>(),
                onOpenOrder = { navigator.navigate(OrderDetailKey(it)) },
            )
        }
        entry<OrderDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
            OrderDetailRoute(
                viewModel = hiltViewModel<OrderDetailViewModel, OrderDetailViewModel.Factory>(
                    creationCallback = { it.create(key.orderId) },
                ),
                onBack = { navigator.goBack() },
            )
        }
        entry<RewardsKey> {
            RewardsRoute(
                viewModel = hiltViewModel<RewardsViewModel>(),
                onBack = { navigator.goBack() },
                onOpenWheel = { navigator.navigate(WheelKey) },
            )
        }
        entry<WheelKey> {
            WheelRoute(
                viewModel = hiltViewModel<WheelViewModel>(),
                onBack = { navigator.goBack() },
            )
        }
        entry<OrderPlacedKey> { key ->
            OrderPlacedRoute(
                viewModel = hiltViewModel<OrderPlacedViewModel, OrderPlacedViewModel.Factory>(
                    creationCallback = { it.create(key.orderId, key.coins, key.spins) },
                ),
                onSpin = { navigator.navigate(WheelKey) },
                onViewOrder = { orderId ->
                    navigator.goBack()
                    navigator.launchIn(OrdersKey, OrderDetailKey(orderId))
                },
                onContinue = {
                    navigator.goBack()
                    navigator.selectTopLevel(HomeKey)
                },
            )
        }
        entry<SavingsKey> {
            SavingsRoute(
                viewModel = hiltViewModel<SavingsViewModel>(),
                onOpenSettings = { navigator.navigate(SettingsKey) },
                onOpenOrders = { navigator.selectTopLevel(OrdersKey) },
            )
        }
        entry<SettingsKey> {
            SettingsRoute(
                viewModel = hiltViewModel<SettingsViewModel>(),
                onBack = { navigator.goBack() },
            )
        }
    }

    val atRoot = state.currentBackStack.size == 1
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val suiteType = if (atRoot) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    } else {
        NavigationSuiteType.None
    }

    val barItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val topLevelSet = remember { TopLevels.map(TopLevel::key).toSet() }

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = suiteType,
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationBarContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            navigationRailContainerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationSuiteItems = {
            TopLevels.forEachIndexed { index, item ->
                item(
                    selected = state.selectedIndex == index,
                    onClick = { navigator.selectTopLevel(item.key) },
                    colors = barItemColors,
                    icon = {
                        if (item.key == CartKey && cartCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        modifier = Modifier.popOn(cartCount),
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) { Text(cartCount.toString()) }
                                },
                            ) {
                                Icon(item.icon, contentDescription = null)
                            }
                        } else {
                            Icon(item.icon, contentDescription = null)
                        }
                    },
                    label = { Text(stringResource(item.label)) },
                )
            }
        },
    ) {
        BackHandler(enabled = navigator.canGoBack()) { navigator.goBack() }
        NavDisplay(
            backStack = state.displayKeys,
            entryDecorators = rememberNavEntryDecorators(),
            sceneStrategies = listOf(listDetail),
            onBack = { navigator.goBack() },
            // Pushing slides in from the end (mirrored in RTL) with a fade; switching tabs just cross-fades.
            transitionSpec = {
                if (isTabSwitch(topLevelSet)) {
                    fadeIn(tween(OnioMotion.Medium)) togetherWith fadeOut(tween(OnioMotion.Short))
                } else {
                    (slideInHorizontally(tween(OnioMotion.Medium, easing = OnioMotion.Easing)) { direction * it / 4 } + fadeIn(tween(OnioMotion.Medium))) togetherWith
                        (slideOutHorizontally(tween(OnioMotion.Medium, easing = OnioMotion.Easing)) { -direction * it / 8 } + fadeOut(tween(OnioMotion.Medium)))
                }
            },
            popTransitionSpec = {
                (slideInHorizontally(tween(OnioMotion.Medium, easing = OnioMotion.Easing)) { -direction * it / 8 } + fadeIn(tween(OnioMotion.Medium))) togetherWith
                    (slideOutHorizontally(tween(OnioMotion.Medium, easing = OnioMotion.Easing)) { direction * it / 4 } + fadeOut(tween(OnioMotion.Medium)))
            },
            predictivePopTransitionSpec = {
                (slideInHorizontally(tween(OnioMotion.Medium)) { -direction * it / 8 } + fadeIn()) togetherWith
                    (slideOutHorizontally(tween(OnioMotion.Medium)) { direction * it / 4 } + fadeOut())
            },
            entryProvider = provider,
        )
    }
}

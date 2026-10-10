package com.ipirangatech.onio.navigation

import androidx.activity.compose.BackHandler
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
                onOrderPlaced = { orderId ->
                    navigator.goBack() // leave checkout so back from the order doesn't reopen an empty form
                    navigator.launchIn(OrdersKey, OrderDetailKey(orderId))
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

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = suiteType,
        navigationSuiteItems = {
            TopLevels.forEachIndexed { index, item ->
                item(
                    selected = state.selectedIndex == index,
                    onClick = { navigator.selectTopLevel(item.key) },
                    icon = {
                        if (item.key == CartKey && cartCount > 0) {
                            BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
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
            entryProvider = provider,
        )
    }
}

package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.CurrencyOption
import com.example.data.model.OrderStatus
import com.example.data.remote.MatjariRepository
import com.example.notifications.OrderNotificationHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProductEditorScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SellerDashboardScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.MatjariTheme
import com.example.ui.viewmodel.CartViewModel
import com.example.ui.viewmodel.MatjariViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_SELLER = "seller"
const val ROUTE_MARKETPLACE = "marketplace"
const val ROUTE_ORDERS = "orders"
const val ROUTE_CART = "cart"
const val ROUTE_PROFILE = "profile"
const val ROUTE_PRODUCT_EDITOR = "product_editor"
const val ROUTE_PRODUCT_DETAIL = "product_detail"
const val ROUTE_CHECKOUT = "checkout"
const val ROUTE_WISHLIST = "wishlist"

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(
        route = ROUTE_SELLER,
        labelRes = R.string.nav_seller,
        selectedIcon = Icons.Filled.Store,
        unselectedIcon = Icons.Outlined.Store,
        testTag = "nav_seller"
    ),
    TopLevelDestination(
        route = ROUTE_MARKETPLACE,
        labelRes = R.string.nav_marketplace,
        selectedIcon = Icons.Filled.Storefront,
        unselectedIcon = Icons.Outlined.Storefront,
        testTag = "nav_marketplace"
    ),
    TopLevelDestination(
        route = ROUTE_ORDERS,
        labelRes = R.string.nav_orders,
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_orders"
    ),
    TopLevelDestination(
        route = ROUTE_CART,
        labelRes = R.string.nav_cart,
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart,
        testTag = "nav_cart"
    ),
    TopLevelDestination(
        route = ROUTE_PROFILE,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_profile"
    )
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    private val pendingRouteState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OrderNotificationHelper.ensureNotificationChannel(this)
        pendingRouteState.value = intent?.getStringExtra(OrderNotificationHelper.EXTRA_NAVIGATE_ROUTE)
        setContent {
            MatjariTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppNavigation(
                        pendingNavigateRoute = pendingRouteState.value,
                        onConsumeNavigateRoute = { pendingRouteState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRouteState.value = intent.getStringExtra(OrderNotificationHelper.EXTRA_NAVIGATE_ROUTE)
    }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    pendingNavigateRoute: String? = null,
    onConsumeNavigateRoute: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MainFeatureScreen(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "تاجر متجري",
                userEmail = user.email,
                pendingNavigateRoute = pendingNavigateRoute,
                onConsumeNavigateRoute = onConsumeNavigateRoute
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    pendingNavigateRoute: String? = null,
    onConsumeNavigateRoute: () -> Unit = {}
) {
    val viewModel: MatjariViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MatjariViewModel(
                    repository = MatjariRepository(db),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName
                )
            }
        }
    )

    val cartViewModel: CartViewModel = viewModel(
        key = "cart_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CartViewModel(
                    repository = MatjariRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val publishedProductsState by viewModel.publishedProducts.collectAsStateWithLifecycle()
    val sellerProductsState by viewModel.sellerProducts.collectAsStateWithLifecycle()
    val cartUiState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val buyerOrdersState by viewModel.buyerOrders.collectAsStateWithLifecycle()
    val sellerOrdersState by viewModel.sellerOrders.collectAsStateWithLifecycle()
    val sellerDiscountsState by viewModel.sellerDiscounts.collectAsStateWithLifecycle()
    val wishlistItemsState by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val wishlistProductIds by viewModel.wishlistProductIds.collectAsStateWithLifecycle()
    val recommendationsState by viewModel.recommendationsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var notificationsEnabled by remember(context) {
        mutableStateOf(OrderNotificationHelper.hasNotificationPermission(context))
    }
    var hasPromptedNotificationPermission by rememberSaveable(currentUserId) {
        mutableStateOf(false)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = granted || OrderNotificationHelper.hasNotificationPermission(context)
    }
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !OrderNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsEnabled = OrderNotificationHelper.hasNotificationPermission(context)
        }
    }

    LaunchedEffect(currentUserId) {
        OrderNotificationHelper.ensureNotificationChannel(context)
        if (!hasPromptedNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !OrderNotificationHelper.hasNotificationPermission(context)
        ) {
            hasPromptedNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val messaging = FirebaseMessaging.getInstance()
        messaging.token.addOnSuccessListener { token ->
            if (!token.isNullOrBlank()) {
                viewModel.syncFcmToken(token)
            }
        }
        messaging.subscribeToTopic("orders_$currentUserId")
    }

    LaunchedEffect(Unit) {
        launch {
            viewModel.snackbarMessage.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }
        launch {
            cartViewModel.events.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }
        launch {
            viewModel.orderNotificationEvents.collect { event ->
                val (title, body) = OrderNotificationHelper.statusNotificationCopy(
                    productTitle = event.productTitle,
                    status = event.newStatus
                )
                OrderNotificationHelper.showOrderStatusNotification(
                    context = context,
                    notificationId = event.orderId.hashCode(),
                    title = title,
                    body = body,
                    navigateRoute = ROUTE_ORDERS
                )
                snackbarHostState.showSnackbar("$title: $body")
            }
        }
    }

    val userProfile = (userProfileState as? UiState.Success)?.data
    val storeName = userProfile?.storeName?.ifBlank { "متجري" } ?: "متجري"
    val defaultCurrency = CurrencyOption.fromCode(userProfile?.currency ?: CurrencyOption.SAR.code)

    val cartBadgeCount = cartUiState.totalItemsCount
    val pendingOrdersCount = (sellerOrdersState as? UiState.Success)?.data?.count {
        it.status == OrderStatus.PENDING.key
    } ?: 0

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevelRoute = TopLevelDestinations.any { it.route == currentRoute }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(pendingNavigateRoute) {
        val target = pendingNavigateRoute
        if (!target.isNullOrBlank()) {
            if (TopLevelDestinations.any { it.route == target }) {
                navigateTopLevel(target)
            } else {
                navController.navigate(target) {
                    launchSingleTop = true
                }
            }
            onConsumeNavigateRoute()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isTopLevelRoute) {
                    val title = when (currentRoute) {
                        ROUTE_SELLER -> stringResource(R.string.nav_seller)
                        ROUTE_MARKETPLACE -> stringResource(R.string.nav_marketplace)
                        ROUTE_ORDERS -> stringResource(R.string.nav_orders)
                        ROUTE_CART -> stringResource(R.string.nav_cart)
                        ROUTE_PROFILE -> stringResource(R.string.nav_profile)
                        else -> stringResource(R.string.app_name)
                    }
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { navController.navigate(ROUTE_WISHLIST) },
                                modifier = Modifier.testTag("topbar_wishlist_button")
                            ) {
                                DestinationIconWithBadge(
                                    icon = if (wishlistProductIds.isNotEmpty()) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    label = "قائمة المفضلة",
                                    badgeCount = wishlistProductIds.size,
                                    tint = if (wishlistProductIds.isNotEmpty()) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useNavigationRail) {
                    NavigationBar {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val badgeCount = when (dest.route) {
                                ROUTE_CART -> cartBadgeCount
                                ROUTE_ORDERS -> pendingOrdersCount
                                else -> 0
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    DestinationIconWithBadge(
                                        icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        label = stringResource(dest.labelRes),
                                        badgeCount = badgeCount
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val badgeCount = when (dest.route) {
                                ROUTE_CART -> cartBadgeCount
                                ROUTE_ORDERS -> pendingOrdersCount
                                else -> 0
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    DestinationIconWithBadge(
                                        icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        label = stringResource(dest.labelRes),
                                        badgeCount = badgeCount
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_SELLER
                    ) {
                        composable(ROUTE_SELLER) {
                            SellerDashboardScreen(
                                storeName = storeName,
                                defaultCurrency = defaultCurrency,
                                sellerProductsState = sellerProductsState,
                                sellerOrdersState = sellerOrdersState,
                                sellerDiscountsState = sellerDiscountsState,
                                isBusy = isBusy,
                                onAddProduct = { navController.navigate(ROUTE_PRODUCT_EDITOR) },
                                onProductClick = { product ->
                                    navController.navigate("$ROUTE_PRODUCT_DETAIL/${product.id}")
                                },
                                onEditProduct = { product ->
                                    navController.navigate("$ROUTE_PRODUCT_EDITOR?productId=${product.id}")
                                },
                                onDeleteProduct = { productId ->
                                    viewModel.deleteProduct(productId)
                                },
                                onCreateDiscount = { code, type, value, minOrder, onSuccess ->
                                    viewModel.createDiscount(
                                        rawCode = code,
                                        type = type,
                                        value = value,
                                        minOrderAmount = minOrder,
                                        onSuccess = onSuccess
                                    )
                                },
                                onToggleDiscountActive = { id, active ->
                                    viewModel.toggleDiscountActive(id, active)
                                },
                                onDeleteDiscount = { id ->
                                    viewModel.deleteDiscount(id)
                                }
                            )
                        }

                        composable(ROUTE_MARKETPLACE) {
                            MarketplaceScreen(
                                productsState = publishedProductsState,
                                cartUiState = cartUiState,
                                currentUserId = currentUserId,
                                wishlistProductIds = wishlistProductIds,
                                recommendationsState = recommendationsState,
                                onAddToCart = { product, qty -> cartViewModel.addProductToCart(product, qty) },
                                onToggleWishlist = { product -> viewModel.toggleWishlist(product) },
                                onOpenWishlist = { navController.navigate(ROUTE_WISHLIST) },
                                onOpenSellProduct = { navController.navigate(ROUTE_PRODUCT_EDITOR) },
                                onProductClick = { product ->
                                    navController.navigate("$ROUTE_PRODUCT_DETAIL/${product.id}")
                                }
                            )
                        }

                        composable(ROUTE_ORDERS) {
                            OrdersScreen(
                                sellerOrdersState = sellerOrdersState,
                                buyerOrdersState = buyerOrdersState,
                                onUpdateOrderStatus = { orderId, status ->
                                    viewModel.updateOrderStatus(orderId, status)
                                },
                                reorderingOrderIds = cartUiState.reorderingOrderIds,
                                onReorder = { order ->
                                    cartViewModel.reorderOrder(order)
                                }
                            )
                        }

                        composable(ROUTE_CART) {
                            CartScreen(
                                cartUiState = cartUiState,
                                onUpdateQuantity = { itemId, qty -> cartViewModel.updateQuantity(itemId, qty) },
                                onRemoveItem = { itemId -> cartViewModel.removeItem(itemId) },
                                onClearCart = { cartViewModel.clearCart() },
                                onProceedToCheckout = { navController.navigate(ROUTE_CHECKOUT) },
                                onExploreMarket = { navigateTopLevel(ROUTE_MARKETPLACE) }
                            )
                        }

                        composable(ROUTE_CHECKOUT) {
                            CheckoutScreen(
                                cartUiState = cartUiState,
                                userProfile = userProfile,
                                onNavigateBack = { navController.popBackStack() },
                                onPlaceOrder = { name, phone, address, note ->
                                    cartViewModel.checkout(name, phone, address, note) {
                                        navController.popBackStack()
                                        navigateTopLevel(ROUTE_ORDERS)
                                    }
                                },
                                onApplyDiscountCode = { code ->
                                    cartViewModel.applyDiscountCode(code)
                                },
                                onRemoveDiscountCode = {
                                    cartViewModel.removeDiscountCode()
                                },
                                onClearDiscountError = {
                                    cartViewModel.clearDiscountError()
                                },
                                onExploreMarket = {
                                    navController.popBackStack()
                                    navigateTopLevel(ROUTE_MARKETPLACE)
                                }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                userEmail = userEmail,
                                profileState = userProfileState,
                                buyerOrdersState = buyerOrdersState,
                                isBusy = isBusy,
                                onSaveProfile = { displayName, sName, bio, phone, address, currency, shippingAddresses ->
                                    viewModel.saveProfile(
                                        displayName = displayName,
                                        storeName = sName,
                                        storeBio = bio,
                                        phone = phone,
                                        defaultAddress = address,
                                        currency = currency,
                                        shippingAddresses = shippingAddresses
                                    )
                                },
                                onUpdateShippingAddresses = { defaultAddress, shippingAddresses ->
                                    viewModel.updateShippingAddresses(
                                        defaultAddress = defaultAddress,
                                        shippingAddresses = shippingAddresses
                                    )
                                },
                                onCancelOrder = { orderId ->
                                    viewModel.updateOrderStatus(orderId, OrderStatus.CANCELLED)
                                },
                                onExploreMarket = { navigateTopLevel(ROUTE_MARKETPLACE) },
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                },
                                notificationsEnabled = notificationsEnabled,
                                onRequestNotificationPermission = {
                                    requestNotificationPermission()
                                },
                                wishlistCount = wishlistProductIds.size,
                                onOpenWishlist = { navController.navigate(ROUTE_WISHLIST) },
                                reorderingOrderIds = cartUiState.reorderingOrderIds,
                                onReorder = { order ->
                                    cartViewModel.reorderOrder(order)
                                }
                            )
                        }

                        composable(ROUTE_WISHLIST) {
                            WishlistScreen(
                                wishlistState = wishlistItemsState,
                                publishedProductsState = publishedProductsState,
                                cartUiState = cartUiState,
                                onNavigateBack = { navController.popBackStack() },
                                onProductClick = { productId ->
                                    navController.navigate("$ROUTE_PRODUCT_DETAIL/$productId")
                                },
                                onAddToCart = { product, qty ->
                                    cartViewModel.addProductToCart(product, qty)
                                },
                                onRemoveFromWishlist = { wishlistItemId ->
                                    viewModel.removeFromWishlist(wishlistItemId)
                                },
                                onExploreMarket = {
                                    navController.popBackStack()
                                    navigateTopLevel(ROUTE_MARKETPLACE)
                                }
                            )
                        }

                        composable(
                            route = "$ROUTE_PRODUCT_EDITOR?productId={productId}",
                            arguments = listOf(
                                navArgument("productId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { entry ->
                            val productId = entry.arguments?.getString("productId")
                            val existingProduct = (sellerProductsState as? UiState.Success)
                                ?.data
                                ?.firstOrNull { it.id == productId }

                            ProductEditorScreen(
                                existingProduct = existingProduct,
                                defaultSellerName = storeName,
                                defaultCurrency = defaultCurrency,
                                isBusy = isBusy,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveProduct = { editingId, sellerName, title, desc, cat, price, curr, stock, published, preset, imgData, extraImages ->
                                    viewModel.saveProduct(
                                        editingProductId = editingId,
                                        sellerName = sellerName,
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        price = price,
                                        currency = curr,
                                        stock = stock,
                                        isPublished = published,
                                        imagePreset = preset,
                                        imageData = imgData,
                                        additionalImages = extraImages,
                                        onSuccess = { navController.popBackStack() }
                                    )
                                }
                            )
                        }

                        composable(
                            route = "$ROUTE_PRODUCT_DETAIL/{productId}",
                            arguments = listOf(
                                navArgument("productId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val productId = entry.arguments?.getString("productId").orEmpty()
                            val publishedList = (publishedProductsState as? UiState.Success)?.data.orEmpty()
                            val sellerList = (sellerProductsState as? UiState.Success)?.data.orEmpty()
                            val product = publishedList.firstOrNull { it.id == productId }
                                ?: sellerList.firstOrNull { it.id == productId }

                            val reviewsFlow = remember(productId) {
                                viewModel.observeProductReviews(productId)
                            }
                            val reviewsState by reviewsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
                            val priceHistoryFlow = remember(productId) {
                                viewModel.observeProductPriceHistory(productId)
                            }
                            val priceHistoryState by priceHistoryFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
                            val reviewerDisplayName = userProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName

                            ProductDetailScreen(
                                product = product,
                                currentUserId = currentUserId,
                                isOwnProduct = product?.sellerId == currentUserId,
                                quantityInCart = if (product != null) cartUiState.quantityInCart(product.id) else 0L,
                                isAddingToCart = product != null && product.id in cartUiState.pendingProductIds,
                                reviewsState = reviewsState,
                                priceHistoryState = priceHistoryState,
                                isReviewBusy = isBusy,
                                isWishlisted = product != null && product.id in wishlistProductIds,
                                onNavigateBack = { navController.popBackStack() },
                                onAddToCart = { target, qty ->
                                    cartViewModel.addProductToCart(target, qty)
                                },
                                onEditOwnProduct = { target ->
                                    navController.navigate("$ROUTE_PRODUCT_EDITOR?productId=${target.id}")
                                },
                                onToggleWishlist = { target ->
                                    viewModel.toggleWishlist(target)
                                },
                                onSubmitReview = { rating, comment ->
                                    viewModel.submitProductReview(
                                        productId = productId,
                                        reviewerName = reviewerDisplayName,
                                        rating = rating,
                                        comment = comment
                                    )
                                },
                                onDeleteReview = {
                                    viewModel.deleteProductReview(productId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIconWithBadge(
    icon: ImageVector,
    label: String,
    badgeCount: Int,
    tint: Color = Color.Unspecified
) {
    val iconTint = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint)
        }
    } else {
        Icon(imageVector = icon, contentDescription = label, tint = iconTint)
    }
}

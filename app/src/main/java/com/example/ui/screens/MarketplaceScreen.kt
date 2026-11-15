package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Product
import com.example.ui.components.AddToCartActionButton
import com.example.ui.components.CategoryFilterChipsRow
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductDetailCard
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.formatPrice
import com.example.ui.viewmodel.CartUiState
import com.example.ui.viewmodel.RecommendationsUiState
import com.example.ui.viewmodel.RecommendedProduct
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    productsState: UiState<List<Product>>,
    cartUiState: CartUiState,
    currentUserId: String,
    wishlistProductIds: Set<String> = emptySet(),
    recommendationsState: RecommendationsUiState = RecommendationsUiState(),
    onAddToCart: (Product, Long) -> Unit,
    onToggleWishlist: (Product) -> Unit = {},
    onOpenWishlist: () -> Unit = {},
    onOpenSellProduct: () -> Unit,
    onProductClick: (Product) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var isListView by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Banner
            item(span = { GridItemSpan(columns) }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_store_hero),
                            contentDescription = "واجهة السوق",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xCC063A31),
                                            Color(0x990F6B5B),
                                            Color(0x44000000)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "بيع واشترِ بسهولة",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "اكتشف منتجات المتاجر أو اعرض بضاعتك",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = onOpenSellProduct,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("hero_sell_now_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("اعرض منتجك الآن")
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar & View Mode Toggle
            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن منتج أو متجر...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "بحث")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("marketplace_clear_search_button")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "مسح البحث")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("marketplace_search_input")
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        IconButton(
                            onClick = onOpenWishlist,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("marketplace_open_wishlist_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (wishlistProductIds.isNotEmpty()) {
                                        Badge {
                                            Text(
                                                text = if (wishlistProductIds.size > 99) "99+" else wishlistProductIds.size.toString()
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (wishlistProductIds.isNotEmpty()) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "قائمة المفضلة",
                                    tint = if (wishlistProductIds.isNotEmpty()) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        IconButton(
                            onClick = { isListView = !isListView },
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("toggle_view_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isListView) Icons.Filled.GridView else Icons.Filled.ViewAgenda,
                                contentDescription = if (isListView) "عرض شبكي" else "عرض قائمة تفصيلية",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Category Filter Chips
            item(span = { GridItemSpan(columns) }) {
                CategoryFilterChipsRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    contentPadding = PaddingValues(0.dp),
                    testTagPrefix = "marketplace_category"
                )
            }

            // Recommended for You Section (based on Firestore purchase history & wishlist)
            if (searchQuery.isBlank() && selectedCategory == null && productsState is UiState.Success && productsState.data.isNotEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    RecommendedForYouSection(
                        recommendationsState = recommendationsState,
                        cartUiState = cartUiState,
                        wishlistProductIds = wishlistProductIds,
                        onProductClick = onProductClick,
                        onAddToCart = { product -> onAddToCart(product, 1L) },
                        onToggleWishlist = onToggleWishlist
                    )
                }
            }

            // Content State
            when (productsState) {
                is UiState.Loading -> {
                    item(span = { GridItemSpan(columns) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item(span = { GridItemSpan(columns) }) {
                        EmptyStateView(
                            title = "تعذر تحميل السوق",
                            subtitle = productsState.message,
                            actionLabel = "أضف منتجك",
                            onAction = onOpenSellProduct
                        )
                    }
                }

                is UiState.Success -> {
                    val filtered = productsState.data.filter { product ->
                        val matchesCat = selectedCategory == null || product.category == selectedCategory
                        val q = searchQuery.trim()
                        val matchesQuery = q.isEmpty() ||
                            product.title.contains(q, ignoreCase = true) ||
                            product.sellerName.contains(q, ignoreCase = true) ||
                            product.description.contains(q, ignoreCase = true)
                        matchesCat && matchesQuery
                    }

                    if (filtered.isEmpty()) {
                        item(span = { GridItemSpan(columns) }) {
                            EmptyStateView(
                                title = if (productsState.data.isEmpty()) "السوق بانتظار أول منتج!" else "لا توجد نتائج مطابقة",
                                subtitle = if (productsState.data.isEmpty())
                                    "ابدأ بعرض منتجك الأول ليظهر للعملاء في السوق فوراً."
                                else
                                    "جرّب البحث بكلمة أخرى أو تصفح جميع التصنيفات.",
                                actionLabel = when {
                                    productsState.data.isEmpty() -> "إضافة منتج جديد"
                                    searchQuery.isNotBlank() || selectedCategory != null -> "إعادة ضبط البحث"
                                    else -> null
                                },
                                onAction = when {
                                    productsState.data.isEmpty() -> onOpenSellProduct
                                    searchQuery.isNotBlank() || selectedCategory != null -> {
                                        {
                                            searchQuery = ""
                                            selectedCategory = null
                                        }
                                    }
                                    else -> null
                                }
                            )
                        }
                    } else if (isListView) {
                        items(
                            items = filtered,
                            key = { it.id },
                            span = { GridItemSpan(columns) }
                        ) { product ->
                            ProductDetailCard(
                                product = product,
                                quantityInCart = cartUiState.quantityInCart(product.id),
                                isAddingToCart = product.id in cartUiState.pendingProductIds,
                                isWishlisted = product.id in wishlistProductIds,
                                onClick = { onProductClick(product) },
                                onAddToCart = { onAddToCart(product, 1L) },
                                onToggleWishlist = { onToggleWishlist(product) }
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { product ->
                            MarketplaceProductCard(
                                product = product,
                                isOwnProduct = product.sellerId == currentUserId,
                                quantityInCart = cartUiState.quantityInCart(product.id),
                                isAddingToCart = product.id in cartUiState.pendingProductIds,
                                isWishlisted = product.id in wishlistProductIds,
                                onClick = { onProductClick(product) },
                                onQuickAddToCart = { onAddToCart(product, 1L) },
                                onToggleWishlist = { onToggleWishlist(product) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceProductCard(
    product: Product,
    isOwnProduct: Boolean,
    quantityInCart: Long,
    isAddingToCart: Boolean,
    isWishlisted: Boolean,
    onClick: () -> Unit,
    onQuickAddToCart: () -> Unit,
    onToggleWishlist: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
            ) {
                ProductThumbnail(
                    imageData = product.imageData,
                    imagePreset = product.imagePreset,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .testTag("wishlist_toggle_${product.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isWishlisted) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                                tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (product.stock <= 0L) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "نفد المخزون",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (isOwnProduct) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "متجرك",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = product.sellerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatPrice(product.price, product.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                AddToCartActionButton(
                    productId = product.id,
                    inStock = product.stock > 0L,
                    quantityInCart = quantityInCart,
                    isAdding = isAddingToCart,
                    onClick = onQuickAddToCart,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RecommendedForYouSection(
    recommendationsState: RecommendationsUiState,
    cartUiState: CartUiState,
    wishlistProductIds: Set<String>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recommended_for_you_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "مقترحة لك",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("recommended_section_header")
                )
                Text(
                    text = "مختارات بناءً على مشترياتك السابقة وقائمة المفضلة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recommendationsState.items.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recommended_empty_hint")
            ) {
                Text(
                    text = "أضف منتجات إلى المفضلة أو أتمم أول طلب لتظهر لك توصيات مخصصة حسب ذوقك هنا.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recommended_products_row")
            ) {
                lazyRowItems(
                    items = recommendationsState.items,
                    key = { "rec_${it.product.id}" }
                ) { rec ->
                    RecommendedProductCard(
                        recommended = rec,
                        quantityInCart = cartUiState.quantityInCart(rec.product.id),
                        isAddingToCart = rec.product.id in cartUiState.pendingProductIds,
                        isWishlisted = rec.product.id in wishlistProductIds,
                        onClick = { onProductClick(rec.product) },
                        onAddToCart = { onAddToCart(rec.product) },
                        onToggleWishlist = { onToggleWishlist(rec.product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendedProductCard(
    recommended: RecommendedProduct,
    quantityInCart: Long,
    isAddingToCart: Boolean,
    isWishlisted: Boolean,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onToggleWishlist: () -> Unit
) {
    val product = recommended.product
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier
            .width(196.dp)
            .clickable(onClick = onClick)
            .testTag("recommended_card_${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(114.dp)
            ) {
                ProductThumbnail(
                    imageData = product.imageData,
                    imagePreset = product.imagePreset,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = recommended.reasonLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .testTag("recommended_wishlist_toggle_${product.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isWishlisted) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                                tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.sellerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatPrice(product.price, product.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                AddToCartActionButton(
                    productId = product.id,
                    inStock = product.stock > 0L,
                    quantityInCart = quantityInCart,
                    isAdding = isAddingToCart,
                    onClick = onAddToCart,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

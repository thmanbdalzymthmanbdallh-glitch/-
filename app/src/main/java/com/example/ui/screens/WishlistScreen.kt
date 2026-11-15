package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.WishlistItem
import com.example.ui.components.AddToCartActionButton
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.formatPrice
import com.example.ui.viewmodel.CartUiState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    wishlistState: UiState<List<WishlistItem>>,
    publishedProductsState: UiState<List<Product>>,
    cartUiState: CartUiState,
    onNavigateBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onAddToCart: (Product, Long) -> Unit,
    onRemoveFromWishlist: (String) -> Unit,
    onExploreMarket: () -> Unit
) {
    val liveProductsById = remember(publishedProductsState) {
        (publishedProductsState as? UiState.Success)?.data?.associateBy { it.id }.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "قائمة المفضلة",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("wishlist_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when (wishlistState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = "تعذر تحميل قائمة المفضلة",
                        subtitle = wishlistState.message,
                        actionLabel = "العودة",
                        onAction = onNavigateBack
                    )
                }
            }

            is UiState.Success -> {
                val items = wishlistState.data
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            title = "قائمة المفضلة فارغة",
                            subtitle = "اضغط على أيقونة القلب على أي منتج يعجبك لحفظه هنا والرجوع إليه لاحقاً.",
                            actionLabel = "تصفح السوق",
                            onAction = onExploreMarket
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxSize()
                                .testTag("wishlist_lazy_column")
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "لديك ${items.size} منتج محفوظ في المفضلة",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            items(items, key = { it.id }) { item ->
                                val liveProduct = liveProductsById[item.productId]
                                val displayTitle = liveProduct?.title ?: item.productTitle
                                val displaySeller = liveProduct?.sellerName ?: item.sellerName
                                val displayPrice = liveProduct?.price ?: item.price
                                val displayCurrency = liveProduct?.currency ?: item.currency
                                val displayCategory = liveProduct?.category ?: item.category
                                val displayImageData = liveProduct?.imageData ?: item.imageData
                                val displayPreset = liveProduct?.imagePreset ?: item.imagePreset
                                val inStock = liveProduct == null || liveProduct.stock > 0L

                                Card(
                                    onClick = { onProductClick(item.productId) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("wishlist_card_${item.productId}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ProductThumbnail(
                                                imageData = displayImageData,
                                                imagePreset = displayPreset,
                                                contentDescription = displayTitle,
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(50)
                                                ) {
                                                    Text(
                                                        text = ProductCategory.fromKey(displayCategory).labelAr,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = displayTitle,
                                                    style = MaterialTheme.typography.titleMedium,
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
                                                        text = displaySeller,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = formatPrice(displayPrice, displayCurrency),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { onRemoveFromWishlist(item.id) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("wishlist_remove_button_${item.productId}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Favorite,
                                                    contentDescription = "إزالة من المفضلة",
                                                    tint = Color(0xFFE11D48)
                                                )
                                            }
                                        }

                                        if (liveProduct != null) {
                                            Spacer(Modifier.height(10.dp))
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            AddToCartActionButton(
                                                productId = liveProduct.id,
                                                inStock = inStock,
                                                quantityInCart = cartUiState.quantityInCart(liveProduct.id),
                                                isAdding = liveProduct.id in cartUiState.pendingProductIds,
                                                onClick = { onAddToCart(liveProduct, 1L) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

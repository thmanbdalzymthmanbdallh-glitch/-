package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PriceHistoryEntry
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductReview
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FullScreenImageViewerDialog
import com.example.ui.components.ProductPriceChartCard
import com.example.ui.components.ProductRatingPill
import com.example.ui.components.ProductReviewsSection
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.formatPrice
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product?,
    currentUserId: String,
    isOwnProduct: Boolean,
    quantityInCart: Long,
    isAddingToCart: Boolean,
    reviewsState: UiState<List<ProductReview>>,
    priceHistoryState: UiState<List<PriceHistoryEntry>> = UiState.Loading,
    isReviewBusy: Boolean,
    isWishlisted: Boolean = false,
    onNavigateBack: () -> Unit,
    onAddToCart: (Product, Long) -> Unit,
    onEditOwnProduct: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit = {},
    onSubmitReview: (rating: Int, comment: String) -> Unit,
    onDeleteReview: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفاصيل المنتج",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("product_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(
                            onClick = { onToggleWishlist(product) },
                            modifier = Modifier.testTag("detail_wishlist_button")
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isWishlisted) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                                tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (product != null) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                            if (isOwnProduct) {
                                OutlinedButton(
                                    onClick = { onEditOwnProduct(product) },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("detail_edit_product_button")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("تعديل هذا المنتج في متجري")
                                }
                            } else if (product.stock > 0L) {
                                var selectedQuantity by remember(product.id) { mutableLongStateOf(1L) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (selectedQuantity > 1L) selectedQuantity-- },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("detail_decrease_qty_button")
                                            ) {
                                                Icon(Icons.Filled.Remove, contentDescription = "تقليل الكمية")
                                            }
                                            Text(
                                                text = selectedQuantity.toString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            IconButton(
                                                onClick = { if (selectedQuantity < product.stock) selectedQuantity++ },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("detail_increase_qty_button")
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = "زيادة الكمية")
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { onAddToCart(product, selectedQuantity) },
                                        enabled = !isAddingToCart,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .testTag("detail_add_to_cart_button")
                                    ) {
                                        if (isAddingToCart) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (quantityInCart > 0L) Icons.Filled.Check else Icons.Filled.AddShoppingCart,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (quantityInCart > 0L)
                                                    "في السلة ($quantityInCart) • أضف المزيد"
                                                else
                                                    "أضف إلى السلة",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text("نفد من المخزون حالياً")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = "المنتج غير متوفر",
                    subtitle = "قد يكون تم حذف هذا المنتج أو لم يعد منشوراً.",
                    actionLabel = "العودة",
                    onAction = onNavigateBack
                )
            }
            return@Scaffold
        }

        val galleryImages = remember(product.id, product.imageData, product.additionalImages) {
            buildList {
                add(product.imageData)
                product.additionalImages
                    .filter { it.isNotBlank() }
                    .forEach { add(it) }
            }
        }
        val pagerState = rememberPagerState(pageCount = { galleryImages.size })
        var isFullScreenViewerOpen by remember { mutableStateOf(false) }

        if (isFullScreenViewerOpen) {
            FullScreenImageViewerDialog(
                images = galleryImages,
                imagePreset = product.imagePreset,
                productTitle = product.title,
                initialPage = pagerState.currentPage,
                onDismiss = { lastPage ->
                    isFullScreenViewerOpen = false
                    if (lastPage in galleryImages.indices && lastPage != pagerState.currentPage) {
                        scope.launch { pagerState.scrollToPage(lastPage) }
                    }
                }
            )
        }

        val scrollState = rememberScrollState()
        var reviewsSectionY by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Image Gallery Pager
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .testTag("product_detail_gallery_pager")
                    ) { page ->
                        ProductThumbnail(
                            imageData = galleryImages[page],
                            imagePreset = product.imagePreset,
                            contentDescription = "${product.title} - صورة ${page + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isFullScreenViewerOpen = true }
                                .testTag("product_detail_hero_image_$page")
                        )
                    }

                    // Expand to Full-Screen Viewer Button
                    Surface(
                        color = Color.Black.copy(alpha = 0.56f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        IconButton(
                            onClick = { isFullScreenViewerOpen = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("product_detail_expand_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomOutMap,
                                contentDescription = "عرض الصور بملء الشاشة",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (galleryImages.size > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.58f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${galleryImages.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!product.isPublished) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "مسودة",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Pager Dots & Additional Images Strip
                if (galleryImages.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(galleryImages.size) { index ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (selected) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Collections,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "معرض الصور (${galleryImages.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("product_detail_thumbnails_row")
                        ) {
                            itemsIndexed(galleryImages) { index, img ->
                                val selected = pagerState.currentPage == index
                                ProductThumbnail(
                                    imageData = img,
                                    imagePreset = product.imagePreset,
                                    contentDescription = "صورة مصغرة ${index + 1}",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                        .testTag("product_detail_thumbnail_$index")
                                )
                            }
                        }
                    }
                }

                // Category, Stock, Title, Price
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = ProductCategory.fromKey(product.category).labelAr,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = if (product.stock > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (product.stock > 0L) "المتوفر: ${product.stock}" else "نفد المخزون",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (product.stock > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }

                            if (product.salesCount > 0L) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalMall,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${product.salesCount} مبيع",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("product_detail_title")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatPrice(product.price, product.currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("product_detail_price")
                        )

                        ProductRatingPill(
                            reviewsState = reviewsState,
                            onClick = {
                                scope.launch {
                                    scrollState.animateScrollTo(reviewsSectionY)
                                }
                            }
                        )
                    }
                }

                // Seller Info Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "البائع",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = product.sellerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Full Product Description Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "الوصف الكامل للمنتج",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        Text(
                            text = product.description.ifBlank { "لم يضف البائع وصفاً تفصيلياً لهذا المنتج بعد." },
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = if (product.description.isBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("product_detail_full_description")
                        )
                    }
                }

                // 30-Day Historical Price Fluctuations Line Chart (Vico)
                ProductPriceChartCard(
                    product = product,
                    priceHistoryState = priceHistoryState,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Product Ratings & Reviews Section
                ProductReviewsSection(
                    reviewsState = reviewsState,
                    currentUserId = currentUserId,
                    isBusy = isReviewBusy,
                    onSubmitReview = onSubmitReview,
                    onDeleteReview = onDeleteReview,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        reviewsSectionY = coords.positionInParent().y.toInt()
                    }
                )
            }
        }
    }
}

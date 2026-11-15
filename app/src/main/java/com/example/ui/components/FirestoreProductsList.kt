package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.authStateFlow
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.remote.MatjariRepository
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.unwrapFirestoreException
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Self-contained Composable that observes published products from Cloud Firestore
 * (scoped to the authenticated user session and named applet database ID),
 * provides a top search bar to filter products by name, and displays each product's
 * details inside a Material 3 [Card] within a [LazyColumn], complete with an Add-to-Cart
 * action button on each card.
 */
@Composable
fun FirestoreProductsList(
    modifier: Modifier = Modifier,
    cartQuantities: Map<String, Long> = emptyMap(),
    pendingProductIds: Set<String> = emptySet(),
    onProductClick: ((Product) -> Unit)? = null,
    onAddToCart: ((Product) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val firestore = remember(context) {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }
    val fallbackRepository = remember(firestore) { MatjariRepository(firestore) }
    var localPendingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedProductIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }

    val productsState by produceState<UiState<List<Product>>>(
        initialValue = UiState.Loading,
        key1 = currentUser?.uid,
        key2 = firestore
    ) {
        val user = currentUser
        if (user == null) {
            value = UiState.Error("يجب تسجيل الدخول بحساب Google لعرض المنتجات")
            return@produceState
        }

        value = UiState.Loading
        val collectionPath = "products"
        firestore.collection(collectionPath)
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Product::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                handleFirestoreError(root, OperationType.LIST, collectionPath)
                value = UiState.Error(root.localizedMessage ?: "تعذر تحميل قائمة المنتجات")
            }
            .collect { products ->
                value = UiState.Success(products)
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث باسم المنتج...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "بحث"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("products_list_clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "مسح البحث"
                        )
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
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
                .testTag("products_list_search_input")
        )

        CategoryFilterChipsRow(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            testTagPrefix = "products_list_category",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (val state = productsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("products_loading_indicator"))
                }
            }

            is UiState.Error -> {
                EmptyStateView(
                    title = "تعذر تحميل المنتجات",
                    subtitle = state.message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            is UiState.Success -> {
                val trimmedQuery = searchQuery.trim()
                val filteredProducts = remember(state.data, trimmedQuery, selectedCategory) {
                    state.data.filter { product ->
                        val matchesCategory = selectedCategory == null || product.category == selectedCategory
                        val matchesQuery = trimmedQuery.isEmpty() ||
                            product.title.contains(trimmedQuery, ignoreCase = true)
                        matchesCategory && matchesQuery
                    }
                }

                if (state.data.isEmpty()) {
                    EmptyStateView(
                        title = "لا توجد منتجات منشورة حالياً",
                        subtitle = "ستظهر المنتجات فور إضافتها ونشرها في السوق.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else if (filteredProducts.isEmpty()) {
                    val categoryLabel = selectedCategory?.let { ProductCategory.fromKey(it).labelAr }
                    EmptyStateView(
                        title = "لا توجد نتائج مطابقة",
                        subtitle = when {
                            trimmedQuery.isNotEmpty() && categoryLabel != null ->
                                "لم يتم العثور على \"$trimmedQuery\" في تصنيف $categoryLabel."
                            trimmedQuery.isNotEmpty() ->
                                "لم يتم العثور على منتج باسم \"$trimmedQuery\"."
                            categoryLabel != null ->
                                "لا توجد منتجات منشورة حالياً في تصنيف $categoryLabel."
                            else -> "جرّب تعديل خيارات البحث."
                        },
                        actionLabel = "إعادة ضبط التصفية",
                        onAction = {
                            searchQuery = ""
                            selectedCategory = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("firestore_products_lazy_column"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = filteredProducts, key = { it.id }) { product ->
                            val isAdding = product.id in pendingProductIds || product.id in localPendingIds
                            val isExpanded = product.id in expandedProductIds
                            ProductDetailCard(
                                product = product,
                                quantityInCart = cartQuantities[product.id] ?: 0L,
                                isAddingToCart = isAdding,
                                isExpanded = isExpanded,
                                showExpandToggle = onProductClick == null,
                                onClick = {
                                    if (onProductClick != null) {
                                        onProductClick(product)
                                    } else {
                                        expandedProductIds = if (isExpanded) {
                                            expandedProductIds - product.id
                                        } else {
                                            expandedProductIds + product.id
                                        }
                                    }
                                },
                                onAddToCart = {
                                    if (onAddToCart != null) {
                                        onAddToCart(product)
                                    } else {
                                        scope.launch {
                                            localPendingIds = localPendingIds + product.id
                                            fallbackRepository.addToCart(product, 1L)
                                            localPendingIds = localPendingIds - product.id
                                        }
                                    }
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
fun ProductDetailCard(
    product: Product,
    quantityInCart: Long = 0L,
    isAddingToCart: Boolean = false,
    isExpanded: Boolean = false,
    showExpandToggle: Boolean = false,
    isWishlisted: Boolean = false,
    onToggleWishlist: (() -> Unit)? = null,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraImages = remember(product.additionalImages) {
        product.additionalImages.filter { it.isNotBlank() }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.size(88.dp)) {
                    ProductThumbnail(
                        imageData = product.imageData,
                        imagePreset = product.imagePreset,
                        contentDescription = product.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    )
                    if (extraImages.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "+${extraImages.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = formatPrice(product.price, product.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!isExpanded && product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Text(
                        text = product.description.ifBlank { "لا يوجد وصف إضافي لهذا المنتج." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (extraImages.isNotEmpty()) {
                        Text(
                            text = "صور إضافية (${extraImages.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(extraImages) { extraImg ->
                                ProductThumbnail(
                                    imageData = extraImg,
                                    imagePreset = product.imagePreset,
                                    contentDescription = product.title,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.sellerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = if (product.stock > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (product.stock > 0L) "المتوفر: ${product.stock}" else "نفد المخزون",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (product.stock > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        if (showExpandToggle) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (isExpanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onToggleWishlist != null) {
                        IconButton(
                            onClick = onToggleWishlist,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("wishlist_toggle_${product.id}")
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isWishlisted) "إزالة من المفضلة" else "إضافة للمفضلة",
                                tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    AddToCartActionButton(
                        productId = product.id,
                        inStock = product.stock > 0L,
                        quantityInCart = quantityInCart,
                        isAdding = isAddingToCart,
                        onClick = onAddToCart
                    )
                }
            }
        }
    }
}

@Composable
fun AddToCartActionButton(
    productId: String,
    inStock: Boolean,
    quantityInCart: Long,
    isAdding: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (quantityInCart > 0L && inStock) {
        FilledTonalButton(
            onClick = onClick,
            enabled = !isAdding,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("add_to_cart_card_button_$productId")
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "في السلة ($quantityInCart) +",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = inStock && !isAdding,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("add_to_cart_card_button_$productId")
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AddShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (inStock) "أضف للسلة" else "نفد المخزون",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

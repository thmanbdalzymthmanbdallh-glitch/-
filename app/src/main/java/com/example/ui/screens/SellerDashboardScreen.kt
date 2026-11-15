package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CurrencyOption
import com.example.data.model.Discount
import com.example.data.model.DiscountType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.ui.components.CategoryFilterChipsRow
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.SellerDiscountsBottomSheet
import com.example.ui.components.formatPrice
import com.example.ui.viewmodel.UiState

private enum class SellerFilter(val label: String) {
    ALL("الكل"),
    PUBLISHED("منشور"),
    DRAFT("مسودة"),
    LOW_STOCK("مخزون منخفض")
}

@Composable
fun SellerDashboardScreen(
    storeName: String,
    defaultCurrency: CurrencyOption,
    sellerProductsState: UiState<List<Product>>,
    sellerOrdersState: UiState<List<Order>>,
    sellerDiscountsState: UiState<List<Discount>> = UiState.Success(emptyList()),
    isBusy: Boolean = false,
    onAddProduct: () -> Unit,
    onProductClick: (Product) -> Unit = {},
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onCreateDiscount: (String, DiscountType, Double, Double, () -> Unit) -> Unit = { _, _, _, _, _ -> },
    onToggleDiscountActive: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteDiscount: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(SellerFilter.ALL) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showDiscountsSheet by remember { mutableStateOf(false) }

    val products = (sellerProductsState as? UiState.Success)?.data.orEmpty()
    val orders = (sellerOrdersState as? UiState.Success)?.data.orEmpty()

    val validOrders = orders.filter { it.status != OrderStatus.CANCELLED.key }
    val totalRevenue = validOrders.sumOf { it.totalPrice }
    val pendingOrdersCount = orders.count { it.status == OrderStatus.PENDING.key }
    val totalUnitsSold = validOrders.sumOf { it.quantity }
    val activeListingsCount = products.count { it.isPublished }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProduct,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة منتج جديد")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 88.dp + innerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = storeName.ifBlank { "متجري" },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "لوحة البائع لإدارة منتجاتك ومبيعاتك",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // KPI Metrics 2x2 Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SellerKpiCard(
                            icon = Icons.Filled.AttachMoney,
                            label = "إجمالي المبيعات",
                            value = formatPrice(totalRevenue, defaultCurrency.code),
                            highlight = true,
                            modifier = Modifier.weight(1f)
                        )
                        SellerKpiCard(
                            icon = Icons.Filled.PendingActions,
                            label = "طلبات بانتظارك",
                            value = pendingOrdersCount.toString(),
                            highlight = pendingOrdersCount > 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SellerKpiCard(
                            icon = Icons.Filled.Inventory2,
                            label = "المنتجات النشطة",
                            value = activeListingsCount.toString(),
                            highlight = false,
                            modifier = Modifier.weight(1f)
                        )
                        SellerKpiCard(
                            icon = Icons.Filled.ShoppingBag,
                            label = "القطع المباعة",
                            value = totalUnitsSold.toString(),
                            highlight = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Promo Codes Manager Button
            item {
                val activeDiscountsCount = (sellerDiscountsState as? UiState.Success)?.data?.count { it.isActive } ?: 0
                OutlinedButton(
                    onClick = { showDiscountsSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("manage_discounts_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalOffer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (activeDiscountsCount > 0) {
                            "أكواد الخصم الترويجية ($activeDiscountsCount نشط)"
                        } else {
                            "إنشاء وإدارة أكواد الخصم"
                        }
                    )
                }
            }

            // Search Bar for Seller Products
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في منتجات متجرك بالاسم...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "بحث")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("seller_products_clear_search_button")
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
                        .fillMaxWidth()
                        .testTag("seller_products_search_input")
                )
            }

            // Category Filter Chips
            item {
                CategoryFilterChipsRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    contentPadding = PaddingValues(0.dp),
                    testTagPrefix = "seller_category"
                )
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SellerFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            when (sellerProductsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        EmptyStateView(
                            title = "تعذر تحميل منتجات متجرك",
                            subtitle = sellerProductsState.message,
                            actionLabel = "إضافة منتج",
                            onAction = onAddProduct
                        )
                    }
                }

                is UiState.Success -> {
                    val trimmedQuery = searchQuery.trim()
                    val filteredProducts = products.filter { p ->
                        val matchesFilter = when (selectedFilter) {
                            SellerFilter.ALL -> true
                            SellerFilter.PUBLISHED -> p.isPublished
                            SellerFilter.DRAFT -> !p.isPublished
                            SellerFilter.LOW_STOCK -> p.stock <= 3L
                        }
                        val matchesCategory = selectedCategory == null || p.category == selectedCategory
                        val matchesName = trimmedQuery.isEmpty() || p.title.contains(trimmedQuery, ignoreCase = true)
                        matchesFilter && matchesCategory && matchesName
                    }

                    if (filteredProducts.isEmpty()) {
                        val hasActiveSearchOrCategory = trimmedQuery.isNotEmpty() || selectedCategory != null
                        item {
                            EmptyStateView(
                                title = when {
                                    products.isEmpty() -> "متجرك فارغ حالياً"
                                    hasActiveSearchOrCategory -> "لا توجد منتجات مطابقة"
                                    else -> "لا توجد منتجات في هذا التصنيف"
                                },
                                subtitle = when {
                                    products.isEmpty() -> "أضف منتجك الأول بالسعر والكمية ليصبح متاحاً للبيع فوراً."
                                    trimmedQuery.isNotEmpty() -> "لم يتم العثور على منتج باسم \"$trimmedQuery\"."
                                    selectedCategory != null -> "لا توجد منتجات ضمن فئة ${ProductCategory.fromKey(selectedCategory!!).labelAr}."
                                    else -> "اختر فلتراً آخر أو أضف منتجاً جديداً."
                                },
                                actionLabel = if (hasActiveSearchOrCategory && products.isNotEmpty()) "إعادة ضبط التصفية" else "أضف منتجاً الآن",
                                onAction = if (hasActiveSearchOrCategory && products.isNotEmpty()) {
                                    {
                                        searchQuery = ""
                                        selectedCategory = null
                                    }
                                } else {
                                    onAddProduct
                                }
                            )
                        }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            SellerProductRow(
                                product = product,
                                onClick = { onProductClick(product) },
                                onEdit = { onEditProduct(product) },
                                onDelete = { productToDelete = product }
                            )
                        }
                    }
                }
            }
        }
    }

    productToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("حذف المنتج") },
            text = { Text("هل أنت متأكد من حذف \"${target.title}\"؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProduct(target.id)
                        productToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showDiscountsSheet) {
        SellerDiscountsBottomSheet(
            discountsState = sellerDiscountsState,
            defaultCurrency = defaultCurrency,
            isBusy = isBusy,
            onDismiss = { showDiscountsSheet = false },
            onCreateDiscount = onCreateDiscount,
            onToggleActive = onToggleDiscountActive,
            onDeleteDiscount = onDeleteDiscount
        )
    }
}

@Composable
private fun SellerKpiCard(
    icon: ImageVector,
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SellerProductRow(
    product: Product,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seller_product_card_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(
                imageData = product.imageData,
                imagePreset = product.imagePreset,
                contentDescription = product.title,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = if (product.isPublished)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (product.isPublished) "منشور" else "مسودة",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (product.isPublished)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = formatPrice(product.price, product.currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${ProductCategory.fromKey(product.category).labelAr} • المخزون: ${product.stock} • مبيعات: ${product.salesCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock <= 3L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "تعديل ${product.title}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف ${product.title}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

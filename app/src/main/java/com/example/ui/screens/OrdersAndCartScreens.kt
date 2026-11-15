package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OrderHistorySearchBar
import com.example.ui.components.OrderProgressTracker
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.filterOrdersByQuery
import com.example.ui.components.formatOrderShortRef
import com.example.ui.components.formatPrice
import com.example.ui.components.shareOrderSummary
import com.example.ui.viewmodel.CartUiState
import com.example.ui.viewmodel.UiState

@Composable
fun CartScreen(
    cartUiState: CartUiState,
    onUpdateQuantity: (String, Long) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onProceedToCheckout: () -> Unit,
    onExploreMarket: () -> Unit
) {

    when {
        cartUiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        cartUiState.errorMessage != null -> {
            EmptyStateView(
                title = "تعذر تحميل السلة",
                subtitle = cartUiState.errorMessage
            )
        }

        cartUiState.items.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    title = "سلة التسوق فارغة",
                    subtitle = "تصفح المنتجات المعروضة في السوق وأضف ما يعجبك هنا لإتمام الشراء.",
                    actionLabel = "تصفح السوق",
                    onAction = onExploreMarket
                )
            }
        }

        else -> {
            val items = cartUiState.items
            val primaryCurrency = cartUiState.primaryCurrency
            val totalAmount = cartUiState.totalAmount

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${cartUiState.totalItemsCount} قطعة في السلة",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onClearCart,
                        enabled = !cartUiState.isBusy,
                        modifier = Modifier.testTag("clear_cart_button")
                    ) {
                        Text("إفراغ السلة", color = MaterialTheme.colorScheme.error)
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items, key = { it.id }) { item ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProductThumbnail(
                                        imageData = item.imageData,
                                        imagePreset = item.imagePreset,
                                        contentDescription = item.productTitle,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = item.productTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.sellerName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatPrice(item.unitPrice * item.quantity, item.currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onUpdateQuantity(item.id, item.quantity - 1L) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Filled.Remove, contentDescription = "إنقاص الكمية")
                                        }
                                        Text(
                                            text = item.quantity.toString(),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        IconButton(
                                            onClick = { onUpdateQuantity(item.id, item.quantity + 1L) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "زيادة الكمية")
                                        }
                                        IconButton(
                                            onClick = { onRemoveItem(item.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "حذف من السلة",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الإجمالي (${items.sumOf { it.quantity }} قطعة)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = formatPrice(totalAmount, primaryCurrency),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(
                                onClick = onProceedToCheckout,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("checkout_button")
                            ) {
                                Text(
                                    text = "إتمام الطلب",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun OrdersScreen(
    sellerOrdersState: UiState<List<Order>>,
    buyerOrdersState: UiState<List<Order>>,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit,
    reorderingOrderIds: Set<String> = emptySet(),
    onReorder: (Order) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("طلبات العملاء (متجري)") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("مشترياتي") }
            )
        }

        val state = if (selectedTab == 0) sellerOrdersState else buyerOrdersState
        val isSellerTab = selectedTab == 0
        val allOrders = (state as? UiState.Success)?.data.orEmpty()
        val filteredOrders = remember(allOrders, searchQuery) {
            filterOrdersByQuery(allOrders, searchQuery)
        }

        OrderHistorySearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            matchedCount = filteredOrders.size,
            totalCount = allOrders.size,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        when (state) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = "تعذر تحميل الطلبات",
                        subtitle = state.message
                    )
                }
            }

            is UiState.Success -> {
                when {
                    allOrders.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                title = if (isSellerTab) "لا توجد طلبات واردة بعد" else "لم تقم بأي طلبات بعد",
                                subtitle = if (isSellerTab)
                                    "عند قيام العملاء بشراء منتجاتك ستظهر تفاصيلهم هنا لتأكيدها وشحنها."
                                else
                                    "عند الشراء من السوق ستتمكن من متابعة حالة طلباتك هنا."
                            )
                        }
                    }

                    filteredOrders.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                title = "لا توجد طلبات مطابقة",
                                subtitle = "لم نعثر على طلبات تطابق \"${searchQuery.trim()}\". جرّب البحث باسم المنتج أو رقم الطلب.",
                                actionLabel = "مسح البحث",
                                onAction = { searchQuery = "" }
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("orders_list")
                        ) {
                            items(filteredOrders, key = { it.id }) { order ->
                                OrderCard(
                                    order = order,
                                    isSellerView = isSellerTab,
                                    isReordering = order.id in reorderingOrderIds,
                                    onUpdateStatus = { newStatus -> onUpdateOrderStatus(order.id, newStatus) },
                                    onReorder = { onReorder(order) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    isSellerView: Boolean,
    isReordering: Boolean = false,
    onUpdateStatus: (OrderStatus) -> Unit,
    onReorder: () -> Unit = {}
) {
    val status = OrderStatus.fromKey(order.status)
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.productTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "رقم الطلب: ${formatOrderShortRef(order.id)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                OrderStatusChip(status = status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الكمية: ${order.quantity} × ${formatPrice(order.unitPrice, order.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatPrice(order.totalPrice, order.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (order.discountCode.isNotBlank() && order.discountAmount > 0.0) {
                Text(
                    text = "كود الخصم (${order.discountCode}): -${formatPrice(order.discountAmount, order.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            if (isSellerView) {
                Text(
                    text = "العميل: ${order.buyerName} • ${order.buyerPhone}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "العنوان: ${order.shippingAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "المتجر: ${order.sellerName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (order.note.isNotBlank()) {
                Text(
                    text = "ملاحظة: ${order.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            OrderProgressTracker(
                order = order,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Action Buttons respecting State Machine, Terminal Locking & Order Sharing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!status.isTerminal && isSellerView) {
                    when (status) {
                        OrderStatus.PENDING -> {
                            Button(
                                onClick = { onUpdateStatus(OrderStatus.CONFIRMED) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تأكيد الطلب")
                            }
                        }
                        OrderStatus.CONFIRMED -> {
                            Button(
                                onClick = { onUpdateStatus(OrderStatus.SHIPPED) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تم الشحن")
                            }
                        }
                        OrderStatus.SHIPPED -> {
                            Button(
                                onClick = { onUpdateStatus(OrderStatus.DELIVERED) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تم التسليم")
                            }
                        }
                        else -> Unit
                    }

                    OutlinedButton(
                        onClick = { onUpdateStatus(OrderStatus.CANCELLED) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("إلغاء")
                    }

                    OutlinedIconButton(
                        onClick = { shareOrderSummary(context, order) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("share_order_button_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "مشاركة تفاصيل الطلب",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { shareOrderSummary(context, order) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("share_order_button_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("مشاركة الطلب")
                    }

                    if (!isSellerView && status == OrderStatus.PENDING) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(OrderStatus.CANCELLED) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("إلغاء الطلب")
                        }
                    } else if (!isSellerView && status == OrderStatus.DELIVERED) {
                        Button(
                            onClick = onReorder,
                            enabled = !isReordering,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("reorder_button_${order.id}")
                        ) {
                            if (isReordering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("إعادة الطلب")
                            }
                        }
                    }
                }
            }
        }
    }
}

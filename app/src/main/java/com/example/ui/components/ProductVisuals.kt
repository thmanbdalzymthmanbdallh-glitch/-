package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CurrencyOption
import com.example.data.model.ImagePreset
import com.example.data.model.OrderStatus
import com.example.data.model.ProductCategory
import java.text.DecimalFormat

fun formatPrice(amount: Double, currencyCode: String): String {
    val currency = CurrencyOption.fromCode(currencyCode)
    val df = DecimalFormat("#,##0.##")
    return "${df.format(amount)} ${currency.symbolAr}"
}

fun presetIcon(presetKey: String): ImageVector = when (ImagePreset.fromKey(presetKey)) {
    ImagePreset.ELECTRONICS -> Icons.Filled.Headphones
    ImagePreset.FASHION -> Icons.Filled.Checkroom
    ImagePreset.PERFUME -> Icons.Filled.Spa
    ImagePreset.HOME -> Icons.Filled.Chair
    ImagePreset.ACCESSORIES -> Icons.Filled.Watch
    ImagePreset.GIFT -> Icons.Filled.CardGiftcard
}

fun categoryIcon(category: ProductCategory): ImageVector = when (category) {
    ProductCategory.ELECTRONICS -> Icons.Filled.Headphones
    ProductCategory.FASHION -> Icons.Filled.Checkroom
    ProductCategory.HOME -> Icons.Filled.Chair
    ProductCategory.BEAUTY -> Icons.Filled.Spa
    ProductCategory.ACCESSORIES -> Icons.Filled.Watch
    ProductCategory.OTHER -> Icons.Filled.Category
}

@Composable
fun CategoryFilterChipsRow(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    testTagPrefix: String = "category_chip"
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_row"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            val isSelected = selectedCategory == null
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(null) },
                label = { Text("الكل") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("${testTagPrefix}_all")
            )
        }

        items(items = ProductCategory.entries, key = { it.key }) { category ->
            val isSelected = selectedCategory == category.key
            FilterChip(
                selected = isSelected,
                onClick = {
                    onCategorySelected(if (isSelected) null else category.key)
                },
                label = { Text(category.labelAr) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Check else categoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("${testTagPrefix}_${category.key}")
            )
        }
    }
}

@Composable
fun ProductThumbnail(
    imageData: String?,
    imagePreset: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val decodedBitmap = remember(imageData) { decodeBase64ToImageBitmap(imageData) }
    val gradientColors = when (ImagePreset.fromKey(imagePreset)) {
        ImagePreset.ELECTRONICS -> listOf(Color(0xFF0F6B5B), Color(0xFF1E9B85))
        ImagePreset.FASHION -> listOf(Color(0xFF8C4A27), Color(0xFFD97706))
        ImagePreset.PERFUME -> listOf(Color(0xFF5D4037), Color(0xFFB47B56))
        ImagePreset.HOME -> listOf(Color(0xFF2D5A4C), Color(0xFF6B9080))
        ImagePreset.ACCESSORIES -> listOf(Color(0xFF334155), Color(0xFF64748B))
        ImagePreset.GIFT -> listOf(Color(0xFFB45309), Color(0xFFF59E0B))
    }

    Box(
        modifier = modifier
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = presetIcon(imagePreset),
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.SHIPPED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.DELIVERED -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = status.labelAr,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_catalog),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(168.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

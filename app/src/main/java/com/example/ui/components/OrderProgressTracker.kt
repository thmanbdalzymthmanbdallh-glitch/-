package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class OrderStepMeta(
    val status: OrderStatus,
    val shortLabel: String,
    val fullTitle: String,
    val description: String,
    val icon: ImageVector
)

private val ORDER_FLOW_STEPS = listOf(
    OrderStepMeta(
        status = OrderStatus.PENDING,
        shortLabel = "المراجعة",
        fullTitle = "استلام ومراجعة الطلب",
        description = "تم استلام طلبك بنجاح وبانتظار تأكيد التاجر وتوفر المخزون.",
        icon = Icons.Filled.ReceiptLong
    ),
    OrderStepMeta(
        status = OrderStatus.CONFIRMED,
        shortLabel = "التجهيز",
        fullTitle = "قيد التجهيز والتغليف",
        description = "تم تأكيد طلبك ويعمل المتجر الآن على تجهيزه وتغليفه للشحن.",
        icon = Icons.Filled.Inventory2
    ),
    OrderStepMeta(
        status = OrderStatus.SHIPPED,
        shortLabel = "في الطريق",
        fullTitle = "تم الشحن وفي الطريق",
        description = "خرجت الشحنة مع مندوب التوصيل وهي في طريقها إلى عنوانك.",
        icon = Icons.Filled.LocalShipping
    ),
    OrderStepMeta(
        status = OrderStatus.DELIVERED,
        shortLabel = "التسليم",
        fullTitle = "تم التسليم بنجاح",
        description = "تم تسليم الطلب بنجاح. نأمل أن ينال المنتج إعجابك!",
        icon = Icons.Filled.CheckCircle
    )
)

@Composable
fun OrderProgressTracker(
    order: Order,
    modifier: Modifier = Modifier
) {
    val currentStatus = OrderStatus.fromKey(order.status)

    if (currentStatus == OrderStatus.CANCELLED) {
        CancelledOrderBanner(order = order, modifier = modifier)
        return
    }

    val activeIndex = when (currentStatus) {
        OrderStatus.PENDING -> 0
        OrderStatus.CONFIRMED -> 1
        OrderStatus.SHIPPED -> 2
        OrderStatus.DELIVERED -> 3
        OrderStatus.CANCELLED -> 0
    }

    var inspectedStepIndex by remember(order.id) { mutableStateOf<Int?>(null) }

    val progressFraction by animateFloatAsState(
        targetValue = activeIndex / 3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "order_progress_fraction"
    )

    val layoutDirection = LocalLayoutDirection.current
    val activeTrackColor = if (currentStatus == OrderStatus.DELIVERED) {
        Color(0xFF0F766E)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_progress_tracker_${order.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تتبع مراحل الطلب",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (inspectedStepIndex == null) "اضغط على أي مرحلة للتفاصيل" else "اضغط مجدداً للإغلاق",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Stepper Track + Interactive Nodes
            Box(modifier = Modifier.fillMaxWidth()) {
                // Connecting Track Line aligned to centers of the 4 equal-width columns
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .height(4.dp)
                ) {
                    val isRtl = layoutDirection == LayoutDirection.Rtl
                    val startX = if (isRtl) size.width * 7f / 8f else size.width * 1f / 8f
                    val fullEndX = if (isRtl) size.width * 1f / 8f else size.width * 7f / 8f
                    val span = size.width * 0.75f
                    val activeEndX = if (isRtl) {
                        startX - span * progressFraction
                    } else {
                        startX + span * progressFraction
                    }
                    val centerY = size.height / 2f

                    drawLine(
                        color = inactiveTrackColor,
                        start = Offset(startX, centerY),
                        end = Offset(fullEndX, centerY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    if (progressFraction > 0f) {
                        drawLine(
                            color = activeTrackColor,
                            start = Offset(startX, centerY),
                            end = Offset(activeEndX, centerY),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    ORDER_FLOW_STEPS.forEachIndexed { index, step ->
                        val isCompleted = index < activeIndex || (currentStatus == OrderStatus.DELIVERED && index == 3)
                        val isCurrent = index == activeIndex && currentStatus != OrderStatus.DELIVERED
                        val isInspected = inspectedStepIndex == index

                        val nodeScale by animateFloatAsState(
                            targetValue = when {
                                isInspected -> 1.12f
                                isCurrent -> 1.06f
                                else -> 1f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "step_scale_$index"
                        )

                        val circleBg by animateColorAsState(
                            targetValue = when {
                                isCompleted -> activeTrackColor
                                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            label = "step_bg_$index"
                        )

                        val circleBorder by animateColorAsState(
                            targetValue = when {
                                isCompleted -> activeTrackColor
                                isCurrent -> activeTrackColor
                                isInspected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            label = "step_border_$index"
                        )

                        val iconTint by animateColorAsState(
                            targetValue = when {
                                isCompleted -> Color.White
                                isCurrent -> activeTrackColor
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            label = "step_tint_$index"
                        )

                        val interactionSource = remember { MutableInteractionSource() }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = true),
                                    onClick = {
                                        inspectedStepIndex = if (inspectedStepIndex == index) null else index
                                    }
                                )
                                .padding(vertical = 4.dp)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("order_step_${order.id}_$index")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(40.dp)
                            ) {
                                if (isCurrent || isInspected) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(activeTrackColor.copy(alpha = 0.16f))
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .graphicsLayer {
                                            scaleX = nodeScale
                                            scaleY = nodeScale
                                        }
                                        .clip(CircleShape)
                                        .background(circleBg)
                                        .border(
                                            width = if (isCurrent || isInspected) 2.dp else 1.5.dp,
                                            color = circleBorder,
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted && index < activeIndex) Icons.Filled.Check else step.icon,
                                        contentDescription = step.fullTitle,
                                        tint = iconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = step.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent || isInspected || isCompleted) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isInspected || isCurrent -> activeTrackColor
                                    isCompleted -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Expandable Interactive Step Details Drawer
            AnimatedVisibility(
                visible = inspectedStepIndex != null,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                val idx = (inspectedStepIndex ?: activeIndex).coerceIn(0, ORDER_FLOW_STEPS.lastIndex)
                val step = ORDER_FLOW_STEPS[idx]
                val isCompleted = idx < activeIndex || (currentStatus == OrderStatus.DELIVERED && idx == 3)
                val isCurrent = idx == activeIndex && currentStatus != OrderStatus.DELIVERED
                val stageTag = when {
                    isCompleted -> "مكتمل"
                    isCurrent -> "المرحلة الحالية"
                    else -> "قادم"
                }
                val stageBadgeBg = when {
                    isCompleted -> Color(0xFFD1FAE5)
                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val stageBadgeFg = when {
                    isCompleted -> Color(0xFF065F46)
                    isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                val timestampText = when {
                    idx == 0 -> formatOrderTimestamp(order.createdAt)
                    idx == activeIndex -> formatOrderTimestamp(order.updatedAt ?: order.createdAt)
                    else -> null
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, activeTrackColor.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_step_detail_${order.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = null,
                                    tint = activeTrackColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = step.fullTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = stageBadgeBg,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = stageTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = stageBadgeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (idx >= 2 && order.shippingAddress.isNotBlank()) {
                            Text(
                                text = "وجهة التوصيل: ${order.shippingAddress}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!timestampText.isNullOrBlank()) {
                            Text(
                                text = "التحديث: $timestampText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelledOrderBanner(
    order: Order,
    modifier: Modifier = Modifier
) {
    var expanded by remember(order.id) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .defaultMinSize(minHeight = 48.dp)
            .testTag("order_cancelled_tracker_${order.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "تم إلغاء هذا الطلب",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                val updatedStr = formatOrderTimestamp(order.updatedAt ?: order.createdAt)
                Text(
                    text = if (updatedStr != null) {
                        "توقف مسار الشحن بعد إلغاء الطلب ($updatedStr)."
                    } else {
                        "توقف مسار الشحن بعد إلغاء الطلب."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun formatOrderTimestamp(timestamp: Timestamp?): String? {
    if (timestamp == null) return null
    val millis = timestamp.toDate().time
    if (millis <= 0L) return null
    return SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale("ar")).format(Date(millis))
}

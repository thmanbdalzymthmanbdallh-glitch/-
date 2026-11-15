package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun buildOrderSummaryText(order: Order): String {
    val statusLabel = OrderStatus.fromKey(order.status).labelAr
    val shortRef = order.id.take(8).ifBlank { "MATJARI" }.uppercase(Locale.ENGLISH)
    val dateText = order.createdAt?.toDate()?.time?.takeIf { it > 0L }?.let { millis ->
        SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale("ar")).format(Date(millis))
    }

    return buildString {
        appendLine("ملخص الطلب #$shortRef - متجري")
        appendLine("━━━━━━━━━━━━━━")
        appendLine("المنتج: ${order.productTitle}")
        appendLine("المتجر: ${order.sellerName}")
        appendLine("الحالة: $statusLabel")
        appendLine("الكمية: ${order.quantity} × ${formatPrice(order.unitPrice, order.currency)}")
        if (order.discountCode.isNotBlank() && order.discountAmount > 0.0) {
            appendLine("كود الخصم (${order.discountCode}): -${formatPrice(order.discountAmount, order.currency)}")
        }
        appendLine("الإجمالي: ${formatPrice(order.totalPrice, order.currency)}")
        if (order.shippingAddress.isNotBlank()) {
            appendLine("عنوان التوصيل: ${order.shippingAddress}")
        }
        if (order.note.isNotBlank()) {
            appendLine("ملاحظة: ${order.note}")
        }
        if (!dateText.isNullOrBlank()) {
            appendLine("تاريخ الطلب: $dateText")
        }
    }.trim()
}

fun shareOrderSummary(context: Context, order: Order): Boolean {
    val summaryText = buildOrderSummaryText(order)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "تفاصيل الطلب - ${order.productTitle}")
        putExtra(Intent.EXTRA_TEXT, summaryText)
    }
    val chooserIntent = Intent.createChooser(sendIntent, "مشاركة تفاصيل الطلب").apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return runCatching {
        context.startActivity(chooserIntent)
        true
    }.getOrDefault(false)
}

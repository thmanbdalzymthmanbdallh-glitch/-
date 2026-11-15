package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class ProductCategory(val key: String, val labelAr: String) {
    ELECTRONICS("electronics", "إلكترونيات"),
    FASHION("fashion", "أزياء"),
    HOME("home", "منزل ومطبخ"),
    BEAUTY("beauty", "عطور وتجميل"),
    ACCESSORIES("accessories", "إكسسوارات"),
    OTHER("other", "أخرى");

    companion object {
        fun fromKey(key: String): ProductCategory =
            entries.firstOrNull { it.key == key } ?: OTHER
    }
}

enum class CurrencyOption(val code: String, val symbolAr: String, val labelAr: String) {
    SAR("SAR", "ر.س", "ريال سعودي"),
    EGP("EGP", "ج.م", "جنيه مصري"),
    AED("AED", "د.إ", "درهم إماراتي"),
    USD("USD", "$", "دولار أمريكي");

    companion object {
        fun fromCode(code: String): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: SAR
    }
}

enum class OrderStatus(val key: String, val labelAr: String) {
    PENDING("pending", "قيد المراجعة"),
    CONFIRMED("confirmed", "تم التأكيد"),
    SHIPPED("shipped", "في الطريق"),
    DELIVERED("delivered", "تم التسليم"),
    CANCELLED("cancelled", "ملغي");

    val isTerminal: Boolean
        get() = this == DELIVERED || this == CANCELLED

    companion object {
        fun fromKey(key: String): OrderStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

enum class ImagePreset(val key: String, val labelAr: String) {
    ELECTRONICS("electronics", "تقنية"),
    FASHION("fashion", "أزياء"),
    PERFUME("perfume", "عطور"),
    HOME("home", "منزل"),
    ACCESSORIES("accessories", "إكسسوار"),
    GIFT("gift", "هدايا");

    companion object {
        fun fromKey(key: String): ImagePreset =
            entries.firstOrNull { it.key == key } ?: GIFT
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val storeName: String = "",
    val storeBio: String = "",
    val phone: String = "",
    val defaultAddress: String = "",
    val shippingAddresses: List<String> = emptyList(),
    val currency: String = CurrencyOption.SAR.code,
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = ProductCategory.OTHER.key,
    val price: Double = 0.0,
    val currency: String = CurrencyOption.SAR.code,
    val stock: Long = 0L,
    val salesCount: Long = 0L,
    @get:PropertyName("isPublished") val isPublished: Boolean = true,
    val imagePreset: String = ImagePreset.GIFT.key,
    val imageData: String? = null,
    val additionalImages: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val productTitle: String = "",
    val unitPrice: Double = 0.0,
    val currency: String = CurrencyOption.SAR.code,
    val quantity: Long = 1L,
    val imagePreset: String = ImagePreset.GIFT.key,
    val imageData: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val quantity: Long = 1L,
    val unitPrice: Double = 0.0,
    val subtotalPrice: Double = 0.0,
    val discountCode: String = "",
    val discountAmount: Double = 0.0,
    val totalPrice: Double = 0.0,
    val currency: String = CurrencyOption.SAR.code,
    val buyerName: String = "",
    val buyerPhone: String = "",
    val shippingAddress: String = "",
    val note: String = "",
    val status: String = OrderStatus.PENDING.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductReview(
    @DocumentId val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val reviewerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class DiscountType(val key: String, val labelAr: String) {
    PERCENTAGE("percentage", "نسبة مئوية (%)"),
    FIXED("fixed", "مبلغ ثابت");

    companion object {
        fun fromKey(key: String): DiscountType = when (key.trim().lowercase()) {
            "fixed", "amount", "flat" -> FIXED
            else -> PERCENTAGE
        }
    }
}

@IgnoreExtraProperties
data class Discount(
    @DocumentId val id: String = "",
    val code: String = "",
    val creatorId: String = "",
    val type: String = DiscountType.PERCENTAGE.key,
    val value: Double = 0.0,
    val minOrderAmount: Double = 0.0,
    val maxDiscountAmount: Double = 0.0,
    val usageCount: Long = 0L,
    val maxUses: Long = 0L,
    @get:PropertyName("isActive") val isActive: Boolean = true,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun computeDiscountDeduction(discount: Discount, subtotal: Double): Double {
    if (!discount.isActive || subtotal <= 0.0 || discount.value <= 0.0) return 0.0
    if (discount.minOrderAmount > 0.0 && subtotal < discount.minOrderAmount) return 0.0
    val raw = when (DiscountType.fromKey(discount.type)) {
        DiscountType.PERCENTAGE -> subtotal * (discount.value.coerceIn(0.0, 100.0) / 100.0)
        DiscountType.FIXED -> discount.value.coerceAtLeast(0.0)
    }
    val capped = if (discount.maxDiscountAmount > 0.0) {
        raw.coerceAtMost(discount.maxDiscountAmount)
    } else {
        raw
    }
    val rounded = kotlin.math.round(capped * 100.0) / 100.0
    return rounded.coerceIn(0.0, subtotal)
}

@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val productTitle: String = "",
    val category: String = ProductCategory.OTHER.key,
    val price: Double = 0.0,
    val currency: String = CurrencyOption.SAR.code,
    val imagePreset: String = ImagePreset.GIFT.key,
    val imageData: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PriceHistoryEntry(
    @DocumentId val id: String = "",
    val productId: String = "",
    val sellerId: String = "",
    val price: Double = 0.0,
    val currency: String = CurrencyOption.SAR.code,
    @ServerTimestamp val recordedAt: Timestamp? = null
)

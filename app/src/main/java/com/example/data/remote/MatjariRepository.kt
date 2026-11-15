package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.CurrencyOption
import com.example.data.model.Discount
import com.example.data.model.DiscountType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PriceHistoryEntry
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.data.model.computeDiscountDeduction
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MatjariRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً.")

    private fun isSignedInAs(uid: String): Boolean = auth.currentUser?.uid == uid

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(userId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun ensureUserProfile(userId: String, displayName: String): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        require(uid == userId) { "User ID mismatch" }
        val docRef = db.collection("users").document(uid)
        val existing = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        if (existing.exists()) {
            return@runCatching existing.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(id = uid, userId = uid, displayName = displayName, storeName = "متجر $displayName")
        }

        val cleanDisplay = displayName.trim().ifBlank { "تاجر متجري" }.take(80)
        val defaultStore = "متجر $cleanDisplay".take(80)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to cleanDisplay,
            "storeName" to defaultStore,
            "storeBio" to "متجر إلكتروني موثوق يقدم منتجات مختارة بعناية",
            "phone" to "",
            "defaultAddress" to "",
            "currency" to CurrencyOption.SAR.code,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            UserProfile(
                id = uid,
                userId = uid,
                displayName = cleanDisplay,
                storeName = defaultStore,
                storeBio = "متجر إلكتروني موثوق يقدم منتجات مختارة بعناية",
                currency = CurrencyOption.SAR.code
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun saveUserProfile(
        displayName: String,
        storeName: String,
        storeBio: String,
        phone: String,
        defaultAddress: String,
        currency: CurrencyOption,
        shippingAddresses: List<String>? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanDisplay = displayName.trim().ifBlank { "تاجر متجري" }.take(80)
        val cleanStore = storeName.trim().ifBlank { "متجر $cleanDisplay" }.take(80)
        val cleanBio = storeBio.trim().take(300)
        val cleanPhone = phone.trim().take(30)
        val cleanAddress = defaultAddress.trim().take(250)

        val snap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        val cleanAddresses = shippingAddresses
            ?.map { it.trim().take(250) }
            ?.filter { it.length >= 4 }
            ?.distinct()
            ?.take(8)
            ?.let { list ->
                if (cleanAddress.length >= 4 && cleanAddress !in list) (listOf(cleanAddress) + list).take(8) else list
            }

        if (snap.exists()) {
            val updates = buildMap<String, Any> {
                put("displayName", cleanDisplay)
                put("storeName", cleanStore)
                put("storeBio", cleanBio)
                put("phone", cleanPhone)
                put("defaultAddress", cleanAddress)
                if (cleanAddresses != null) {
                    put("shippingAddresses", cleanAddresses)
                }
                put("currency", currency.code)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        } else {
            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("displayName", cleanDisplay)
                put("storeName", cleanStore)
                put("storeBio", cleanBio)
                put("phone", cleanPhone)
                put("defaultAddress", cleanAddress)
                if (cleanAddresses != null) {
                    put("shippingAddresses", cleanAddresses)
                } else if (cleanAddress.length >= 4) {
                    put("shippingAddresses", listOf(cleanAddress))
                }
                put("currency", currency.code)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun updateShippingAddresses(
        defaultAddress: String,
        shippingAddresses: List<String>,
        fallbackDisplayName: String = "تاجر متجري"
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanDefault = defaultAddress.trim().take(250)
        val cleanList = shippingAddresses
            .map { it.trim().take(250) }
            .filter { it.length >= 4 }
            .distinct()
            .take(8)

        val snap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        if (snap.exists()) {
            try {
                docRef.update(
                    mapOf(
                        "defaultAddress" to cleanDefault,
                        "shippingAddresses" to cleanList,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        } else {
            val cleanDisplay = fallbackDisplayName.trim().ifBlank { "تاجر متجري" }.take(80)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to cleanDisplay,
                "storeName" to "متجر $cleanDisplay".take(80),
                "storeBio" to "متجر إلكتروني موثوق يقدم منتجات مختارة بعناية",
                "phone" to "",
                "defaultAddress" to cleanDefault,
                "shippingAddresses" to cleanList,
                "currency" to CurrencyOption.SAR.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun saveFcmToken(token: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanToken = token.trim().take(512)
        require(cleanToken.isNotEmpty()) { "FCM token must not be blank" }
        val docRef = db.collection("users").document(uid)
        val snap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
        if (snap.exists()) {
            val currentToken = snap.getString("fcmToken")
            if (currentToken == cleanToken) return@runCatching
            try {
                docRef.update(
                    mapOf(
                        "fcmToken" to cleanToken,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }
    }

    // --- PRODUCTS ---

    fun observePublishedProducts(currentUserId: String): Flow<List<Product>> {
        val path = "products"
        return db.collection(path)
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(currentUserId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeSellerProducts(sellerId: String): Flow<List<Product>> {
        val path = "products"
        return db.collection(path)
            .whereEqualTo("sellerId", sellerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(sellerId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createProduct(
        sellerName: String,
        title: String,
        description: String,
        category: String,
        price: Double,
        currency: String,
        stock: Long,
        isPublished: Boolean,
        imagePreset: String,
        imageData: String?,
        additionalImages: List<String> = emptyList()
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("products").document()
        val cleanExtras = additionalImages.filter { it.isNotBlank() }.take(4)
        val payload = buildMap<String, Any> {
            put("sellerId", uid)
            put("sellerName", sellerName.trim().ifBlank { "متجري" }.take(80))
            put("title", title.trim().take(100))
            put("description", description.trim().take(600))
            put("category", category)
            put("price", price)
            put("currency", currency)
            put("stock", stock)
            put("salesCount", 0L)
            put("isPublished", isPublished)
            put("imagePreset", imagePreset)
            if (!imageData.isNullOrBlank()) {
                put("imageData", imageData)
            }
            if (cleanExtras.isNotEmpty()) {
                put("additionalImages", cleanExtras)
            }
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.set(payload).await()
            runCatching {
                docRef.collection("priceHistory").document().set(
                    mapOf(
                        "productId" to docRef.id,
                        "sellerId" to uid,
                        "price" to price,
                        "currency" to currency,
                        "recordedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProduct(
        productId: String,
        sellerName: String,
        title: String,
        description: String,
        category: String,
        price: Double,
        currency: String,
        stock: Long,
        isPublished: Boolean,
        imagePreset: String,
        imageData: String?,
        additionalImages: List<String> = emptyList()
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(productId.isNotBlank()) { "Product ID must not be blank" }
        val docRef = db.collection("products").document(productId)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
        val previousPrice = existingSnap.getDouble("price")
        val previousCurrency = existingSnap.getString("currency")
        val cleanExtras = additionalImages.filter { it.isNotBlank() }.take(4)
        val updates = buildMap<String, Any> {
            put("sellerName", sellerName.trim().ifBlank { "متجري" }.take(80))
            put("title", title.trim().take(100))
            put("description", description.trim().take(600))
            put("category", category)
            put("price", price)
            put("currency", currency)
            put("stock", stock)
            put("isPublished", isPublished)
            put("imagePreset", imagePreset)
            put("imageData", if (!imageData.isNullOrBlank()) imageData else FieldValue.delete())
            put("additionalImages", if (cleanExtras.isNotEmpty()) cleanExtras else FieldValue.delete())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.update(updates).await()
            if (previousPrice == null || previousPrice != price || previousCurrency != currency) {
                runCatching {
                    docRef.collection("priceHistory").document().set(
                        mapOf(
                            "productId" to productId,
                            "sellerId" to uid,
                            "price" to price,
                            "currency" to currency,
                            "recordedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeProductPriceHistory(
        productId: String,
        currentUserId: String
    ): Flow<List<PriceHistoryEntry>> {
        val path = "products/$productId/priceHistory"
        return db.collection("products")
            .document(productId)
            .collection("priceHistory")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PriceHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.recordedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(currentUserId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        requireUserId()
        require(productId.isNotBlank()) { "Product ID must not be blank" }
        val docRef = db.collection("products").document(productId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- CART ---

    fun observeCart(userId: String): Flow<List<CartItem>> {
        val path = "users/$userId/cart"
        return db.collection("users").document(userId).collection("cart")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(userId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun reorderOrder(order: Order): Result<Long> = runCatching {
        requireUserId()
        require(order.productId.isNotBlank()) { "معرّف المنتج غير صالح لإعادة الطلب" }
        val productRef = db.collection("products").document(order.productId)
        val snap = try {
            productRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, productRef.path)
            throw e
        }
        if (!snap.exists()) {
            throw IllegalStateException("عذراً، هذا المنتج لم يعد متوفراً في المتجر")
        }
        val product = snap.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: throw IllegalStateException("تعذر قراءة بيانات المنتج")
        require(product.isPublished) { "عذراً، هذا المنتج غير معروض للبيع حالياً" }
        require(product.stock > 0L) { "عذراً، نفد مخزون \"${product.title}\" حالياً" }

        val targetQuantity = order.quantity.coerceIn(1L, product.stock)
        addToCart(product, targetQuantity).getOrThrow()
        targetQuantity
    }

    suspend fun addToCart(product: Product, quantity: Long = 1L): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(product.id.isNotBlank()) { "Product ID must not be blank" }
        require(product.stock >= quantity) { "الكمية المطلوبة غير متوفرة في المخزون" }

        val cartRef = db.collection("users").document(uid).collection("cart").document(product.id)
        val existingSnap = try {
            cartRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, cartRef.path)
            throw e
        }

        if (existingSnap.exists()) {
            val currentQty = existingSnap.getLong("quantity") ?: 0L
            val newQty = (currentQty + quantity).coerceAtMost(product.stock.coerceAtLeast(1L))
            val updates = buildMap<String, Any> {
                put("quantity", newQty)
                put("unitPrice", product.price)
                put("productTitle", product.title.take(100))
                put("sellerName", product.sellerName.take(80))
                put("imagePreset", product.imagePreset)
                if (!product.imageData.isNullOrBlank()) {
                    put("imageData", product.imageData)
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try {
                cartRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, cartRef.path)
                throw e
            }
        } else {
            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("productId", product.id)
                put("sellerId", product.sellerId)
                put("sellerName", product.sellerName.take(80))
                put("productTitle", product.title.take(100))
                put("unitPrice", product.price)
                put("currency", product.currency)
                put("quantity", quantity.coerceIn(1L, product.stock.coerceAtLeast(1L)))
                put("imagePreset", product.imagePreset)
                if (!product.imageData.isNullOrBlank()) {
                    put("imageData", product.imageData)
                }
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try {
                cartRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, cartRef.path)
                throw e
            }
        }
    }

    suspend fun updateCartItemQuantity(cartItemId: String, newQuantity: Long): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cartRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
        if (newQuantity <= 0L) {
            try {
                cartRef.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, cartRef.path)
                throw e
            }
        } else {
            try {
                cartRef.update(
                    mapOf(
                        "quantity" to newQuantity.coerceAtMost(999L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, cartRef.path)
                throw e
            }
        }
    }

    suspend fun removeFromCart(cartItemId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cartRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
        try {
            cartRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, cartRef.path)
            throw e
        }
    }

    suspend fun clearCart(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cartCol = db.collection("users").document(uid).collection("cart")
        val snapshot = try {
            cartCol.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, cartCol.path)
            throw e
        }
        if (snapshot.isEmpty) return@runCatching

        val batch = db.batch()
        snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, cartCol.path)
            throw e
        }
    }

    // --- ORDERS ---

    fun observeBuyerOrders(buyerId: String): Flow<List<Order>> {
        val path = "orders"
        return db.collection(path)
            .whereEqualTo("buyerId", buyerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(buyerId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeSellerOrders(sellerId: String): Flow<List<Order>> {
        val path = "orders"
        return db.collection(path)
            .whereEqualTo("sellerId", sellerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(sellerId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun checkoutCart(
        cartItems: List<CartItem>,
        buyerName: String,
        buyerPhone: String,
        shippingAddress: String,
        note: String,
        appliedDiscount: Discount? = null
    ): Result<List<String>> = runCatching {
        val uid = requireUserId()
        require(cartItems.isNotEmpty()) { "السلة فارغة" }

        val cleanName = buyerName.trim().take(80)
        val cleanPhone = buyerPhone.trim().take(30)
        val cleanAddress = shippingAddress.trim().take(250)
        val cleanNote = note.trim().take(250)
        require(cleanName.length >= 2) { "يرجى إدخال اسم المستلم" }
        require(cleanPhone.length >= 5) { "يرجى إدخال رقم هاتف صحيح" }
        require(cleanAddress.length >= 4) { "يرجى إدخال عنوان التوصيل" }

        data class VerifiedLine(
            val item: CartItem,
            val productRef: com.google.firebase.firestore.DocumentReference,
            val currentStock: Long,
            val currentSales: Long,
            val unitPrice: Double,
            val sellerId: String,
            val sellerName: String,
            val currency: String,
            val title: String
        )

        val verifiedLines = mutableListOf<VerifiedLine>()
        var orderSubtotal = 0.0

        for (item in cartItems) {
            val productRef = db.collection("products").document(item.productId)
            val productSnap = try {
                productRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, productRef.path)
                throw e
            }
            require(productSnap.exists()) { "المنتج ${item.productTitle} لم يعد متاحاً" }
            val currentStock = productSnap.getLong("stock") ?: 0L
            val currentSales = productSnap.getLong("salesCount") ?: 0L
            val published = productSnap.getBoolean("isPublished") ?: false
            val unitPrice = productSnap.getDouble("price") ?: item.unitPrice
            val sellerId = productSnap.getString("sellerId") ?: item.sellerId
            val sellerName = productSnap.getString("sellerName") ?: item.sellerName
            val currency = productSnap.getString("currency") ?: item.currency
            val title = productSnap.getString("title") ?: item.productTitle

            require(published) { "المنتج $title غير منشور حالياً" }
            require(currentStock >= item.quantity) { "الكمية المطلوبة من $title تتجاوز المخزون المتاح ($currentStock)" }

            orderSubtotal += unitPrice * item.quantity
            verifiedLines += VerifiedLine(
                item = item,
                productRef = productRef,
                currentStock = currentStock,
                currentSales = currentSales,
                unitPrice = unitPrice,
                sellerId = sellerId,
                sellerName = sellerName,
                currency = currency,
                title = title
            )
        }

        val verifiedDiscount = appliedDiscount?.let { candidate ->
            val docId = candidate.id.ifBlank { candidate.code.trim().uppercase() }
            val discountSnap = try {
                db.collection("discounts").document(docId).get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "discounts/$docId")
                throw e
            }
            val fresh = parseDiscountSnapshot(discountSnap)
                ?: throw IllegalArgumentException("كود الخصم لم يعد صالحاً")
            validateDiscountEligibility(fresh, orderSubtotal)
            fresh
        }

        val totalDeduction = verifiedDiscount?.let {
            computeDiscountDeduction(it, orderSubtotal)
        } ?: 0.0

        val batch = db.batch()
        val createdOrderIds = mutableListOf<String>()
        var remainingDiscount = totalDeduction

        verifiedLines.forEachIndexed { idx, line ->
            val itemSubtotal = line.unitPrice * line.item.quantity
            val itemDiscount = if (totalDeduction > 0.0 && orderSubtotal > 0.0) {
                val rawShare = if (idx == verifiedLines.lastIndex) {
                    remainingDiscount
                } else {
                    totalDeduction * (itemSubtotal / orderSubtotal)
                }
                val rounded = (kotlin.math.round(rawShare * 100.0) / 100.0)
                    .coerceIn(0.0, minOf(itemSubtotal, remainingDiscount))
                remainingDiscount = (remainingDiscount - rounded).coerceAtLeast(0.0)
                rounded
            } else {
                0.0
            }
            val itemFinalTotal = (kotlin.math.round((itemSubtotal - itemDiscount) * 100.0) / 100.0)
                .coerceAtLeast(0.0)

            val orderRef = db.collection("orders").document()
            createdOrderIds += orderRef.id

            val orderPayload = buildMap<String, Any> {
                put("buyerId", uid)
                put("sellerId", line.sellerId)
                put("sellerName", line.sellerName)
                put("productId", line.item.productId)
                put("productTitle", line.title)
                put("quantity", line.item.quantity)
                put("unitPrice", line.unitPrice)
                put("subtotalPrice", itemSubtotal)
                if (verifiedDiscount != null && itemDiscount > 0.0) {
                    put("discountCode", verifiedDiscount.code)
                    put("discountAmount", itemDiscount)
                }
                put("totalPrice", itemFinalTotal)
                put("currency", line.currency)
                put("buyerName", cleanName)
                put("buyerPhone", cleanPhone)
                put("shippingAddress", cleanAddress)
                if (cleanNote.isNotEmpty()) {
                    put("note", cleanNote)
                }
                put("status", OrderStatus.PENDING.key)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            batch.set(orderRef, orderPayload)
            batch.update(
                line.productRef,
                mapOf(
                    "stock" to (line.currentStock - line.item.quantity),
                    "salesCount" to (line.currentSales + line.item.quantity),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val cartRef = db.collection("users").document(uid).collection("cart").document(line.item.id)
            batch.delete(cartRef)
        }

        if (verifiedDiscount != null &&
            totalDeduction > 0.0 &&
            verifiedDiscount.creatorId.isNotBlank() &&
            verifiedDiscount.createdAt != null
        ) {
            val discountRef = db.collection("discounts").document(verifiedDiscount.id)
            batch.update(
                discountRef,
                mapOf(
                    "usageCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        try {
            batch.commit().await()
            createdOrderIds
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "orders")
            throw e
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> = runCatching {
        requireUserId()
        require(orderId.isNotBlank()) { "Order ID must not be blank" }
        val docRef = db.collection("orders").document(orderId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- PRODUCT REVIEWS ---

    fun observeProductReviews(productId: String, currentUserId: String): Flow<List<ProductReview>> {
        val path = "products/$productId/reviews"
        return db.collection("products").document(productId).collection("reviews")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ProductReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(currentUserId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun submitProductReview(
        productId: String,
        reviewerName: String,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(productId.isNotBlank()) { "Product ID must not be blank" }
        val cleanComment = comment.trim().take(500)
        require(cleanComment.isNotEmpty()) { "يرجى كتابة نص التقييم" }
        val cleanName = reviewerName.trim().ifBlank { "عميل متجري" }.take(80)
        val clampedRating = rating.coerceIn(1, 5).toLong()

        val docRef = db.collection("products").document(productId).collection("reviews").document(uid)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        if (existingSnap.exists()) {
            try {
                docRef.update(
                    mapOf(
                        "reviewerName" to cleanName,
                        "rating" to clampedRating,
                        "comment" to cleanComment,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        } else {
            try {
                docRef.set(
                    mapOf(
                        "productId" to productId,
                        "userId" to uid,
                        "reviewerName" to cleanName,
                        "rating" to clampedRating,
                        "comment" to cleanComment,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun deleteProductReview(productId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(productId.isNotBlank()) { "Product ID must not be blank" }
        val docRef = db.collection("products").document(productId).collection("reviews").document(uid)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- PROMOTIONAL DISCOUNTS ---

    fun observeSellerDiscounts(creatorId: String): Flow<List<Discount>> {
        val path = "discounts"
        return db.collection(path)
            .whereEqualTo("creatorId", creatorId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { parseDiscountSnapshot(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(creatorId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun validateDiscountCode(rawCode: String, subtotal: Double): Result<Discount> = runCatching {
        requireUserId()
        val trimmed = rawCode.trim()
        val normalized = trimmed.uppercase()
        require(normalized.length in 3..32 && Regex("^[A-Z0-9_\\-]+$").matches(normalized)) {
            "يرجى إدخال كود خصم صحيح (٣ أحرف أو أرقام إنجليزية على الأقل)"
        }

        val colRef = db.collection("discounts")
        var snapshot: DocumentSnapshot? = try {
            colRef.document(normalized).get().await().takeIf { it.exists() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "discounts/$normalized")
            throw e
        }

        if (snapshot == null) {
            val querySnap = try {
                colRef.whereEqualTo("isActive", true)
                    .whereEqualTo("code", normalized)
                    .limit(1)
                    .get()
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, "discounts")
                throw e
            }
            snapshot = querySnap.documents.firstOrNull()
        }

        val discount = parseDiscountSnapshot(snapshot)
            ?: throw IllegalArgumentException("كود الخصم \"$normalized\" غير موجود")

        validateDiscountEligibility(discount, subtotal)
        discount
    }

    suspend fun createDiscount(
        rawCode: String,
        type: DiscountType,
        value: Double,
        minOrderAmount: Double = 0.0,
        maxUses: Long = 0L
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val code = rawCode.trim().uppercase()
        require(code.length in 3..32 && Regex("^[A-Z0-9_\\-]+$").matches(code)) {
            "كود الخصم يجب أن يتكون من ٣ إلى ٣٢ حرفاً إنجليزياً أو رقماً بدون مسافات"
        }
        require(value > 0.0) { "قيمة الخصم يجب أن تكون أكبر من صفر" }
        if (type == DiscountType.PERCENTAGE) {
            require(value <= 100.0) { "النسبة المئوية للخصم لا يمكن أن تتجاوز 100%" }
        } else {
            require(value <= 1000000.0) { "قيمة الخصم الثابت غير صالحة" }
        }

        val docRef = db.collection("discounts").document(code)
        val existing = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
        require(!existing.exists()) { "كود الخصم \"$code\" مسجل مسبقاً" }

        val payload = mapOf(
            "code" to code,
            "creatorId" to uid,
            "type" to type.key,
            "value" to value,
            "minOrderAmount" to minOrderAmount.coerceIn(0.0, 1000000.0),
            "usageCount" to 0L,
            "maxUses" to maxUses.coerceIn(0L, 10000000L),
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleDiscountActive(discountId: String, isActive: Boolean): Result<Unit> = runCatching {
        requireUserId()
        require(discountId.isNotBlank()) { "Discount ID must not be blank" }
        val docRef = db.collection("discounts").document(discountId)
        try {
            docRef.update(
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDiscount(discountId: String): Result<Unit> = runCatching {
        requireUserId()
        require(discountId.isNotBlank()) { "Discount ID must not be blank" }
        val docRef = db.collection("discounts").document(discountId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    private fun parseDiscountSnapshot(snapshot: DocumentSnapshot?): Discount? {
        if (snapshot == null || !snapshot.exists()) return null
        val mapped = snapshot.toObject(Discount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return null
        val normalizedCode = mapped.code.ifBlank { snapshot.id }.trim().uppercase()
        return mapped.copy(
            id = snapshot.id,
            code = normalizedCode
        )
    }

    private fun validateDiscountEligibility(discount: Discount, subtotal: Double) {
        require(discount.isActive) { "كود الخصم \"${discount.code}\" غير مفعل حالياً" }
        val expiresAt = discount.expiresAt
        if (expiresAt != null && expiresAt.toDate().time <= System.currentTimeMillis()) {
            throw IllegalArgumentException("انتهت صلاحية كود الخصم \"${discount.code}\"")
        }
        if (discount.maxUses > 0L && discount.usageCount >= discount.maxUses) {
            throw IllegalArgumentException("تم استنفاد الحد الأقصى لاستخدام كود \"${discount.code}\"")
        }
        if (discount.minOrderAmount > 0.0 && subtotal < discount.minOrderAmount) {
            throw IllegalArgumentException(
                "الحد الأدنى للطلب لتطبيق هذا الكود هو ${discount.minOrderAmount.toInt()}"
            )
        }
        val deduction = computeDiscountDeduction(discount, subtotal)
        require(deduction > 0.0) { "لا يمكن تطبيق خصم على هذا المبلغ" }
    }

    // --- WISHLIST ---

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> {
        val path = "wishlist"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WishlistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (isSignedInAs(userId)) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun toggleWishlist(product: Product): Result<Boolean> = runCatching {
        val uid = requireUserId()
        require(product.id.isNotBlank()) { "Product ID must not be blank" }
        val docId = "${uid}_${product.id}"
        val docRef = db.collection("wishlist").document(docId)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        if (existingSnap.exists()) {
            try {
                docRef.delete().await()
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                throw e
            }
        } else {
            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("productId", product.id)
                put("sellerId", product.sellerId)
                put("sellerName", product.sellerName.trim().ifBlank { "متجر" }.take(80))
                put("productTitle", product.title.trim().take(100))
                put("category", product.category)
                put("price", product.price)
                put("currency", product.currency)
                put("imagePreset", product.imagePreset)
                if (!product.imageData.isNullOrBlank()) {
                    put("imageData", product.imageData)
                }
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try {
                docRef.set(payload).await()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun removeFromWishlist(wishlistItemId: String): Result<Unit> = runCatching {
        requireUserId()
        require(wishlistItemId.isNotBlank()) { "Wishlist item ID must not be blank" }
        val docRef = db.collection("wishlist").document(wishlistItemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}

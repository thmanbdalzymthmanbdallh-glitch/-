package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.remote.MatjariRepository
import com.example.data.remote.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecommendedProduct(
    val product: Product,
    val reasonLabel: String
)

data class RecommendationsUiState(
    val items: List<RecommendedProduct> = emptyList(),
    val hasUserSignals: Boolean = false
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class OrderStatusNotificationEvent(
    val orderId: String,
    val productTitle: String,
    val newStatus: OrderStatus
)

class MatjariViewModel(
    private val repository: MatjariRepository,
    val currentUserId: String,
    private val userDisplayName: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _orderNotificationEvents = MutableSharedFlow<OrderStatusNotificationEvent>(
        extraBufferCapacity = 8
    )
    val orderNotificationEvents: SharedFlow<OrderStatusNotificationEvent> =
        _orderNotificationEvents.asSharedFlow()

    private var knownBuyerOrderStatuses: Map<String, String>? = null

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل بيانات المتجر"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publishedProducts: StateFlow<UiState<List<Product>>> = repository.observePublishedProducts(currentUserId)
        .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing published products", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل منتجات السوق"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val sellerProducts: StateFlow<UiState<List<Product>>> = repository.observeSellerProducts(currentUserId)
        .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing seller products", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل منتجاتك"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val cartItems: StateFlow<UiState<List<CartItem>>> = repository.observeCart(currentUserId)
        .map<List<CartItem>, UiState<List<CartItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing cart", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل السلة"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val buyerOrders: StateFlow<UiState<List<Order>>> = repository.observeBuyerOrders(currentUserId)
        .onEach { orders ->
            val previous = knownBuyerOrderStatuses
            if (previous != null) {
                orders.forEach { order ->
                    val oldStatus = previous[order.id]
                    if (oldStatus != null && oldStatus != order.status) {
                        _orderNotificationEvents.emit(
                            OrderStatusNotificationEvent(
                                orderId = order.id,
                                productTitle = order.productTitle,
                                newStatus = OrderStatus.fromKey(order.status)
                            )
                        )
                    }
                }
            }
            knownBuyerOrderStatuses = orders.associate { it.id to it.status }
        }
        .map<List<Order>, UiState<List<Order>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing buyer orders", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل مشترياتك"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val sellerOrders: StateFlow<UiState<List<Order>>> = repository.observeSellerOrders(currentUserId)
        .map<List<Order>, UiState<List<Order>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing seller orders", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل طلبات العملاء"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val sellerDiscounts: StateFlow<UiState<List<Discount>>> = repository.observeSellerDiscounts(currentUserId)
        .map<List<Discount>, UiState<List<Discount>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing discounts", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل أكواد الخصم"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val wishlistItems: StateFlow<UiState<List<WishlistItem>>> = repository.observeWishlist(currentUserId)
        .map<List<WishlistItem>, UiState<List<WishlistItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing wishlist", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل قائمة المفضلة"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val wishlistProductIds: StateFlow<Set<String>> = wishlistItems
        .map { state ->
            (state as? UiState.Success)?.data?.map { it.productId }?.toSet().orEmpty()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val recommendationsState: StateFlow<RecommendationsUiState> = combine(
        publishedProducts,
        buyerOrders,
        wishlistItems
    ) { productsState, ordersState, wishlistState ->
        val products = (productsState as? UiState.Success)?.data.orEmpty()
        val orders = (ordersState as? UiState.Success)?.data.orEmpty()
        val wishlist = (wishlistState as? UiState.Success)?.data.orEmpty()
        computeRecommendations(
            products = products,
            orders = orders,
            wishlist = wishlist
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = RecommendationsUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, userDisplayName)
        }
    }

    fun syncFcmToken(token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            repository.saveFcmToken(token)
        }
    }

    fun saveProfile(
        displayName: String,
        storeName: String,
        storeBio: String,
        phone: String,
        defaultAddress: String,
        currency: CurrencyOption,
        shippingAddresses: List<String>? = null
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveUserProfile(
                displayName = displayName,
                storeName = storeName,
                storeBio = storeBio,
                phone = phone,
                defaultAddress = defaultAddress,
                currency = currency,
                shippingAddresses = shippingAddresses
            )
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit("تم حفظ البيانات الشخصية وإعدادات الحساب بنجاح")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ الإعدادات")
            }
        }
    }

    fun updateShippingAddresses(
        defaultAddress: String,
        shippingAddresses: List<String>,
        feedbackMessage: String = "تم تحديث عناوين التوصيل"
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateShippingAddresses(
                defaultAddress = defaultAddress,
                shippingAddresses = shippingAddresses,
                fallbackDisplayName = userDisplayName
            )
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit(feedbackMessage)
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر تحديث العناوين")
            }
        }
    }

    fun saveProduct(
        editingProductId: String?,
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
        additionalImages: List<String> = emptyList(),
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = if (editingProductId.isNullOrBlank()) {
                repository.createProduct(
                    sellerName = sellerName,
                    title = title,
                    description = description,
                    category = category,
                    price = price,
                    currency = currency,
                    stock = stock,
                    isPublished = isPublished,
                    imagePreset = imagePreset,
                    imageData = imageData,
                    additionalImages = additionalImages
                ).map { }
            } else {
                repository.updateProduct(
                    productId = editingProductId,
                    sellerName = sellerName,
                    title = title,
                    description = description,
                    category = category,
                    price = price,
                    currency = currency,
                    stock = stock,
                    isPublished = isPublished,
                    imagePreset = imagePreset,
                    imageData = imageData,
                    additionalImages = additionalImages
                )
            }
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit(
                    if (editingProductId.isNullOrBlank()) "تم إضافة المنتج إلى متجرك" else "تم تحديث بيانات المنتج"
                )
                onSuccess()
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "حدث خطأ أثناء حفظ المنتج")
            }
        }
    }

    fun deleteProduct(productId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteProduct(productId)
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit("تم حذف المنتج")
                onSuccess()
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر حذف المنتج")
            }
        }
    }

    fun addToCart(product: Product, quantity: Long = 1L) {
        viewModelScope.launch {
            val result = repository.addToCart(product, quantity)
            if (result.isSuccess) {
                _snackbarMessage.emit("تمت إضافة \"${product.title}\" إلى السلة")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر الإضافة إلى السلة")
            }
        }
    }

    fun updateCartQuantity(cartItemId: String, quantity: Long) {
        viewModelScope.launch {
            val result = repository.updateCartItemQuantity(cartItemId, quantity)
            if (result.isFailure) {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر تحديث الكمية")
            }
        }
    }

    fun removeFromCart(cartItemId: String) {
        viewModelScope.launch {
            val result = repository.removeFromCart(cartItemId)
            if (result.isSuccess) {
                _snackbarMessage.emit("تمت إزالة المنتج من السلة")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر إزالة المنتج")
            }
        }
    }

    fun checkoutCart(
        cartItems: List<CartItem>,
        buyerName: String,
        buyerPhone: String,
        shippingAddress: String,
        note: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.checkoutCart(
                cartItems = cartItems,
                buyerName = buyerName,
                buyerPhone = buyerPhone,
                shippingAddress = shippingAddress,
                note = note
            )
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit("تم إرسال طلبك بنجاح! يمكنك متابعته من الطلبات")
                onSuccess()
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر إتمام الطلب")
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateOrderStatus(orderId, newStatus)
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit("تم تحديث حالة الطلب إلى: ${newStatus.labelAr}")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر تحديث حالة الطلب")
            }
        }
    }

    fun observeProductReviews(productId: String): Flow<UiState<List<ProductReview>>> =
        repository.observeProductReviews(productId, currentUserId)
            .map<List<ProductReview>, UiState<List<ProductReview>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing product reviews", root)
                emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل التقييمات"))
            }

    fun observeProductPriceHistory(productId: String): Flow<UiState<List<PriceHistoryEntry>>> =
        repository.observeProductPriceHistory(productId, currentUserId)
            .map<List<PriceHistoryEntry>, UiState<List<PriceHistoryEntry>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing price history", root)
                emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل سجل الأسعار"))
            }

    fun submitProductReview(
        productId: String,
        reviewerName: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitProductReview(
                productId = productId,
                reviewerName = reviewerName.ifBlank { userDisplayName },
                rating = rating,
                comment = comment
            )
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit("شكراً لك! تم حفظ تقييمك للمنتج")
                onSuccess()
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر إرسال التقييم")
            }
        }
    }

    fun deleteProductReview(productId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteProductReview(productId)
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit("تم حذف تقييمك")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر حذف التقييم")
            }
        }
    }

    fun createDiscount(
        rawCode: String,
        type: DiscountType,
        value: Double,
        minOrderAmount: Double = 0.0,
        maxUses: Long = 0L,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createDiscount(
                rawCode = rawCode,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                maxUses = maxUses
            )
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit("تم إنشاء كود الخصم ${rawCode.trim().uppercase()}")
                onSuccess()
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء كود الخصم")
            }
        }
    }

    fun toggleDiscountActive(discountId: String, isActive: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleDiscountActive(discountId, isActive)
            if (result.isSuccess) {
                _snackbarMessage.emit(if (isActive) "تم تفعيل كود الخصم" else "تم إيقاف كود الخصم")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر تحديث حالة الكود")
            }
        }
    }

    fun deleteDiscount(discountId: String) {
        viewModelScope.launch {
            val result = repository.deleteDiscount(discountId)
            if (result.isSuccess) {
                _snackbarMessage.emit("تم حذف كود الخصم")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر حذف كود الخصم")
            }
        }
    }

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            val result = repository.toggleWishlist(product)
            if (result.isSuccess) {
                val added = result.getOrDefault(false)
                _snackbarMessage.emit(
                    if (added) "تمت إضافة \"${product.title}\" إلى المفضلة"
                    else "تمت إزالة \"${product.title}\" من المفضلة"
                )
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر تحديث المفضلة")
            }
        }
    }

    fun removeFromWishlist(wishlistItemId: String) {
        viewModelScope.launch {
            val result = repository.removeFromWishlist(wishlistItemId)
            if (result.isSuccess) {
                _snackbarMessage.emit("تمت إزالة المنتج من المفضلة")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر الإزالة من المفضلة")
            }
        }
    }

    private fun computeRecommendations(
        products: List<Product>,
        orders: List<Order>,
        wishlist: List<WishlistItem>
    ): RecommendationsUiState {
        val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED.key }
            .ifEmpty { orders }
        val hasSignals = activeOrders.isNotEmpty() || wishlist.isNotEmpty()
        if (products.isEmpty() || !hasSignals) {
            return RecommendationsUiState(items = emptyList(), hasUserSignals = hasSignals)
        }

        val productsById = products.associateBy { it.id }
        val purchasedCatWeights = mutableMapOf<String, Double>()
        val favoriteSellerWeights = mutableMapOf<String, Double>()
        val purchasedProductIds = mutableSetOf<String>()

        activeOrders.forEach { order ->
            purchasedProductIds += order.productId
            val cat = productsById[order.productId]?.category
            if (!cat.isNullOrBlank()) {
                purchasedCatWeights[cat] = (purchasedCatWeights[cat] ?: 0.0) + order.quantity.coerceAtLeast(1L).toDouble()
            }
            if (order.sellerId.isNotBlank()) {
                favoriteSellerWeights[order.sellerId] = (favoriteSellerWeights[order.sellerId] ?: 0.0) + 1.5
            }
        }

        val wishlistedCatWeights = mutableMapOf<String, Double>()
        val wishlistedProductIds = mutableSetOf<String>()
        wishlist.forEach { item ->
            wishlistedProductIds += item.productId
            if (item.category.isNotBlank()) {
                wishlistedCatWeights[item.category] = (wishlistedCatWeights[item.category] ?: 0.0) + 1.0
            }
            if (item.sellerId.isNotBlank()) {
                favoriteSellerWeights[item.sellerId] = (favoriteSellerWeights[item.sellerId] ?: 0.0) + 1.2
            }
        }

        val available = products.filter { it.isPublished && it.stock > 0L }
        val discoveryPool = available.filter { it.id !in wishlistedProductIds }.ifEmpty { available }

        val scored = discoveryPool.mapNotNull { product ->
            val purchaseCatScore = (purchasedCatWeights[product.category] ?: 0.0) * 5.0
            val wishlistCatScore = (wishlistedCatWeights[product.category] ?: 0.0) * 4.0
            val sellerScore = (favoriteSellerWeights[product.sellerId] ?: 0.0) * 2.5
            val repurchaseScore = if (product.id in purchasedProductIds) 2.0 else 0.0
            val affinity = purchaseCatScore + wishlistCatScore + sellerScore + repurchaseScore
            if (affinity <= 0.0) {
                null
            } else {
                val reason = when {
                    purchaseCatScore > 0.0 && purchaseCatScore >= wishlistCatScore -> "بناءً على مشترياتك"
                    wishlistCatScore > 0.0 -> "مشابه لمفضلتك"
                    sellerScore > 0.0 -> "من متجر تفضله"
                    else -> "اشترِ مجدداً"
                }
                val tieBreaker = product.salesCount.coerceAtMost(25L) * 0.05
                (affinity + tieBreaker) to RecommendedProduct(product = product, reasonLabel = reason)
            }
        }.sortedByDescending { it.first }
            .map { it.second }
            .take(8)

        val finalItems = if (scored.isNotEmpty()) {
            scored
        } else {
            available
                .filter { it.id in purchasedProductIds || it.id in wishlistedProductIds }
                .ifEmpty { available.sortedByDescending { it.salesCount } }
                .take(6)
                .map { product ->
                    val reason = when {
                        product.id in purchasedProductIds -> "اشترِ مجدداً"
                        product.id in wishlistedProductIds -> "في مفضلتك"
                        else -> "مقترح لك"
                    }
                    RecommendedProduct(product = product, reasonLabel = reason)
                }
        }

        return RecommendationsUiState(
            items = finalItems,
            hasUserSignals = true
        )
    }

    private companion object {
        const val TAG = "MatjariVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}

package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.CurrencyOption
import com.example.data.model.Discount
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.computeDiscountDeduction
import com.example.data.remote.MatjariRepository
import com.example.data.remote.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val quantitiesByProductId: Map<String, Long> = emptyMap(),
    val totalItemsCount: Int = 0,
    val totalAmount: Double = 0.0,
    val appliedDiscount: Discount? = null,
    val discountAmount: Double = 0.0,
    val finalTotalAmount: Double = 0.0,
    val isValidatingDiscount: Boolean = false,
    val discountError: String? = null,
    val primaryCurrency: String = CurrencyOption.SAR.code,
    val pendingProductIds: Set<String> = emptySet(),
    val reorderingOrderIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val errorMessage: String? = null
) {
    fun quantityInCart(productId: String): Long = quantitiesByProductId[productId] ?: 0L
    fun isInCart(productId: String): Boolean = quantityInCart(productId) > 0L
}

private data class DiscountUiHolder(
    val appliedDiscount: Discount? = null,
    val isValidating: Boolean = false,
    val errorMessage: String? = null
)

class CartViewModel(
    private val repository: MatjariRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _pendingProductIds = MutableStateFlow<Set<String>>(emptySet())
    private val _reorderingOrderIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isBusy = MutableStateFlow(false)
    private val _discountState = MutableStateFlow(DiscountUiHolder())

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val cartStreamState: StateFlow<UiState<List<CartItem>>> = repository.observeCart(currentUserId)
        .map<List<CartItem>, UiState<List<CartItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing cart", root)
            emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل السلة"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val uiState: StateFlow<CartUiState> = combine(
        cartStreamState,
        _pendingProductIds,
        _reorderingOrderIds,
        _isBusy,
        _discountState
    ) { stream, pendingIds, reorderingIds, busy, discountHolder ->
        when (stream) {
            is UiState.Loading -> CartUiState(
                isLoading = true,
                isBusy = busy,
                pendingProductIds = pendingIds,
                reorderingOrderIds = reorderingIds
            )
            is UiState.Error -> CartUiState(
                isLoading = false,
                isBusy = busy,
                pendingProductIds = pendingIds,
                reorderingOrderIds = reorderingIds,
                errorMessage = stream.message
            )
            is UiState.Success -> {
                val items = stream.data
                val subtotal = items.sumOf { it.unitPrice * it.quantity }
                val activeDiscount = discountHolder.appliedDiscount
                val deduction = if (activeDiscount != null) {
                    computeDiscountDeduction(activeDiscount, subtotal)
                } else {
                    0.0
                }
                val finalTotal = (subtotal - deduction).coerceAtLeast(0.0)
                val dynamicDiscountError = when {
                    discountHolder.errorMessage != null -> discountHolder.errorMessage
                    activeDiscount != null && activeDiscount.minOrderAmount > 0.0 && subtotal < activeDiscount.minOrderAmount ->
                        "الحد الأدنى لتفعيل كود ${activeDiscount.code} هو ${activeDiscount.minOrderAmount.toInt()}"
                    else -> null
                }
                CartUiState(
                    items = items,
                    quantitiesByProductId = items.associate { it.productId to it.quantity },
                    totalItemsCount = items.sumOf { it.quantity.toInt() },
                    totalAmount = subtotal,
                    appliedDiscount = activeDiscount,
                    discountAmount = deduction,
                    finalTotalAmount = finalTotal,
                    isValidatingDiscount = discountHolder.isValidating,
                    discountError = dynamicDiscountError,
                    primaryCurrency = items.firstOrNull()?.currency ?: CurrencyOption.SAR.code,
                    pendingProductIds = pendingIds,
                    reorderingOrderIds = reorderingIds,
                    isLoading = false,
                    isBusy = busy,
                    errorMessage = null
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CartUiState(isLoading = true)
    )

    fun addProductToCart(product: Product, quantity: Long = 1L) {
        if (product.id.isBlank()) return
        viewModelScope.launch {
            _pendingProductIds.update { it + product.id }
            val result = repository.addToCart(product, quantity)
            _pendingProductIds.update { it - product.id }
            if (result.isSuccess) {
                _events.emit("تمت إضافة \"${product.title}\" إلى السلة")
            } else {
                _events.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر الإضافة إلى السلة")
            }
        }
    }

    fun reorderOrder(order: Order, onAdded: () -> Unit = {}) {
        if (order.id.isBlank() || order.id in _reorderingOrderIds.value) return
        viewModelScope.launch {
            _reorderingOrderIds.update { it + order.id }
            val result = repository.reorderOrder(order)
            _reorderingOrderIds.update { it - order.id }
            if (result.isSuccess) {
                val addedQty = result.getOrDefault(1L)
                val message = if (addedQty < order.quantity) {
                    "تمت إضافة $addedQty من \"${order.productTitle}\" إلى السلة (حسب المتوفر)"
                } else {
                    "تمت إعادة إضافة \"${order.productTitle}\" إلى السلة"
                }
                _events.emit(message)
                onAdded()
            } else {
                _events.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر إعادة الطلب")
            }
        }
    }

    fun incrementQuantity(cartItem: CartItem) {
        updateQuantity(cartItem.id, cartItem.quantity + 1L)
    }

    fun decrementQuantity(cartItem: CartItem) {
        updateQuantity(cartItem.id, cartItem.quantity - 1L)
    }

    fun updateQuantity(cartItemId: String, quantity: Long) {
        viewModelScope.launch {
            val result = repository.updateCartItemQuantity(cartItemId, quantity)
            if (result.isFailure) {
                _events.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر تحديث الكمية")
            }
        }
    }

    fun removeItem(cartItemId: String) {
        viewModelScope.launch {
            val result = repository.removeFromCart(cartItemId)
            if (result.isSuccess) {
                _events.emit("تمت إزالة المنتج من السلة")
            } else {
                _events.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر إزالة المنتج")
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.clearCart()
            _isBusy.value = false
            if (result.isSuccess) {
                _discountState.value = DiscountUiHolder()
                _events.emit("تم إفراغ السلة")
            } else {
                _events.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر إفراغ السلة")
            }
        }
    }

    fun applyDiscountCode(rawCode: String) {
        val subtotal = uiState.value.totalAmount
        if (subtotal <= 0.0) {
            _discountState.update { it.copy(errorMessage = "السلة فارغة") }
            return
        }
        viewModelScope.launch {
            _discountState.update { it.copy(isValidating = true, errorMessage = null) }
            val result = repository.validateDiscountCode(rawCode, subtotal)
            if (result.isSuccess) {
                val discount = result.getOrThrow()
                _discountState.value = DiscountUiHolder(
                    appliedDiscount = discount,
                    isValidating = false,
                    errorMessage = null
                )
                _events.emit("تم تطبيق كود الخصم ${discount.code} بنجاح")
            } else {
                _discountState.update {
                    it.copy(
                        isValidating = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "كود الخصم غير صالح"
                    )
                }
            }
        }
    }

    fun removeDiscountCode() {
        _discountState.value = DiscountUiHolder()
    }

    fun clearDiscountError() {
        _discountState.update { it.copy(errorMessage = null) }
    }

    fun checkout(
        buyerName: String,
        buyerPhone: String,
        shippingAddress: String,
        note: String,
        onSuccess: () -> Unit
    ) {
        val currentState = uiState.value
        val currentItems = currentState.items
        if (currentItems.isEmpty()) return
        val discountToApply = if (currentState.discountAmount > 0.0) currentState.appliedDiscount else null

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.checkoutCart(
                cartItems = currentItems,
                buyerName = buyerName,
                buyerPhone = buyerPhone,
                shippingAddress = shippingAddress,
                note = note,
                appliedDiscount = discountToApply
            )
            _isBusy.value = false
            if (result.isSuccess) {
                _discountState.value = DiscountUiHolder()
                _events.emit("تم إرسال طلبك بنجاح! يمكنك متابعته من الطلبات")
                onSuccess()
            } else {
                _events.emit(result.exceptionOrNull()?.localizedMessage ?: "تعذر إتمام الطلب")
            }
        }
    }

    private companion object {
        const val TAG = "CartVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}

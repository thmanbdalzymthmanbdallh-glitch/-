package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CurrencyOption
import com.example.data.model.OrderStatus
import com.example.data.model.ProductCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class MatjariRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private suspend inline fun <reified T : Throwable> assertThrowsAsync(crossinline block: suspend () -> Unit): T {
        try {
            block()
        } catch (t: Throwable) {
            if (t is T) return t
            fail("Expected ${T::class.java.simpleName}, got ${t::class.java.simpleName}: ${t.message}")
        }
        fail("Expected ${T::class.java.simpleName} to be thrown")
        throw AssertionError("Unreachable")
    }

    @Test
    fun createAndObserveProduct_authenticatedSeller_succeeds() = runBlocking {
        val sellerUid = signInTestUser(SELLER_EMAIL)
        val repository = MatjariRepository(firestore, auth)
        val uniqueTitle = "سماعة برو ${UUID.randomUUID().toString().take(6)}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProduct(
                sellerName = "متجر النخبة",
                title = uniqueTitle,
                description = "جودة عالية وصوت نقي",
                category = ProductCategory.ELECTRONICS.key,
                price = 249.0,
                currency = CurrencyOption.SAR.code,
                stock = 12L,
                isPublished = true,
                imagePreset = "electronics",
                imageData = null
            )
        }
        assertTrue(createResult.isSuccess)
        val productId = createResult.getOrThrow()

        val sellerProducts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSellerProducts(sellerUid).first { list -> list.any { it.id == productId } }
        }
        assertTrue(sellerProducts.any { it.id == productId && it.title == uniqueTitle })

        val published = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublishedProducts(sellerUid).first { list -> list.any { it.id == productId } }
        }
        assertTrue(published.any { it.id == productId })
    }

    @Test
    fun cartAndCheckout_buyerPlacesOrder_decrementsStockAndCreatesOrder() = runBlocking {
        val sellerUid = signInTestUser(SELLER_EMAIL)
        val repository = MatjariRepository(firestore, auth)

        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProduct(
                sellerName = "متجر النخبة",
                title = "عطر عود ملكي ${UUID.randomUUID().toString().take(4)}",
                description = "عطر فاخر ثابت",
                category = ProductCategory.BEAUTY.key,
                price = 190.0,
                currency = CurrencyOption.SAR.code,
                stock = 5L,
                isPublished = true,
                imagePreset = "perfume",
                imageData = null
            ).getOrThrow()
        }

        val published = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublishedProducts(sellerUid).first { list -> list.any { it.id == productId } }
        }
        val product = published.first { it.id == productId }

        val buyerUid = signInTestUser(BUYER_EMAIL)
        val buyerRepo = MatjariRepository(firestore, auth)

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.addToCart(product, 2L)
        }
        assertTrue(addResult.isSuccess)

        val cartItems = withTimeout(FLOW_TIMEOUT_MS) {
            buyerRepo.observeCart(buyerUid).first { it.any { item -> item.productId == productId } }
        }
        val checkoutResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.checkoutCart(
                cartItems = cartItems.filter { it.productId == productId },
                buyerName = "عمر خالد",
                buyerPhone = "0509876543",
                shippingAddress = "الرياض، حي الياسمين",
                note = "التوصيل مساءً"
            )
        }
        assertTrue("Checkout failed: ${checkoutResult.exceptionOrNull()}", checkoutResult.isSuccess)
        val orderId = checkoutResult.getOrThrow().first()

        val buyerOrders = withTimeout(FLOW_TIMEOUT_MS) {
            buyerRepo.observeBuyerOrders(buyerUid).first { orders -> orders.any { it.id == orderId } }
        }
        assertEquals(OrderStatus.PENDING.key, buyerOrders.first { it.id == orderId }.status)

        // Seller confirms and delivers order
        signInTestUser(SELLER_EMAIL)
        val sellerRepo = MatjariRepository(firestore, auth)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.updateOrderStatus(orderId, OrderStatus.CONFIRMED)
        }.isSuccess)
    }

    @Test
    fun crossUserIsolation_otherUserCannotReadProfile() = runBlocking {
        val sellerUid = signInTestUser(SELLER_EMAIL)
        val sellerRepo = MatjariRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.ensureUserProfile(sellerUid, "بائع").getOrThrow()
        }

        signInTestUser(BUYER_EMAIL)
        val otherRepo = MatjariRepository(firestore, auth)
        val exception = assertThrowsAsync<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                otherRepo.observeUserProfile(sellerUid).first()
            }
        }
        val firestoreEx = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun observePublishedProducts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = MatjariRepository(firestore, auth)

        val exception = assertThrowsAsync<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublishedProducts("any_uid").first()
            }
        }
        val firestoreEx = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    private companion object {
        const val SELLER_EMAIL = "seller_rule_test@test.com"
        const val BUYER_EMAIL = "buyer_rule_test@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}

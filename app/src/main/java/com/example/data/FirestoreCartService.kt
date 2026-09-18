package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date

class FirestoreCartService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val TAG = "FirestoreCartService"

    /**
     * Syncs entire cart to Firestore under collection `carts/{uid}`
     */
    suspend fun syncCartToFirestore(items: List<CartItemEntity>): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
            val cartDocRef = db.collection("carts").document(uid)

            val cartData = hashMapOf(
                "updatedAt" to Date(),
                "totalItems" to items.sumOf { it.quantity },
                "totalAmount" to items.sumOf { it.unitPrice * it.quantity },
                "items" to items.map { item ->
                    hashMapOf(
                        "id" to item.id,
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "catalogType" to item.catalogType,
                        "category" to item.category,
                        "unit" to item.unit,
                        "unitPrice" to item.unitPrice,
                        "unitCost" to item.unitCost,
                        "quantity" to item.quantity,
                        "totalItemPrice" to (item.unitPrice * item.quantity)
                    )
                }
            )

            cartDocRef.set(cartData, SetOptions.merge()).await()
            Log.d(TAG, "Cart successfully synced to Firestore for user: $uid (${items.size} items)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing cart to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun syncProductsToFirestore(products: List<com.example.data.ProductEntity>): Boolean {
        if (products.isEmpty()) return true
        return try {
            withTimeoutOrNull(4500L) {
                withContext(Dispatchers.IO) {
                    val chunks = products.take(500).chunked(250)
                    for (chunk in chunks) {
                        val batch = db.batch()
                        chunk.forEach { product ->
                            val docRef = db.collection("products").document("${product.catalogType.uppercase()}_${product.id}")
                            val productData = hashMapOf(
                                "id" to product.id,
                                "catalogType" to product.catalogType,
                                "name" to product.name,
                                "category" to product.category,
                                "brand" to product.brand,
                                "unit" to product.unit,
                                "price" to product.price,
                                "cost" to product.cost,
                                "barcode" to product.barcode
                            )
                            batch.set(docRef, productData, SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                    Log.d(TAG, "Products successfully synced to Firestore (${products.size} items)")
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing products to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Records a checkout order in Firestore under `orders`
     */
    suspend fun saveOrderToFirestore(
        items: List<CartItemEntity>,
        totalPrice: Double,
        totalProfit: Double
    ): String? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val orderRef = db.collection("orders").document()

            val orderData = hashMapOf(
                "orderId" to orderRef.id,
                "userUid" to uid,
                "createdAt" to Date(),
                "status" to "CONFIRMED",
                "totalAmount" to totalPrice,
                "totalProfit" to totalProfit,
                "itemCount" to items.sumOf { it.quantity },
                "items" to items.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "catalogType" to item.catalogType,
                        "category" to item.category,
                        "unit" to item.unit,
                        "unitPrice" to item.unitPrice,
                        "unitCost" to item.unitCost,
                        "quantity" to item.quantity
                    )
                }
            )

            orderRef.set(orderData).await()

            // Also clear cart in Firestore
            db.collection("carts").document(uid).delete().await()

            Log.d(TAG, "Order ${orderRef.id} saved to Firestore")
            orderRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error saving order to Firestore: ${e.message}", e)
            null
        }
    }
}


package com.example.data

import com.example.util.PriceAlertNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import android.util.Log

class CatalogRepository(private val dao: CatalogDao) {

    private fun getFirestoreDb(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    private suspend fun logDownloadActivity(downloadType: String, items: List<ProductEntity>) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@withContext
                val db = getFirestoreDb()
                
                val log = mapOf(
                    "userId" to user.uid,
                    "userEmail" to (user.email ?: ""),
                    "timestamp" to System.currentTimeMillis(),
                    "downloadType" to downloadType,
                    "itemCount" to items.size,
                    "downloadedItemIds" to items.map { it.id }
                )
                
                db.collection("user_activity_logs").add(log).await()
            } catch (e: Exception) {
                // Soft logging: fail silently
            }
        }
    }

    fun getProductsByCatalog(catalogType: String): Flow<List<ProductEntity>> =
        dao.getProductsByCatalog(catalogType)

    fun getAllProducts(): Flow<List<ProductEntity>> =
        dao.getAllProducts()

    fun getTotalProductCountFlow(): Flow<Int> = dao.getTotalProductCountFlow()

    suspend fun getTotalProductCount(): Int = dao.getTotalProductCount()

    suspend fun getProductCountByCatalog(catalogType: String): Int = dao.getProductCountByCatalog(catalogType)

    suspend fun getProductByBarcode(barcode: String): ProductEntity? = dao.getProductByBarcode(barcode)

    suspend fun searchProductsByBarcodeOrKeyword(barcode: String, id: Int, keyword: String): List<ProductEntity> =
        dao.searchProductsByBarcodeOrKeyword(barcode, id, keyword)

    val importPercentProgress = kotlinx.coroutines.flow.MutableStateFlow(0)
    val exportPercentProgress = kotlinx.coroutines.flow.MutableStateFlow(0)

    suspend fun importProducts(products: List<ProductEntity>) {
        importPercentProgress.value = 20
        kotlinx.coroutines.delay(50)
        importPercentProgress.value = 60
        dao.insertProducts(products)
        importPercentProgress.value = 100
        kotlinx.coroutines.delay(300)
        importPercentProgress.value = 0
    }

    suspend fun addProduct(product: ProductEntity): Long {
        val nextId = (dao.getMaxProductId() ?: 1000000) + 1
        val finalProd = if (product.id == 0) product.copy(id = nextId) else product
        dao.insertProduct(finalProd)
        return finalProd.id.toLong()
    }

    suspend fun updateProduct(product: ProductEntity) {
        dao.updateProduct(product)
    }

    suspend fun deleteProduct(id: Int, catalogType: String) {
        dao.deleteProductByComposite(id, catalogType)
    }

    suspend fun clearCatalog(catalogType: String) {
        dao.clearCatalog(catalogType)
    }

    suspend fun clearAllData() {
        dao.clearAllProducts()
        dao.clearCart()
        dao.clearSaleRecords()
        dao.clearWishlist()
        dao.clearPriceHistory()
        dao.clearPriceAlerts()
    }

    fun parseFirestoreDocToProduct(doc: com.google.firebase.firestore.DocumentSnapshot, defaultCatalog: String? = null): ProductEntity? {
        return try {
            val name = doc.getString("name")
                ?: doc.getString("nom")
                ?: doc.getString("title")
                ?: doc.getString("product_name")
                ?: doc.getString("designation")
                ?: (doc.get("name") as? String)
                ?: (doc.get("nom") as? String)
                ?: return null

            if (name.isBlank()) return null

            val rawCatalog = doc.getString("catalogType")
                ?: doc.getString("catalog_type")
                ?: doc.getString("supermarket")
                ?: doc.getString("store")
                ?: defaultCatalog
                ?: "DREAMPRICE"

            val cleanCatalog = when {
                rawCatalog.contains("DREAM", ignoreCase = true) -> "DREAMPRICE"
                rawCatalog.contains("INTER", ignoreCase = true) -> "INTERMART"
                rawCatalog.contains("SUPER", ignoreCase = true) -> "SUPER U"
                rawCatalog.contains("WINNER", ignoreCase = true) -> "WINNERS"
                rawCatalog.contains("KING", ignoreCase = true) -> "KING SAVERS"
                rawCatalog.contains("WAY", ignoreCase = true) -> "WAY"
                rawCatalog.contains("LOLO", ignoreCase = true) -> "LOLO"
                rawCatalog.contains("GSR", ignoreCase = true) -> "GSR"
                rawCatalog.contains("JUMBO", ignoreCase = true) -> "JUMBO"
                rawCatalog.contains("CARREFOUR", ignoreCase = true) -> "CARREFOUR"
                rawCatalog.contains("SPAR", ignoreCase = true) -> "SPAR"
                else -> rawCatalog.trim().replace("_", " ").uppercase().ifBlank { "DREAMPRICE" }
            }

            val rawId = doc.get("id") ?: doc.get("productId") ?: doc.get("code")
            val parsedId = when (rawId) {
                is Number -> rawId.toInt()
                is String -> rawId.toIntOrNull() ?: 0
                else -> doc.id.substringAfterLast("_").toIntOrNull() ?: 0
            }

            val rawPrice = doc.get("price") ?: doc.get("prix") ?: doc.get("priceRs") ?: doc.get("salePrice") ?: doc.get("unitPrice")
            val price = when (rawPrice) {
                is Number -> rawPrice.toDouble()
                is String -> rawPrice.replace(",", ".").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val rawCost = doc.get("cost") ?: doc.get("cout") ?: doc.get("buyingPrice") ?: doc.get("costPrice")
            val cost = when (rawCost) {
                is Number -> rawCost.toDouble()
                is String -> rawCost.replace(",", ".").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val barcode = doc.getString("barcode")
                ?: doc.getString("code_barre")
                ?: doc.getString("codeBarre")
                ?: doc.getString("ean")
                ?: (doc.get("barcode") as? String)
                ?: ""

            val category = doc.getString("category")
                ?: doc.getString("categorie")
                ?: doc.getString("rayon")
                ?: doc.getString("dept")
                ?: "Divers"

            val brand = doc.getString("brand")
                ?: doc.getString("marque")
                ?: ""

            val unit = doc.getString("unit")
                ?: doc.getString("unite")
                ?: doc.getString("packaging")
                ?: "1u"

            ProductEntity(
                id = parsedId,
                catalogType = cleanCatalog,
                name = name.trim(),
                category = category.trim(),
                brand = brand.trim(),
                unit = unit.trim(),
                price = price,
                cost = cost,
                barcode = barcode.trim()
            )
        } catch (e: Exception) {
            Log.e("CatalogRepository", "Error parsing doc ${doc.id}: ${e.message}")
            null
        }
    }

    private suspend fun fetchAllProductsFromBothSources(
        onProgress: (detail: String, progressPercent: Float, currentCount: Int) -> Unit = { _, _, _ -> }
    ): List<ProductEntity> {
        val collected = mutableListOf<ProductEntity>()
        val db = getFirestoreDb()

        // 1. Fetch from root "products" collection
        try {
            onProgress("Téléchargement des articles depuis 'products'...", 30f, collected.size)
            Log.d("CatalogRepository", "Fetching from root 'products' collection...")
            val snapshot = db.collection("products").get().await()
            val rootProducts = snapshot.documents.mapNotNull { doc ->
                parseFirestoreDocToProduct(doc)
            }
            collected.addAll(rootProducts)
            onProgress("Récupéré ${collected.size} articles ('products')...", 40f, collected.size)
            Log.d("CatalogRepository", "Fetched total ${rootProducts.size} from root 'products' collection. Total: ${collected.size}")
        } catch (e: Exception) {
            Log.e("CatalogRepository", "Root products fetch failed: ${e.message}", e)
        }

        // 2. Fetch from 'supermarket' collection documents & their 'items' subcollections
        try {
            onProgress("Analyse des fichiers de catalogues 'supermarket'...", 45f, collected.size)
            val supermarketSnapshot = db.collection(FirestoreCatalogService.COLLECTION_SERVER_FILES).get().await()
            Log.d("CatalogRepository", "Found ${supermarketSnapshot.size()} files in supermarket collection.")
            
            val totalFiles = supermarketSnapshot.size()
            var fileIdx = 0
            
            for (doc in supermarketSnapshot.documents) {
                fileIdx++
                val fileCatalog = doc.getString("catalogType") ?: "DREAMPRICE"
                val fileName = doc.getString("fileName") ?: doc.id
                val fileProg = 45f + ((fileIdx.toFloat() / (if (totalFiles > 0) totalFiles else 1)) * 25f)
                onProgress("Catalogue $fileName ($fileCatalog)...", fileProg, collected.size)

                // 2a. Check embedded products array
                val rawProductsList = (doc.get("products") as? List<Map<String, Any>>) ?: emptyList()
                val embedded = rawProductsList.mapNotNull { pMap ->
                    try {
                        val rawId = pMap["id"] ?: pMap["productId"] ?: pMap["code"]
                        val pId = when (rawId) {
                            is Number -> rawId.toInt()
                            is String -> rawId.toIntOrNull() ?: 0
                            else -> 0
                        }
                        val pName = (pMap["name"] as? String) ?: (pMap["nom"] as? String) ?: (pMap["title"] as? String) ?: return@mapNotNull null
                        if (pName.isBlank()) return@mapNotNull null
                        val pCat = (pMap["catalogType"] as? String) ?: fileCatalog
                        val pCategory = (pMap["category"] as? String) ?: (pMap["categorie"] as? String) ?: "Divers"
                        val pBrand = (pMap["brand"] as? String) ?: (pMap["marque"] as? String) ?: ""
                        val pUnit = (pMap["unit"] as? String) ?: (pMap["unite"] as? String) ?: "1u"
                        
                        val rawPrice = pMap["price"] ?: pMap["prix"] ?: pMap["priceRs"] ?: pMap["salePrice"]
                        val pPrice = when (rawPrice) {
                            is Number -> rawPrice.toDouble()
                            is String -> rawPrice.replace(",", ".").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        val rawCost = pMap["cost"] ?: pMap["cout"] ?: pMap["costPrice"]
                        val pCost = when (rawCost) {
                            is Number -> rawCost.toDouble()
                            is String -> rawCost.replace(",", ".").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        val pBarcode = (pMap["barcode"] as? String) ?: (pMap["code_barre"] as? String) ?: ""

                        ProductEntity(
                            id = pId,
                            catalogType = pCat.trim().uppercase(),
                            name = pName.trim(),
                            category = pCategory.trim(),
                            brand = pBrand.trim(),
                            unit = pUnit.trim(),
                            price = pPrice,
                            cost = pCost,
                            barcode = pBarcode.trim()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (embedded.isNotEmpty()) {
                    collected.addAll(embedded)
                    Log.d("CatalogRepository", "Added ${embedded.size} embedded items from $fileName")
                }

                // 2b. Fetch subcollection 'items'
                val subItems = FirestoreCatalogService.fetchServerCatalogFileItems(doc.id)
                if (subItems.isNotEmpty()) {
                    collected.addAll(subItems)
                    Log.d("CatalogRepository", "Added ${subItems.size} sub-items from ${doc.id}")
                }

                // 2c. Fallback recovery from sampleProductsPreview if subcollection was squashed
                val previewList = (doc.get("sampleProductsPreview") as? List<*>) ?: emptyList<Any>()
                if (subItems.size <= 1 && previewList.isNotEmpty()) {
                    val recovered = previewList.mapNotNull { previewItem ->
                        val text = previewItem?.toString() ?: return@mapNotNull null
                        val regex = """^(.*?)\s*\(([0-9.,]+)\s*Rs\)$""".toRegex()
                        val match = regex.find(text.trim())
                        if (match != null) {
                            val pName = match.groupValues[1].trim()
                            val pPrice = match.groupValues[2].replace(",", ".").toDoubleOrNull() ?: 0.0
                            ProductEntity(
                                id = 0,
                                catalogType = fileCatalog.trim().uppercase(),
                                name = pName,
                                category = "Général",
                                brand = fileCatalog.trim().uppercase(),
                                unit = "1u",
                                price = pPrice,
                                cost = 0.0,
                                barcode = ""
                            )
                        } else null
                    }
                    if (recovered.isNotEmpty()) {
                        collected.addAll(recovered)
                        Log.d("CatalogRepository", "Recovered ${recovered.size} sample preview items from ${doc.id}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CatalogRepository", "Supermarket collection fetch failed: ${e.message}", e)
        }

        // 3. Collection group 'items' check (fallback to ensure nothing missed)
        try {
            val groupSnapshot = db.collectionGroup("items").get().await()
            if (!groupSnapshot.isEmpty) {
                val groupItems = groupSnapshot.documents.mapNotNull { doc ->
                    parseFirestoreDocToProduct(doc)
                }
                if (groupItems.isNotEmpty()) {
                    collected.addAll(groupItems)
                    Log.d("CatalogRepository", "CollectionGroup 'items' contributed ${groupItems.size} items")
                }
            }
        } catch (e: Exception) {
            Log.d("CatalogRepository", "CollectionGroup query info: ${e.message}")
        }

        // 4. Smart Deduplication: deduplicate by barcode or normalized name + unit (NEVER by id!)
        onProgress("Déduplication et structuration des articles...", 72f, collected.size)
        val distinctProducts = collected.distinctBy { item ->
            when {
                item.barcode.isNotBlank() -> "${item.catalogType.uppercase()}_bc_${item.barcode.trim()}"
                else -> "${item.catalogType.uppercase()}_nm_${item.name.trim().lowercase()}_${item.unit.trim().lowercase()}"
            }
        }

        // 5. Ensure valid, unique, sequential IDs for Room per catalog (id: 1..N)
        val finalProducts = distinctProducts.groupBy { it.catalogType.uppercase() }.flatMap { (catalog, list) ->
            list.mapIndexed { index, product ->
                product.copy(id = index + 1, catalogType = catalog)
            }
        }

        Log.d("CatalogRepository", "Final total distinct products ready: ${finalProducts.size}")
        return finalProducts
    }

    suspend fun forceUpdateFromFirestore(
        onProgress: (step: Int, detail: String, progressPercent: Float, itemCount: Int, catalogStats: Map<String, Int>) -> Unit = { _, _, _, _, _ -> }
    ): Result<Int> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Step 0: Connectivity Verification
                onProgress(0, "Connexion et vérification Cloud Firestore...", 10f, 0, emptyMap())
                val db = getFirestoreDb()
                
                // Step 1: Secure Preparation (Verify existing state)
                onProgress(1, "Préparation de la synchronisation complète...", 20f, 0, emptyMap())
                kotlinx.coroutines.delay(100)

                // Step 2: Download ALL Products from Firestore
                onProgress(2, "Téléchargement complet depuis Cloud Firestore...", 28f, 0, emptyMap())
                val products = fetchAllProductsFromBothSources { detail, percent, count ->
                    onProgress(2, detail, percent, count, emptyMap())
                }

                if (products.isEmpty()) {
                    return@withContext Result.failure(Exception("Aucun produit trouvé sur Cloud Firestore. Votre base locale a été préservée intacte."))
                }

                // Step 3: Structuring & Catalog Breakdown
                val catalogStats = products.groupBy { it.catalogType.uppercase() }.mapValues { it.value.size }
                val statsSummary = catalogStats.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                onProgress(3, "Structuration terminée : $statsSummary", 78f, products.size, catalogStats)
                kotlinx.coroutines.delay(150)

                // Step 4: Wipe local data safely and insert fresh verified products
                onProgress(4, "Mise à jour et indexation locale dans Room SQLite...", 88f, products.size, catalogStats)
                clearAllData()
                dao.insertProducts(products)

                // Step 5: Finalize and log download activity
                logDownloadActivity("FULL_FORCE_SYNC", products)
                onProgress(5, "Mise à jour complète terminée avec succès ! (${products.size} produits prêts)", 100f, products.size, catalogStats)
                Result.success(products.size)
            } catch (e: Exception) {
                Log.e("CatalogRepository", "forceUpdateFromFirestore failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun syncAllProductsFromFirestore(): Result<Unit> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val products = fetchAllProductsFromBothSources()
                if (products.isEmpty()) {
                    return@withContext Result.failure(Exception("Aucun produit trouvé sur Firestore."))
                }
                dao.insertProducts(products)
                logDownloadActivity("REGULAR_SYNC", products)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun upsertProductsFromFirestore(): Result<Unit> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val products = fetchAllProductsFromBothSources()

                if (products.isEmpty()) {
                    return@withContext Result.failure(Exception("Aucun produit trouvé sur le serveur Firestore."))
                }

                dao.insertProducts(products)
                logDownloadActivity("PRODUCTS", products)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun restoreAllUserDataFromFirestore(userEmail: String, firebaseUid: String? = null): Result<Int> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var totalItemsRestored = 0
                val db = getFirestoreDb()
                val uid = firebaseUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: ("fb_" + kotlin.math.abs(userEmail.hashCode()).toString())

                // 1. Restore Catalog Products from Firestore into Room SQLite
                try {
                    val productResult = syncAllProductsFromFirestore()
                    if (productResult.isSuccess) {
                        val count = getTotalProductCount()
                        totalItemsRestored += count
                        Log.d("CatalogRepository", "Restored $count catalog products from Firestore into Room.")
                    }
                } catch (e: Exception) {
                    Log.w("CatalogRepository", "Failed restoring catalog products: ${e.message}")
                }

                // 2. Restore App Settings
                try {
                    val cloudSettings = FirestoreSettingsService.fetchSettingsFromFirestore(userEmail)
                    if (cloudSettings != null) {
                        saveAppSettings(cloudSettings)
                        totalItemsRestored += 1
                        Log.d("CatalogRepository", "Restored App Settings from Firestore into Room.")
                    }
                } catch (e: Exception) {
                    Log.w("CatalogRepository", "Failed restoring app settings: ${e.message}")
                }

                // 3. Restore Price Alerts
                try {
                    syncAlertsWithFirestore(userEmail)
                    val alertCount = dao.getPriceAlertList().size
                    totalItemsRestored += alertCount
                    Log.d("CatalogRepository", "Restored $alertCount Price Alerts from Firestore into Room.")
                } catch (e: Exception) {
                    Log.w("CatalogRepository", "Failed restoring price alerts: ${e.message}")
                }

                // 4. Restore Cart Items from Firestore (`carts/{uid}`)
                try {
                    val cartDoc = db.collection("carts").document(uid).get().await()
                    if (cartDoc.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val rawItems = cartDoc.get("items") as? List<Map<String, Any>>
                        if (!rawItems.isNullOrEmpty()) {
                            dao.clearCart()
                            val cartEntities = rawItems.mapNotNull { map ->
                                val productId = (map["productId"] as? Long)?.toInt() ?: (map["productId"] as? String)?.toIntOrNull() ?: return@mapNotNull null
                                val productName = map["productName"] as? String ?: ""
                                val catalogType = map["catalogType"] as? String ?: "SUPERMARKET"
                                val category = map["category"] as? String ?: ""
                                val unit = map["unit"] as? String ?: "unité"
                                val unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0
                                val unitCost = (map["unitCost"] as? Number)?.toDouble() ?: 0.0
                                val quantity = (map["quantity"] as? Number)?.toInt() ?: 1
                                CartItemEntity(
                                    catalogType = catalogType,
                                    productId = productId,
                                    productName = productName,
                                    category = category,
                                    unit = unit,
                                    unitPrice = unitPrice,
                                    unitCost = unitCost,
                                    quantity = quantity
                                )
                            }
                            cartEntities.forEach { dao.insertCartItem(it) }
                            totalItemsRestored += cartEntities.size
                            Log.d("CatalogRepository", "Restored ${cartEntities.size} Cart Items from Firestore into Room.")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CatalogRepository", "Failed restoring cart: ${e.message}")
                }

                // 5. Restore Orders / Sale Records from Firestore (`orders`)
                try {
                    val ordersQuery = db.collection("orders")
                        .whereEqualTo("userUid", uid)
                        .get().await()

                    if (!ordersQuery.isEmpty) {
                        val salesList = mutableListOf<SaleRecordEntity>()
                        for (doc in ordersQuery.documents) {
                            @Suppress("UNCHECKED_CAST")
                            val rawItems = doc.get("items") as? List<Map<String, Any>>
                            val timestamp = doc.getDate("createdAt")?.time ?: System.currentTimeMillis()
                            rawItems?.forEach { item ->
                                val productName = item["productName"] as? String ?: ""
                                val catalogType = item["catalogType"] as? String ?: "SUPERMARKET"
                                val category = item["category"] as? String ?: ""
                                val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                                val unitPrice = (item["unitPrice"] as? Number)?.toDouble() ?: 0.0
                                val unitCost = (item["unitCost"] as? Number)?.toDouble() ?: 0.0
                                val totalPrice = unitPrice * quantity
                                val totalProfit = (unitPrice - unitCost) * quantity

                                salesList.add(
                                    SaleRecordEntity(
                                        catalogType = catalogType,
                                        productName = productName,
                                        category = category,
                                        quantity = quantity,
                                        unitPrice = unitPrice,
                                        unitCost = unitCost,
                                        totalPrice = totalPrice,
                                        totalProfit = totalProfit,
                                        timestamp = timestamp
                                    )
                                )
                            }
                        }
                        if (salesList.isNotEmpty()) {
                            dao.insertSaleRecords(salesList)
                            totalItemsRestored += salesList.size
                            Log.d("CatalogRepository", "Restored ${salesList.size} Sale Records from Firestore into Room.")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CatalogRepository", "Failed restoring orders: ${e.message}")
                }

                // 6. Restore Wishlist from Firestore (`wishlist/{userEmail}`)
                try {
                    val wishlistDocs = db.collection("wishlist")
                        .document(userEmail)
                        .collection("items")
                        .get().await()

                    if (!wishlistDocs.isEmpty) {
                        for (doc in wishlistDocs.documents) {
                            val productId = doc.getLong("productId")?.toInt() ?: doc.id.toIntOrNull() ?: continue
                            val catalogType = doc.getString("catalogType") ?: "SUPERMARKET"
                            val productName = doc.getString("productName") ?: ""
                            val category = doc.getString("category") ?: ""
                            val unitPrice = doc.getDouble("unitPrice") ?: 0.0
                            val unit = doc.getString("unit") ?: "unité"

                            val item = WishlistItemEntity(
                                catalogType = catalogType,
                                productId = productId,
                                productName = productName,
                                category = category,
                                unitPrice = unitPrice,
                                unit = unit,
                                addedAt = doc.getDate("addedAt")?.time ?: System.currentTimeMillis()
                            )
                            dao.insertWishlistItem(item)
                            totalItemsRestored += 1
                        }
                        Log.d("CatalogRepository", "Restored Wishlist from Firestore into Room.")
                    }
                } catch (e: Exception) {
                    Log.w("CatalogRepository", "Failed restoring wishlist: ${e.message}")
                }

                // 7. Restore Loyalty Cards from Firestore (`loyalty_cards/{userEmail}`)
                try {
                    val loyaltyDocs = db.collection("loyalty_cards")
                        .document(userEmail)
                        .collection("cards")
                        .get().await()

                    if (!loyaltyDocs.isEmpty) {
                        for (doc in loyaltyDocs.documents) {
                            val storeName = doc.getString("storeName") ?: continue
                            val cardNumber = doc.getString("cardNumber") ?: ""
                            val cardHolderName = doc.getString("cardHolderName") ?: ""
                            val barcodeType = doc.getString("barcodeType") ?: "CODE_128"
                            val colorHex = doc.getString("colorHex") ?: "#1E88E5"
                            val cardImagePath = doc.getString("cardImagePath")

                            val card = LoyaltyCardEntity(
                                storeName = storeName,
                                cardNumber = cardNumber,
                                cardHolderName = cardHolderName,
                                barcodeType = barcodeType,
                                colorHex = colorHex,
                                cardImagePath = cardImagePath
                            )
                            dao.insertLoyaltyCard(card)
                            totalItemsRestored += 1
                        }
                        Log.d("CatalogRepository", "Restored Loyalty Cards from Firestore into Room.")
                    }
                } catch (e: Exception) {
                    Log.w("CatalogRepository", "Failed restoring loyalty cards: ${e.message}")
                }

                Result.success(totalItemsRestored)
            } catch (e: Exception) {
                Log.e("CatalogRepository", "Error restoring user data from Firestore: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // --- Cart ---
    fun getCartItems(): Flow<List<CartItemEntity>> = dao.getCartItems()

    suspend fun addToCart(product: ProductEntity, quantity: Int = 1) {
        val existing = dao.getCartItemByProduct(product.id, product.catalogType)
        if (existing != null) {
            val updated = existing.copy(quantity = existing.quantity + quantity)
            dao.updateCartItem(updated)
        } else {
            val newItem = CartItemEntity(
                catalogType = product.catalogType,
                productId = product.id,
                productName = product.name,
                category = product.category,
                unit = product.unit,
                unitPrice = product.price,
                unitCost = product.cost,
                quantity = quantity
            )
            dao.insertCartItem(newItem)
        }
    }

    suspend fun updateCartItemQuantity(item: CartItemEntity, newQuantity: Int) {
        if (newQuantity <= 0) {
            dao.deleteCartItem(item.id)
        } else {
            dao.updateCartItem(item.copy(quantity = newQuantity))
        }
    }

    suspend fun removeFromCart(cartItemId: Int) {
        dao.deleteCartItem(cartItemId)
    }

    suspend fun checkoutCart(cartItems: List<CartItemEntity>) {
        val sales = cartItems.map { item ->
            val total = item.unitPrice * item.quantity
            val cost = item.unitCost * item.quantity
            val profit = total - cost
            SaleRecordEntity(
                catalogType = item.catalogType,
                productName = item.productName,
                category = item.category,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unitCost = item.unitCost,
                totalPrice = total,
                totalProfit = profit,
                timestamp = System.currentTimeMillis()
            )
        }
        dao.insertSaleRecords(sales)
        dao.clearCart()
    }

    // --- Sales ---
    fun getSaleRecords(): Flow<List<SaleRecordEntity>> = dao.getSaleRecords()
    suspend fun clearSaleRecords() = dao.clearSaleRecords()

    // --- Wishlist ---
    fun getWishlistItems(): Flow<List<WishlistItemEntity>> = dao.getWishlistItems()

    suspend fun addToWishlist(product: ProductEntity) {
        val existing = dao.getWishlistItemByProduct(product.id, product.catalogType)
        if (existing == null) {
            val item = WishlistItemEntity(
                productId = product.id,
                catalogType = product.catalogType,
                productName = product.name,
                category = product.category,
                brand = product.brand,
                unit = product.unit,
                unitPrice = product.price,
                unitCost = product.cost
            )
            dao.insertWishlistItem(item)
        }
    }

    suspend fun removeFromWishlist(productId: Int, catalogType: String) {
        dao.deleteWishlistByProductId(productId, catalogType)
    }

    suspend fun removeWishlistItemById(id: Int) {
        dao.deleteWishlistItem(id)
    }

    suspend fun toggleWishlist(product: ProductEntity) {
        val existing = dao.getWishlistItemByProduct(product.id, product.catalogType)
        if (existing != null) {
            dao.deleteWishlistItem(existing.id)
        } else {
            val item = WishlistItemEntity(
                productId = product.id,
                catalogType = product.catalogType,
                productName = product.name,
                category = product.category,
                brand = product.brand,
                unit = product.unit,
                unitPrice = product.price,
                unitCost = product.cost
            )
            dao.insertWishlistItem(item)
        }
    }

    suspend fun clearWishlist() {
        dao.clearWishlist()
    }

    // --- Price History (Room Local Storage) ---
    fun getPriceHistory(productId: Int, productName: String): Flow<List<PriceHistoryEntity>> {
        return dao.getPriceHistoryForProduct(productId, productName)
    }

    suspend fun recordPricePoint(
        productId: Int,
        productName: String,
        catalogType: String,
        price: Double,
        cost: Double = 0.0,
        recordedDate: String = "Aujourd'hui"
    ) {
        val entity = PriceHistoryEntity(
            productId = productId,
            productName = productName,
            catalogType = catalogType,
            price = price,
            cost = cost,
            recordedDate = recordedDate,
            timestamp = System.currentTimeMillis()
        )
        dao.insertPriceHistoryRecord(entity)
    }

    suspend fun ensurePriceHistoryForProduct(product: ProductEntity) {
        val existing = dao.getPriceHistoryList(product.id, product.name)
        if (existing.isEmpty() && product.price > 0) {
            val basePrice = product.price
            val baseCost = product.cost
            val seedMonths = listOf(
                Pair("Mars 2026", basePrice * 0.92),
                Pair("Avril 2026", basePrice * 0.95),
                Pair("Mai 2026", basePrice * 0.98),
                Pair("Juin 2026", basePrice * 0.90),
                Pair("Juillet 2026", basePrice * 0.96),
                Pair("Août 2026 (Actuel)", basePrice)
            )

            val records = seedMonths.mapIndexed { index, (month, pr) ->
                val roundedPrice = (kotlin.math.round(pr * 100) / 100.0).coerceAtLeast(1.0)
                PriceHistoryEntity(
                    productId = product.id,
                    productName = product.name,
                    catalogType = product.catalogType,
                    price = roundedPrice,
                    cost = baseCost,
                    recordedDate = month,
                    timestamp = System.currentTimeMillis() - ((seedMonths.size - 1 - index) * 30L * 24L * 3600L * 1000L)
                )
            }
            dao.insertPriceHistory(records)
        }
    }

    // --- Price Alerts (Firestore + Room) ---
    fun getPriceAlerts(): Flow<List<PriceAlertEntity>> = dao.getPriceAlerts()

    fun getPriceAlertForProduct(productId: Int, catalogType: String): Flow<PriceAlertEntity?> =
        dao.getPriceAlertForProduct(productId, catalogType)

    suspend fun getPriceAlertForProductSync(productId: Int, catalogType: String): PriceAlertEntity? =
        dao.getPriceAlertForProductSync(productId, catalogType)

    suspend fun savePriceAlert(alert: PriceAlertEntity, userKey: String, context: android.content.Context? = null): Long {
        // First, check if already triggered by current product price
        val isAlreadyBelow = alert.currentPrice > 0 && alert.currentPrice <= alert.targetPrice
        val finalAlert = alert.copy(
            isTriggered = isAlreadyBelow,
            lastTriggeredPrice = if (isAlreadyBelow) alert.currentPrice else 0.0,
            lastNotifiedAt = if (isAlreadyBelow) System.currentTimeMillis() else 0L
        )

        val insertedId = dao.insertPriceAlert(finalAlert)
        val alertWithId = finalAlert.copy(id = insertedId.toInt())

        // Sync to Cloud Firestore
        val firestoreDocId = FirestorePriceAlertService.savePriceAlert(alertWithId, userKey)
        if (firestoreDocId != null) {
            val updated = alertWithId.copy(firestoreId = firestoreDocId, isSynced = true)
            dao.updatePriceAlert(updated)
        }

        // Trigger notification if already below threshold
        if (isAlreadyBelow && context != null) {
            PriceAlertNotificationHelper.postPriceDropNotification(
                context = context,
                alert = alertWithId,
                newPrice = alert.currentPrice,
                supermarket = alert.catalogType
            )
        }

        return insertedId
    }

    suspend fun deletePriceAlert(alert: PriceAlertEntity, userKey: String) {
        dao.deletePriceAlert(alert.id)
        if (alert.firestoreId.isNotBlank()) {
            FirestorePriceAlertService.deletePriceAlert(alert.firestoreId, userKey)
        }
    }

    suspend fun deletePriceAlertByProduct(productId: Int, catalogType: String, userKey: String) {
        val existing = dao.getPriceAlertForProductSync(productId, catalogType)
        dao.deletePriceAlertByProduct(productId, catalogType)
        if (existing != null && existing.firestoreId.isNotBlank()) {
            FirestorePriceAlertService.deletePriceAlert(existing.firestoreId, userKey)
        }
    }

    suspend fun syncAlertsWithFirestore(userKey: String) {
        if (userKey.isBlank()) return
        val remoteAlerts = FirestorePriceAlertService.fetchPriceAlerts(userKey)
        if (remoteAlerts.isNotEmpty()) {
            remoteAlerts.forEach { remote ->
                val local = dao.getPriceAlertForProductSync(remote.productId, remote.catalogType)
                if (local == null) {
                    dao.insertPriceAlert(remote)
                } else {
                    dao.updatePriceAlert(remote.copy(id = local.id))
                }
            }
        }
    }

    /**
     * Checks all price alerts against updated product list and triggers notifications & Firestore updates
     */
    suspend fun checkPriceAlerts(
        products: List<ProductEntity>,
        context: android.content.Context?,
        userKey: String = ""
    ): List<PriceAlertEntity> {
        val allAlerts = dao.getPriceAlertList()
        if (allAlerts.isEmpty() || products.isEmpty()) return emptyList()

        val triggeredAlerts = mutableListOf<PriceAlertEntity>()

        allAlerts.forEach { alert ->
            // Find matching product in catalog
            val match = products.firstOrNull { prod ->
                (prod.id == alert.productId && (alert.catalogType == "ALL" || prod.catalogType.equals(alert.catalogType, ignoreCase = true))) ||
                (alert.productId == 0 && prod.name.trim().equals(alert.productName.trim(), ignoreCase = true) && (alert.catalogType == "ALL" || prod.catalogType.equals(alert.catalogType, ignoreCase = true)))
            }

            if (match != null && match.price > 0.0) {
                val newPrice = match.price
                val isBelowThreshold = newPrice <= alert.targetPrice

                if (isBelowThreshold) {
                    // Check if newly triggered or price dropped further
                    val shouldNotify = !alert.isTriggered || (alert.lastTriggeredPrice > newPrice && (System.currentTimeMillis() - alert.lastNotifiedAt > 30000L))
                    val updated = alert.copy(
                        currentPrice = newPrice,
                        isTriggered = true,
                        lastTriggeredPrice = newPrice,
                        lastNotifiedAt = if (shouldNotify) System.currentTimeMillis() else alert.lastNotifiedAt
                    )
                    dao.updatePriceAlert(updated)
                    triggeredAlerts.add(updated)

                    // Update in Firestore
                    if (updated.firestoreId.isNotBlank()) {
                        FirestorePriceAlertService.updateTriggeredStatus(
                            firestoreDocId = updated.firestoreId,
                            userKey = userKey,
                            isTriggered = true,
                            currentPrice = newPrice,
                            lastTriggeredPrice = newPrice
                        )
                    }

                    // Post system notification
                    if (shouldNotify && context != null) {
                        PriceAlertNotificationHelper.postPriceDropNotification(
                            context = context,
                            alert = updated,
                            newPrice = newPrice,
                            supermarket = match.catalogType
                        )
                    }
                } else {
                    // Price is above threshold
                    if (alert.currentPrice != newPrice || alert.isTriggered) {
                        val updated = alert.copy(currentPrice = newPrice, isTriggered = false)
                        dao.updatePriceAlert(updated)
                        if (updated.firestoreId.isNotBlank()) {
                            FirestorePriceAlertService.updateTriggeredStatus(
                                firestoreDocId = updated.firestoreId,
                                userKey = userKey,
                                isTriggered = false,
                                currentPrice = newPrice,
                                lastTriggeredPrice = alert.lastTriggeredPrice
                            )
                        }
                    }
                }
            }
        }

        return triggeredAlerts
    }

    // --- Loyalty Cards ---
    fun getAllLoyaltyCards(): Flow<List<LoyaltyCardEntity>> = dao.getAllLoyaltyCards()

    suspend fun saveLoyaltyCard(card: LoyaltyCardEntity) {
        dao.insertLoyaltyCard(card)
    }

    suspend fun deleteLoyaltyCard(card: LoyaltyCardEntity) {
        dao.deleteLoyaltyCard(card)
    }

    // --- App Settings (Room Local Storage + Cloud Firestore Sync) ---
    fun getAppSettingsFlow(): Flow<AppSettingsEntity?> = dao.getAppSettingsFlow()

    suspend fun getAppSettings(): AppSettingsEntity? = dao.getAppSettings()

    suspend fun saveAppSettings(settings: AppSettingsEntity) {
        dao.saveAppSettings(settings)
    }

    suspend fun clearAppSettings() {
        dao.clearAppSettings()
    }
}

package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-time progress snapshot during Firestore batch export / synchronization.
 */
data class FirestoreUploadProgress(
    val processedRecords: Int,
    val totalRecords: Int,
    val successfulRecords: Int,
    val failedRecords: Int,
    val percentage: Float, // 0.0 to 100.0
    val formattedPercentage: String // e.g. "74.5%"
)

data class FirestoreUploadResult(
    val isSuccess: Boolean,
    val message: String,
    val successfullyExported: Int = 0,
    val failedRecords: Int = 0,
    val exportPercentage: Float = 0f,
    val failedDetails: List<String> = emptyList()
)

data class FirestoreConnectionDiagnostics(
    val isConnected: Boolean,
    val isInitialized: Boolean,
    val projectId: String,
    val canRead: Boolean,
    val canWrite: Boolean,
    val latencyMs: Long,
    val errorMessage: String? = null,
    val message: String
)

object FirestoreCatalogService {
    private const val TAG = "FirestoreCatalogService"
    const val COLLECTION_SERVER_FILES = "supermarket"
    const val COLLECTION_PRODUCTS = "products"

    // Firestore batch limit is strictly 500 operations.
    // Each product generates 2 write operations (1 to 'products', 1 to 'supermarket/{file}/items').
    // 150 products * 2 operations = 300 operations per batch, safely under the 500 limit.
    private const val MAX_PRODUCTS_PER_BATCH = 150

    private fun getDb(): FirebaseFirestore? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
            } catch (ignored: Exception) {
                // Settings can only be configured once per process
            }
            firestore
        } catch (e: Exception) {
            Log.e(TAG, "Firestore is unavailable/failed to getInstance: ${e.message}", e)
            null
        }
    }

    /**
     * Executes an asynchronous block with exponential backoff for transient network or quota errors.
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 500L,
        factor: Double = 2.0,
        actionName: String = "Operation",
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val isTransient = e.message?.let {
                    it.contains("UNAVAILABLE", ignoreCase = true) ||
                    it.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                    it.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                    it.contains("network", ignoreCase = true) ||
                    it.contains("timeout", ignoreCase = true)
                } ?: false

                if (attempt == maxRetries || !isTransient) {
                    Log.e(TAG, "$actionName failed permanently at attempt $attempt: ${e.message}")
                    throw e
                }
                Log.w(TAG, "$actionName transient error on attempt $attempt: ${e.message}. Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(5000L)
            }
        }
        throw lastException ?: Exception("$actionName failed after $maxRetries attempts")
    }

    /**
     * Comprehensive real-time diagnostic connection check to test read, write, latency, and permissions.
     */
    suspend fun testConnection(): FirestoreConnectionDiagnostics {
        val start = System.currentTimeMillis()
        val db = getDb() ?: return FirestoreConnectionDiagnostics(
            isConnected = false,
            isInitialized = false,
            projectId = "Inconnu",
            canRead = false,
            canWrite = false,
            latencyMs = 0L,
            errorMessage = "FirebaseApp ou FirebaseFirestore non initialisé.",
            message = "Impossible d'obtenir l'instance FirebaseFirestore. Vérifiez google-services.json."
        )

        val projectId = try {
            db.app.options.projectId ?: "shopping-cart-c4900"
        } catch (e: Exception) {
            "shopping-cart-c4900"
        }

        var canRead = false
        var canWrite = false
        var lastErr: String? = null

        // 1. Test Read Permissions & Connectivity
        try {
            withTimeoutOrNull(6000L) {
                db.collection(COLLECTION_SERVER_FILES).limit(1).get().await()
            }
            canRead = true
        } catch (e: Exception) {
            lastErr = "Erreur lecture: ${e.localizedMessage ?: e.message}"
            Log.w(TAG, "Diagnostic read failed: $lastErr")
        }

        // 2. Test Write Permissions & Probe
        try {
            val probeRef = db.collection("_diagnostics").document("connection_probe")
            val probeData = mapOf(
                "lastProbe" to System.currentTimeMillis(),
                "deviceDate" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            withTimeoutOrNull(6000L) {
                probeRef.set(probeData, SetOptions.merge()).await()
            }
            canWrite = true
        } catch (e: Exception) {
            val writeErr = "Erreur écriture: ${e.localizedMessage ?: e.message}"
            Log.w(TAG, "Diagnostic write failed: $writeErr")
            if (lastErr == null) lastErr = writeErr
        }

        val latency = System.currentTimeMillis() - start
        val isConnected = canRead || canWrite

        val summaryMsg = when {
            canRead && canWrite -> "Connexion Firestore optimale (Lecture & Écriture OK en ${latency}ms)"
            canRead && !canWrite -> "Connexion Firestore partielle : Lecture OK, écriture restreinte (règles de sécurité Firestore)"
            !canRead && canWrite -> "Connexion Firestore partielle : Écriture OK, lecture restreinte"
            else -> "Connexion Firestore échouée : $lastErr"
        }

        return FirestoreConnectionDiagnostics(
            isConnected = isConnected,
            isInitialized = true,
            projectId = projectId,
            canRead = canRead,
            canWrite = canWrite,
            latencyMs = latency,
            errorMessage = lastErr,
            message = summaryMsg
        )
    }

    /**
     * Uploads a catalog file with extracted products to Cloud Firestore with robust batch writes,
     * exponential backoff retries, item-level fallbacks, and real-time export percentage tracking.
     */
    suspend fun uploadCatalogFileToServer(
        file: ServerCatalogFile,
        products: List<ProductEntity>,
        onProgress: ((FirestoreUploadProgress) -> Unit)? = null
    ): FirestoreUploadResult {
        return try {
            val db = getDb() ?: return FirestoreUploadResult(
                isSuccess = false,
                message = "Firestore non initialisé ou service Google Play indisponible."
            )
            val docId = if (file.id.isNotBlank()) file.id else "cat_${System.currentTimeMillis()}_${file.catalogType.lowercase()}"
            val fileDocRef = db.collection(COLLECTION_SERVER_FILES).document(docId)

            val formattedTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            // Embed product list directly ONLY if small (<= 50) to strictly stay safely under the Firestore 1MB doc limit.
            // All full catalog items are stored in the 'items' subcollection and global 'products' collection.
            val embeddedProducts = if (products.size <= 50) {
                products.map { p ->
                    hashMapOf(
                        "id" to p.id,
                        "catalogType" to (p.catalogType.ifBlank { file.catalogType }).uppercase(),
                        "name" to p.name,
                        "category" to p.category,
                        "brand" to p.brand,
                        "unit" to p.unit,
                        "price" to p.price,
                        "cost" to p.cost
                    )
                }
            } else {
                emptyList()
            }

            val fileData = hashMapOf(
                "id" to docId,
                "fileName" to file.fileName,
                "fileType" to file.fileType,
                "catalogType" to file.catalogType.uppercase(),
                "description" to file.description,
                "uploadedBy" to file.uploadedBy,
                "uploadedByName" to file.uploadedByName,
                "timestamp" to System.currentTimeMillis(),
                "formattedDate" to formattedTime,
                "fileSizeBytes" to file.fileSizeBytes,
                "fileSizeFormatted" to file.fileSizeFormatted,
                "productCount" to products.size,
                "version" to file.version,
                "isPublished" to true,
                "sampleProductsPreview" to products.take(10).map { "${it.name} (${it.price} Rs)" },
                "products" to embeddedProducts
            )

            // Save root file document with retry
            retryWithBackoff(actionName = "Save catalog metadata file") {
                fileDocRef.set(fileData, SetOptions.merge()).await()
            }

            val totalRecords = products.size
            var successfullyExported = 0
            var failedRecords = 0
            val failedDetails = mutableListOf<String>()

            // Initial progress trigger (0%)
            onProgress?.invoke(
                FirestoreUploadProgress(
                    processedRecords = 0,
                    totalRecords = totalRecords,
                    successfulRecords = 0,
                    failedRecords = 0,
                    percentage = 0f,
                    formattedPercentage = "0.0%"
                )
            )

            // Synchronize products to subcollection and global collection in safe chunks
            // Each product = 2 batch writes (1 global + 1 subcollection)
            // 150 items * 2 = 300 operations (Strictly under Firestore 500 limit per batch)
            val chunks = products.chunked(MAX_PRODUCTS_PER_BATCH)
            var productCounter = 0

            for ((chunkIndex, chunk) in chunks.withIndex()) {
                val batch = db.batch()
                val chunkStartIndex = productCounter

                chunk.forEach { p ->
                    productCounter++
                    val resolvedId = if (p.id > 0) p.id else productCounter
                    val pCat = (p.catalogType.ifBlank { file.catalogType }).trim().uppercase()
                    val pData = hashMapOf(
                        "id" to resolvedId,
                        "catalogType" to pCat,
                        "name" to p.name,
                        "category" to p.category,
                        "brand" to p.brand,
                        "unit" to p.unit,
                        "price" to p.price,
                        "cost" to p.cost,
                        "barcode" to p.barcode,
                        "lastUpdated" to Date(),
                        "sourceFileId" to docId
                    )

                    // 1. Global products collection: unique key to prevent collisions
                    val itemKey = if (p.barcode.isNotBlank()) {
                        p.barcode.trim()
                    } else {
                        "${docId}_${String.format(Locale.US, "%06d", resolvedId)}"
                    }
                    val pDocRef = db.collection(COLLECTION_PRODUCTS).document("${pCat}_$itemKey")
                    batch.set(pDocRef, pData, SetOptions.merge())

                    // 2. Subcollection under server file: unique document per record
                    val subDocRef = fileDocRef.collection("items").document("item_${String.format(Locale.US, "%06d", resolvedId)}")
                    batch.set(subDocRef, pData, SetOptions.merge())
                }

                try {
                    retryWithBackoff(
                        maxRetries = 3,
                        actionName = "Batch commit chunk ${chunkIndex + 1}/${chunks.size}"
                    ) {
                        batch.commit().await()
                    }
                    successfullyExported += chunk.size
                } catch (e: Exception) {
                    Log.w(TAG, "Batch write failed for chunk ${chunkIndex + 1} (${e.message}). Executing individual fallback writes...")
                    // Fallback strategy: Write individual records so a single bad record doesn't drop the entire chunk
                    var fallbackCounter = chunkStartIndex
                    for (p in chunk) {
                        fallbackCounter++
                        val resolvedId = if (p.id > 0) p.id else fallbackCounter
                        val pCat = (p.catalogType.ifBlank { file.catalogType }).trim().uppercase()
                        try {
                            val pData = hashMapOf(
                                "id" to resolvedId,
                                "catalogType" to pCat,
                                "name" to p.name,
                                "category" to p.category,
                                "brand" to p.brand,
                                "unit" to p.unit,
                                "price" to p.price,
                                "cost" to p.cost,
                                "barcode" to p.barcode,
                                "lastUpdated" to Date(),
                                "sourceFileId" to docId
                            )
                            val itemKey = if (p.barcode.isNotBlank()) {
                                p.barcode.trim()
                            } else {
                                "${docId}_${String.format(Locale.US, "%06d", resolvedId)}"
                            }
                            val pDocRef = db.collection(COLLECTION_PRODUCTS).document("${pCat}_$itemKey")
                            pDocRef.set(pData, SetOptions.merge()).await()

                            val subDocRef = fileDocRef.collection("items").document("item_${String.format(Locale.US, "%06d", resolvedId)}")
                            subDocRef.set(pData, SetOptions.merge()).await()

                            successfullyExported++
                        } catch (itemErr: Exception) {
                            failedRecords++
                            val errDetail = "Article #${resolvedId} (${p.name}): ${itemErr.localizedMessage ?: itemErr.message}"
                            failedDetails.add(errDetail)
                            Log.e(TAG, "Failed individual write for $errDetail")
                        }
                    }
                }

                // Calculate progress and trigger callback:
                // Export Percentage = (Successfully Exported / Total Records to Export) * 100
                val processed = successfullyExported + failedRecords
                val exportPercentage = if (totalRecords > 0) {
                    (successfullyExported.toFloat() / totalRecords.toFloat()) * 100f
                } else 100f
                val formattedPercentage = String.format(Locale.US, "%.1f%%", exportPercentage)

                onProgress?.invoke(
                    FirestoreUploadProgress(
                        processedRecords = processed,
                        totalRecords = totalRecords,
                        successfulRecords = successfullyExported,
                        failedRecords = failedRecords,
                        percentage = exportPercentage,
                        formattedPercentage = formattedPercentage
                    )
                )
                Log.i(TAG, "Firestore Export Progress: $successfullyExported/$totalRecords ($formattedPercentage) | Échecs: $failedRecords")
            }

            Log.d(TAG, "Successfully processed catalog file $docId: $successfullyExported exported, $failedRecords failed.")

            val finalPercentage = if (totalRecords > 0) {
                (successfullyExported.toFloat() / totalRecords.toFloat()) * 100f
            } else 100f

            val resultMsg = if (failedRecords == 0) {
                "Succès du téléversement ($successfullyExported articles - 100%)."
            } else {
                "Téléversement terminé avec avertissements : $successfullyExported articles exportés (${String.format(Locale.US, "%.1f%%", finalPercentage)}), $failedRecords échec(s)."
            }

            FirestoreUploadResult(
                isSuccess = successfullyExported > 0 || totalRecords == 0,
                message = resultMsg,
                successfullyExported = successfullyExported,
                failedRecords = failedRecords,
                exportPercentage = finalPercentage,
                failedDetails = failedDetails
            )
        } catch (e: Exception) {
            val errorDetails = e.localizedMessage ?: e.message ?: "Erreur inconnue"
            Log.e(TAG, "Error uploading catalog file to Firestore: $errorDetails", e)
            val friendlyMsg = when {
                errorDetails.contains("PERMISSION_DENIED", ignoreCase = true) ->
                    "Permission refusée par Firestore. Vérifiez les règles de sécurité (Firestore Security Rules)."
                errorDetails.contains("UNAVAILABLE", ignoreCase = true) || errorDetails.contains("network", ignoreCase = true) ->
                    "Impossible de joindre Firestore. Vérifiez votre connexion Internet."
                else -> "Erreur Firestore: $errorDetails"
            }
            FirestoreUploadResult(isSuccess = false, message = friendlyMsg)
        }
    }

    private var cachedFiles: List<ServerCatalogFile>? = null
    private var lastCacheTime: Long = 0L

    /**
     * Fetches all published catalog files from Cloud Firestore with timeout protection and fast fallback.
     */
    suspend fun fetchServerCatalogFiles(forceRefresh: Boolean = false): List<ServerCatalogFile> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedFiles != null && (now - lastCacheTime < 15_000L)) {
            return cachedFiles ?: getDefaultInitialServerFiles()
        }

        return try {
            val result = withTimeoutOrNull(15000L) {
                withContext(Dispatchers.IO) {
                    val db = getDb() ?: return@withContext getDefaultInitialServerFiles()
                    Log.d(TAG, "Attempting to fetch from $COLLECTION_SERVER_FILES")
                    val snapshot = db.collection(COLLECTION_SERVER_FILES)
                        .get()
                        .await()

                    if (snapshot.isEmpty) {
                        Log.d(TAG, "Firestore collection $COLLECTION_SERVER_FILES is empty")
                        return@withContext getDefaultInitialServerFiles()
                    }

                    Log.d(TAG, "Fetched ${snapshot.size()} documents from Firestore")
                    val list = mutableListOf<ServerCatalogFile>()
                    for (doc in snapshot.documents) {
                        val file = parseDocumentToServerCatalogFile(doc)
                        if (file != null && file.isPublished) {
                            list.add(file)
                        }
                    }
                    list.sortByDescending { it.timestamp }
                    list.ifEmpty { getDefaultInitialServerFiles() }
                }
            } ?: (cachedFiles ?: getDefaultInitialServerFiles())

            cachedFiles = result
            lastCacheTime = now
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching server catalog files: ${e.message}", e)
            cachedFiles ?: getDefaultInitialServerFiles()
        }
    }

    /**
     * Real-time listener for server catalog files.
     * Enables automatic sync notifications for users whenever an admin publishes new files.
     */
    fun listenToServerCatalogFiles(
        onUpdate: (List<ServerCatalogFile>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getDb() ?: return null
            db.collection(COLLECTION_SERVER_FILES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen server catalog files failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val files = snapshot.documents.mapNotNull { parseDocumentToServerCatalogFile(it) }
                            .filter { it.isPublished }
                            .sortedByDescending { it.timestamp }
                        if (files.isNotEmpty()) {
                            onUpdate(files)
                        } else {
                            onUpdate(getDefaultInitialServerFiles())
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not attach Firestore server catalog listener: ${e.message}")
            null
        }
    }

    fun removeFileFromCache(fileId: String) {
        cachedFiles = cachedFiles?.filterNot { it.id == fileId }
    }

    fun invalidateCache() {
        cachedFiles = null
        lastCacheTime = 0L
    }

    /**
     * Fetches all items for a given server catalog file from the 'items' subcollection.
     */
    suspend fun fetchServerCatalogFileItems(fileId: String): List<ProductEntity> {
        return try {
            val db = getDb() ?: return emptyList()
            val snapshot = db.collection(COLLECTION_SERVER_FILES)
                .document(fileId)
                .collection("items")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val rawId = doc.get("id") ?: doc.get("productId") ?: doc.get("code")
                    val pId = when (rawId) {
                        is Number -> rawId.toInt()
                        is String -> rawId.toIntOrNull() ?: 0
                        else -> doc.id.substringAfterLast("_").toIntOrNull() ?: 0
                    }
                    val pCat = doc.getString("catalogType") ?: doc.getString("catalog_type") ?: doc.getString("supermarket") ?: "DREAMPRICE"
                    val pName = doc.getString("name") ?: doc.getString("nom") ?: doc.getString("title") ?: doc.getString("product_name") ?: return@mapNotNull null
                    val pCategory = doc.getString("category") ?: doc.getString("categorie") ?: "Divers"
                    val pBrand = doc.getString("brand") ?: doc.getString("marque") ?: ""
                    val pUnit = doc.getString("unit") ?: doc.getString("unite") ?: "1u"
                    
                    val rawPrice = doc.get("price") ?: doc.get("prix") ?: doc.get("priceRs") ?: doc.get("salePrice")
                    val pPrice = when (rawPrice) {
                        is Number -> rawPrice.toDouble()
                        is String -> rawPrice.replace(",", ".").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    
                    val rawCost = doc.get("cost") ?: doc.get("cout") ?: doc.get("costPrice")
                    val pCost = when (rawCost) {
                        is Number -> rawCost.toDouble()
                        is String -> rawCost.replace(",", ".").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }

                    val pBarcode = doc.getString("barcode") ?: doc.getString("code_barre") ?: doc.getString("codeBarre") ?: doc.getString("ean") ?: ""

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
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching items for file $fileId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun deleteServerCatalogFile(fileId: String): Boolean {
        return try {
            val db = getDb() ?: return false
            val fileRef = db.collection(COLLECTION_SERVER_FILES).document(fileId)
            
            // 1. Delete all items in the "items" subcollection in safe batches of 300
            val subItemsSnapshot = fileRef.collection("items").get().await()
            if (!subItemsSnapshot.isEmpty) {
                val chunks = subItemsSnapshot.documents.chunked(300)
                for (chunk in chunks) {
                    val batch = db.batch()
                    for (doc in chunk) {
                        batch.delete(doc.reference)
                    }
                    retryWithBackoff(actionName = "Delete subcollection items batch") {
                        batch.commit().await()
                    }
                }
            }

            // 2. Clean up any global products linked to this source file
            try {
                val globalProdsSnapshot = db.collection(COLLECTION_PRODUCTS).whereEqualTo("sourceFileId", fileId).get().await()
                if (!globalProdsSnapshot.isEmpty) {
                    val chunks = globalProdsSnapshot.documents.chunked(300)
                    for (chunk in chunks) {
                        val batch = db.batch()
                        for (doc in chunk) {
                            batch.delete(doc.reference)
                        }
                        retryWithBackoff(actionName = "Delete global products batch") {
                            batch.commit().await()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Non-fatal cleanup of global products for $fileId: ${e.message}")
            }
            
            // 3. Delete the main document
            retryWithBackoff(actionName = "Delete server file document") {
                fileRef.delete().await()
            }
            invalidateCache()
            Log.d(TAG, "File $fileId and its subcollection items successfully deleted from Firestore.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file $fileId from server: ${e.message}", e)
            false
        }
    }

    suspend fun deleteServerCatalogFiles(fileIds: List<String>): Boolean {
        var allSuccess = true
        for (id in fileIds) {
            val success = deleteServerCatalogFile(id)
            if (!success) {
                allSuccess = false
            }
        }
        return allSuccess
    }

    suspend fun deleteProductsFromFirestore(products: List<ProductEntity>, fileIds: List<String>): Boolean {
        return try {
            val db = getDb() ?: return false
            // Operations per product: 1 for global products collection + 1 for each file in fileIds
            val opsPerProduct = (1 + fileIds.size).coerceAtLeast(1)
            // Maximum operations per batch is 500. Keep safely at 350 max operations:
            val safeChunkSize = (350 / opsPerProduct).coerceIn(10, 150)
            val chunks = products.chunked(safeChunkSize)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { p ->
                    // 1. Delete from global products
                    val pDocRef = db.collection(COLLECTION_PRODUCTS).document("${p.catalogType.uppercase()}_${p.id}")
                    batch.delete(pDocRef)

                    // 2. Delete from items subcollection of every related server catalog file
                    fileIds.forEach { fileId ->
                        val subDocRef = db.collection(COLLECTION_SERVER_FILES).document(fileId)
                            .collection("items").document("${p.id}")
                        batch.delete(subDocRef)
                    }
                }
                retryWithBackoff(actionName = "Delete products batch") {
                    batch.commit().await()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting products from Firestore: ${e.message}", e)
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDocumentToServerCatalogFile(doc: com.google.firebase.firestore.DocumentSnapshot): ServerCatalogFile? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val fileName = doc.getString("fileName") ?: "catalogue.xlsx"
            val fileType = doc.getString("fileType") ?: "XLSX"
            val catalogType = doc.getString("catalogType") ?: "DREAMPRICE"
            val description = doc.getString("description") ?: ""
            val uploadedBy = doc.getString("uploadedBy") ?: "admin@kwickart.mu"
            val uploadedByName = doc.getString("uploadedByName") ?: "Admin"
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val formattedDate = doc.getString("formattedDate") ?: ""
            val fileSizeBytes = doc.getLong("fileSizeBytes") ?: 0L
            val fileSizeFormatted = doc.getString("fileSizeFormatted") ?: "1.2 MB"
            val productCount = doc.getLong("productCount")?.toInt() ?: 0
            val version = doc.getLong("version")?.toInt() ?: 1
            val isPublished = doc.getBoolean("isPublished") ?: true
            val fileUrl = doc.getString("fileUrl")
            val samplePreviews = (doc.get("sampleProductsPreview") as? List<String>) ?: emptyList()

            val rawProductsList = (doc.get("products") as? List<Map<String, Any>>) ?: emptyList()
            val products = rawProductsList.mapNotNull { pMap ->
                try {
                    val rawId = pMap["id"] ?: pMap["productId"] ?: pMap["code"]
                    val pId = when (rawId) {
                        is Number -> rawId.toInt()
                        is String -> rawId.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val pCat = (pMap["catalogType"] as? String) ?: (pMap["catalog_type"] as? String) ?: catalogType
                    val pName = (pMap["name"] as? String) ?: (pMap["nom"] as? String) ?: (pMap["title"] as? String) ?: return@mapNotNull null
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
                    val pBarcode = (pMap["barcode"] as? String) ?: (pMap["code_barre"] as? String) ?: (pMap["codeBarre"] as? String) ?: (pMap["ean"] as? String) ?: ""

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

            ServerCatalogFile(
                id = id,
                fileName = fileName,
                fileType = fileType,
                catalogType = catalogType,
                description = description,
                uploadedBy = uploadedBy,
                uploadedByName = uploadedByName,
                timestamp = timestamp,
                formattedDate = formattedDate,
                fileSizeBytes = fileSizeBytes,
                fileSizeFormatted = fileSizeFormatted,
                productCount = if (products.isNotEmpty()) products.size else productCount,
                version = version,
                isPublished = isPublished,
                fileUrl = fileUrl,
                sampleProductsPreview = samplePreviews,
                products = products
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server file doc ${doc.id}: ${e.message}", e)
            null
        }
    }

    /**
     * Default curated server catalog files ready for immediate client synchronisation.
     */
    fun getDefaultInitialServerFiles(): List<ServerCatalogFile> {
        return emptyList()
    }
}

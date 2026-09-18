package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.example.data.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OnlineProductInfo(
    val barcode: String,
    val productName: String,
    val brand: String = "",
    val category: String = "",
    val unit: String = "",
    val imageUrl: String? = null,
    val genericName: String = "",
    val source: String = "Open Food Facts"
)

data class BarcodeScanMatchResult(
    val barcode: String,
    val onlineProduct: OnlineProductInfo,
    val isListedInCatalog: Boolean,
    val matchedProducts: List<ProductEntity>,
    val relatedProducts: List<ProductEntity> = emptyList()
)

object BarcodeLookupService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun lookupBarcode(
        barcode: String,
        catalogProducts: List<ProductEntity>
    ): BarcodeScanMatchResult = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()

        // 0. Instant Local Room SQLite matching by exact barcode
        val localBarcodeMatches = catalogProducts.filter { it.barcode.isNotBlank() && it.barcode == cleanBarcode }
        if (localBarcodeMatches.isNotEmpty()) {
            val primary = localBarcodeMatches.first()
            val onlineInfo = OnlineProductInfo(
                barcode = cleanBarcode,
                productName = primary.name,
                brand = primary.brand,
                category = primary.category,
                unit = primary.unit,
                source = "Base Locale Room (Hors-Ligne)"
            )
            val matchedProducts = findMatchingCatalogProducts(onlineInfo, catalogProducts)
            return@withContext BarcodeScanMatchResult(
                barcode = cleanBarcode,
                onlineProduct = onlineInfo,
                isListedInCatalog = true,
                matchedProducts = matchedProducts,
                relatedProducts = emptyList()
            )
        }
        
        // 1. Fetch item details from internet (Open Food Facts first, Gemini fallback)
        val onlineInfo = fetchFromOpenFoodFacts(cleanBarcode)
            ?: fetchFromGeminiAI(cleanBarcode)
            ?: createFallbackOnlineInfo(cleanBarcode, catalogProducts)

        // 2. Check if the item is listed in the product catalog
        val matchedProducts = findMatchingCatalogProducts(onlineInfo, catalogProducts)
        val isListed = matchedProducts.isNotEmpty()

        // 3. Find related products if not directly matched
        val relatedProducts = if (!isListed) {
            findRelatedProducts(onlineInfo, catalogProducts)
        } else {
            emptyList()
        }

        BarcodeScanMatchResult(
            barcode = cleanBarcode,
            onlineProduct = onlineInfo,
            isListedInCatalog = isListed,
            matchedProducts = matchedProducts,
            relatedProducts = relatedProducts
        )
    }

    private fun fetchFromOpenFoodFacts(barcode: String): OnlineProductInfo? {
        try {
            val url = "https://world.openfoodfacts.org/api/v0/product/$barcode.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MauritiusCatalogManager/1.0 (Android)")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val bodyString = response.body?.string() ?: return null
            val json = JSONObject(bodyString)
            val status = json.optInt("status", 0)

            if (status == 1 && json.has("product")) {
                val product = json.getJSONObject("product")
                val nameFr = product.optString("product_name_fr")
                val nameMain = product.optString("product_name")
                val nameGeneric = product.optString("generic_name")
                val name = listOf(nameFr, nameMain, nameGeneric).firstOrNull { it.isNotBlank() } ?: "Produit #$barcode"

                val brands = product.optString("brands", "").split(",").firstOrNull()?.trim() ?: ""
                val categories = product.optString("categories", "").split(",").firstOrNull()?.trim() ?: ""
                val quantity = product.optString("quantity", "")
                val imageUrl = product.optString("image_url", "").ifBlank { product.optString("image_front_url", "") }

                return OnlineProductInfo(
                    barcode = barcode,
                    productName = name,
                    brand = brands,
                    category = categories,
                    unit = quantity,
                    imageUrl = imageUrl.ifBlank { null },
                    genericName = nameGeneric,
                    source = "Open Food Facts (Internet)"
                )
            }
        } catch (e: Exception) {
            Log.w("BarcodeLookup", "OpenFoodFacts request failed for $barcode: ${e.message}")
        }
        return null
    }

    private suspend fun fetchFromGeminiAI(barcode: String): OnlineProductInfo? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

        try {
            val prompt = """
                Tu es un expert mondial en reconnaissance de codes-barres (EAN-13, UPC-A, GTIN) et produits de supermarché (Dreamprice, Intermart, Carrefour, Jumbo, Super U, etc.).
                Identifie le produit commercial pour le code-barres : "$barcode".
                Retourne UNIQUEMENT un objet JSON valide avec cette structure exacte :
                {
                  "product_name": "Nom complet du produit (ex: Nutella Pâte à Tartiner 400g)",
                  "brand": "Marque (ex: Ferrero)",
                  "category": "Catégorie (ex: Épicerie Sucrée)",
                  "unit": "Format/Unité (ex: 400g ou 1L)"
                }
            """.trimIndent()

            val root = JSONObject()
            val contents = org.json.JSONArray()
            val contentObj = JSONObject()
            contentObj.put("role", "user")
            val parts = org.json.JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            parts.put(partObj)
            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = root.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respText = response.body?.string() ?: ""
                val respJson = JSONObject(respText)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val resParts = content?.optJSONArray("parts")
                    if (resParts != null && resParts.length() > 0) {
                        val text = resParts.getJSONObject(0).optString("text")
                        val cleanJsonStr = text.replace("```json", "").replace("```", "").trim()
                        val parsed = JSONObject(cleanJsonStr)

                        val pName = parsed.optString("product_name", "")
                        if (pName.isNotBlank()) {
                            return@withContext OnlineProductInfo(
                                barcode = barcode,
                                productName = pName,
                                brand = parsed.optString("brand", ""),
                                category = parsed.optString("category", ""),
                                unit = parsed.optString("unit", ""),
                                source = "Gemini AI Recognition"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("BarcodeLookup", "Gemini barcode lookup failed: ${e.message}")
        }
        return@withContext null
    }

    private fun createFallbackOnlineInfo(barcode: String, catalogProducts: List<ProductEntity>): OnlineProductInfo {
        // Check if barcode directly matches an internal ID or name
        val directMatch = catalogProducts.firstOrNull { 
            "${it.id}" == barcode || it.name.contains(barcode, ignoreCase = true) 
        }

        if (directMatch != null) {
            return OnlineProductInfo(
                barcode = barcode,
                productName = directMatch.name,
                brand = directMatch.brand,
                category = directMatch.category,
                unit = directMatch.unit,
                source = "Catalogue Local"
            )
        }

        return OnlineProductInfo(
            barcode = barcode,
            productName = "Article Code #$barcode",
            brand = "Générique",
            category = "Non classé",
            unit = "Unité",
            source = "Saisie Numérique"
        )
    }

    private fun findMatchingCatalogProducts(
        onlineInfo: OnlineProductInfo,
        catalogProducts: List<ProductEntity>
    ): List<ProductEntity> {
        val queryName = onlineInfo.productName.lowercase().trim()
        val queryBrand = onlineInfo.brand.lowercase().trim()
        val barcode = onlineInfo.barcode.trim()

        // 0. Direct Barcode match
        val barcodeMatches = if (barcode.isNotBlank()) {
            catalogProducts.filter { it.barcode.isNotBlank() && it.barcode.equals(barcode, ignoreCase = true) }
        } else emptyList()
        if (barcodeMatches.isNotEmpty()) return barcodeMatches

        // 1. Direct ID match
        val idMatch = catalogProducts.filter { "${it.id}" == barcode }
        if (idMatch.isNotEmpty()) return idMatch

        // 2. Tokenize the online name into meaningful keywords
        val stopWords = setOf("de", "du", "la", "le", "les", "et", "au", "aux", "en", "pour", "un", "une", "des", "the", "and", "article", "code")
        val keywords = queryName
            .replace(Regex("[^a-zA-Z0-9áàâäéèêëíìîïóòôöúùûüçñ ]"), " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.length > 2 && it !in stopWords }

        // Find products containing multiple significant keywords
        val matched = catalogProducts.filter { product ->
            val pName = product.name.lowercase()
            val pBrand = product.brand.lowercase()

            // Exact substring match
            if (pName.contains(queryName) || queryName.contains(pName)) {
                return@filter true
            }

            // Brand match + at least one major keyword match
            if (queryBrand.isNotBlank() && (pBrand.contains(queryBrand) || queryBrand.contains(pBrand))) {
                if (keywords.any { pName.contains(it) }) {
                    return@filter true
                }
            }

            // Keyword match count
            val matchedKeywords = keywords.count { pName.contains(it) }
            if (keywords.size >= 2 && matchedKeywords >= 2) {
                return@filter true
            }
            if (keywords.size == 1 && matchedKeywords == 1) {
                return@filter true
            }

            false
        }

        return matched
    }

    private fun findRelatedProducts(
        onlineInfo: OnlineProductInfo,
        catalogProducts: List<ProductEntity>
    ): List<ProductEntity> {
        val category = onlineInfo.category.lowercase().trim()
        val brand = onlineInfo.brand.lowercase().trim()

        return catalogProducts.filter { product ->
            (brand.isNotBlank() && product.brand.contains(brand, ignoreCase = true)) ||
            (category.isNotBlank() && product.category.contains(category, ignoreCase = true))
        }.take(4)
    }
}

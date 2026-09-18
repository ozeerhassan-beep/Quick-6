package com.example.util

import com.example.BuildConfig
import com.example.data.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SemanticSearchResult(
    val query: String,
    val matchedProductIds: List<Int>,
    val explanation: String,
    val suggestedKeywords: String = "",
    val suggestedCategory: String = "",
    val suggestedCatalog: String = "",
    val isAiGenerated: Boolean = true
)

data class RecipeSearchResult(
    val recipeTitle: String,
    val subtitle: String,
    val prepTime: String,
    val ingredients: List<String>,
    val explanation: String = "",
    val isAiGenerated: Boolean = true
)

object GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val MODEL_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    suspend fun sendMessage(
        history: List<ChatMessage>,
        appContextPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Clé API Gemini non configurée. Veuillez ajouter votre clé dans les paramètres de secrets AI Studio pour activer l'assistant."
        }

        try {
            val root = JSONObject()

            // System Instruction
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            val sysPartObj = JSONObject()
            sysPartObj.put("text", "Vous êtes l'assistant intelligent du Gestionnaire de Catalogues Commercials (Dreamprice & Intermart). " +
                    "Vous aidez l'utilisateur à comprendre ses produits, analyser ses marges, trouver des produits, gérant son panier, et utiliser l'application (importation CSV/Excel, gestion d'accès, etc.). " +
                    "Soyez toujours courtois, précis, utile et structuré dans vos réponses.\n\nContext de l'application à ce moment précis :\n$appContextPrompt")
            sysParts.put(sysPartObj)
            systemInstruction.put("parts", sysParts)
            root.put("systemInstruction", systemInstruction)

            // Conversation Contents
            val contentsArray = JSONArray()
            history.takeLast(10).forEach { msg ->
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.role == "user") "user" else "model")
                val partsArr = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msg.text)
                partsArr.put(partObj)
                contentObj.put("parts", partsArr)
                contentsArray.put(contentObj)
            }
            root.put("contents", contentsArray)

            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$MODEL_ENDPOINT?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext "Désolé, une erreur s'est produite lors de la communication avec l'assistant Gemini (${response.code})."
                }

                val jsonResponse = JSONObject(responseStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Pas de réponse reçue.")
                    }
                }
                return@withContext "L'assistant n'a pas pu générer de texte pour votre demande."
            }
        } catch (e: Exception) {
            return@withContext "Erreur de connexion : ${e.localizedMessage ?: "Problème réseau"}"
        }
    }

    /**
     * Performs natural language semantic search across the catalog items using Gemini API (gemini-3.5-flash).
     * Automatically falls back to a smart multi-criteria semantic token matcher if offline or key is missing.
     */
    suspend fun performSemanticSearch(
        query: String,
        products: List<ProductEntity>
    ): SemanticSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext SemanticSearchResult(
                query = query,
                matchedProductIds = emptyList(),
                explanation = "Veuillez formuler une recherche vocale ou textuelle.",
                isAiGenerated = false
            )
        }

        if (products.isEmpty()) {
            return@withContext SemanticSearchResult(
                query = cleanQuery,
                matchedProductIds = emptyList(),
                explanation = "Aucun produit n'est actuellement disponible dans le catalogue.",
                isAiGenerated = false
            )
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                // Build compact representation of products (ID, name, brand, category, catalog, price)
                val catalogSummary = products.take(160).joinToString("\n") { p ->
                    "ID:${p.id}|${p.name}|${p.brand}|${p.category}|${p.catalogType}|Prix:${p.price}Rs"
                }

                val prompt = """
                    Vous êtes le moteur de recherche sémantique en langage naturel pour un catalogue de supermarché (Dreamprice, Intermart, etc.).
                    L'utilisateur a dicté ou recherché la phrase suivante :
                    "$cleanQuery"

                    Voici les données des produits disponibles (format: ID|Nom|Marque|Catégorie|Enseigne|Prix) :
                    $catalogSummary

                    Votre tâche :
                    1. Comprenez l'intention profonde de l'utilisateur (besoin culinaire, type de produit, contrainte de budget, enseigne ciblée, promotions, substituts ou synonymes).
                    2. Identifiez la liste ordonnée des IDs de produits correspondants par ordre de pertinence (maximum 20 IDs pertinents).
                    3. Rédigez une brève explication claire et bienveillante en français (1 ou 2 phrases) pour expliquer pourquoi ces produits ont été choisis.

                    Répondez STRICTEMENT avec un JSON valide respectant cette structure exacte (sans bloc markdown autour si possible) :
                    {
                      "matchedIds": [1, 2, 3],
                      "explanation": "Voici les meilleures options sélectionnées pour votre recherche.",
                      "suggestedKeywords": "Mots clés simplifiés",
                      "suggestedCategory": "Nom de catégorie si applicable",
                      "suggestedCatalog": "TOUS"
                    }
                """.trimIndent()

                val root = JSONObject()
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                contentObj.put("role", "user")
                val partsArr = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", prompt)
                partsArr.put(partObj)
                contentObj.put("parts", partsArr)
                contentsArray.put(contentObj)
                root.put("contents", contentsArray)

                val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("$MODEL_ENDPOINT?key=$apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(responseStr)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val rawText = parts.getJSONObject(0).optString("text", "")
                                val jsonStartIndex = rawText.indexOf('{')
                                val jsonEndIndex = rawText.lastIndexOf('}')
                                if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
                                    val jsonSnippet = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
                                    val parsed = JSONObject(jsonSnippet)
                                    val idsArray = parsed.optJSONArray("matchedIds")
                                    val matchedIds = mutableListOf<Int>()
                                    if (idsArray != null) {
                                        for (i in 0 until idsArray.length()) {
                                            matchedIds.add(idsArray.getInt(i))
                                        }
                                    }
                                    val explanation = parsed.optString("explanation", "Résultats trouvés par l'IA Gemini pour \"$cleanQuery\".")
                                    val suggestedKeywords = parsed.optString("suggestedKeywords", cleanQuery)
                                    val suggestedCategory = parsed.optString("suggestedCategory", "")
                                    val suggestedCatalog = parsed.optString("suggestedCatalog", "")

                                    if (matchedIds.isNotEmpty()) {
                                        return@withContext SemanticSearchResult(
                                            query = cleanQuery,
                                            matchedProductIds = matchedIds,
                                            explanation = explanation,
                                            suggestedKeywords = suggestedKeywords,
                                            suggestedCategory = suggestedCategory,
                                            suggestedCatalog = suggestedCatalog,
                                            isAiGenerated = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fall through to local semantic search
            }
        }

        // Smart local semantic matcher fallback
        return@withContext computeLocalSemanticSearch(cleanQuery, products)
    }

    private fun computeLocalSemanticSearch(
        query: String,
        products: List<ProductEntity>
    ): SemanticSearchResult {
        val queryLower = query.lowercase().trim()
        val tokens = queryLower.split(" ", ",", "-", "'", "/", "&")
            .filter { it.length > 2 && it !in listOf("pour", "avec", "dans", "sans", "les", "des", "une", "est", "qui", "par", "sur", "que", "cherche", "trouve", "veux", "donne", "produit", "produits") }

        // Detect Price Constraints (e.g. "moins de 50", "< 100", "pas cher")
        var maxPrice: Double? = null
        val priceRegex = Regex("""(?:moins\s+de|<|sous)\s*(\d+(?:\.\d+)?)""")
        priceRegex.find(queryLower)?.let { match ->
            maxPrice = match.groupValues[1].toDoubleOrNull()
        }

        val preferCheap = queryLower.contains("pas cher") || queryLower.contains("moins cher") || queryLower.contains("economique") || queryLower.contains("meilleur prix")

        // Detect Store preference
        var preferredCatalog: String? = null
        if (queryLower.contains("dreamprice") || queryLower.contains("dream")) preferredCatalog = "DREAMPRICE"
        if (queryLower.contains("intermart") || queryLower.contains("inter")) preferredCatalog = "INTERMART"
        if (queryLower.contains("super u") || queryLower.contains("superu")) preferredCatalog = "SUPER U"
        if (queryLower.contains("winners")) preferredCatalog = "WINNERS"

        // Concept mappings (e.g. "petit dejeuner" -> lait, cafe, the, sucre, cereales, beurre, pain)
        val synonymTokens = mutableSetOf<String>()
        synonymTokens.addAll(tokens)

        if (queryLower.contains("petit dejeuner") || queryLower.contains("déjeuner")) {
            synonymTokens.addAll(listOf("lait", "café", "thé", "pain", "céréale", "beurre", "sucre", "chocolat", "confiture"))
        }
        if (queryLower.contains("boisson") || queryLower.contains("boire") || queryLower.contains("soif")) {
            synonymTokens.addAll(listOf("jus", "eau", "soda", "coca", "pepsi", "boisson", "thé", "sirop"))
        }
        if (queryLower.contains("nettoyage") || queryLower.contains("linge") || queryLower.contains("menage") || queryLower.contains("laver")) {
            synonymTokens.addAll(listOf("lessive", "savon", "javel", "détergent", "assouplissant", "nettoyant", "éponge"))
        }
        if (queryLower.contains("bebe") || queryLower.contains("bébé")) {
            synonymTokens.addAll(listOf("couche", "lait", "lingette", "bouillie"))
        }
        if (queryLower.contains("repas") || queryLower.contains("diner") || queryLower.contains("cuisiner")) {
            synonymTokens.addAll(listOf("riz", "huile", "farine", "pâtes", "sauce", "sel", "épice", "poisson", "poulet"))
        }

        val scoredList = products.mapNotNull { product ->
            var score = 0
            val pName = product.name.lowercase()
            val pBrand = product.brand.lowercase()
            val pCat = product.category.lowercase()
            val pStore = product.catalogType.uppercase()

            if (preferredCatalog != null && pStore == preferredCatalog) {
                score += 30
            }

            if (maxPrice != null && product.price <= maxPrice!!) {
                score += 25
            } else if (maxPrice != null && product.price > maxPrice!!) {
                score -= 40
            }

            for (token in synonymTokens) {
                if (pName.contains(token)) score += 40
                if (pBrand.contains(token)) score += 25
                if (pCat.contains(token)) score += 20
            }

            if (preferCheap) {
                // Lower price gives extra boost
                score += (100 - product.price.coerceIn(0.0, 100.0)).toInt() / 5
            }

            if (score > 0) Pair(product, score) else null
        }

        val matchedIds = if (preferCheap) {
            scoredList.sortedWith(compareByDescending<Pair<ProductEntity, Int>> { it.second }.thenBy { it.first.price })
                .take(20)
                .map { it.first.id }
        } else {
            scoredList.sortedByDescending { it.second }
                .take(20)
                .map { it.first.id }
        }

        val count = matchedIds.size
        val explanation = if (count > 0) {
            "Analyse sémantique : $count ${if (count > 1) "articles trouvés" else "article trouvé"} correspondant à \"$query\"."
        } else {
            "Aucun produit ne correspond exactement à \"$query\". Essayez avec d'autres termes."
        }

        return SemanticSearchResult(
            query = query,
            matchedProductIds = matchedIds,
            explanation = explanation,
            suggestedKeywords = tokens.firstOrNull() ?: query,
            isAiGenerated = false
        )
    }

    /**
     * Generates a recipe with its ingredients list using Gemini AI given a recipe name query.
     */
    suspend fun generateRecipeIngredients(
        recipeQuery: String
    ): RecipeSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = recipeQuery.trim()
        if (cleanQuery.isBlank()) {
            return@withContext RecipeSearchResult(
                recipeTitle = "Recette inconnue",
                subtitle = "Veuillez entrer le nom d'une recette",
                prepTime = "15 min",
                ingredients = emptyList(),
                explanation = "Veuillez saisir une recherche.",
                isAiGenerated = false
            )
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Vous êtes un chef cuisinier et expert culinaire. L'utilisateur souhaite cuisiner la recette suivante :
                    "$cleanQuery"

                    Votre tâche :
                    1. Identifiez le nom élégant de la recette.
                    2. Rédigez un sous-titre attractif résumant les saveurs principales.
                    3. Estimez le temps de préparation (ex: "30 min").
                    4. Listez entre 4 et 8 ingrédients de base trouvables en supermarché (noms simples en français, ex: "Farine de blé", "Beurre", "Œufs", "Pommes").
                    5. Donnez une explication très courte.

                    Répondez STRICTEMENT avec un JSON valide respectant cette structure exacte (sans bloc markdown) :
                    {
                      "recipeTitle": "Nom de la recette",
                      "subtitle": "Description des saveurs",
                      "prepTime": "25 min",
                      "ingredients": ["Ingrédient 1", "Ingrédient 2", "Ingrédient 3"],
                      "explanation": "Ingrédients identifiés par l'IA Gemini pour votre préparation."
                    }
                """.trimIndent()

                val root = JSONObject()
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                contentObj.put("role", "user")
                val partsArr = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", prompt)
                partsArr.put(partObj)
                contentObj.put("parts", partsArr)
                contentsArray.put(contentObj)
                root.put("contents", contentsArray)

                val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("$MODEL_ENDPOINT?key=$apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(responseStr)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val rawText = parts.getJSONObject(0).optString("text", "")
                                val jsonStartIndex = rawText.indexOf('{')
                                val jsonEndIndex = rawText.lastIndexOf('}')
                                if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
                                    val jsonSnippet = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
                                    val parsed = JSONObject(jsonSnippet)
                                    val title = parsed.optString("recipeTitle", cleanQuery)
                                    val subtitle = parsed.optString("subtitle", "Recette gourmande suggérée par l'IA")
                                    val prepTime = parsed.optString("prepTime", "25 min")
                                    val exp = parsed.optString("explanation", "Ingrédients extraits par Gemini")
                                    val ingArray = parsed.optJSONArray("ingredients")
                                    val ingredientsList = mutableListOf<String>()
                                    if (ingArray != null) {
                                        for (i in 0 until ingArray.length()) {
                                            ingredientsList.add(ingArray.getString(i))
                                        }
                                    }
                                    if (ingredientsList.isNotEmpty()) {
                                        return@withContext RecipeSearchResult(
                                            recipeTitle = title,
                                            subtitle = subtitle,
                                            prepTime = prepTime,
                                            ingredients = ingredientsList,
                                            explanation = exp,
                                            isAiGenerated = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to local recipe generator
            }
        }

        // Smart local recipe generator fallback
        return@withContext computeLocalRecipeIngredients(cleanQuery)
    }

    private fun computeLocalRecipeIngredients(query: String): RecipeSearchResult {
        val q = query.lowercase().trim()
        val title = query.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val ingredients = when {
            q.contains("tarte") || q.contains("pie") -> listOf("Pâte feuilletée", "Pommes", "Sucre en poudre", "Beurre doux", "Cannelle")
            q.contains("pizza") -> listOf("Pâte à pizza", "Sauce tomate", "Mozzarella râpée", "Jambon blanc", "Olives noires")
            q.contains("couscous") -> listOf("Semoule de blé", "Poulet", "Merguez", "Courgettes", "Carottes", "Pois chiches", "Épices à couscous")
            q.contains("tacos") || q.contains("burrito") -> listOf("Galettes tortilla", "Escalope de poulet", "Sauce fromagère", "Frites", "Gouda râpé")
            q.contains("pâte") || q.contains("spaghetti") || q.contains("pasta") -> listOf("Spaghetti", "Lardons fumés", "Crème fraîche", "Œufs", "Parmesan")
            q.contains("burger") -> listOf("Pains burger", "Steaks hachés", "Tranches de cheddar", "Oignons", "Sauce burger", "Salade verte")
            q.contains("salade") -> listOf("Salade verte", "Tomates cerises", "Concombre", "Féta", "Huile d'olive", "Vinaigre")
            q.contains("poulet") || q.contains("chicken") -> listOf("Poulet entier", "Ail", "Oignons", "Herbes de Provence", "Huile d'olive", "Pommes de terre")
            q.contains("soupe") || q.contains("potage") -> listOf("Carottes", "Poireaux", "Pommes de terre", "Oignons", "Bouillon de légumes", "Crème fraîche")
            q.contains("gâteau") || q.contains("chocolat") -> listOf("Chocolat noir", "Farine de blé", "Beurre doux", "Sucre en poudre", "Œufs frais")
            else -> listOf("Farine de blé", "Beurre doux", "Œufs frais", "Lait entier", "Sel & Poivre", "Légumes de saison")
        }

        return RecipeSearchResult(
            recipeTitle = title,
            subtitle = "Recette savoureuse préparée maison",
            prepTime = "30 min",
            ingredients = ingredients,
            explanation = "Ingrédients suggérés pour \"$title\".",
            isAiGenerated = false
        )
    }
}



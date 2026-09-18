package com.example.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.CartItemEntity
import kotlin.math.hypot
import kotlin.math.roundToInt

data class AisleDefinition(
    val id: String,                  // e.g. "AISLE_1A"
    val code: String,                // e.g. "1A"
    val label: String,               // e.g. "Rayon 1A"
    val categoryName: String,        // e.g. "Fruits & Légumes"
    val description: String,         // e.g. "Pommes, bananes, oignons, tomates"
    val normX: Float,                // Normalized 0.0 .. 1.0 floor coordinate
    val normY: Float,
    val widthNorm: Float = 0.12f,
    val heightNorm: Float = 0.16f,
    val sectionColor: Long = 0xFF10B981, // Emerald
    val keywords: List<String> = emptyList()
)

data class StoreFloorPlan(
    val id: String,
    val name: String,
    val banner: String,
    val location: String,
    val entrance: Offset,            // normalized coordinate (0..1)
    val checkouts: Offset,           // normalized coordinate (0..1)
    val aisles: List<AisleDefinition>
)

data class RouteStop(
    val stepIndex: Int,
    val aisle: AisleDefinition,
    val items: List<CartItemEntity>,
    val distanceMetersFromPrev: Int,
    val isCompleted: Boolean = false
)

data class OptimizedShoppingRoute(
    val store: StoreFloorPlan,
    val stops: List<RouteStop>,
    val totalDistanceMeters: Int,
    val estimatedMinutes: Int,
    val minutesSaved: Int,
    val pathPoints: List<Offset>,
    val currentStopIndex: Int,
    val completedItemCount: Int,
    val totalItemCount: Int
)

object StoreRouteOptimizer {

    // Pre-configured supermarket floor plans
    val STORE_PLANS = listOf(
        createWinnersFloorPlan(),
        createSuperUFloorPlan(),
        createIntermartFloorPlan(),
        createDreampriceFloorPlan()
    )

    fun getDefaultStore(): StoreFloorPlan = STORE_PLANS.first()

    fun getStoreByCatalogType(catalogType: String): StoreFloorPlan {
        val clean = catalogType.trim().uppercase()
        return when {
            clean.contains("WINNER") -> STORE_PLANS[0]
            clean.contains("SUPER U") || clean.contains("SUPERU") -> STORE_PLANS[1]
            clean.contains("INTERMART") -> STORE_PLANS[2]
            clean.contains("DREAM") -> STORE_PLANS[3]
            else -> STORE_PLANS[0]
        }
    }

    /**
     * Map any product name and category to the best matching Aisle definition
     */
    fun findAisleForProduct(store: StoreFloorPlan, productName: String, category: String): AisleDefinition {
        val search = (productName + " " + category).lowercase()

        // 1. Match specific keywords defined in the floor plan
        val matchedByKeyword = store.aisles.firstOrNull { aisle ->
            aisle.keywords.any { kw -> search.contains(kw.lowercase()) }
        }
        if (matchedByKeyword != null) return matchedByKeyword

        // 2. Match by category name keywords
        val matchedByCategory = store.aisles.firstOrNull { aisle ->
            aisle.categoryName.lowercase().split("&", ",", " ").filter { it.length > 2 }.any { term ->
                search.contains(term)
            }
        }
        if (matchedByCategory != null) return matchedByCategory

        // 3. Fallback to middle grocery aisle (Aisle 3A or first available)
        return store.aisles.find { it.code.startsWith("3") } ?: store.aisles.first()
    }

    /**
     * Solves Travelling Salesperson Problem (TSP) using Nearest-Neighbor heuristic
     * starting at Entrance -> visiting all required aisles -> ending at Checkouts.
     */
    fun buildOptimizedRoute(
        store: StoreFloorPlan,
        cartItems: List<CartItemEntity>,
        completedItemIds: Set<Int>
    ): OptimizedShoppingRoute {
        if (cartItems.isEmpty()) {
            return OptimizedShoppingRoute(
                store = store,
                stops = emptyList(),
                totalDistanceMeters = 0,
                estimatedMinutes = 0,
                minutesSaved = 0,
                pathPoints = listOf(store.entrance, store.checkouts),
                currentStopIndex = 0,
                completedItemCount = 0,
                totalItemCount = 0
            )
        }

        // Group cart items by their target aisle
        val itemsByAisle = mutableMapOf<AisleDefinition, MutableList<CartItemEntity>>()
        for (item in cartItems) {
            val aisle = findAisleForProduct(store, item.productName, item.category)
            itemsByAisle.getOrPut(aisle) { mutableListOf() }.add(item)
        }

        val unvisitedAisles = itemsByAisle.keys.toMutableList()
        val orderedStops = mutableListOf<RouteStop>()
        val pathCoordinates = mutableListOf<Offset>()

        // Start path at store entrance
        pathCoordinates.add(store.entrance)
        var currentPosition = store.entrance
        var accumulatedDistanceMeters = 0
        var stepCounter = 1

        // Scale: 1 normalized unit in our grid ~ 80 meters of store walking
        val METERS_PER_UNIT = 85.0f

        while (unvisitedAisles.isNotEmpty()) {
            // Find closest unvisited aisle
            val nextAisle = unvisitedAisles.minByOrNull { aisle ->
                distance(currentPosition, Offset(aisle.normX, aisle.normY))
            } ?: unvisitedAisles.first()

            val aislePos = Offset(nextAisle.normX, nextAisle.normY)
            val distUnits = distance(currentPosition, aislePos)
            val stepMeters = (distUnits * METERS_PER_UNIT).roundToInt().coerceAtLeast(6)
            accumulatedDistanceMeters += stepMeters

            val itemsInAisle = itemsByAisle[nextAisle] ?: emptyList()
            val allItemsCompleted = itemsInAisle.isNotEmpty() && itemsInAisle.all { completedItemIds.contains(it.id) }

            orderedStops.add(
                RouteStop(
                    stepIndex = stepCounter++,
                    aisle = nextAisle,
                    items = itemsInAisle,
                    distanceMetersFromPrev = stepMeters,
                    isCompleted = allItemsCompleted
                )
            )

            // Add midpoint transit waypoints for visual realism (walk along central supermarket alley)
            val alleyY = 0.52f
            if (kotlin.math.abs(currentPosition.y - aislePos.y) > 0.25f && currentPosition != store.entrance) {
                pathCoordinates.add(Offset(currentPosition.x, alleyY))
                pathCoordinates.add(Offset(aislePos.x, alleyY))
            }
            pathCoordinates.add(aislePos)

            currentPosition = aislePos
            unvisitedAisles.remove(nextAisle)
        }

        // Final leg from last aisle to Checkouts
        val finalLegUnits = distance(currentPosition, store.checkouts)
        val finalLegMeters = (finalLegUnits * METERS_PER_UNIT).roundToInt().coerceAtLeast(8)
        accumulatedDistanceMeters += finalLegMeters
        pathCoordinates.add(store.checkouts)

        // Estimated minutes walking + item picking (assuming 1.2 min per stop)
        val walkingMinutes = (accumulatedDistanceMeters / 60.0).roundToInt()
        val pickingMinutes = (orderedStops.size * 1.2).roundToInt()
        val totalMinutes = walkingMinutes + pickingMinutes

        // Estimated time saved compared to random unoptimized wandering (backtracking)
        val minutesSaved = (totalMinutes * 0.35).roundToInt().coerceAtLeast(3)

        // Find index of current active stop (first incomplete stop)
        val currentStopIdx = orderedStops.indexOfFirst { !it.isCompleted }.let {
            if (it == -1) orderedStops.size else it
        }

        val completedCount = cartItems.count { completedItemIds.contains(it.id) }

        return OptimizedShoppingRoute(
            store = store,
            stops = orderedStops,
            totalDistanceMeters = accumulatedDistanceMeters,
            estimatedMinutes = totalMinutes,
            minutesSaved = minutesSaved,
            pathPoints = pathCoordinates,
            currentStopIndex = currentStopIdx,
            completedItemCount = completedCount,
            totalItemCount = cartItems.size
        )
    }

    private fun distance(p1: Offset, p2: Offset): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }

    // ==========================================
    // Supermarket Floor Plan Configurations
    // ==========================================

    private fun createWinnersFloorPlan(): StoreFloorPlan {
        return StoreFloorPlan(
            id = "WINNERS_HYPER",
            name = "Winner's Hypermarket",
            banner = "Winner's Mauritius",
            location = "Trianon / Phoenix Mall",
            entrance = Offset(0.12f, 0.90f),
            checkouts = Offset(0.85f, 0.90f),
            aisles = listOf(
                AisleDefinition(
                    id = "WIN_1A",
                    code = "1A",
                    label = "Rayon 1A",
                    categoryName = "Fruits & Légumes Frais",
                    description = "Pommes, bananes, carottes, oignons, salades locales",
                    normX = 0.15f,
                    normY = 0.25f,
                    sectionColor = 0xFF10B981,
                    keywords = listOf("pomme", "banane", "tomate", "oignon", "pomme de terre", "legume", "fruit", "salade", "carotte", "ail")
                ),
                AisleDefinition(
                    id = "WIN_1B",
                    code = "1B",
                    label = "Rayon 1B",
                    categoryName = "Boulangerie & Pâtisserie",
                    description = "Pains frais, baguettes, viennoiseries, brioches",
                    normX = 0.15f,
                    normY = 0.65f,
                    sectionColor = 0xFFF59E0B,
                    keywords = listOf("pain", "baguette", "croissant", "brioche", "boulangerie", "gateau", "flan", "biscuit")
                ),
                AisleDefinition(
                    id = "WIN_2A",
                    code = "2A",
                    label = "Rayon 2A",
                    categoryName = "Conserves & Sauces",
                    description = "Tomates pelées, maïs, thon, sardines, sauces tomate",
                    normX = 0.32f,
                    normY = 0.25f,
                    sectionColor = 0xFFEF4444,
                    keywords = listOf("thon", "sardine", "conserve", "sauce", "ketchup", "mayonnaise", "mais", "pois", "champignon")
                ),
                AisleDefinition(
                    id = "WIN_2B",
                    code = "2B",
                    label = "Rayon 2B",
                    categoryName = "Épices & Huiles de Cuisson",
                    description = "Huile de tournesol, huile d'olive, massala, curry, sel, poivre",
                    normX = 0.32f,
                    normY = 0.65f,
                    sectionColor = 0xFFD97706,
                    keywords = listOf("huile", "epice", "massala", "curry", "sel", "poivre", "moutarde", "vinaigre", "morne")
                ),
                AisleDefinition(
                    id = "WIN_3A",
                    code = "3A",
                    label = "Rayon 3A",
                    categoryName = "Riz, Farine & Céréales",
                    description = "Riz Basmati, riz long grain, farine blanche, avoine",
                    normX = 0.50f,
                    normY = 0.25f,
                    sectionColor = 0xFF8B5CF6,
                    keywords = listOf("riz", "basmati", "farine", "cereale", "corn flakes", "avoine", "lentille", "grain")
                ),
                AisleDefinition(
                    id = "WIN_3B",
                    code = "3B",
                    label = "Rayon 3B",
                    categoryName = "Pâtes, Nouilles & Féculents",
                    description = "Spaghetti, nouilles instantanées Apollo, macaroni",
                    normX = 0.50f,
                    normY = 0.65f,
                    sectionColor = 0xFF6366F1,
                    keywords = listOf("pate", "spaghetti", "apollo", "nouille", "macaroni", "penne", "lasagne")
                ),
                AisleDefinition(
                    id = "WIN_4A",
                    code = "4A",
                    label = "Rayon 4A",
                    categoryName = "Boissons, Jus & Eau Minérale",
                    description = "Eau Vital, jus Ceres, sodas Phoenix, thés Bois Chéri",
                    normX = 0.68f,
                    normY = 0.25f,
                    sectionColor = 0xFF0284C7,
                    keywords = listOf("eau", "vital", "jus", "coca", "pepsi", "boisson", "the", "bois cheri", "cafe", "sirop")
                ),
                AisleDefinition(
                    id = "WIN_4B",
                    code = "4B",
                    label = "Rayon 4B",
                    categoryName = "Biscuits, Snacks & Chocolats",
                    description = "Biscuits Manioc, chips, crackers, chocolat",
                    normX = 0.68f,
                    normY = 0.65f,
                    sectionColor = 0xFFEC4899,
                    keywords = listOf("snack", "chips", "biscuit", "chocolat", "bonbon", "gaufrette", "aperitif", "manioc")
                ),
                AisleDefinition(
                    id = "WIN_5A",
                    code = "5A",
                    label = "Rayon 5A",
                    categoryName = "Produits Laitiers & Œufs",
                    description = "Lait Twin Cows, Red Cow, fromages, yaourts, beurre",
                    normX = 0.85f,
                    normY = 0.25f,
                    sectionColor = 0xFF3B82F6,
                    keywords = listOf("lait", "twin cows", "red cow", "fromage", "yaourt", "beurre", "oeuf", "creme")
                ),
                AisleDefinition(
                    id = "WIN_5B",
                    code = "5B",
                    label = "Rayon 5B",
                    categoryName = "Surgelés, Viandes & Poissons",
                    description = "Poulet entier Chantecler, steak haché, crevettes, frites",
                    normX = 0.85f,
                    normY = 0.65f,
                    sectionColor = 0xFF06B6D4,
                    keywords = listOf("poulet", "chantecler", "viande", "poisson", "crevette", "surgele", "glace", "frite")
                ),
                AisleDefinition(
                    id = "WIN_6A",
                    code = "6A",
                    label = "Rayon 6A",
                    categoryName = "Hygiène & Entretien Maison",
                    description = "Lessive Ariel, savon Lux, shampooing, nettoyant sol",
                    normX = 0.50f,
                    normY = 0.88f,
                    sectionColor = 0xFF14B8A6,
                    keywords = listOf("lessive", "savon", "shampooing", "dentifrice", "ariel", "omo", "papier", "nettoyant", "javel")
                )
            )
        )
    }

    private fun createSuperUFloorPlan(): StoreFloorPlan {
        return StoreFloorPlan(
            id = "SUPER_U_GB",
            name = "Super U Grand Baie",
            banner = "Super U Hyper",
            location = "Grand Baie La Croisette",
            entrance = Offset(0.10f, 0.88f),
            checkouts = Offset(0.88f, 0.88f),
            aisles = listOf(
                AisleDefinition("SU_1", "1", "Rayon 1", "Marché Frais & Bio", "Fruits, légumes, herbes fraîches", 0.16f, 0.22f, sectionColor = 0xFF10B981, keywords = listOf("fruit", "legume", "bio", "salade")),
                AisleDefinition("SU_2", "2", "Rayon 2", "Boucherie & Charcuterie", "Poulet, bœuf, saucisses, jambon", 0.16f, 0.58f, sectionColor = 0xFFEF4444, keywords = listOf("viande", "poulet", "boeuf", "jambon", "charcuterie")),
                AisleDefinition("SU_3", "3", "Rayon 3", "Poissonnerie & Surgelés", "Poissons frais, crevettes, plats cuisinés", 0.36f, 0.22f, sectionColor = 0xFF06B6D4, keywords = listOf("poisson", "surgele", "crevette", "glace")),
                AisleDefinition("SU_4", "4", "Rayon 4", "Épicerie Salée & Huiles", "Riz, pâtes, conserves, huiles, sauces", 0.36f, 0.58f, sectionColor = 0xFFF59E0B, keywords = listOf("riz", "pate", "conserve", "huile", "epice", "sauce")),
                AisleDefinition("SU_5", "5", "Rayon 5", "Épicerie Sucrée & Petit Déjeuner", "Café, thé, céréales, biscuits, chocolat", 0.58f, 0.22f, sectionColor = 0xFF8B5CF6, keywords = listOf("the", "cafe", "cereale", "biscuit", "chocolat", "sucre", "confiture")),
                AisleDefinition("SU_6", "6", "Rayon 6", "Boissons & Vins", "Eaux, sodas, jus, bières locales Phoenix", 0.58f, 0.58f, sectionColor = 0xFF0284C7, keywords = listOf("eau", "jus", "biere", "vin", "soda", "boisson")),
                AisleDefinition("SU_7", "7", "Rayon 7", "Laiterie & Crèmerie", "Lait, yaourts, fromages français et locaux", 0.80f, 0.22f, sectionColor = 0xFF3B82F6, keywords = listOf("lait", "yaourt", "fromage", "beurre", "oeuf")),
                AisleDefinition("SU_8", "8", "Rayon 8", "Droguerie & Hygiène", "Savons, shampoings, produits ménagers", 0.80f, 0.58f, sectionColor = 0xFF14B8A6, keywords = listOf("savon", "shampoing", "lessive", "nettoyant", "soin"))
            )
        )
    }

    private fun createIntermartFloorPlan(): StoreFloorPlan {
        return StoreFloorPlan(
            id = "INTERMART_EBENE",
            name = "Intermart Express",
            banner = "Intermart Mauritius",
            location = "Ébène Cybercity / Bagatelle",
            entrance = Offset(0.12f, 0.88f),
            checkouts = Offset(0.85f, 0.88f),
            aisles = listOf(
                AisleDefinition("IM_1", "1", "Allée 1", "Primeurs & Fruits", "Fruits exotiques, légumes locaux", 0.20f, 0.25f, sectionColor = 0xFF10B981, keywords = listOf("fruit", "legume", "tomate", "pomme")),
                AisleDefinition("IM_2", "2", "Allée 2", "Épicerie & Condiments", "Riz basmati, sauces, conserves", 0.20f, 0.65f, sectionColor = 0xFFF59E0B, keywords = listOf("riz", "conserve", "sauce", "epice", "huile")),
                AisleDefinition("IM_3", "3", "Allée 3", "Boissons & Rafraîchissements", "Jus frais, eau Vital, sodas", 0.50f, 0.25f, sectionColor = 0xFF0284C7, keywords = listOf("eau", "jus", "boisson", "soda", "the")),
                AisleDefinition("IM_4", "4", "Allée 4", "Snacks & Confiserie", "Biscuits, gâteaux, chocolats", 0.50f, 0.65f, sectionColor = 0xFFEC4899, keywords = listOf("snack", "biscuit", "chocolat", "chips")),
                AisleDefinition("IM_5", "5", "Allée 5", "Produits Frais & Laitages", "Lait, yaourt, fromage, charcuterie", 0.80f, 0.25f, sectionColor = 0xFF3B82F6, keywords = listOf("lait", "fromage", "yaourt", "beurre", "oeuf")),
                AisleDefinition("IM_6", "6", "Allée 6", "Surgelés & Entretien", "Glaces, poissons surgelés, détergents", 0.80f, 0.65f, sectionColor = 0xFF06B6D4, keywords = listOf("surgele", "glace", "lessive", "savon", "nettoyant"))
            )
        )
    }

    private fun createDreampriceFloorPlan(): StoreFloorPlan {
        return StoreFloorPlan(
            id = "DREAMPRICE_PORT_LOUIS",
            name = "Dreamprice Discount",
            banner = "Dreamprice Mauritius",
            location = "Port Louis / Rose Hill",
            entrance = Offset(0.12f, 0.85f),
            checkouts = Offset(0.85f, 0.85f),
            aisles = listOf(
                AisleDefinition("DP_1", "1", "Rayon 1", "Riz & Grains Secs Grossiste", "Sacs de riz 5kg, grains secs, lentilles", 0.22f, 0.28f, sectionColor = 0xFF8B5CF6, keywords = listOf("riz", "grain", "lentille", "farine", "sucre")),
                AisleDefinition("DP_2", "2", "Rayon 2", "Huiles, Conserves & Épices", "Huiles économiques, sardines, thon", 0.22f, 0.68f, sectionColor = 0xFFD97706, keywords = listOf("huile", "sardine", "thon", "conserve", "epice", "massala")),
                AisleDefinition("DP_3", "3", "Rayon 3", "Boissons & Thés", "Thé Bois Chéri, cafés, boissons, eau", 0.52f, 0.28f, sectionColor = 0xFF0284C7, keywords = listOf("the", "cafe", "eau", "boisson", "jus")),
                AisleDefinition("DP_4", "4", "Rayon 4", "Snacks & Biscuits", "Biscuits Manioc, gaufrettes, snacks", 0.52f, 0.68f, sectionColor = 0xFFEC4899, keywords = listOf("biscuit", "snack", "bonbon", "chocolat")),
                AisleDefinition("DP_5", "5", "Rayon 5", "Laiteries & Fromages", "Lait poudre Red Cow/Twin Cows, fromages", 0.78f, 0.28f, sectionColor = 0xFF3B82F6, keywords = listOf("lait", "red cow", "twin cows", "fromage", "beurre")),
                AisleDefinition("DP_6", "6", "Rayon 6", "Lessives & Nettoyants", "Lessives en poudre, savons, eau de javel", 0.78f, 0.68f, sectionColor = 0xFF14B8A6, keywords = listOf("lessive", "savon", "javel", "nettoyant"))
            )
        )
    }
}

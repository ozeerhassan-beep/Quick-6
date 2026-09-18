package com.example

import com.example.data.ProductEntity
import com.example.util.BarcodeLookupService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testBarcodeCatalogMatching() = runBlocking {
        val dummyCatalog = listOf(
            ProductEntity(
                id = 1,
                catalogType = "DREAMPRICE",
                name = "Nutella Pâte à tartiner",
                category = "Épicerie",
                brand = "Ferrero",
                unit = "400g",
                price = 189.0,
                cost = 150.0
            ),
            ProductEntity(
                id = 2,
                catalogType = "INTERMART",
                name = "Nutella Pâte à tartiner",
                category = "Épicerie",
                brand = "Ferrero",
                unit = "400g",
                price = 195.0,
                cost = 155.0
            ),
            ProductEntity(
                id = 3,
                catalogType = "DREAMPRICE",
                name = "Lait Candia Silhouette",
                category = "Frais",
                brand = "Candia",
                unit = "1L",
                price = 55.0,
                cost = 42.0
            )
        )

        // Lookup Nutella EAN 3017620422003
        val result = BarcodeLookupService.lookupBarcode("3017620422003", dummyCatalog)
        assertNotNull(result)
        assertTrue(result.isListedInCatalog)
        assertEquals(2, result.matchedProducts.size)
        assertTrue(result.onlineProduct.productName.contains("Nutella", ignoreCase = true))
    }
}

package com.despesas.gestor.data.ai

import com.despesas.gestor.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiResponseParserTest {

    @Test
    fun parse_readsItemsTotalAndCategory() {
        val json = """
            {
              "merchant": "Continente",
              "category": "supermercado",
              "total": 3.10,
              "date": "2026-08-14",
              "items": [
                {"name": "Leite", "price": 0.79, "quantity": 1},
                {"name": "Pão", "price": 2.31, "quantity": 2}
              ]
            }
        """.trimIndent()

        val r = GeminiResponseParser.parse(json)

        assertEquals("Continente", r.merchant)
        assertEquals(ExpenseCategory.SUPERMERCADO, r.category)
        assertEquals(3.10, r.total, 0.001)
        assertEquals(2, r.items.size)
        assertEquals("Leite", r.items[0].name)
        assertEquals(0.79, r.items[0].price, 0.001)
        assertEquals(2.0, r.items[1].quantity, 0.001)
    }

    @Test
    fun parse_stripsCodeFencesAndMapsRestaurant() {
        val json = """
            ```json
            {"merchant":"McDonald's","category":"restauracao","total":8.5,"items":[{"name":"Menu","price":8.5}]}
            ```
        """.trimIndent()

        val r = GeminiResponseParser.parse(json)

        assertEquals(ExpenseCategory.RESTAURACAO, r.category)
        assertEquals(8.5, r.total, 0.001)
        assertEquals("Menu", r.items[0].name)
        assertEquals(1.0, r.items[0].quantity, 0.001) // default quantity
    }

    @Test
    fun parse_fallsBackToItemsSumWhenTotalMissing() {
        val json = """
            {"merchant":"Cafe","category":"restauracao","items":[
              {"name":"Bica","price":0.70},{"name":"Tosta","price":2.50}
            ]}
        """.trimIndent()

        val r = GeminiResponseParser.parse(json)
        assertEquals(3.20, r.total, 0.001)
    }
}

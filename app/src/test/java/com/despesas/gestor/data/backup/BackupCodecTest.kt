package com.despesas.gestor.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {

    @Test
    fun encodeThenDecode_roundTrips() {
        val data = BackupData(
            version = 1,
            exportedAt = 1723200000000L,
            income = listOf(IncomeDto("2026-08", 1500.0)),
            receipts = listOf(
                ReceiptDto(1, "Continente", "supermercado", 12.34, 1723200000000L, "2026-08", null, "raw texto")
            ),
            items = listOf(
                ReceiptItemDto(1, 1, "Leite", 0.79, 1.0),
                ReceiptItemDto(2, 1, "Pão", 1.29, 2.0)
            ),
            fixed = listOf(
                FixedDto(1, "Luz", "EDP", 45.0, 1723200000000L, "2026-08", false, true)
            ),
            budgets = listOf(BudgetDto("supermercado", 250.0)),
            shoppingLists = listOf(ShoppingListDto(1, "Semana", 1723200000000L)),
            shoppingItems = listOf(ShoppingItemDto(1, 1, "Arroz", false))
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(data))

        assertEquals(data.version, decoded.version)
        assertEquals(data.income, decoded.income)
        assertEquals(data.receipts, decoded.receipts)
        assertEquals(data.items, decoded.items)
        assertEquals(data.fixed, decoded.fixed)
        assertEquals(data.budgets, decoded.budgets)
        assertEquals(data.shoppingLists, decoded.shoppingLists)
        assertEquals(data.shoppingItems, decoded.shoppingItems)
    }

    @Test
    fun decode_toleratesMissingOptionalFields() {
        val json = """
            {
              "version": 1,
              "receipts": [
                {"id":5,"merchant":"Loja","categoryId":"outros","total":3.0,"dateMillis":1,"monthKey":"2026-08"}
              ]
            }
        """.trimIndent()
        val decoded = BackupCodec.decode(json)
        assertEquals(1, decoded.receipts.size)
        assertEquals(null, decoded.receipts[0].imagePath)
        assertEquals("Loja", decoded.receipts[0].merchant)
    }
}

package com.despesas.gestor.data.ocr

import com.despesas.gestor.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do parser de faturas. Simulam a saída do OCR (linhas com posição) e
 * verificam a extração de itens, total, categoria e a interpretação de preços.
 */
class ReceiptParserTest {

    /** Ajuda a criar uma "linha" numa dada altura y, coluna x. */
    private fun line(text: String, y: Int, x: Int = 0) =
        OcrTextLine(text = text, left = x, top = y, right = x + 200, bottom = y + 20)

    @Test
    fun parseAmount_handlesEuropeanAndThousands() {
        assertEquals(1.99, ReceiptParser.parseAmount("1,99")!!, 0.001)
        assertEquals(12.50, ReceiptParser.parseAmount("12.50")!!, 0.001)
        assertEquals(1234.56, ReceiptParser.parseAmount("1.234,56")!!, 0.001)
        assertEquals(1234.56, ReceiptParser.parseAmount("1,234.56")!!, 0.001)
        assertEquals(0.89, ReceiptParser.parseAmount("0,89 €")!!, 0.001)
    }

    @Test
    fun parse_extractsItemsTotalAndCategory() {
        // Fatura simulada de supermercado (descrição e preço na mesma altura).
        val lines = listOf(
            line("Continente Modelo", y = 0),
            line("Rua das Flores, Lisboa", y = 30),
            line("Leite Meio Gordo", y = 100, x = 0),
            line("0,79", y = 100, x = 400),
            line("Pao de Forma", y = 130, x = 0),
            line("1,29", y = 130, x = 400),
            line("Bananas 1,2kg", y = 160, x = 0),
            line("2,15", y = 160, x = 400),
            line("TOTAL A PAGAR", y = 220, x = 0),
            line("4,23", y = 220, x = 400),
            line("Data: 08/08/2026", y = 260)
        )

        val result = ReceiptParser.parse(lines)

        assertEquals(ExpenseCategory.SUPERMERCADO, result.category)
        assertEquals(4.23, result.total, 0.001)
        assertEquals(3, result.items.size)
        assertEquals("Leite Meio Gordo", result.items[0].name)
        assertEquals(0.79, result.items[0].price, 0.001)
        // O total não deve ser tratado como item.
        assertTrue(result.items.none { it.name.contains("TOTAL", ignoreCase = true) })
    }

    @Test
    fun parse_classifiesFuelAsTransport() {
        val lines = listOf(
            line("GALP Energia", y = 0),
            line("Gasoleo Simples", y = 100, x = 0),
            line("65,00", y = 100, x = 400),
            line("TOTAL", y = 160, x = 0),
            line("65,00", y = 160, x = 400)
        )
        val result = ReceiptParser.parse(lines)
        assertEquals(ExpenseCategory.TRANSPORTES, result.category)
        assertEquals(65.00, result.total, 0.001)
    }

    @Test
    fun parse_ignoresDiscountLines() {
        val lines = listOf(
            line("Continente", y = 0),
            line("Iogurtes", y = 100, x = 0),
            line("2,49", y = 100, x = 400),
            line("Promocao", y = 130, x = 0),
            line("-0,50", y = 130, x = 400),
            line("TOTAL A PAGAR", y = 200, x = 0),
            line("1,99", y = 200, x = 400)
        )
        val result = ReceiptParser.parse(lines)
        // A linha de desconto (-0,50) não deve virar item.
        assertEquals(1, result.items.size)
        assertEquals("Iogurtes", result.items[0].name)
        assertEquals(1.99, result.total, 0.001)
    }

    @Test
    fun parse_readsWeightAsQuantity() {
        val lines = listOf(
            line("Frutaria", y = 0),
            line("Bananas 0,512 kg", y = 100, x = 0),
            line("1,02", y = 100, x = 400),
            line("TOTAL", y = 160, x = 0),
            line("1,02", y = 160, x = 400)
        )
        val result = ReceiptParser.parse(lines)
        assertEquals(1, result.items.size)
        assertEquals(1.02, result.items[0].price, 0.001)
        assertEquals(0.512, result.items[0].quantity, 0.0001)
    }

    @Test
    fun parse_fallsBackToItemsSumWhenNoTotal() {
        val lines = listOf(
            line("Cafe Central", y = 0),
            line("Bica", y = 100, x = 0),
            line("0,70", y = 100, x = 400),
            line("Tosta Mista", y = 130, x = 0),
            line("2,50", y = 130, x = 400)
        )
        val result = ReceiptParser.parse(lines)
        assertEquals(3.20, result.total, 0.001)
        assertEquals(ExpenseCategory.RESTAURACAO, result.category)
    }
}

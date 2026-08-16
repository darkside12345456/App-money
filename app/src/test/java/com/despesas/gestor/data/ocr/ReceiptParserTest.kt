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
            line("Galp", y = 0),
            line("Gasoleo Simples", y = 100, x = 0),
            line("65,00", y = 100, x = 400),
            line("TOTAL", y = 160, x = 0),
            line("65,00", y = 160, x = 400)
        )
        val result = ReceiptParser.parse(lines)
        assertEquals(ExpenseCategory.TRANSPORTES, result.category)
        assertEquals(65.00, result.total, 0.001)
    }

    private fun categoryOf(merchant: String, vararg items: String): ExpenseCategory {
        val lines = mutableListOf(line(merchant, y = 0))
        items.forEachIndexed { i, it -> lines.add(line(it, y = 100 + i * 30)) }
        return ReceiptParser.parse(lines).category
    }

    @Test
    fun classify_handlesManyReceiptTypes() {
        assertEquals(ExpenseCategory.SAUDE, categoryOf("Farmácia Central", "Ben-u-ron 1,99"))
        assertEquals(ExpenseCategory.VESTUARIO, categoryOf("ZARA", "Camisa 19,95"))
        assertEquals(ExpenseCategory.CASA, categoryOf("Worten", "Torradeira 24,99"))
        assertEquals(ExpenseCategory.LAZER, categoryOf("Netflix", "Subscricao 13,49"))
        assertEquals(ExpenseCategory.CONTAS, categoryOf("EDP Comercial", "Eletricidade 42,10"))
        assertEquals(ExpenseCategory.CONTAS, categoryOf("MEO", "Fibra e TV 39,99"))
        assertEquals(ExpenseCategory.TRANSPORTES, categoryOf("Via Verde", "Portagem 2,45"))
        // Comprar água no supermercado não deve virar "Contas".
        assertEquals(
            ExpenseCategory.SUPERMERCADO,
            categoryOf("Continente", "Agua 1,5L 0,45", "Agua com gas 0,59")
        )
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
    fun parse_readsTotalOnSeparateLine() {
        // "TOTAL A PAGAR" numa linha e o montante na linha imediatamente abaixo.
        val lines = listOf(
            line("Padaria", y = 0),
            line("Cafe", y = 100, x = 0),
            line("0,60", y = 100, x = 400),
            line("Bolo", y = 130, x = 0),
            line("0,95", y = 130, x = 400),
            line("TOTAL A PAGAR", y = 200, x = 0),
            line("1,60", y = 240, x = 400)
        )
        val result = ReceiptParser.parse(lines)
        // 1,60 vem da linha seguinte ao rótulo (não da soma dos itens, que é 1,55).
        assertEquals(1.60, result.total, 0.001)
        assertEquals(2, result.items.size)
    }

    @Test
    fun parse_keepsProductThatContainsKeywordSubstring() {
        // "Activia" contém "iva" mas não deve ser filtrado como linha de IVA.
        val lines = listOf(
            line("Pingo Doce", y = 0),
            line("Activia Natural", y = 100, x = 0),
            line("1,99", y = 100, x = 400),
            line("TOTAL", y = 160, x = 0),
            line("1,99", y = 160, x = 400)
        )
        val result = ReceiptParser.parse(lines)
        assertEquals(1, result.items.size)
        assertEquals("Activia Natural", result.items[0].name)
    }

    @Test
    fun parse_cleansRestaurantStyleLinesAndReadsGrandTotal() {
        // Formato tipo McDonald's: "QTD DESC  UNID IVA%  TOTAL", com o total a
        // pagar em "TOTAL LEVAR (incl IVA)" e linhas de IVA a seguir.
        val lines = listOf(
            line("McDonald's Evora", y = 0),
            line("1 M Philly Doubl 8.47 13% 8.47", y = 100),
            line("1 Coca-Cola Mn 1.28 23% 1.28", y = 130),
            line("1 Molho Agrido 0.90 13% 0.00", y = 160),
            line("Total Liquido: 25.12", y = 200),
            line("Total IVA Incluido 13.00% 3.00", y = 230),
            line("TOTAL LEVAR Total (incl IVA)", y = 260),
            line("28.60", y = 290, x = 400)
        )
        val result = ReceiptParser.parse(lines)

        assertEquals(ExpenseCategory.RESTAURACAO, result.category)
        // O total a pagar (28,60), não o líquido (25,12) nem o IVA.
        assertEquals(28.60, result.total, 0.001)
        // Nome sem o preço unitário nem a taxa de IVA, e sem a quantidade inicial.
        assertEquals("M Philly Doubl", result.items[0].name)
        assertEquals(8.47, result.items[0].price, 0.001)
        assertEquals("Coca-Cola Mn", result.items[1].name)
        // Extra grátis (0,00) também é incluído.
        assertTrue(result.items.any { it.name.contains("Molho") && it.price == 0.0 })
    }

    @Test
    fun parse_skipsSectionHeadersAndStripsVatPrefix() {
        // Estilo Pingo Doce: cabeçalhos de secção terminados em ":" e itens
        // prefixados com a taxa de IVA "(A)"/"(B)"/"(C)".
        val lines = listOf(
            line("Pingo Doce", y = 0),
            line("Charcutaria/Queijos: 0,74", y = 100),
            line("(A) ARROZ BASMATI 1KG 2,98", y = 130),
            line("(B) LEITE UHT 1L 1,12", y = 160),
            line("TOTAL A PAGAR 42,33", y = 220)
        )
        val result = ReceiptParser.parse(lines)

        assertEquals(ExpenseCategory.SUPERMERCADO, result.category)
        assertEquals(42.33, result.total, 0.001)
        // O cabeçalho "Charcutaria/Queijos:" não é item; ficam 2 itens.
        assertEquals(2, result.items.size)
        assertEquals("ARROZ BASMATI 1KG", result.items[0].name)
        assertEquals("LEITE UHT 1L", result.items[1].name)
        // O comerciante é a loja, não uma linha de produto.
        assertEquals("Pingo Doce", result.merchant)
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

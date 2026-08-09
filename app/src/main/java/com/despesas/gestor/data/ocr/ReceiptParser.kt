package com.despesas.gestor.data.ocr

import com.despesas.gestor.domain.model.ExpenseCategory
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Um item extraído de uma fatura. */
data class ParsedItem(
    val name: String,
    val price: Double,
    val quantity: Double = 1.0
)

/** Resultado completo da leitura automática de uma fatura. */
data class ParsedReceipt(
    val merchant: String,
    val category: ExpenseCategory,
    val total: Double,
    val dateMillis: Long,
    val items: List<ParsedItem>,
    val rawText: String
)

/**
 * Transforma as linhas de texto devolvidas pelo OCR numa fatura estruturada:
 * comerciante, categoria, itens individuais, total e data — tudo de forma
 * automática, sem introdução manual.
 *
 * A estratégia:
 *  1. Agrupar fragmentos de texto que estão à mesma altura numa "linha visual"
 *     (o preço costuma ficar numa coluna à direita da descrição).
 *  2. Detetar o total procurando linhas com "TOTAL".
 *  3. Detetar itens: linhas com uma descrição seguida de um preço no fim.
 *  4. Classificar a categoria por palavras-chave.
 *  5. Detetar a data por expressões regulares.
 */
object ReceiptParser {

    // Preço no formato europeu: opcionais milhares + 2 casas decimais.
    // Ex.: "1,99"  "12,50"  "1.234,56"  "3.49"
    private val PRICE = Regex("""(?:\d{1,3}(?:[.,]\d{3})*|\d+)[.,]\d{2}""")

    private val QUANTITY = Regex("""(?i)(\d+(?:[.,]\d+)?)\s*[xX*]\s*""")

    // Peso de um item vendido a granel, ex.: "0,512 kg".
    private val WEIGHT = Regex("""(?i)(\d+(?:[.,]\d+)?)\s*kg\b""")

    // Percentagem de IVA, ex.: "13%", "23,00%".
    private val PERCENT = Regex("""\d+(?:[.,]\d+)?\s*%""")

    // Quantidade no início da linha, ex.: "1 " em "1 Batata Média".
    private val LEADING_QTY = Regex("""^(\d{1,2})\s+""")

    // "Ruído" no fim da descrição: preço unitário e taxa de IVA, ex.: "8.47 13%".
    private val TRAILING_NOISE = Regex("""[\s]*(?:\d+(?:[.,]\d+)?\s*%|\d{1,3}[.,]\d{2})$""")

    private val DATE_PATTERNS = listOf(
        Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})""") to "dmy4",
        Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2})\b""") to "dmy2",
        Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""") to "ymd"
    )

    // Palavras que indicam que uma linha NÃO é um item comprado.
    private val NON_ITEM = listOf(
        "total", "subtotal", "sub-total", "iva", "troco", "troca", "nif",
        "n.i.f", "contribuinte", "n.c", "n. contribuinte", "cartao", "cartão",
        "dinheiro", "numerario", "numerário", "multibanco", " mb ", "cash",
        "obrigado", "obrigada", "volte sempre", "fatura", "factura", "recibo",
        "documento", "doc.", "operador", "caixa", "loja", "data", "hora",
        "atendimento", "cliente", "morada", "telefone", "tel.", "email",
        "www", "http", "taxa", "s/iva", "c/iva", "euros", "eur ", "valor",
        "desconto", "poupanca", "poupança", "pontos", "saldo", "a pagar",
        "pagamento", "capital social", "sede", "matriz", "linha", "artigos",
        "qtd", "artigo", "descricao", "descrição", "preco", "preço", "p.unit",
        "arred", "arredond"
    )

    fun parse(lines: List<OcrTextLine>): ParsedReceipt {
        val rows = groupIntoRows(lines)
        val rowTexts = rows.map { it.trim() }.filter { it.isNotEmpty() }
        val rawText = rowTexts.joinToString("\n")

        val merchant = detectMerchant(rowTexts)
        val category = classify(rawText, merchant)
        val items = detectItems(rowTexts)
        val total = detectTotal(rowTexts) ?: items.sumOf { it.price }.takeIf { it > 0.0 } ?: 0.0
        val dateMillis = detectDate(rawText)

        return ParsedReceipt(
            merchant = merchant,
            category = category,
            total = total,
            dateMillis = dateMillis,
            items = items,
            rawText = rawText
        )
    }

    // --- Agrupamento geométrico em linhas visuais ------------------------------

    /**
     * Junta fragmentos que estão sensivelmente à mesma altura vertical numa só
     * linha, ordenando-os da esquerda para a direita. Assim a descrição de um
     * item e o seu preço (muitas vezes em colunas separadas) ficam juntos.
     */
    private fun groupIntoRows(lines: List<OcrTextLine>): List<String> {
        if (lines.isEmpty()) return emptyList()
        // Ordena de cima para baixo; dentro de cada faixa, da esquerda para a direita.
        val sorted = lines.sortedBy { it.top }
        val rows = mutableListOf<MutableList<OcrTextLine>>()

        for (line in sorted) {
            val row = rows.lastOrNull()
            if (row != null && verticallyAligned(row, line)) {
                row.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        return rows.map { row ->
            row.sortedBy { it.left }.joinToString("  ") { it.text.trim() }
        }
    }

    /**
     * Dois fragmentos pertencem à mesma linha visual se as suas caixas se
     * sobrepõem verticalmente numa fração significativa — mais robusto do que
     * comparar centros, porque a descrição e o preço podem ter alturas diferentes.
     */
    private fun verticallyAligned(row: List<OcrTextLine>, line: OcrTextLine): Boolean {
        val top = row.minOf { it.top }
        val bottom = row.maxOf { it.bottom }
        val overlap = minOf(bottom, line.bottom) - maxOf(top, line.top)
        val minHeight = minOf(bottom - top, line.height).coerceAtLeast(1)
        return overlap > 0.5 * minHeight
    }

    // --- Comerciante -----------------------------------------------------------

    private fun detectMerchant(rows: List<String>): String {
        // Procura nas primeiras linhas a que tem mais letras e não é um cabeçalho
        // fiscal (contribuinte, morada, etc.) nem contém um preço.
        val candidates = rows.take(6).filter { row ->
            val lower = row.lowercase()
            val letters = row.count { it.isLetter() }
            letters >= 3 &&
                !PRICE.containsMatchIn(row) &&
                !isNonItem(lower) &&
                !lower.contains("contribuinte")
        }
        return candidates.maxByOrNull { it.count { c -> c.isLetter() } }
            ?.trim()
            ?.take(60)
            ?: "Fatura"
    }

    // --- Total -----------------------------------------------------------------

    private fun detectTotal(rows: List<String>): Double? {
        // O total a pagar costuma ser "TOTAL A PAGAR" / "TOTAL LEVAR" / "TOTAL
        // (incl IVA)". As linhas de repartição do IVA ("Total IVA 13,00%") e o
        // subtotal/líquido NÃO são o total — reconhecem-se por trazerem uma
        // percentagem ou as palavras "líquido"/"subtotal".
        fun score(row: String): Int {
            val u = row.uppercase()
            var s = 0
            if (u.contains("A PAGAR")) s += 100
            if (u.contains("TOTAL LEVAR") || u.contains("INCL IVA") ||
                u.contains("INCL. IVA") || u.contains("A PAGAR")
            ) s += 50
            if (u.contains("TOTAL EUR") || u.contains("TOTAL €")) s += 30
            if (u.contains("TOTAL")) s += 10
            // Linhas de imposto/subtotal: têm percentagem ou "líquido".
            if (PERCENT.containsMatchIn(u)) s -= 80
            if (u.contains("LIQUIDO") || u.contains("LÍQUIDO")) s -= 40
            if (u.contains("SUBTOTAL") || u.contains("SUB-TOTAL")) s -= 50
            return s
        }

        val totalIndices = rows.indices.filter { rows[it].uppercase().contains("TOTAL") }
        if (totalIndices.isEmpty()) return null
        val bestIndex = totalIndices.maxByOrNull { score(rows[it]) } ?: return null

        // O valor pode estar na mesma linha ou na linha imediatamente a seguir
        // (quando o rótulo "TOTAL" e o montante ficam em linhas separadas).
        lastPriceIn(rows[bestIndex])?.let { return it }
        if (bestIndex + 1 < rows.size) lastPriceIn(rows[bestIndex + 1])?.let { return it }
        return null
    }

    // --- Itens -----------------------------------------------------------------

    /**
     * Verdadeiro se a linha for claramente cabeçalho/rodapé e não um item
     * comprado. Palavras curtas exigem fronteiras de palavra para não apanharem
     * produtos por acaso (ex.: "iva" dentro de "Activia").
     */
    private fun isNonItem(lowerRow: String): Boolean = NON_ITEM.any { kw ->
        if (kw.any { !it.isLetter() && it != ' ' } || kw.contains(' ')) {
            lowerRow.contains(kw)
        } else {
            Regex("""(?<![\p{L}])${Regex.escape(kw)}(?![\p{L}])""").containsMatchIn(lowerRow)
        }
    }

    private fun detectItems(rows: List<String>): List<ParsedItem> {
        val items = mutableListOf<ParsedItem>()
        for (row in rows) {
            val lower = row.lowercase()
            if (isNonItem(lower)) continue

            val price = lastPriceIn(row) ?: continue
            // Aceita 0,00 (ex.: molhos/extras grátis), mas rejeita negativos.
            if (price < 0.0) continue

            // Descrição = tudo antes do último preço.
            val match = PRICE.findAll(row).lastOrNull() ?: continue

            // Linha de desconto/estorno (preço negativo): não é um item comprado.
            val beforePrice = row.substring(0, match.range.first).trimEnd()
            if (beforePrice.endsWith("-")) continue

            var description = beforePrice.trim().trim('-', '*', ':', '.', ' ')

            // Remove o "ruído" no fim: preço unitário + taxa de IVA
            // (ex.: "1 M Philly Doubl 8.47 13%" -> "1 M Philly Doubl").
            var previous: String
            do {
                previous = description
                description = TRAILING_NOISE.replace(description, "").trim()
            } while (description != previous)

            // Quantidade: "2 x 0,99" ou número no início da linha ("1 Batata").
            var quantity = 1.0
            val qMatch = QUANTITY.find(description)
            if (qMatch != null) {
                quantity = parseAmount(qMatch.groupValues[1]) ?: 1.0
                description = description.substring(qMatch.range.last + 1).trim()
            } else {
                val lead = LEADING_QTY.find(description)
                if (lead != null) {
                    quantity = lead.groupValues[1].toDoubleOrNull() ?: 1.0
                    description = description.substring(lead.range.last + 1).trim()
                }
            }

            // Itens vendidos ao peso ("Bananas 0,512 kg").
            val wMatch = WEIGHT.find(description)
            if (wMatch != null && quantity == 1.0) {
                quantity = parseAmount(wMatch.groupValues[1]) ?: 1.0
            }

            // Uma descrição válida tem de ter texto real.
            if (description.count { it.isLetter() } < 2) continue
            if (description.length > 60) description = description.take(60)

            items.add(ParsedItem(name = description, price = price, quantity = quantity))
        }
        return items
    }

    // --- Categoria -------------------------------------------------------------

    private fun classify(rawText: String, merchant: String): ExpenseCategory {
        val haystack = (merchant + "\n" + rawText).lowercase()
        for (category in ExpenseCategory.entries) {
            if (category == ExpenseCategory.OUTROS) continue
            if (category.keywords.any { keyword -> matchesKeyword(haystack, keyword) }) {
                return category
            }
        }
        return ExpenseCategory.OUTROS
    }

    /** Palavras curtas exigem fronteiras de palavra para evitar falsos positivos. */
    private fun matchesKeyword(haystack: String, keyword: String): Boolean {
        return if (keyword.length <= 4) {
            Regex("""(?<![\p{L}])${Regex.escape(keyword)}(?![\p{L}])""").containsMatchIn(haystack)
        } else {
            haystack.contains(keyword)
        }
    }

    // --- Data ------------------------------------------------------------------

    private fun detectDate(rawText: String): Long {
        val now = System.currentTimeMillis()
        for ((regex, kind) in DATE_PATTERNS) {
            val m = regex.find(rawText) ?: continue
            try {
                val date = when (kind) {
                    "dmy4" -> LocalDate.of(
                        m.groupValues[3].toInt(),
                        m.groupValues[2].toInt(),
                        m.groupValues[1].toInt()
                    )
                    "dmy2" -> LocalDate.of(
                        2000 + m.groupValues[3].toInt(),
                        m.groupValues[2].toInt(),
                        m.groupValues[1].toInt()
                    )
                    else -> LocalDate.of(
                        m.groupValues[1].toInt(),
                        m.groupValues[2].toInt(),
                        m.groupValues[3].toInt()
                    )
                }
                // Ignora datas absurdas (OCR trocado); só aceita datas plausíveis.
                if (date.year in 2000..2100) {
                    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            } catch (_: Exception) {
                // padrão inválido, tenta o próximo
            }
        }
        return now
    }

    // --- Utilidades de preço ---------------------------------------------------

    private fun lastPriceIn(row: String): Double? {
        val match = PRICE.findAll(row).lastOrNull() ?: return null
        return parseAmount(match.value)
    }

    /**
     * Converte um número escrito em formato europeu ou anglo-saxónico para Double.
     * Lida com "1.234,56", "1,99", "12.50" e "1234,56".
     */
    fun parseAmount(token: String): Double? {
        var t = token.trim().replace(" ", "").replace("€", "")
        val hasComma = t.contains(',')
        val hasDot = t.contains('.')
        t = when {
            hasComma && hasDot -> {
                val decimalIsComma = t.lastIndexOf(',') > t.lastIndexOf('.')
                if (decimalIsComma) t.replace(".", "").replace(',', '.')
                else t.replace(",", "")
            }
            hasComma -> t.replace(',', '.')
            else -> t
        }
        return t.toDoubleOrNull()
    }

    // Formatador reservado para uso futuro/testes de data.
    @Suppress("unused")
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
}

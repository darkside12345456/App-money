package com.despesas.gestor.data.ai

import com.despesas.gestor.data.ocr.ParsedItem
import com.despesas.gestor.data.ocr.ParsedReceipt
import com.despesas.gestor.domain.model.ExpenseCategory
import com.despesas.gestor.util.Dates
import org.json.JSONObject
import java.time.LocalDate

/**
 * Converte o JSON devolvido pelo modelo (Gemini) numa [ParsedReceipt].
 *
 * É código puro (sem rede nem Android), pelo que é testável em testes unitários
 * de JVM — a parte mais fácil de errar é esta, por isso fica isolada.
 *
 * Formato esperado do JSON do modelo:
 * ```
 * {
 *   "merchant": "Continente",
 *   "category": "supermercado",
 *   "total": 42.33,
 *   "date": "2026-08-14",
 *   "items": [ { "name": "Leite", "price": 0.79, "quantity": 1 } ]
 * }
 * ```
 */
object GeminiResponseParser {

    fun parse(modelJson: String): ParsedReceipt {
        val clean = stripCodeFences(modelJson)
        val root = JSONObject(clean)

        val merchant = root.optString("merchant").ifBlank { "Fatura" }
        val category = ExpenseCategory.fromId(root.optString("category"))
        val total = root.optDouble("total", 0.0)
        val dateMillis = parseDate(root.optString("date"))

        val itemsJson = root.optJSONArray("items")
        val items = buildList {
            if (itemsJson != null) {
                for (i in 0 until itemsJson.length()) {
                    val o = itemsJson.optJSONObject(i) ?: continue
                    val name = o.optString("name").trim()
                    if (name.isEmpty()) continue
                    val price = o.optDouble("price", 0.0)
                    val quantity = o.optDouble("quantity", 1.0).takeIf { it > 0 } ?: 1.0
                    add(ParsedItem(name = name, price = price, quantity = quantity))
                }
            }
        }

        val effectiveTotal = if (total > 0.0) total
        else items.sumOf { it.price }.takeIf { it > 0.0 } ?: 0.0

        return ParsedReceipt(
            merchant = merchant,
            category = category,
            total = effectiveTotal,
            dateMillis = dateMillis,
            items = items,
            rawText = clean
        )
    }

    /** Remove cercas de código ```json ... ``` caso o modelo as inclua. */
    private fun stripCodeFences(text: String): String {
        var t = text.trim()
        if (t.startsWith("```")) {
            t = t.substringAfter('\n', t).substringBeforeLast("```").trim()
        }
        return t
    }

    private fun parseDate(date: String?): Long {
        if (date.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching {
            Dates.startOfDayMillis(LocalDate.parse(date.trim()))
        }.getOrDefault(System.currentTimeMillis())
    }
}

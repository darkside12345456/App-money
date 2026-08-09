package com.despesas.gestor.data.backup

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converte uma [BackupData] de/para JSON. Usa a API `org.json` (disponível no
 * Android e, em testes, através do artefacto `org.json:json`), pelo que o
 * codec é testável fora do Android.
 */
object BackupCodec {

    const val CURRENT_VERSION = 1

    fun encode(data: BackupData): String {
        val root = JSONObject()
        root.put("version", data.version)
        root.put("exportedAt", data.exportedAt)

        root.put("income", JSONArray().apply {
            data.income.forEach { put(JSONObject().put("monthKey", it.monthKey).put("amount", it.amount)) }
        })

        root.put("receipts", JSONArray().apply {
            data.receipts.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("merchant", it.merchant)
                        .put("categoryId", it.categoryId)
                        .put("total", it.total)
                        .put("dateMillis", it.dateMillis)
                        .put("monthKey", it.monthKey)
                        .putOpt("imagePath", it.imagePath)
                        .putOpt("rawText", it.rawText)
                )
            }
        })

        root.put("items", JSONArray().apply {
            data.items.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("receiptId", it.receiptId)
                        .put("name", it.name)
                        .put("price", it.price)
                        .put("quantity", it.quantity)
                )
            }
        })

        root.put("fixed", JSONArray().apply {
            data.fixed.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .putOpt("provider", it.provider)
                        .put("amount", it.amount)
                        .put("dateMillis", it.dateMillis)
                        .put("monthKey", it.monthKey)
                        .put("paid", it.paid)
                        .put("recurring", it.recurring)
                )
            }
        })

        root.put("budgets", JSONArray().apply {
            data.budgets.forEach { put(JSONObject().put("categoryId", it.categoryId).put("amount", it.amount)) }
        })

        root.put("shoppingLists", JSONArray().apply {
            data.shoppingLists.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("createdAtMillis", it.createdAtMillis)
                )
            }
        })

        root.put("shoppingItems", JSONArray().apply {
            data.shoppingItems.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("listId", it.listId)
                        .put("name", it.name)
                        .put("checked", it.checked)
                )
            }
        })

        return root.toString(2)
    }

    fun decode(json: String): BackupData {
        val root = JSONObject(json)
        return BackupData(
            version = root.optInt("version", 1),
            exportedAt = root.optLong("exportedAt", 0L),
            income = root.optJSONArray("income").map {
                IncomeDto(it.getString("monthKey"), it.getDouble("amount"))
            },
            receipts = root.optJSONArray("receipts").map {
                ReceiptDto(
                    id = it.getLong("id"),
                    merchant = it.getString("merchant"),
                    categoryId = it.getString("categoryId"),
                    total = it.getDouble("total"),
                    dateMillis = it.getLong("dateMillis"),
                    monthKey = it.getString("monthKey"),
                    imagePath = it.optStringOrNull("imagePath"),
                    rawText = it.optStringOrNull("rawText")
                )
            },
            items = root.optJSONArray("items").map {
                ReceiptItemDto(
                    id = it.getLong("id"),
                    receiptId = it.getLong("receiptId"),
                    name = it.getString("name"),
                    price = it.getDouble("price"),
                    quantity = it.optDouble("quantity", 1.0)
                )
            },
            fixed = root.optJSONArray("fixed").map {
                FixedDto(
                    id = it.getLong("id"),
                    name = it.getString("name"),
                    provider = it.optStringOrNull("provider"),
                    amount = it.getDouble("amount"),
                    dateMillis = it.getLong("dateMillis"),
                    monthKey = it.getString("monthKey"),
                    paid = it.optBoolean("paid", false),
                    recurring = it.optBoolean("recurring", false)
                )
            },
            budgets = root.optJSONArray("budgets").map {
                BudgetDto(it.getString("categoryId"), it.getDouble("amount"))
            },
            shoppingLists = root.optJSONArray("shoppingLists").map {
                ShoppingListDto(it.getLong("id"), it.getString("name"), it.getLong("createdAtMillis"))
            },
            shoppingItems = root.optJSONArray("shoppingItems").map {
                ShoppingItemDto(
                    id = it.getLong("id"),
                    listId = it.getLong("listId"),
                    name = it.getString("name"),
                    checked = it.optBoolean("checked", false)
                )
            }
        )
    }

    // --- Auxiliares ------------------------------------------------------------

    private inline fun <T> JSONArray?.map(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) out.add(transform(getJSONObject(i)))
        return out
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key)
}

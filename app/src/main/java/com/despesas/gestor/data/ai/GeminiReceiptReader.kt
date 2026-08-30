package com.despesas.gestor.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.despesas.gestor.data.ocr.ParsedReceipt
import com.despesas.gestor.util.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Leitura de faturas com IA (Gemini), opcional. É usada **apenas** para ler a
 * fatura: envia a foto ao modelo e recebe os itens, o total e a categoria já
 * estruturados. Fica desligada por defeito; quando está desligada ou falha,
 * devolve `null` para que a app volte à leitura local (ML Kit).
 *
 * Quando ligada, a foto é enviada para a API do Gemini (nuvem da Google).
 */
class GeminiReceiptReader(
    private val context: Context,
    private val prefs: AppPrefs
) {
    /** Devolve a fatura lida por IA, ou null se desligada/sem chave/erro. */
    suspend fun read(uri: Uri): ParsedReceipt? {
        if (!prefs.aiReceiptEnabled) return null
        val key = prefs.geminiApiKey?.trim().orEmpty()
        if (key.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            runCatching {
                val base64 = encodeImage(uri) ?: return@runCatching null
                val responseText = callGemini(key, base64)
                val inner = extractText(responseText) ?: return@runCatching null
                GeminiResponseParser.parse(inner)
            }.getOrNull()
        }
    }

    // --- Imagem ----------------------------------------------------------------

    private fun encodeImage(uri: Uri): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (maxDim / sample > 1280) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap: Bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // --- Chamada à API ---------------------------------------------------------

    private fun callGemini(apiKey: String, imageBase64: String): String {
        val url = URL("$ENDPOINT?key=$apiKey")
        val body = buildRequestBody(imageBase64)

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 40_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        conn.outputStream.use { it.write(body.toByteArray()) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) error("Gemini HTTP $code: ${text.take(300)}")
        return text
    }

    private fun buildRequestBody(imageBase64: String): String {
        val parts = JSONArray()
            .put(JSONObject().put("text", PROMPT))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject().put("mime_type", "image/jpeg").put("data", imageBase64)
                )
            )
        val contents = JSONArray().put(JSONObject().put("parts", parts))
        val generationConfig = JSONObject()
            .put("responseMimeType", "application/json")
            .put("temperature", 0)
        return JSONObject()
            .put("contents", contents)
            .put("generationConfig", generationConfig)
            .toString()
    }

    /** Extrai o texto (JSON da fatura) da resposta da API. */
    private fun extractText(apiResponse: String): String? {
        val root = JSONObject(apiResponse)
        val candidates = root.optJSONArray("candidates") ?: return null
        val first = candidates.optJSONObject(0) ?: return null
        val content = first.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.optJSONObject(i)?.optString("text").orEmpty())
        }
        return sb.toString().ifBlank { null }
    }

    companion object {
        private const val MODEL = "gemini-2.0-flash"
        private const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

        private val PROMPT = """
            És um leitor de faturas/talões portugueses. Analisa a imagem e devolve
            APENAS um objeto JSON (sem texto à volta) com este formato:
            {
              "merchant": "nome da loja",
              "category": "uma de: supermercado, restauracao, transportes, saude, vestuario, casa, lazer, contas",
              "total": número (total a pagar, em euros, ponto decimal),
              "date": "AAAA-MM-DD ou null",
              "items": [ { "name": "descrição do artigo", "price": número, "quantity": número } ]
            }
            Regras:
            - Inclui CADA artigo comprado, com o preço da linha (não o unitário se diferir).
            - Não incluas descontos, IVA, subtotais nem o total como artigos.
            - Usa ponto como separador decimal. Se não conseguires um valor, usa 0.
            - Escolhe a categoria mais adequada da lista.
        """.trimIndent()
    }
}

package com.despesas.gestor.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Uma linha de texto detetada pelo OCR, com a sua caixa delimitadora expressa
 * em coordenadas simples (sem depender de android.graphics.Rect), o que torna
 * o parser testável em testes unitários de JVM.
 */
data class OcrTextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = (bottom - top)
}

/**
 * Serviço de reconhecimento de texto (OCR) 100% no dispositivo, usando o
 * ML Kit da Google. Não há qualquer chamada à nuvem — o modelo corre
 * localmente, o que respeita o requisito de "dados só no telemóvel".
 */
class OcrService(private val context: Context) {

    private val recognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Corre o OCR sobre a imagem em [uri] e devolve as linhas detetadas. */
    suspend fun recognize(uri: Uri): List<OcrTextLine> {
        // Pré-processa a imagem (rotação EXIF + escala de cinzentos e contraste)
        // para melhorar a fiabilidade do reconhecimento em faturas.
        val bitmap = withContext(Dispatchers.IO) { preprocess(uri) }
        val image = if (bitmap != null) {
            InputImage.fromBitmap(bitmap, 0)
        } else {
            InputImage.fromFilePath(context, uri)
        }
        return recognize(image)
    }

    /**
     * Carrega a imagem com uma resolução razoável, corrige a orientação segundo
     * os metadados EXIF e aplica escala de cinzentos com mais contraste — o que
     * ajuda o OCR a distinguir o texto do talão.
     */
    private fun preprocess(uri: Uri): Bitmap? = runCatching {
        // 1) Amostragem para limitar a memória (~2000 px no lado maior).
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (maxDim / sample > 2000) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        // 2) Rotação segundo EXIF.
        val rotation = context.contentResolver.openInputStream(uri)?.use { input ->
            when (ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val rotated = if (rotation != 0f) {
            val m = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        } else decoded

        // 3) Escala de cinzentos + aumento de contraste.
        val output = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val grayscale = ColorMatrix().apply { setSaturation(0f) }
        val contrast = 1.4f
        val translate = (-.5f * contrast + .5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        grayscale.postConcat(contrastMatrix)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(grayscale) }
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        output
    }.getOrNull()

    private suspend fun recognize(image: InputImage): List<OcrTextLine> =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val lines = buildList {
                        for (block in result.textBlocks) {
                            for (line in block.lines) {
                                val box = line.boundingBox
                                if (box != null && line.text.isNotBlank()) {
                                    add(
                                        OcrTextLine(
                                            text = line.text,
                                            left = box.left,
                                            top = box.top,
                                            right = box.right,
                                            bottom = box.bottom
                                        )
                                    )
                                }
                            }
                        }
                    }
                    cont.resume(lines)
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}

package com.despesas.gestor.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
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
        val image = InputImage.fromFilePath(context, uri)
        return recognize(image)
    }

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

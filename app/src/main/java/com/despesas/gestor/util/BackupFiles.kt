package com.despesas.gestor.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Escreve a cópia de segurança num ficheiro temporário partilhável (cache) e
 * devolve um Uri via FileProvider, para poder ser enviado por WhatsApp/email
 * com o menu de partilha do Android. Continua tudo local — é o próprio
 * utilizador a escolher para onde envia.
 */
object BackupFiles {

    fun writeShareableBackup(context: Context, json: String): Uri {
        val dir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val file = File(dir, "despesas-backup.json")
        file.writeText(json)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}

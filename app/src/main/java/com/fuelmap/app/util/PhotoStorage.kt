package com.fuelmap.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/** Копирует выбранное изображение во внутреннее хранилище и возвращает путь к файлу. */
object PhotoStorage {
    fun copyToInternal(context: Context, uri: Uri): String? = runCatching {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "mark_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file.absolutePath
    }.getOrNull()
}

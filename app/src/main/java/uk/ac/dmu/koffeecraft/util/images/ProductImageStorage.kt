package uk.ac.dmu.koffeecraft.util.images

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ProductImageStorage {

    private const val DIRECTORY_NAME = "product_images"

    fun copyPickedImageToAppStorage(context: Context, sourceUri: Uri): String? {
        val imagesDir = File(context.filesDir, DIRECTORY_NAME)
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val extension = context.contentResolver.getType(sourceUri)
            ?.substringAfterLast('/', "jpg")
            ?.ifBlank { "jpg" }
            ?: "jpg"

        val targetFile = File(imagesDir, "product_${UUID.randomUUID()}.$extension")

        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFileAtPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false

        return runCatching {
            val file = File(path)
            file.exists() && file.delete()
        }.getOrDefault(false)
    }
}
package uk.ac.dmu.koffeecraft.util.images

import android.graphics.BitmapFactory
import android.widget.ImageView
import java.io.File

object ProductImageLoader {

    fun load(
        imageView: ImageView,
        productFamily: String,
        rewardEnabled: Boolean,
        imageKey: String?,
        customImagePath: String?
    ) {
        val customFile = customImagePath
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.exists() }

        if (customFile != null) {
            val targetWidth = imageView.width.takeIf { it > 0 } ?: 320
            val targetHeight = imageView.height.takeIf { it > 0 } ?: 320
            val bitmap = decodeSampledBitmap(customFile.absolutePath, targetWidth, targetHeight)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                return
            }
        }

        val drawableResId = ProductImageCatalog.drawableForKey(imageKey)
            ?: ProductImageCatalog.fallbackDrawable(productFamily, rewardEnabled)
        imageView.setImageResource(drawableResId)
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int) = runCatching {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, bounds)

        val sampleOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds, reqWidth, reqHeight)
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }

        BitmapFactory.decodeFile(path, sampleOptions)
    }.getOrNull()

    private fun calculateSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var sampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            while ((height / sampleSize) >= reqHeight * 2 && (width / sampleSize) >= reqWidth * 2) {
                sampleSize *= 2
            }
        }

        return sampleSize.coerceAtLeast(1)
    }
}
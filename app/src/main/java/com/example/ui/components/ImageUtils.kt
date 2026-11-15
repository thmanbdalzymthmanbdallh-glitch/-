package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

suspend fun compressImageUriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val maxDim = 480
        var sampleSize = 1
        val largest = max(boundsOpts.outWidth, boundsOpts.outHeight)
        while (largest / sampleSize > maxDim * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@runCatching null

        val scale = maxDim.toFloat() / max(decoded.width, decoded.height).coerceAtLeast(1)
        val scaled = if (scale < 1f) {
            val w = (decoded.width * scale).roundToInt().coerceAtLeast(1)
            val h = (decoded.height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, w, h, true).also {
                if (it != decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        val output = ByteArrayOutputStream()
        var quality = 78
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        while (output.size() > 110_000 && quality > 35) {
            output.reset()
            quality -= 12
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }
        scaled.recycle()

        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }.getOrNull()
}

fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
    if (base64.isNullOrBlank()) return null
    return runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

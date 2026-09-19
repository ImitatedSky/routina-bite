package com.routina.bite.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 紀錄照片的檔案層：存在 App 私有目錄，一張一個檔，紀錄只記檔名。
 *
 * 這裡的函式都會做檔案 I/O 與圖片解碼，一律在 IO 執行緒呼叫。
 */

/** 長邊上限。1600px 壓出來一張大約 150–400 KB，全螢幕看仍然清楚 */
private const val MAX_EDGE = 1600
private const val QUALITY = 85
private const val DIR = "photos"

fun photosDir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

/** 檔名為空或檔案不在就回 null——那一筆就當作沒有照片 */
fun photoFile(context: Context, name: String): File? {
    if (name.isEmpty()) return null
    return File(photosDir(context), name).takeIf { it.isFile }
}

/**
 * 把選擇器給的圖複製一份進 photos/，縮到長邊 [MAX_EDGE] 並壓成 JPEG。回傳檔名，失敗回 null。
 *
 * URI 的授權只在選擇器回呼那一趟有效，所以拿到就要複製，不能只記 URI。
 */
fun importPhotoFile(context: Context, uri: Uri): String? {
    val bitmap = decodeUri(context, uri) ?: return null
    val name = UUID.randomUUID().toString() + ".jpg"
    val target = File(photosDir(context), name)
    return try {
        FileOutputStream(target).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out) }
        name
    } catch (t: Throwable) {
        target.delete()
        null
    } finally {
        bitmap.recycle()
    }
}

/** 顯示用：依目標尺寸解一張夠用就好的縮圖，不把整張 1600px 讀進記憶體 */
fun decodePhoto(file: File, maxEdgePx: Int): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdgePx)
    }
    BitmapFactory.decodeFile(file.path, options)
} catch (t: Throwable) {
    null
}

// 兩段式：inSampleSize 只會是 2 的次方（4000 → 2000 或 1000），先用它把記憶體壓下來，
// 再用 createScaledBitmap 精縮到剛好 1600。
private fun decodeUri(context: Context, uri: Uri): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        null
    } else {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_EDGE)
        }
        val decoded = context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
        decoded?.let { rotate(context, uri, scaleDown(it, MAX_EDGE)) }
    }
} catch (t: Throwable) {
    null
}

private fun sampleSizeFor(width: Int, height: Int, maxEdgePx: Int): Int {
    var sample = 1
    var edge = maxOf(width, height)
    while (edge / 2 >= maxEdgePx && sample < 64) {
        edge /= 2
        sample *= 2
    }
    return sample
}

/** 比上限小的圖不放大 */
private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
    val edge = maxOf(source.width, source.height)
    if (edge <= maxEdge) return source
    val ratio = maxEdge.toDouble() / edge
    val width = (source.width * ratio).toInt().coerceAtLeast(1)
    val height = (source.height * ratio).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(source, width, height, true)
    if (scaled != source) source.recycle()
    return scaled
}

/**
 * 相簿裡的照片常常是橫著拍、靠 EXIF 轉正的，不轉會看到躺著的圖。
 * 用平台內建的 ExifInterface（不是 androidx 那個要加相依的版本）。
 */
private fun rotate(context: Context, uri: Uri, source: Bitmap): Bitmap {
    val degrees = try {
        val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (t: Throwable) {
        0f
    }
    if (degrees == 0f) return source
    val matrix = Matrix().apply { postRotate(degrees) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    if (rotated != source) source.recycle()
    return rotated
}

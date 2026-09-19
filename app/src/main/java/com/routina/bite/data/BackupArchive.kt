package com.routina.bite.data

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 帶照片的備份：一個 ZIP，裡面是 backup.json（格式與單一 JSON 的備份完全一樣）加 photos/。
 * 沒有任何照片時不會走到這裡，備份仍然是單一 JSON。
 */
object BackupArchive {

    const val JSON_ENTRY = "backup.json"
    private const val PHOTO_PREFIX = "photos/"

    fun writeZip(output: OutputStream, json: String, photos: List<File>) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(json.toByteArray())
            zip.closeEntry()
            photos.forEach { photo ->
                zip.putNextEntry(ZipEntry(PHOTO_PREFIX + photo.name))
                photo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * 邊掃邊做：photos/ 底下的直接落地到 [photosDir]（同名覆蓋——檔名是 UUID，同名就是同一張），
     * backup.json 讀成字串回傳。沒有 backup.json 就回 null，由呼叫端當作讀不懂。
     */
    fun readZip(input: InputStream, photosDir: File): String? {
        var json: String? = null
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                when {
                    entry.isDirectory -> Unit
                    name == JSON_ENTRY -> json = zip.readBytes().decodeToString()
                    name.startsWith(PHOTO_PREFIX) -> extractPhoto(zip, name, photosDir)
                }
                zip.closeEntry()
            }
        }
        return json
    }

    // 檔名只取最後一段：備份是外來檔案，entry 名稱帶 ../ 就會寫到目錄外面去
    private fun extractPhoto(zip: ZipInputStream, entryName: String, photosDir: File) {
        val name = entryName.substringAfterLast('/')
        if (name.isEmpty()) return
        photosDir.mkdirs()
        File(photosDir, name).outputStream().use { zip.copyTo(it) }
    }
}

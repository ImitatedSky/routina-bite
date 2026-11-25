package com.routina.bite.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun backupJson(app: String = "bite", schema: Int = 1) =
        """{"app":"$app","schemaVersion":$schema,"exportedAt":0,"foods":[],"diary":[],"weights":[],"dayNotes":[]}"""

    @Test
    fun parseAcceptsOwnBackupAndRejectsOthers() {
        assertTrue(BackupCodec.parse(backupJson()) is BackupCodec.Parsed.Ok)
        assertEquals(
            BackupCodec.Parsed.Failed(BackupCodec.Reject.NOT_BITE),
            BackupCodec.parse(backupJson(app = "fit"))
        )
        assertEquals(
            BackupCodec.Parsed.Failed(BackupCodec.Reject.NEWER_SCHEMA),
            BackupCodec.parse(backupJson(schema = BackupCodec.SCHEMA_VERSION + 1))
        )
        assertEquals(
            BackupCodec.Parsed.Failed(BackupCodec.Reject.UNREADABLE),
            BackupCodec.parse("not json")
        )
    }

    @Test
    fun mergeKeepsExistingAndLetsIncomingWin() {
        val merged = mergeBy(listOf("a1", "b1"), listOf("b2", "c2")) { it.take(1) }
        assertEquals(listOf("a1", "b2", "c2"), merged)
        // 同一份匯入兩次不會多出資料
        assertEquals(merged, mergeBy(merged, listOf("b2", "c2")) { it.take(1) })
    }

    @Test
    fun zipPhotosNeverLandOutsidePhotosDir() {
        val zip = ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { out ->
                out.putNextEntry(ZipEntry(BackupArchive.JSON_ENTRY))
                out.write(backupJson().toByteArray())
                out.putNextEntry(ZipEntry("photos/../../evil.jpg"))
                out.write(byteArrayOf(1))
                out.putNextEntry(ZipEntry("photos/a.jpg"))
                out.write(byteArrayOf(2))
            }
        }.toByteArray()
        val photos = temp.newFolder("files", "photos")

        assertEquals(backupJson(), BackupArchive.readJson(ByteArrayInputStream(zip)))
        // 只讀 JSON 時不能先落地照片
        assertTrue(photos.list()!!.isEmpty())

        BackupArchive.extractPhotos(ByteArrayInputStream(zip), photos)
        assertEquals(setOf("a.jpg", "evil.jpg"), photos.list()!!.toSet())
        assertFalse(temp.root.resolve("evil.jpg").exists())
        assertFalse(temp.root.resolve("files/evil.jpg").exists())
    }

    @Test
    fun zipWithoutBackupJsonReadsAsNull() {
        val zip = ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { it.putNextEntry(ZipEntry("photos/a.jpg")) }
        }.toByteArray()
        assertNull(BackupArchive.readJson(ByteArrayInputStream(zip)))
    }

    @Test
    fun photoNamesWithPathsAreRejected() {
        assertTrue(isPlainFileName("3f2a.jpg"))
        listOf("", ".", "..", "../diary.json", "a/b.jpg", "..\\diary.json").forEach {
            assertFalse(it, isPlainFileName(it))
        }
    }
}

package com.samsungvault.backup.backup

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** ملف يُضمَّن في الأرشيف باسم مدخل فريد (يمنع تصادم الأسماء المتشابهة من مجلدات مختلفة). */
data class FileEntry(val entryName: String, val file: File)

/**
 * يضغط بيانات النسخة (JSON لكل فئة + الملفات المختارة) في أرشيف ZIP بأقصى ضغط،
 * ثم يُمرَّر الناتج إلى CryptoUtil للتشفير.
 */
object CompressionUtil {

    fun zipEntries(jsonEntries: Map<String, String>, files: List<FileEntry> = emptyList()): ByteArray {
        val byteStream = ByteArrayOutputStream()
        ZipOutputStream(byteStream).use { zip ->
            zip.setLevel(Deflater.BEST_COMPRESSION)
            for ((name, content) in jsonEntries) {
                zip.putNextEntry(ZipEntry("$name.json"))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            for (item in files) {
                if (!item.file.exists() || !item.file.isFile) continue
                zip.putNextEntry(ZipEntry(item.entryName))
                item.file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return byteStream.toByteArray()
    }

    fun unzipToMap(zipBytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                result[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }
}

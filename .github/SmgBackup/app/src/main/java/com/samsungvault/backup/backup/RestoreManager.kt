package com.samsungvault.backup.backup

import android.content.Context
import com.samsungvault.backup.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * يفك تشفير ملف النسخة (.svbackup) ويفك ضغطه، ثم يعرض محتوياته عنصرًا عنصرًا
 * ويستعيد فقط ما يختاره المستخدم صراحةً.
 */
class RestoreManager(private val context: Context) {

    private val contactsRepo = ContactsRepository(context)
    private val smsRepo = SmsRepository(context)
    private val callLogRepo = CallLogRepository(context)
    private val vaultRepo = PasswordVaultRepository(context)

    /** كلمة مرور خاطئة أو ملف معدَّل → استثناء (AEADBadTagException). */
    fun openBackupPreview(file: File, password: CharArray): Map<String, ByteArray> {
        val blob = CryptoUtil.unpackFromFile(file.readBytes())
        return CompressionUtil.unzipToMap(CryptoUtil.decrypt(blob, password))
    }

    private fun array(contents: Map<String, ByteArray>, name: String): JSONArray? =
        contents[name]?.let { runCatching { JSONArray(String(it, Charsets.UTF_8)) }.getOrNull() }

    private fun toItems(a: JSONArray, map: (Int, JSONObject) -> SelectableItem): List<SelectableItem> =
        (0 until a.length()).map { map(it, a.getJSONObject(it)) }

    /** يبني قائمة الفئات والعناصر الفردية القابلة للاختيار من محتوى النسخة. */
    fun describe(contents: Map<String, ByteArray>): List<CategoryState> {
        val out = mutableListOf<CategoryState>()
        array(contents, "contacts.json")?.let { a ->
            out.add(CategoryState(BackupCategory.CONTACTS, true, toItems(a) { _, o ->
                SelectableItem(o.optString("contactId"), o.optString("name").ifBlank { "بدون اسم" },
                    o.optJSONArray("phones")?.optString(0) ?: "")
            }))
        }
        array(contents, "sms.json")?.let { a ->
            out.add(CategoryState(BackupCategory.SMS, true, toItems(a) { i, o ->
                SelectableItem(o.optString("sourceId").ifBlank { "idx$i" }, o.optString("address"), o.optString("body").take(40))
            }))
        }
        array(contents, "call_log.json")?.let { a ->
            out.add(CategoryState(BackupCategory.CALL_LOG, true, toItems(a) { i, o ->
                SelectableItem(o.optString("sourceId").ifBlank { "idx$i" }, o.optString("number").ifBlank { "غير معروف" },
                    "مدة ${o.optLong("duration")} ث")
            }))
        }
        array(contents, "password_vault.json")?.let { a ->
            out.add(CategoryState(BackupCategory.PASSWORD_VAULT, true, toItems(a) { _, o ->
                SelectableItem(o.optString("id"), o.optString("label"), o.optString("username"))
            }))
        }
        for ((group, cat) in listOf("internal" to BackupCategory.INTERNAL_FILES, "sd" to BackupCategory.SD_CARD_FILES)) {
            val items = fileEntries(contents, group).map { (entry, name) -> SelectableItem(entry, name, "${(contents[entry]?.size ?: 0) / 1024} KB") }
            if (items.isNotEmpty()) out.add(CategoryState(cat, true, items))
        }
        // فئات للعرض فقط: النظام لا يسمح لتطبيق عادي بكتابتها
        listOf(
            "device_info" to BackupCategory.DEVICE_INFO, "accounts" to BackupCategory.ACCOUNTS,
            "installed_apps" to BackupCategory.INSTALLED_APPS, "system_settings" to BackupCategory.SYSTEM_SETTINGS
        ).forEach { (key, cat) ->
            if (contents.containsKey("$key.json")) out.add(CategoryState(cat, true, emptyList(), false))
        }
        return out
    }

    /** المحدَّد فقط: القائمة الفارغة تعني "لا شيء" (وليس "الكل"). */
    fun restoreSelected(
        contents: Map<String, ByteArray>,
        categoriesToRestore: Map<BackupCategory, Set<String>>,
        onProgress: (Int, String) -> Unit
    ) {
        val totalSteps = categoriesToRestore.size.coerceAtLeast(1)
        var step = 0
        for ((category, ids) in categoriesToRestore) {
            step++
            val percent = (step * 100) / totalSteps
            when (category) {
                BackupCategory.CONTACTS -> {
                    onProgress(percent, "جاري استعادة جهات الاتصال…")
                    array(contents, "contacts.json")?.let { contactsRepo.restore(filter(it, ids) { _, o -> o.optString("contactId") }) }
                }
                BackupCategory.SMS -> {
                    onProgress(percent, "جاري استعادة الرسائل…")
                    array(contents, "sms.json")?.let {
                        val n = smsRepo.restore(filter(it, ids) { i, o -> o.optString("sourceId").ifBlank { "idx$i" } })
                        onProgress(percent, "تمت استعادة $n رسالة")
                    }
                }
                BackupCategory.CALL_LOG -> {
                    onProgress(percent, "جاري استعادة سجل المكالمات…")
                    array(contents, "call_log.json")?.let {
                        val n = callLogRepo.restore(filter(it, ids) { i, o -> o.optString("sourceId").ifBlank { "idx$i" } })
                        onProgress(percent, "تمت استعادة $n مكالمة")
                    }
                }
                BackupCategory.PASSWORD_VAULT -> {
                    onProgress(percent, "جاري استعادة خزنة كلمات المرور…")
                    array(contents, "password_vault.json")?.let { vaultRepo.restore(filter(it, ids) { _, o -> o.optString("id") }) }
                }
                BackupCategory.INTERNAL_FILES -> { onProgress(percent, "جاري استعادة الملفات…"); restoreFiles(contents, ids, "internal") }
                BackupCategory.SD_CARD_FILES -> { onProgress(percent, "جاري استعادة ملفات البطاقة…"); restoreFiles(contents, ids, "sd") }
                else -> onProgress(percent, "تم عرض البيانات (للاطلاع فقط)")
            }
        }
    }

    private fun filter(array: JSONArray, ids: Set<String>, idOf: (Int, JSONObject) -> String): JSONArray {
        val result = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (idOf(i, obj) in ids) result.put(obj)
        }
        return result
    }

    /** entry -> الاسم الأصلي. تدعم النسخ القديمة التي بلا فهرس. */
    private fun fileEntries(contents: Map<String, ByteArray>, group: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        val index = array(contents, "files_index.json")
        if (index != null) {
            for (i in 0 until index.length()) {
                val o = index.getJSONObject(i)
                if (o.optString("group") == group && contents.containsKey(o.optString("entry"))) map[o.getString("entry")] = o.optString("name")
            }
        } else if (group == "internal") {
            contents.keys.filter { it.startsWith("files/") }.forEach { map[it] = it.removePrefix("files/") }
        }
        return map
    }

    private fun restoreFiles(contents: Map<String, ByteArray>, selectedIds: Set<String>, group: String) {
        val targetDir = File(context.getExternalFilesDir(null), "RestoredFiles")
        if (!targetDir.exists()) targetDir.mkdirs()
        val base = targetDir.canonicalFile
        for ((entry, rawName) in fileEntries(contents, group)) {
            if (entry !in selectedIds) continue
            val name = File(rawName).name // يزيل أي مسار
            if (name.isBlank() || name == "." || name == "..") continue
            var dest = File(base, name).canonicalFile
            check(dest.parentFile == base) { "مسار ملف غير آمن داخل النسخة" }
            var n = 1
            while (dest.exists()) { dest = File(base, "${n++}_$name").canonicalFile }
            dest.writeBytes(contents.getValue(entry))
        }
    }
}

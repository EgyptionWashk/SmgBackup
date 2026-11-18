package com.samsungvault.backup.backup

import android.content.Context
import android.os.Environment
import com.samsungvault.backup.data.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ينسّق جمع البيانات من كل فئة مُفعّلة، ثم يضغطها ويشفّرها في ملف واحد (.svbackup)
 * يُحفظ محليًا على الجهاز تحت Documents/VaultBackups.
 */
class BackupManager(private val context: Context) {

    private val contactsRepo = ContactsRepository(context)
    private val smsRepo = SmsRepository(context)
    private val callLogRepo = CallLogRepository(context)
    private val appsRepo = AppsRepository(context)
    private val accountsRepo = AccountsRepository(context)
    private val deviceInfoRepo = DeviceInfoRepository()
    private val filesRepo = FilesRepository(context)
    private val settingsRepo = SettingsRepository(context)
    private val vaultRepo = PasswordVaultRepository(context)

    /**
     * @param selections خريطة من كل فئة مفعّلة إلى مجموعة معرّفات العناصر المحددة بداخلها
     * @param password كلمة مرور التشفير التي يحددها المستخدم
     * @param onProgress استدعاء لتحديث الواجهة بنسبة التقدم
     */
    fun createBackup(
        selections: Map<BackupCategory, Set<String>>,
        password: CharArray,
        onProgress: (Int, String) -> Unit
    ): File {
        val jsonEntries = mutableMapOf<String, String>()
        val filesToInclude = mutableListOf<FileEntry>()
        val filesIndex = org.json.JSONArray()
        val totalSteps = selections.size.coerceAtLeast(1)
        var step = 0

        for ((category, selectedIds) in selections) {
            step++
            val percent = (step * 100) / totalSteps
            when (category) {
                BackupCategory.DEVICE_INFO -> {
                    onProgress(percent, "جاري حفظ معلومات الجهاز…")
                    jsonEntries["device_info"] = deviceInfoRepo.collect().toString()
                }
                BackupCategory.CONTACTS -> {
                    onProgress(percent, "جاري حفظ جهات الاتصال…")
                    jsonEntries["contacts"] = contactsRepo.exportSelected(selectedIds).toString()
                }
                BackupCategory.ACCOUNTS -> {
                    onProgress(percent, "جاري حفظ قائمة الحسابات…")
                    jsonEntries["accounts"] = accountsRepo.exportSelected(selectedIds).toString()
                }
                BackupCategory.SMS -> {
                    onProgress(percent, "جاري حفظ الرسائل النصية…")
                    jsonEntries["sms"] = smsRepo.exportSelected(selectedIds).toString()
                }
                BackupCategory.CALL_LOG -> {
                    onProgress(percent, "جاري حفظ سجل المكالمات…")
                    jsonEntries["call_log"] = callLogRepo.exportSelected(selectedIds).toString()
                }
                BackupCategory.INSTALLED_APPS -> {
                    onProgress(percent, "جاري حفظ قائمة التطبيقات…")
                    jsonEntries["installed_apps"] = appsRepo.exportSelected(selectedIds).toString()
                }
                BackupCategory.INTERNAL_FILES -> {
                    onProgress(percent, "جاري حفظ الملفات الداخلية…")
                    addFiles(filesRepo.filesFromPaths(selectedIds), "internal", filesToInclude, filesIndex)
                }
                BackupCategory.SD_CARD_FILES -> {
                    onProgress(percent, "جاري حفظ ملفات بطاقة الذاكرة…")
                    addFiles(filesRepo.filesFromPaths(selectedIds), "sd", filesToInclude, filesIndex)
                }
                BackupCategory.SYSTEM_SETTINGS -> {
                    onProgress(percent, "جاري حفظ الإعدادات…")
                    jsonEntries["system_settings"] = settingsRepo.collect().toString()
                }
                BackupCategory.PASSWORD_VAULT -> {
                    onProgress(percent, "جاري حفظ خزنة كلمات المرور…")
                    jsonEntries["password_vault"] = vaultRepo.exportSelected(selectedIds).toString()
                }
            }
        }

        if (filesIndex.length() > 0) jsonEntries["files_index"] = filesIndex.toString()
        onProgress(95, "جاري الضغط والتشفير…")
        val zipped = CompressionUtil.zipEntries(jsonEntries, filesToInclude)
        val encrypted = CryptoUtil.encrypt(zipped, password)
        val packed = CryptoUtil.packToFile(encrypted)

        val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "VaultBackups")
        if (!outDir.exists()) outDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(outDir, "backup_$timestamp.svbackup")
        outFile.writeBytes(packed)

        onProgress(100, "تم الانتهاء")
        return outFile
    }

    private fun addFiles(
        files: List<File>, group: String,
        target: MutableList<FileEntry>, index: org.json.JSONArray
    ) {
        for (file in files) {
            val entry = "files/${target.size + 1}_${file.name}"
            target.add(FileEntry(entry, file))
            index.put(org.json.JSONObject().apply {
                put("entry", entry); put("name", file.name)
                put("path", file.absolutePath); put("group", group)
            })
        }
    }
}

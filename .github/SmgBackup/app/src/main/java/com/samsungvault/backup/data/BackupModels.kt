package com.samsungvault.backup.data

/**
 * كل فئة بيانات يمكن نسخها أو استرجاعها.
 * كل فئة تُنسخ عبر واجهات أندرويد الرسمية العامة فقط (Content Providers / System APIs)،
 * ولا توجد هنا أي محاولة لتجاوز صلاحيات النظام.
 */
enum class BackupCategory(val id: String) {
    DEVICE_INFO("device_info"),
    CONTACTS("contacts"),
    ACCOUNTS("accounts"),
    SMS("sms"),
    CALL_LOG("call_log"),
    INSTALLED_APPS("installed_apps"),
    INTERNAL_FILES("internal_files"),
    SD_CARD_FILES("sd_card_files"),
    SYSTEM_SETTINGS("system_settings"),
    PASSWORD_VAULT("password_vault")
}

/**
 * عنصر قابل للتحديد الفردي داخل فئة (مثال: جهة اتصال واحدة، أو رسالة واحدة).
 */
data class SelectableItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val selected: Boolean = true
)

/**
 * حالة فئة كاملة في شاشة الاختيار: هل هي مفعّلة، وما هي العناصر المتاحة بداخلها.
 */
data class CategoryState(
    val category: BackupCategory,
    val enabled: Boolean = false,
    val items: List<SelectableItem> = emptyList(),
    val supportsItemSelection: Boolean = true
)

/** نتيجة عملية نسخ أو استرجاع لعرضها للمستخدم. */
sealed class OperationResult {
    data class Progress(val percent: Int, val message: String) : OperationResult()
    data class Success(val outputPath: String?) : OperationResult()
    data class Failure(val message: String) : OperationResult()
}

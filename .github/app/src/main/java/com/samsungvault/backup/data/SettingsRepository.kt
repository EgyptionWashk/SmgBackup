package com.samsungvault.backup.data

import android.content.Context
import android.provider.Settings
import org.json.JSONObject

/**
 * يقرأ فقط الإعدادات العامة المتاحة قراءتها لأي تطبيق بشكل رسمي (Settings.System / Settings.Global).
 * لا توجد قراءة أو كتابة لإعدادات محمية أو تتطلب صلاحيات WRITE_SECURE_SETTINGS الخاصة بالنظام.
 */
class SettingsRepository(private val context: Context) {

    private val readableKeys = listOf(
        Settings.System.SCREEN_BRIGHTNESS,
        Settings.System.SCREEN_OFF_TIMEOUT,
        Settings.System.SOUND_EFFECTS_ENABLED,
        Settings.System.AUTO_TIME,
        Settings.System.TIME_12_24
    )

    fun collect(): JSONObject {
        val obj = JSONObject()
        for (key in readableKeys) {
            try {
                val value = Settings.System.getString(context.contentResolver, key)
                obj.put(key, value ?: JSONObject.NULL)
            } catch (_: Exception) {
                // تجاهل أي مفتاح غير متاح على هذا الجهاز/الإصدار
            }
        }
        return obj
    }
}

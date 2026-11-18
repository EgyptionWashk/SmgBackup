package com.samsungvault.backup.data

import android.accounts.AccountManager
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * يقرأ فقط "أسماء ونوع" الحسابات المسجّلة على الجهاز (مثل عنوان بريد Gmail، أو حساب Samsung)
 * عبر AccountManager الرسمي. هذا لا يشمل كلمات المرور أو رموز الدخول (tokens) إطلاقًا؛
 * فنظام أندرويد لا يمنح أي تطبيق طرف ثالث الوصول إليها، وهذا تصميم أمني مقصود من جوجل.
 */
class AccountsRepository(private val context: Context) {

    fun listAccounts(): List<SelectableItem> {
        return try {
            val manager = AccountManager.get(context)
            manager.accounts.map { account ->
                SelectableItem(
                    id = "${account.type}:${account.name}",
                    title = account.name,
                    subtitle = labelForType(account.type)
                )
            }
        } catch (_: SecurityException) {
            emptyList() // المستخدم لم يمنح إذن GET_ACCOUNTS
        }
    }

    fun exportSelected(selectedIds: Set<String>): JSONArray {
        val array = JSONArray()
        for (entry in selectedIds) {
            val parts = entry.split(":", limit = 2)
            if (parts.size != 2) continue
            val obj = JSONObject()
            obj.put("type", parts[0])
            obj.put("name", parts[1])
            array.put(obj)
        }
        return array
    }

    private fun labelForType(type: String): String = when {
        type.contains("google", ignoreCase = true) -> "حساب Google"
        type.contains("samsung", ignoreCase = true) -> "حساب Samsung"
        type.contains("whatsapp", ignoreCase = true) -> "واتساب"
        type.contains("telegram", ignoreCase = true) -> "تيليجرام"
        else -> type
    }
}

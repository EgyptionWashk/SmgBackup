package com.samsungvault.backup.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject

/**
 * قراءة الرسائل النصية عبر Telephony.Sms Provider الرسمي (يتطلب READ_SMS).
 * ملاحظة: الاستعادة الكاملة للرسائل تتطلب أن يكون التطبيق هو تطبيق SMS الافتراضي على النظام،
 * وهذا قيد من أندرويد نفسه وليس من التطبيق.
 */
class SmsRepository(private val context: Context) {

    fun listMessages(limit: Int = 500): List<SelectableItem> {
        val result = mutableListOf<SelectableItem>()
        val uri = Telephony.Sms.CONTENT_URI
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null,
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                val addr = it.getString(addrIdx) ?: "غير معروف"
                val body = it.getString(bodyIdx) ?: ""
                result.add(SelectableItem(id = id, title = addr, subtitle = body.take(40)))
            }
        }
        return result
    }

    fun exportSelected(selectedIds: Set<String>): JSONArray {
        val array = JSONArray()
        val uri = Telephony.Sms.CONTENT_URI
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null, null, null
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                if (id !in selectedIds) continue
                val obj = JSONObject()
                obj.put("sourceId", id)
                obj.put("address", it.getString(addrIdx))
                obj.put("body", it.getString(bodyIdx))
                obj.put("date", it.getLong(dateIdx))
                obj.put("type", it.getInt(typeIdx))
                array.put(obj)
            }
        }
        return array
    }

    private fun exists(address: String, date: Long, body: String): Boolean =
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} = ? AND ${Telephony.Sms.BODY} = ?",
            arrayOf(address, date.toString(), body), null
        )?.use { it.count > 0 } ?: false

    /** Restore is permitted only while this package is the current default SMS handler. */
    fun restore(array: JSONArray): Int {
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        check(defaultPackage != null && defaultPackage == context.packageName) {
            "يجب تعيين التطبيق كتطبيق الرسائل الافتراضي قبل استعادة الرسائل النصية"
        }
        var restored = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val address = obj.optString("address")
            val body = obj.optString("body")
            if (address.isBlank() || body.isBlank()) continue
            if (exists(address, obj.optLong("date"), body)) continue
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, obj.optLong("date"))
                put(Telephony.Sms.TYPE, obj.optInt("type", Telephony.Sms.MESSAGE_TYPE_INBOX))
                put(Telephony.Sms.READ, 1)
            }
            val inserted = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                ?: throw IllegalStateException("رفض مزود الرسائل حفظ إحدى الرسائل")
            restored++
        }
        return restored
    }

}

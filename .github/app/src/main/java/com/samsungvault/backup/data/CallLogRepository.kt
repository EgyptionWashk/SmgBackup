package com.samsungvault.backup.data

import android.content.ContentValues
import android.content.Context
import android.provider.CallLog
import org.json.JSONArray
import org.json.JSONObject

/** قراءة واستعادة سجل المكالمات عبر CallLog Provider الرسمي (READ_CALL_LOG / WRITE_CALL_LOG). */
class CallLogRepository(private val context: Context) {

    fun listCalls(limit: Int = 500): List<SelectableItem> {
        val result = mutableListOf<SelectableItem>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null, null,
            "${CallLog.Calls.DATE} DESC LIMIT $limit"
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numIdx = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                val number = it.getString(numIdx) ?: "غير معروف"
                val typeLabel = when (it.getInt(typeIdx)) {
                    CallLog.Calls.INCOMING_TYPE -> "واردة"
                    CallLog.Calls.OUTGOING_TYPE -> "صادرة"
                    CallLog.Calls.MISSED_TYPE -> "فائتة"
                    else -> "أخرى"
                }
                result.add(SelectableItem(id = id, title = number, subtitle = typeLabel))
            }
        }
        return result
    }

    fun exportSelected(selectedIds: Set<String>): JSONArray {
        val array = JSONArray()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
            null, null, null
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numIdx = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durIdx = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                if (id !in selectedIds) continue
                val obj = JSONObject()
                obj.put("sourceId", id)
                obj.put("number", it.getString(numIdx))
                obj.put("type", it.getInt(typeIdx))
                obj.put("date", it.getLong(dateIdx))
                obj.put("duration", it.getLong(durIdx))
                array.put(obj)
            }
        }
        return array
    }

    private fun exists(number: String, date: Long): Boolean =
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID),
            "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ?",
            arrayOf(number, date.toString()), null
        )?.use { it.count > 0 } ?: false

    /** يرجع عدد السجلات المُضافة فعليًا (يتجاوز المكرر). */
    fun restore(array: JSONArray): Int {
        var restored = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val number = obj.optString("number")
            val date = obj.optLong("date")
            if (exists(number, date)) continue
            val values = ContentValues().apply {
                put(CallLog.Calls.NUMBER, number)
                put(CallLog.Calls.TYPE, obj.optInt("type"))
                put(CallLog.Calls.DATE, date)
                put(CallLog.Calls.DURATION, obj.optLong("duration"))
            }
            if (context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values) != null) restored++
        }
        return restored
    }
}

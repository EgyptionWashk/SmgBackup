package com.samsungvault.backup.data

import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import org.json.JSONArray
import org.json.JSONObject

/**
 * قراءة وكتابة جهات الاتصال عبر ContactsProvider الرسمي.
 * يتطلب READ_CONTACTS للقراءة، WRITE_CONTACTS للاستعادة.
 */
class ContactsRepository(private val context: Context) {

    fun listContacts(): List<SelectableItem> {
        val result = mutableListOf<SelectableItem>()
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null, null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIdx = it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                val name = it.getString(nameIdx) ?: "بدون اسم"
                result.add(SelectableItem(id = id, title = name))
            }
        }
        return result
    }

    /** يصدّر جهات الاتصال المحددة فقط إلى JSON (مع أرقام الهاتف والبريد). */
    fun exportSelected(selectedIds: Set<String>): JSONArray {
        val array = JSONArray()
        for (contactId in selectedIds) {
            val obj = JSONObject()
            obj.put("contactId", contactId)

            val nameCursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId), null
            )
            nameCursor?.use {
                if (it.moveToFirst()) {
                    obj.put("name", it.getString(0))
                }
            }

            val phones = JSONArray()
            val phoneCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId), null
            )
            phoneCursor?.use {
                while (it.moveToNext()) phones.put(it.getString(0))
            }
            obj.put("phones", phones)

            val emails = JSONArray()
            val emailCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId), null
            )
            emailCursor?.use {
                while (it.moveToNext()) emails.put(it.getString(0))
            }
            obj.put("emails", emails)

            array.put(obj)
        }
        return array
    }

    /** يستعيد جهات الاتصال من JSON إلى جهاز جديد (بعد الفورمات مثلاً). */
    fun restore(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.optString("name", "")

            val ops = ArrayList<ContentValues>()
            val rawContactValues = ContentValues()
            val rawContactUri = context.contentResolver.insert(
                ContactsContract.RawContacts.CONTENT_URI, rawContactValues
            ) ?: continue
            val rawContactId = rawContactUri.lastPathSegment

            val nameValues = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            }
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

            obj.optJSONArray("phones")?.let { phones ->
                for (p in 0 until phones.length()) {
                    val phoneValues = ContentValues().apply {
                        put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        put(ContactsContract.CommonDataKinds.Phone.NUMBER, phones.getString(p))
                        put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    }
                    context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)
                }
            }

            obj.optJSONArray("emails")?.let { emails ->
                for (e in 0 until emails.length()) {
                    val emailValues = ContentValues().apply {
                        put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        put(ContactsContract.CommonDataKinds.Email.ADDRESS, emails.getString(e))
                    }
                    context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, emailValues)
                }
            }
        }
    }
}

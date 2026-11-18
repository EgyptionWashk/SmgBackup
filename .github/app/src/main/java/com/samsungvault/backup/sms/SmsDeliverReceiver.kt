package com.samsungvault.backup.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * مطلوب ليكون التطبيق مؤهلًا لدور SMS الافتراضي (شرط أندرويد لكتابة الرسائل).
 * أثناء كون التطبيق هو الافتراضي تُحفظ الرسائل الواردة في صندوق الوارد حتى لا تضيع.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        if (Telephony.Sms.getDefaultSmsPackage(context) != context.packageName) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, messages[0].displayOriginatingAddress)
            put(Telephony.Sms.BODY, messages.joinToString("") { it.displayMessageBody ?: "" })
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 0)
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
    }
}

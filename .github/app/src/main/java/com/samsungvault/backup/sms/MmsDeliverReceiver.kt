package com.samsungvault.backup.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** مطلوب لأهلية دور SMS. معالجة MMS غير مُنفَّذة في هذا الإصدار. */
class MmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { /* غير مُنفَّذ */ }
}

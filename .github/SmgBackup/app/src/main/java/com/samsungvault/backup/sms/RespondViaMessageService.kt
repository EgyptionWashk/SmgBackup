package com.samsungvault.backup.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** مطلوب لأهلية دور SMS (الرد السريع عند رفض مكالمة). غير مُنفَّذ. */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

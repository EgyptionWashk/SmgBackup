package com.samsungvault.backup.data

import android.os.Build
import org.json.JSONObject

class DeviceInfoRepository {
    fun collect(): JSONObject {
        return JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("device", Build.DEVICE)
            put("androidVersion", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("buildId", Build.ID)
        }
    }
}

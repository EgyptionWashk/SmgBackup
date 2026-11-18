package com.samsungvault.backup.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * يقرأ قائمة التطبيقات المثبتة فقط (الاسم، معرّف الحزمة، رقم الإصدار).
 * لا يقرأ بيانات التطبيقات الداخلية، فهذا غير متاح لأي تطبيق طرف ثالث على أندرويد الحديث
 * بدون صلاحيات نظام (root)، وهو خارج نطاق هذا التطبيق تمامًا.
 */
class AppsRepository(private val context: Context) {

    fun listInstalledApps(): List<SelectableItem> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // تطبيقات المستخدم فقط
            .map { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString()
                SelectableItem(id = appInfo.packageName, title = label, subtitle = appInfo.packageName)
            }
            .sortedBy { it.title }
    }

    fun exportSelected(selectedIds: Set<String>): JSONArray {
        val pm = context.packageManager
        val array = JSONArray()
        for (packageName in selectedIds) {
            try {
                val info = pm.getPackageInfo(packageName, 0)
                val obj = JSONObject()
                obj.put("packageName", packageName)
                obj.put("versionName", info.versionName ?: "")
                obj.put("label", pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString())
                array.put(obj)
            } catch (_: PackageManager.NameNotFoundException) {
                // تجاهل التطبيقات التي أُزيلت
            }
        }
        return array
    }

    /**
     * "الاستعادة" هنا تعني فتح صفحة التطبيق على متجر Google Play لتثبيته يدويًا،
     * لأن تثبيت APK تلقائيًا بدون تفاعل المستخدم يتطلب صلاحيات نظام غير متاحة لتطبيق عادي،
     * وأي محاولة لتجاوز ذلك تُعد تجاوزًا لحماية النظام.
     */
    fun buildPlayStoreRestoreList(array: JSONArray): List<String> {
        val links = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val pkg = array.getJSONObject(i).optString("packageName")
            if (pkg.isNotBlank()) links.add("https://play.google.com/store/apps/details?id=$pkg")
        }
        return links
    }
}

package com.samsungvault.backup.data

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * يسرد الملفات القابلة للقراءة على التخزين الداخلي و(إن وُجدت) بطاقة الذاكرة الخارجية،
 * عبر واجهات التخزين العامة في أندرويد (Scoped Storage / MANAGE_EXTERNAL_STORAGE بعد موافقة المستخدم الصريحة من إعدادات النظام).
 */
class FilesRepository(private val context: Context) {

    fun listInternalFiles(maxItems: Int = 2000): List<SelectableItem> {
        val root = Environment.getExternalStorageDirectory()
        return walk(root, maxItems)
    }

    fun listSdCardFiles(maxItems: Int = 2000): List<SelectableItem> {
        val dirs = context.getExternalFilesDirs(null)
        // أول عنصر عادة هو التخزين الداخلي؛ أي عنصر إضافي يمثل بطاقة ذاكرة فعلية
        val sdCard = dirs.filterNotNull().drop(1).firstOrNull() ?: return emptyList()
        // الصعود لجذر بطاقة الذاكرة بدلاً من مجلد التطبيق فقط
        var root = sdCard
        repeat(4) { root.parentFile?.let { root = it } }
        return walk(root, maxItems)
    }

    private fun walk(root: File, maxItems: Int): List<SelectableItem> {
        val result = mutableListOf<SelectableItem>()
        if (!root.exists()) return result
        val stack = ArrayDeque<File>()
        stack.add(root)
        while (stack.isNotEmpty() && result.size < maxItems) {
            val current = stack.removeFirst()
            val children = current.listFiles() ?: continue
            for (child in children) {
                if (result.size >= maxItems) break
                if (child.isDirectory) {
                    stack.add(child)
                } else {
                    result.add(
                        SelectableItem(
                            id = child.absolutePath,
                            title = child.name,
                            subtitle = "${child.length() / 1024} KB"
                        )
                    )
                }
            }
        }
        return result
    }

    fun filesFromPaths(paths: Set<String>): List<File> = paths.map { File(it) }.filter { it.exists() }
}

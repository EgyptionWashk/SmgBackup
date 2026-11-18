package com.samsungvault.backup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

/** يعرض كل ملفات .svbackup الموجودة في مجلد VaultBackups على الجهاز ليختار المستخدم منها. */
@Composable
fun RestoreFilePickerScreen(backupFiles: List<File>, onPick: (File) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("اختر النسخة الاحتياطية للاستعادة منها", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        if (backupFiles.isEmpty()) {
            Text("لا توجد نسخ احتياطية محفوظة على هذا الجهاز بعد.")
        } else {
            LazyColumn {
                items(backupFiles) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        onClick = { onPick(file) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(file.name, style = MaterialTheme.typography.titleMedium)
                            Text("${file.length() / 1024} KB", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

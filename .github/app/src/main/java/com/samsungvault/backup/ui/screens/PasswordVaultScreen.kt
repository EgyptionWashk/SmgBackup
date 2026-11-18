package com.samsungvault.backup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.samsungvault.backup.data.PasswordVaultRepository
import com.samsungvault.backup.data.SelectableItem

/**
 * خزنة كلمات مرور يُدخلها المستخدم يدويًا بنفسه (لا جلب تلقائي من أي حساب)،
 * مخزّنة مشفّرة محليًا عبر PasswordVaultRepository.
 */
@Composable
fun PasswordVaultScreen(repo: PasswordVaultRepository, onBack: () -> Unit) {
    var entries by remember { mutableStateOf(repo.listEntries()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("خزنة كلمات المرور") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("رجوع") }
            }
        )

        LazyColumn(Modifier.weight(1f).padding(12.dp)) {
            items(entries) { entry ->
                VaultEntryRow(entry) {
                    repo.deleteEntry(entry.id)
                    entries = repo.listEntries()
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("إضافة كلمة مرور جديدة")
        }
    }

    if (showAddDialog) {
        AddVaultEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { label, username, secret ->
                repo.addEntry(label, username, secret)
                entries = repo.listEntries()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun VaultEntryRow(entry: SelectableItem, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                Text(entry.subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف")
            }
        }
    }
}

@Composable
private fun AddVaultEntryDialog(
    onDismiss: () -> Unit,
    onSave: (label: String, username: String, secret: String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة كلمة مرور") },
        text = {
            Column {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("اسم الحساب (مثال: Gmail)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم / البريد") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("كلمة المرور") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (label.isNotBlank()) onSave(label, username, secret) }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

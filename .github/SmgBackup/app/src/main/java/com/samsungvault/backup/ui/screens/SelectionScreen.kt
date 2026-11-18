package com.samsungvault.backup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samsungvault.backup.R
import com.samsungvault.backup.data.BackupCategory
import com.samsungvault.backup.data.CategoryState

private fun labelFor(category: BackupCategory): Int = when (category) {
    BackupCategory.DEVICE_INFO -> R.string.category_device_info
    BackupCategory.CONTACTS -> R.string.category_contacts
    BackupCategory.ACCOUNTS -> R.string.category_accounts
    BackupCategory.SMS -> R.string.category_sms
    BackupCategory.CALL_LOG -> R.string.category_call_log
    BackupCategory.INSTALLED_APPS -> R.string.category_apps
    BackupCategory.INTERNAL_FILES -> R.string.category_files
    BackupCategory.SD_CARD_FILES -> R.string.category_sdcard
    BackupCategory.SYSTEM_SETTINGS -> R.string.category_settings
    BackupCategory.PASSWORD_VAULT -> R.string.category_passwords
}

/**
 * شاشة موحّدة تُستخدم لكل من اختيار عناصر النسخ الاحتياطي واختيار عناصر الاستعادة.
 * كل فئة قابلة للطي/التوسيع، مع خيار "تحديد الكل" / "إلغاء تحديد الكل" وتحديد فردي للعناصر،
 * على غرار واجهات أدوات مثل Norton Ghost.
 */
@Composable
fun SelectionScreen(
    categories: List<CategoryState>,
    onToggleCategory: (BackupCategory, Boolean) -> Unit,
    onToggleItem: (BackupCategory, String, Boolean) -> Unit,
    onSetAllItems: (BackupCategory, Boolean) -> Unit,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit
) {
    var expandedCategory by remember { mutableStateOf<BackupCategory?>(null) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(categories) { state ->
                CategoryCard(
                    state = state,
                    expanded = expandedCategory == state.category,
                    onExpandToggle = {
                        expandedCategory = if (expandedCategory == state.category) null else state.category
                    },
                    onToggleCategory = { onToggleCategory(state.category, it) },
                    onToggleItem = { itemId, sel -> onToggleItem(state.category, itemId, sel) },
                    onSetAllItems = { onSetAllItems(state.category, it) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Surface(shadowElevation = 8.dp) {
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                enabled = categories.any { it.enabled }
            ) {
                Text(primaryActionLabel)
            }
        }
    }
}

@Composable
private fun CategoryCard(
    state: CategoryState,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onToggleCategory: (Boolean) -> Unit,
    onToggleItem: (String, Boolean) -> Unit,
    onSetAllItems: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Checkbox(checked = state.enabled, onCheckedChange = onToggleCategory)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(labelFor(state.category)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (state.supportsItemSelection) {
                        Text("${state.items.count { it.selected }} / ${state.items.size}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.supportsItemSelection && state.items.isNotEmpty()) {
                    IconButton(onClick = onExpandToggle) {
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                    }
                }
            }

            if (expanded && state.supportsItemSelection) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onSetAllItems(true) }) { Text(stringResource(R.string.select_all)) }
                        TextButton(onClick = { onSetAllItems(false) }) { Text(stringResource(R.string.deselect_all)) }
                    }
                    Divider(Modifier.padding(vertical = 4.dp))
                    state.items.take(300).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.selected,
                                onCheckedChange = { onToggleItem(item.id, it) }
                            )
                            Column {
                                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                                if (item.subtitle.isNotBlank()) {
                                    Text(item.subtitle, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.samsungvault.backup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samsungvault.backup.R

@Composable
fun HomeScreen(
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onVaultClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(32.dp))

        HomeActionCard(
            icon = Icons.Filled.CloudUpload,
            title = stringResource(R.string.action_backup),
            onClick = onBackupClick
        )
        Spacer(Modifier.height(16.dp))
        HomeActionCard(
            icon = Icons.Filled.Restore,
            title = stringResource(R.string.action_restore),
            onClick = onRestoreClick
        )
        Spacer(Modifier.height(16.dp))
        HomeActionCard(
            icon = Icons.Filled.Key,
            title = stringResource(R.string.action_vault),
            onClick = onVaultClick
        )
    }
}

@Composable
private fun HomeActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

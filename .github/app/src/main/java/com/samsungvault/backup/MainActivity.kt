package com.samsungvault.backup

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.role.RoleManager
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.samsungvault.backup.backup.BiometricHelper
import com.samsungvault.backup.data.PasswordVaultRepository
import com.samsungvault.backup.ui.screens.*
import com.samsungvault.backup.ui.theme.SamsungVaultTheme
import com.samsungvault.backup.viewmodel.MainViewModel
import com.samsungvault.backup.viewmodel.ScreenState
import java.io.File

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SamsungVaultTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val permissionsState = rememberMultiplePermissionsState(
                        permissions = buildList {
                            add(Manifest.permission.READ_CONTACTS)
                            add(Manifest.permission.WRITE_CONTACTS)
                            add(Manifest.permission.READ_SMS)
                            add(Manifest.permission.READ_CALL_LOG)
                            add(Manifest.permission.WRITE_CALL_LOG)
                            add(Manifest.permission.GET_ACCOUNTS)
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    )

                    var showSmsRoleExplanation by remember { mutableStateOf(false) }
                    var allFilesPrompted by remember { mutableStateOf(false) }
                    val smsRoleLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {
                        if (Telephony.Sms.getDefaultSmsPackage(this@MainActivity) == packageName) {
                            viewModel.runRestore()
                        } else {
                            Toast.makeText(this@MainActivity,
                                "لم يتم تعيين التطبيق كتطبيق رسائل افتراضي؛ لم تتم استعادة الرسائل.",
                                Toast.LENGTH_LONG).show()
                        }
                    }

                    fun requireBiometric(action: () -> Unit) {
                        if (BiometricHelper.canAuthenticate(this@MainActivity)) {
                            BiometricHelper.authenticate(
                                this@MainActivity,
                                onSuccess = { action() },
                                onError = { /* يبقى المستخدم في نفس الشاشة، يمكنه إعادة المحاولة */ }
                            )
                        } else {
                            // جهاز بلا بصمة مُعدّة: نسمح بالمتابعة حتى لا يُحجب المستخدم كليًا،
                            // لكن يُفضّل إعداد قفل الشاشة لأقصى حماية.
                            action()
                        }
                    }

                    val screen by viewModel.screen.collectAsState()
                    val categories by viewModel.categories.collectAsState()
                    val progress by viewModel.progress.collectAsState()

                    BackHandler(enabled = screen !is ScreenState.Consent && screen !is ScreenState.Home && screen !is ScreenState.InProgress) {
                        viewModel.goHome()
                    }
                    when (val s = screen) {
                        is ScreenState.Consent -> ConsentScreen(
                            onAccept = {
                                if (!permissionsState.allPermissionsGranted) {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                                viewModel.acceptConsent()
                            },
                            onDecline = { finish() }
                        )

                        is ScreenState.Home -> HomeScreen(
                            onBackupClick = {
                                if (needsAllFilesAccess() && !allFilesPrompted) {
                                    allFilesPrompted = true
                                    Toast.makeText(this@MainActivity,
                                        "امنح التطبيق إذن «الوصول لكل الملفات» ثم ارجع واضغط نسخ احتياطي من جديد.",
                                        Toast.LENGTH_LONG).show()
                                    requestManageAllFilesPermission()
                                } else {
                                    requireBiometric { viewModel.loadBackupCategories() }
                                }
                            },
                            onRestoreClick = {
                                requireBiometric { viewModel.goToRestoreFilePicker() }
                            },
                            onVaultClick = { viewModel.goToPasswordVault() }
                        )

                        is ScreenState.BackupSelection -> {
                            var showPasswordPrompt by remember { mutableStateOf(false) }
                            if (!showPasswordPrompt) {
                                SelectionScreen(
                                    categories = categories,
                                    onToggleCategory = viewModel::toggleCategory,
                                    onToggleItem = viewModel::toggleItem,
                                    onSetAllItems = viewModel::setAllItems,
                                    primaryActionLabel = stringResourceCompat(R.string.start_backup),
                                    onPrimaryAction = { showPasswordPrompt = true }
                                )
                            } else {
                                PasswordPromptScreen(requireConfirmation = true) { password ->
                                    viewModel.runBackup(password)
                                }
                            }
                        }

                        is ScreenState.RestoreSelection -> {
                            SelectionScreen(
                                categories = categories,
                                onToggleCategory = viewModel::toggleCategory,
                                onToggleItem = viewModel::toggleItem,
                                onSetAllItems = viewModel::setAllItems,
                                primaryActionLabel = stringResourceCompat(R.string.start_restore),
                                onPrimaryAction = {
                                    val smsSelected = categories.any {
                                        it.category == com.samsungvault.backup.data.BackupCategory.SMS && it.enabled
                                    }
                                    if (!smsSelected || Telephony.Sms.getDefaultSmsPackage(this@MainActivity) == packageName) {
                                        viewModel.runRestore()
                                    } else {
                                        showSmsRoleExplanation = true
                                    }
                                }
                            )
                            if (showSmsRoleExplanation) {
                                AlertDialog(
                                    onDismissRequest = { showSmsRoleExplanation = false },
                                    title = { Text("استعادة الرسائل النصية") },
                                    text = { Text("تتطلب قواعد أندرويد أن يكون التطبيق هو تطبيق الرسائل الافتراضي حتى يسمح النظام بكتابة الرسائل. سيطلب النظام موافقتك. بعد الاستعادة، أعد اختيار تطبيق الرسائل الذي تفضله من إعدادات التطبيقات الافتراضية؛ لا يسمح أندرويد للتطبيق بإعادة تعيين التطبيق السابق بصمت.") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showSmsRoleExplanation = false
                                            try {
                                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    val roleManager = getSystemService(RoleManager::class.java)
                                                    if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) null
                                                    else roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                                } else {
                                                    Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(
                                                        Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                                                }
                                                if (intent == null) {
                                                    Toast.makeText(this@MainActivity, "هذا التطبيق غير مؤهل حاليًا لدور تطبيق الرسائل الافتراضي على هذا الجهاز.", Toast.LENGTH_LONG).show()
                                                } else smsRoleLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(this@MainActivity, "تعذر فتح طلب تطبيق الرسائل الافتراضي: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }) { Text("متابعة إلى إعدادات النظام") }
                                    },
                                    dismissButton = { TextButton(onClick = { showSmsRoleExplanation = false }) { Text("إلغاء") } }
                                )
                            }
                        }

                        is ScreenState.PasswordVault -> {
                            val repo = remember { PasswordVaultRepository(this@MainActivity) }
                            PasswordVaultScreen(repo = repo, onBack = { viewModel.goHome() })
                        }

                        is ScreenState.InProgress -> ProgressScreen(progress.first, progress.second)

                        is ScreenState.Finished -> FinishedScreen(s.success, s.message) { viewModel.goHome() }

                        is ScreenState.RestoreFilePicker -> {
                            // اختيار ملف الاستعادة ثم كلمة المرور الخاصة به
                            val backupDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "VaultBackups")
                            val files = backupDir.listFiles()?.filter { it.extension == "svbackup" } ?: emptyList()
                            var pickedFile by remember { mutableStateOf<File?>(null) }

                            if (pickedFile == null) {
                                RestoreFilePickerScreen(backupFiles = files) { file ->
                                    requireBiometric { pickedFile = file }
                                }
                            } else {
                                PasswordPromptScreen(requireConfirmation = false) { password ->
                                    viewModel.openBackupFile(pickedFile!!, password)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** يفتح إعدادات النظام لمنح إذن "إدارة كل الملفات" الخاص بأندرويد 11+ (MANAGE_EXTERNAL_STORAGE)،
     * وهو إذن حساس لا يمكن منحه إلا من المستخدم يدويًا من صفحة إعدادات النظام نفسها — هذا تصميم أمني من جوجل
     * ولا توجد طريقة برمجية لتجاوزه، وهذا هو الصحيح. */
    private fun needsAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()

    fun requestManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = android.content.Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int): String = androidx.compose.ui.res.stringResource(id)

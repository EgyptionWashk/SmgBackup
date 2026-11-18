package com.samsungvault.backup.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsungvault.backup.backup.BackupManager
import com.samsungvault.backup.backup.RestoreManager
import com.samsungvault.backup.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ScreenState {
    object Consent : ScreenState()
    object Home : ScreenState()
    object BackupSelection : ScreenState()
    object RestoreFilePicker : ScreenState()
    object RestoreSelection : ScreenState()
    object PasswordVault : ScreenState()
    object InProgress : ScreenState()
    data class Finished(val success: Boolean, val message: String) : ScreenState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _screen = MutableStateFlow<ScreenState>(ScreenState.Consent)
    val screen: StateFlow<ScreenState> = _screen

    private val _categories = MutableStateFlow<List<CategoryState>>(emptyList())
    val categories: StateFlow<List<CategoryState>> = _categories

    private val _progress = MutableStateFlow(0 to "")
    val progress: StateFlow<Pair<Int, String>> = _progress

    private var restoreFile: File? = null
    private var restoreContents: Map<String, ByteArray> = emptyMap()

    fun acceptConsent() { _screen.value = ScreenState.Home }
    fun goHome() { _screen.value = ScreenState.Home }
    fun goToRestoreFilePicker() { _screen.value = ScreenState.RestoreFilePicker }

    private fun safeList(block: () -> List<SelectableItem>): List<SelectableItem> =
        try { block() } catch (_: Exception) { emptyList() } // إذن غير ممنوح → قائمة فارغة بدل انهيار التطبيق

    fun loadBackupCategories() {
        val contactsRepo = ContactsRepository(context)
        val smsRepo = SmsRepository(context)
        val callLogRepo = CallLogRepository(context)
        val appsRepo = AppsRepository(context)
        val accountsRepo = AccountsRepository(context)
        val filesRepo = FilesRepository(context)
        val vaultRepo = PasswordVaultRepository(context)
        viewModelScope.launch(Dispatchers.IO) {
            _categories.value = listOf(
                CategoryState(BackupCategory.DEVICE_INFO, enabled = true, supportsItemSelection = false),
                CategoryState(BackupCategory.CONTACTS, items = safeList { contactsRepo.listContacts() }),
                CategoryState(BackupCategory.ACCOUNTS, items = safeList { accountsRepo.listAccounts() }),
                CategoryState(BackupCategory.SMS, items = safeList { smsRepo.listMessages() }),
                CategoryState(BackupCategory.CALL_LOG, items = safeList { callLogRepo.listCalls() }),
                CategoryState(BackupCategory.INSTALLED_APPS, items = safeList { appsRepo.listInstalledApps() }),
                CategoryState(BackupCategory.INTERNAL_FILES, items = safeList { filesRepo.listInternalFiles() }),
                CategoryState(BackupCategory.SD_CARD_FILES, items = safeList { filesRepo.listSdCardFiles() }),
                CategoryState(BackupCategory.SYSTEM_SETTINGS, enabled = true, supportsItemSelection = false),
                CategoryState(BackupCategory.PASSWORD_VAULT, items = safeList { vaultRepo.listEntries() })
            )
            _screen.value = ScreenState.BackupSelection
        }
    }

    fun toggleCategory(category: BackupCategory, enabled: Boolean) {
        _categories.value = _categories.value.map { if (it.category == category) it.copy(enabled = enabled) else it }
    }

    fun toggleItem(category: BackupCategory, itemId: String, selected: Boolean) {
        _categories.value = _categories.value.map { state ->
            if (state.category != category) state
            else state.copy(items = state.items.map { if (it.id == itemId) it.copy(selected = selected) else it })
        }
    }

    fun setAllItems(category: BackupCategory, selected: Boolean) {
        _categories.value = _categories.value.map { state ->
            if (state.category != category) state
            else state.copy(items = state.items.map { it.copy(selected = selected) })
        }
    }

    /** فئة مفعّلة ولها عناصر مختارة (أو فئة معلومات فقط). */
    private fun activeSelections(): Map<BackupCategory, Set<String>> = _categories.value
        .filter { it.enabled && (!it.supportsItemSelection || it.items.any { i -> i.selected }) }
        .associate { s -> s.category to s.items.filter { it.selected }.map { it.id }.toSet() }

    fun runBackup(password: CharArray) {
        val selections = activeSelections()

        _screen.value = ScreenState.InProgress
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manager = BackupManager(context)
                val file = manager.createBackup(selections, password) { percent, message ->
                    _progress.value = percent to message
                }
                _screen.value = ScreenState.Finished(true, "تم حفظ النسخة في:\n${file.absolutePath}")
            } catch (e: Exception) {
                _screen.value = ScreenState.Finished(false, "فشل النسخ: ${e.message}")
            }
        }
    }

    fun openBackupFile(file: File, password: CharArray) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manager = RestoreManager(context)
                restoreContents = manager.openBackupPreview(file, password)
                restoreFile = file

                val categoryList = manager.describe(restoreContents)
                _categories.value = categoryList
                _screen.value = ScreenState.RestoreSelection
            } catch (e: Exception) {
                _screen.value = ScreenState.Finished(false, "كلمة المرور غير صحيحة أو الملف تالف")
            }
        }
    }

    fun runRestore() {
        val selections = activeSelections()

        _screen.value = ScreenState.InProgress
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manager = RestoreManager(context)
                manager.restoreSelected(restoreContents, selections) { percent, message ->
                    _progress.value = percent to message
                }
                _screen.value = ScreenState.Finished(true, "تمت استعادة البيانات المحددة بنجاح")
            } catch (e: Exception) {
                _screen.value = ScreenState.Finished(false, "فشلت الاستعادة: ${e.message}")
            }
        }
    }

    fun goToPasswordVault() { _screen.value = ScreenState.PasswordVault }
}

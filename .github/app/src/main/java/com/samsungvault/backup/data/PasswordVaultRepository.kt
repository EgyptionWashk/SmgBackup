package com.samsungvault.backup.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** User-managed password vault encrypted at rest with a non-exportable Android Keystore key. */
data class VaultEntry(val id: String, val label: String, val username: String, val secret: String)

class PasswordVaultRepository(context: Context) {
    private val appContext = context.applicationContext
    private val vaultFile = File(appContext.noBackupFilesDir, "vault_store.bin")

    @Synchronized private fun loadArray(): JSONArray {
        if (!vaultFile.exists()) return JSONArray()
        val bytes = vaultFile.readBytes()
        require(bytes.size > IV_SIZE + TAG_SIZE_BYTES) { "ملف الخزنة تالف" }
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val encrypted = bytes.copyOfRange(IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return JSONArray(String(cipher.doFinal(encrypted), Charsets.UTF_8))
    }

    @Synchronized private fun saveArray(array: JSONArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(array.toString().toByteArray(Charsets.UTF_8))
        val temp = File(vaultFile.parentFile, "vault_store.tmp")
        temp.outputStream().use { out -> out.write(iv); out.write(encrypted); out.fd.sync() }
        try {
            Files.move(temp.toPath(), vaultFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), vaultFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    fun listEntries(): List<SelectableItem> {
        val array = loadArray()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(SelectableItem(obj.getString("id"), obj.getString("label"), obj.optString("username")))
            }
        }
    }

    fun addEntry(label: String, username: String, secret: String) {
        require(label.isNotBlank()) { "اسم العنصر مطلوب" }
        val array = loadArray()
        array.put(JSONObject().apply {
            put("id", java.util.UUID.randomUUID().toString())
            put("label", label.trim())
            put("username", username)
            put("secret", secret)
        })
        saveArray(array)
    }

    fun deleteEntry(id: String) {
        val current = loadArray()
        val updated = JSONArray()
        for (i in 0 until current.length()) {
            val item = current.getJSONObject(i)
            if (item.optString("id") != id) updated.put(item)
        }
        saveArray(updated)
    }

    fun exportSelected(selectedIds: Set<String>): JSONArray {
        val current = loadArray()
        val result = JSONArray()
        for (i in 0 until current.length()) {
            val item = current.getJSONObject(i)
            if (item.optString("id") in selectedIds) result.put(item)
        }
        return result
    }

    fun restore(array: JSONArray) {
        val current = loadArray()
        val knownIds = HashSet<String>()
        for (i in 0 until current.length()) knownIds.add(current.getJSONObject(i).optString("id"))
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (item.optString("id").isBlank()) item.put("id", java.util.UUID.randomUUID().toString())
            if (knownIds.add(item.optString("id"))) current.put(item)
        }
        saveArray(current)
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "samsung_vault_aes256_gcm_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE_BITS = 128
        private const val TAG_SIZE_BYTES = 16
    }
}

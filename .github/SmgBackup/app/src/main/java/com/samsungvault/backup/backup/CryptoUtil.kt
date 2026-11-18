package com.samsungvault.backup.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * تشفير قوي بمعيار صناعي: AES-256 في وضع GCM (يوفر السرية + التحقق من سلامة البيانات معًا)،
 * مع اشتقاق المفتاح من كلمة مرور المستخدم عبر PBKDF2WithHmacSHA256 و100,000 تكرار
 * لمقاومة هجمات القوة الغاشمة. هذا هو نفس المعيار المستخدم في أدوات مثل VeraCrypt وBitwarden.
 */
object CryptoUtil {

    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12

    data class EncryptedBlob(val salt: ByteArray, val iv: ByteArray, val ciphertext: ByteArray)

    fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val key = factory.generateSecret(spec).encoded
            try { SecretKeySpec(key, "AES") } finally { key.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(plainData: ByteArray, password: CharArray): EncryptedBlob {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { random.nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plainData)

        return EncryptedBlob(salt, iv, ciphertext)
    }

    fun decrypt(blob: EncryptedBlob, password: CharArray): ByteArray {
        val key = deriveKey(password, blob.salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, blob.iv))
        return cipher.doFinal(blob.ciphertext) // يفشل تلقائيًا (AEADBadTagException) لو كلمة المرور خاطئة أو الملف تالف/معدَّل
    }

    /** يحزم (salt + iv + ciphertext) في ملف واحد بترويسة بسيطة لسهولة الاسترجاع لاحقًا. */
    fun packToFile(blob: EncryptedBlob): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(MAGIC)
        out.write(blob.salt.size)
        out.write(blob.salt)
        out.write(blob.iv.size)
        out.write(blob.iv)
        out.write(blob.ciphertext)
        return out.toByteArray()
    }

    fun unpackFromFile(data: ByteArray): EncryptedBlob {
        require(data.size >= MAGIC.size + 1 + SALT_LENGTH_BYTES + 1 + IV_LENGTH_BYTES + 16) { "ملف غير صالح أو غير مكتمل" }
        var offset = 0
        require(data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) { "ملف غير صالح أو تالف" }
        offset += MAGIC.size
        val saltLen = data[offset++].toInt() and 0xff
        require(saltLen == SALT_LENGTH_BYTES && offset + saltLen < data.size) { "ترويسة النسخة غير صالحة" }
        val salt = data.copyOfRange(offset, offset + saltLen); offset += saltLen
        val ivLen = data[offset++].toInt() and 0xff
        require(ivLen == IV_LENGTH_BYTES && offset + ivLen + 16 <= data.size) { "متجه التهيئة غير صالح" }
        val iv = data.copyOfRange(offset, offset + ivLen); offset += ivLen
        val ciphertext = data.copyOfRange(offset, data.size)
        return EncryptedBlob(salt, iv, ciphertext)
    }

    private val MAGIC = byteArrayOf('S'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
}

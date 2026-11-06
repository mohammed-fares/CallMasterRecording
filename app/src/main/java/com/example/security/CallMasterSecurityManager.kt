package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CallMasterSecurityManager {

    private val keyStore: KeyStore? by lazy {
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        }.getOrNull()
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val ks = keyStore ?: return null
            if (!ks.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            } else {
                val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey
            }
        } catch (_: Exception) {
            null
        }
    }

    fun encryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val secretKey = getOrCreateSecretKey() ?: return false
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv

            FileOutputStream(outputFile).use { fos ->
                fos.write(iv.size)
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    FileInputStream(inputFile).use { fis ->
                        fis.copyTo(cos)
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun decryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val secretKey = getOrCreateSecretKey() ?: return false
            FileInputStream(inputFile).use { fis ->
                val ivSize = fis.read()
                val iv = ByteArray(ivSize)
                fis.read(iv)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(outputFile).use { fos ->
                        cis.copyTo(fos)
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        return hashPin(inputPin) == storedHash
    }

    fun secureDelete(file: File): Boolean {
        if (!file.exists()) return true
        return try {
            val length = file.length()
            if (length > 0) {
                FileOutputStream(file).use { fos ->
                    val zeros = ByteArray(minOf(length, 4096).toInt())
                    var written = 0L
                    while (written < length) {
                        val toWrite = minOf(zeros.size.toLong(), length - written).toInt()
                        fos.write(zeros, 0, toWrite)
                        written += toWrite
                    }
                    fos.flush()
                }
            }
            file.delete()
        } catch (_: Exception) {
            file.delete()
        }
    }

    companion object {
        private const val KEY_ALIAS = "CallMasterKeystoreKey_v1"
    }
}

package com.xingmihai.sukisu.themeeditor.theme

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Byte-compatible with SukiSU-Ultra's `ThemePackageCrypto.kt`.
 *
 * Layout: "SKTH" (4 B) + random IV (12 B) + AES-256-GCM ciphertext with a
 * 128-bit tag appended by `Cipher.doFinal()`.
 */
object ThemePackageCrypto {

    const val MIME = "application/x-sukisu-theme"
    const val HEADER_SIZE = 16

    private val MAGIC = byteArrayOf(0x53, 0x4B, 0x54, 0x48) // "SKTH"
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128

    // 48-byte HASH_BYTES from the manager; the AES key is its first 32 bytes.
    private val KEY: ByteArray =
        ("41dc174e5096b4ee93311cc892b132e21961012b14cf2179" +
            "540f203ca72ab9c1d9cda5452e58e6df5679a1464d440a0a")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
            .copyOf(32)

    private fun cipher(mode: Int, iv: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(KEY, "AES"), GCMParameterSpec(TAG_BITS, iv))
        }

    fun hasValidHeader(bytes: ByteArray): Boolean =
        bytes.size >= MAGIC.size && MAGIC.indices.all { bytes[it] == MAGIC[it] }

    fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val cipherText = cipher(Cipher.ENCRYPT_MODE, iv).doFinal(plain)
        return MAGIC + iv + cipherText
    }

    fun decrypt(raw: ByteArray): ByteArray {
        if (raw.size < HEADER_SIZE + 16) error("文件太小，不是有效的 .sukisu 主题包")
        if (!hasValidHeader(raw)) error("文件头不是 SKTH，可能不是 .sukisu 主题包")
        val iv = raw.copyOfRange(MAGIC.size, MAGIC.size + IV_SIZE)
        return cipher(Cipher.DECRYPT_MODE, iv).doFinal(raw, HEADER_SIZE, raw.size - HEADER_SIZE)
    }
}

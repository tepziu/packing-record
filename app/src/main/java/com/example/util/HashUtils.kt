package com.example.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashUtils {
    /**
     * Computes the SHA-256 hexadecimal hash string for a given file.
     */
    fun calculateSHA256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "SHA256_ERROR_${e.localizedMessage?.take(20)}"
        }
    }

    /**
     * Verifies if the file matches expected SHA-256 hash.
     */
    fun verifyIntegrity(file: File, expectedHash: String): Boolean {
        if (!file.exists() || expectedHash.isBlank()) return false
        val currentHash = calculateSHA256(file)
        return currentHash.equals(expectedHash, ignoreCase = true)
    }
}

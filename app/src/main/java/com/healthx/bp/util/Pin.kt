package com.healthx.bp.util

import java.security.MessageDigest
import java.security.SecureRandom

/** Salted SHA-256 hashing for the 6-digit unlock PIN. No plaintext is stored. */
object Pin {
    const val LENGTH = 6

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest((salt + pin).toByteArray(Charsets.UTF_8)).toHex()
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        expectedHash.isNotBlank() && hash(pin, salt) == expectedHash

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}

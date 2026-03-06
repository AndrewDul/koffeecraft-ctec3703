package uk.ac.dmu.koffeecraft.util.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    fun generateSaltBase64(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hashPasswordBase64(password: CharArray, saltBase64: String): String {
        val saltBytes = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(password, saltBytes, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verify(password: CharArray, saltBase64: String, expectedHashBase64: String): Boolean {
        val actualHash = hashPasswordBase64(password, saltBase64)
        return actualHash == expectedHashBase64
    }
}
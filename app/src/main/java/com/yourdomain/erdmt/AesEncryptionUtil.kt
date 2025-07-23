package com.yourdomain.erdmt

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object AesEncryptionUtil {
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    fun encrypt(data: ByteArray, secretKey: SecretKey): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data)
        return Pair(Base64.encodeToString(encrypted, Base64.DEFAULT), Base64.encodeToString(iv, Base64.DEFAULT))
    }

    fun decrypt(data: String, secretKey: SecretKey, iv: String): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            IvParameterSpec(Base64.decode(iv, Base64.DEFAULT))
        )
        return cipher.doFinal(Base64.decode(data, Base64.DEFAULT))
    }
}
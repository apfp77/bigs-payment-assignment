package im.bigs.pg.external.pg.crypto

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-256-GCM 암호화 유틸리티. TestPg API 요청 본문 암호화에 사용됩니다. */
object AesGcmCrypto {
  private const val ALGORITHM = "AES/GCM/NoPadding"
  private const val TAG_LENGTH_BIT = 128

  /**
   * API-KEY를 SHA-256 해시하여 32바이트 암호화 키 생성.
   *
   * @param apiKey UUID 형식의 API-KEY
   * @return 32바이트 암호화 키
   */
  fun deriveKey(apiKey: String): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(apiKey.toByteArray(Charsets.UTF_8))
  }

  /**
   * Base64URL 인코딩된 IV를 12바이트로 디코딩.
   *
   * @param ivBase64Url Base64URL 인코딩된 IV
   * @return 12바이트 IV 배열
   */
  fun decodeIv(ivBase64Url: String): ByteArray {
    return Base64.getUrlDecoder().decode(ivBase64Url)
  }

  /**
   * 평문을 AES-256-GCM으로 암호화하고 Base64URL로 인코딩.
   *
   * @param plaintext 암호화할 평문 (JSON)
   * @param key 32바이트 암호화 키
   * @param iv 12바이트 IV
   * @return Base64URL(ciphertext || tag) 패딩 없음
   */
  fun encrypt(plaintext: String, key: ByteArray, iv: ByteArray): String {
    val cipher = Cipher.getInstance(ALGORITHM)
    val keySpec = SecretKeySpec(key, "AES")
    val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
    val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

    return Base64.getUrlEncoder().withoutPadding().encodeToString(cipherBytes)
  }

  /**
   * Base64URL 인코딩된 암호문을 AES-256-GCM으로 복호화.
   *
   * @param encBase64Url Base64URL 인코딩된 암호문
   * @param key 32바이트 암호화 키
   * @param iv 12바이트 IV
   * @return 복호화된 평문
   */
  fun decrypt(encBase64Url: String, key: ByteArray, iv: ByteArray): String {
    val cipher = Cipher.getInstance(ALGORITHM)
    val keySpec = SecretKeySpec(key, "AES")
    val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
    val cipherBytes = Base64.getUrlDecoder().decode(encBase64Url)
    val plainBytes = cipher.doFinal(cipherBytes)

    return String(plainBytes, Charsets.UTF_8)
  }
}

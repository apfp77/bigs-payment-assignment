package im.bigs.pg.external.pg.crypto

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AesGcmCryptoTest {

  private val testApiKey = "11111111-1111-4111-8111-111111111111"
  private val testIvBase64Url = "AAAAAAAAAAAAAAAA" // 12바이트 (패딩 포함하면 16자)

  @Test
  @DisplayName("SHA256으로 API_KEY를 32바이트 키로 변환해야 한다")
  fun `SHA256으로 API_KEY를 32바이트 키로 변환해야 한다`() {
    val key = AesGcmCrypto.deriveKey(testApiKey)

    assertEquals(32, key.size)
  }

  @Test
  @DisplayName("Base64URL IV를 12바이트로 디코딩해야 한다")
  fun `Base64URL IV를 12바이트로 디코딩해야 한다`() {
    val iv = AesGcmCrypto.decodeIv(testIvBase64Url)

    assertEquals(12, iv.size)
  }

  @Test
  @DisplayName("AES GCM 암호화 복호화가 일치해야 한다")
  fun `AES GCM 암호화 복호화가 일치해야 한다`() {
    val plaintext = """{"cardNumber":"1111-1111-1111-1111","amount":10000}"""
    val key = AesGcmCrypto.deriveKey(testApiKey)
    val iv = AesGcmCrypto.decodeIv(testIvBase64Url)

    val encrypted = AesGcmCrypto.encrypt(plaintext, key, iv)
    val decrypted = AesGcmCrypto.decrypt(encrypted, key, iv)

    assertEquals(plaintext, decrypted)
  }

  @Test
  @DisplayName("Base64URL 패딩없이 인코딩해야 한다")
  fun `Base64URL 패딩없이 인코딩해야 한다`() {
    val plaintext = """{"test":"data"}"""
    val key = AesGcmCrypto.deriveKey(testApiKey)
    val iv = AesGcmCrypto.decodeIv(testIvBase64Url)

    val encrypted = AesGcmCrypto.encrypt(plaintext, key, iv)

    // Base64URL 패딩 문자(=)가 없어야 함
    assertFalse(encrypted.contains("="))
    // Base64URL 문자만 포함 (A-Z, a-z, 0-9, -, _)
    assertTrue(encrypted.matches(Regex("[A-Za-z0-9_-]+")))
  }

  @Test
  @DisplayName("동일 입력에 대해 동일 암호문이 생성되어야 한다")
  fun `동일 입력에 대해 동일 암호문이 생성되어야 한다`() {
    val plaintext = """{"cardNumber":"1111-1111-1111-1111","amount":10000}"""
    val key = AesGcmCrypto.deriveKey(testApiKey)
    val iv = AesGcmCrypto.decodeIv(testIvBase64Url)

    val encrypted1 = AesGcmCrypto.encrypt(plaintext, key, iv)
    val encrypted2 = AesGcmCrypto.encrypt(plaintext, key, iv)

    // 동일 key, iv, plaintext → 동일 ciphertext
    assertEquals(encrypted1, encrypted2)
  }
}

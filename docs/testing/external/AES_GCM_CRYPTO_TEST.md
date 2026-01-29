# AesGcmCryptoTest

> 파일: `modules/external/pg-client/src/test/kotlin/im/bigs/pg/external/pg/crypto/AesGcmCryptoTest.kt`

## 개요

| 항목        | 내용           |
| ----------- | -------------- |
| 테스트 대상 | `AesGcmCrypto` |
| 테스트 유형 | 단위 테스트    |
| 의존성      | 없음 (JCE)     |

## 테스트 케이스

### 1. SHA256으로 API_KEY를 32바이트 키로 변환해야 한다

**검증 항목**:

- `deriveKey(apiKey)` 결과가 32바이트
- AES-256 키 요구사항 충족

---

### 2. Base64URL IV를 12바이트로 디코딩해야 한다

**검증 항목**:

- `decodeIv(base64Url)` 결과가 12바이트
- AES-GCM IV 요구사항 충족

---

### 3. AES GCM 암호화 복호화가 일치해야 한다

**시나리오**:

```json
{ "cardNumber": "1111-1111-1111-1111", "amount": 10000 }
```

**검증 항목**:

- `encrypt()` → `decrypt()` 결과가 원본과 동일

---

### 4. Base64URL 패딩없이 인코딩해야 한다

**검증 항목**:

- 암호문에 `=` 패딩 문자 없음
- URL-safe 문자만 사용 (`A-Za-z0-9_-`)

---

### 5. 동일 입력에 대해 동일 암호문이 생성되어야 한다

**검증 항목**:

- 동일 key + iv + plaintext → 동일 ciphertext
- 결정적 암호화 동작 확인

---

## 테스트 실행

```bash
./gradlew :modules:external:pg-client:test --tests "AesGcmCryptoTest"
```

## 관련 코드

- `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/crypto/AesGcmCrypto.kt`

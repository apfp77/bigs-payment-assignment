# TestPgClientTest

> 파일: `modules/external/pg-client/src/test/kotlin/im/bigs/pg/external/pg/TestPgClientTest.kt`

## 개요

| 항목        | 내용                   |
| ----------- | ---------------------- |
| 테스트 대상 | `TestPgClient`         |
| 테스트 유형 | 단위 테스트 (WireMock) |
| 의존성      | WireMock, RestTemplate |

## 테스트 케이스

### 1. partnerId=2만 지원해야 한다

**검증 항목**:

- `supports(1L)` → `false`
- `supports(2L)` → `true`
- `supports(3L)` → `false`

---

### 2. 성공 카드로 승인 요청 시 APPROVED 반환

**시나리오**:

- WireMock이 200 OK 응답
- approvalCode: "10080728"

**검증 항목**:

- `status` = `APPROVED`
- `approvalCode` 반환됨
- `approvedAt` not null

---

### 3. 422 응답 시 PgRejectedException 발생

**시나리오**:

- WireMock이 422 Unprocessable Entity 반환
- errorCode: "INSUFFICIENT_LIMIT"

**검증 항목**:

- `PgRejectedException` 발생
- errorCode, message, referenceId 매핑됨

---

### 4. 401 응답 시 PgAuthenticationException 발생

**시나리오**:

- WireMock이 401 Unauthorized 반환

**검증 항목**:

- `PgAuthenticationException` 발생

---

### 5. 5xx 응답 시 PgServerException 발생

**시나리오**:

- WireMock이 500 Internal Server Error 반환

**검증 항목**:

- `PgServerException` 발생

---

### 6. 요청에 enc 필드가 포함되어야 한다

**검증 항목**:

- HTTP 요청 body에 `$.enc` 필드 존재
- AES-GCM 암호화된 데이터 전송 확인

---

## 테스트 실행

```bash
./gradlew :modules:external:pg-client:test --tests "TestPgClientTest"
```

## 관련 문서

- `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/TestPgClient.kt`
- [AES_GCM_CRYPTO_TEST.md](./AES_GCM_CRYPTO_TEST.md)
- [TEST_PG_CLIENT.md](../../payment/pg/TEST_PG_CLIENT.md)

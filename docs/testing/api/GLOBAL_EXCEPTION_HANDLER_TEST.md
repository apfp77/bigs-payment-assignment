# GlobalExceptionHandlerTest

> 파일: `modules/bootstrap/api-payment-gateway/src/test/kotlin/im/bigs/pg/api/config/GlobalExceptionHandlerTest.kt`

## 개요

| 항목        | 내용                     |
| ----------- | ------------------------ |
| 테스트 대상 | `GlobalExceptionHandler` |
| 테스트 유형 | 단위 테스트              |
| 의존성      | 없음                     |

## 테스트 케이스

### 1. PartnerNotFoundException은 404 NOT_FOUND를 반환해야 한다

**검증 항목**:

- HTTP 상태: `404 NOT_FOUND`
- code: "PARTNER_NOT_FOUND"
- message: "Partner not found: 999"

---

### 2. PartnerInactiveException은 400 BAD_REQUEST를 반환해야 한다

**검증 항목**:

- HTTP 상태: `400 BAD_REQUEST`
- code: "PARTNER_INACTIVE"

---

### 3. FeePolicyNotFoundException은 500 INTERNAL_SERVER_ERROR를 반환해야 한다

**검증 항목**:

- HTTP 상태: `500 INTERNAL_SERVER_ERROR`
- code: "FEE_POLICY_NOT_FOUND"

---

### 4. PgClientNotFoundException은 400 BAD_REQUEST 반환해야 한다

**검증 항목**:

- HTTP 상태: `500 BAD_REQUEST`
- code: "PG_CLIENT_NOT_FOUND"

---

## HTTP 상태 코드 매핑 요약

| 예외                         | HTTP 상태 | 사유            |
| ---------------------------- |---------| --------------- |
| `PartnerNotFoundException`   | 404     | 리소스 없음     |
| `PartnerInactiveException`   | 400     | 클라이언트 오류 |
| `FeePolicyNotFoundException` | 500     | 서버 설정 오류  |
| `PgClientNotFoundException`  | 400     | 서버 설정 오류  |

## 테스트 실행

```bash
./gradlew :modules:bootstrap:api-payment-gateway:test --tests "GlobalExceptionHandlerTest"
```

## 관련 코드

- `modules/bootstrap/api-payment-gateway/src/main/kotlin/im/bigs/pg/api/config/GlobalExceptionHandler.kt`

# 결제 도메인 서버

나노바나나 페이먼츠의 결제 도메인 서버입니다.

## 프로젝트 개요

### 배경
나노바나나 페이먼츠는 여러 제휴사와 PG(Payment Gateway)를 연동하여 결제 서비스를 제공합니다.  
본 프로젝트는 **결제 승인**, **결제 내역 조회**, **제휴사별 수수료 정책 관리**를 담당하는 백엔드 서버입니다.

### 주요 기능
- **결제 생성**: 외부 PG 연동을 통한 카드 결제 승인 및 수수료 계산
- **결제 조회**: 다양한 필터 조건과 커서 기반 페이지네이션을 지원하는 조회 API
- **통계 집계**: 조회 조건에 맞는 결제 건수, 총 금액, 총 정산금 계산
- **수수료 정책**: 제휴사별, 시점별 수수료 정책 적용 (effective_from 기준)
- **다중 PG 지원**: MockPG(테스트용), TestPG(실제 연동) 어댑터 구현

---

## 구현 완료 기능

### 필수 요구사항

| 기능 | 설명 |
|------|------|
| 결제 생성 API |  `POST /api/v1/payments` - PG 연동 후 수수료/정산금 계산하여 저장 |
| 결제 조회 API |  `GET /api/v1/payments` - 필터(partnerId, status, from, to) + 커서 페이지네이션 |
| 통계 집계 |  필터 조건과 동일한 집합에 대해 count, totalAmount, totalNetAmount 계산 |
| 수수료 정책 적용 |  effective_from 기준 최신 정책 조회, HALF_UP 반올림 |
| TestPG 연동 |  AES-256-GCM 암호화, REST API 연동 |
| 민감정보 보호 |  카드번호 마스킹, 부분 저장만 수행 |

### 선택 요구사항

| 기능 | 설명 |
|------|------|
| 추가 PG 연동 |  MockPG, TestPG 어댑터 구현 |
| OpenAPI 문서화 |  Swagger UI (springdoc-openapi) |
| 외부 DB 전환 |  MariaDB + docker-compose + Flyway 마이그레이션 |

### 테스트 현황

| 레이어 | 테스트 파일 | 테스트 수 |
|--------|-------------|-----------|
| Domain | FeeCalculatorTest | 2 |
| Application | PaymentServiceTest | 6 |
| Application | QueryPaymentsServiceTest | 14 |
| Infrastructure | PaymentRepositoryPagingTest | 8 |
| Infrastructure | FeePolicyPersistenceAdapterTest | 3 |
| External | TestPgClientTest | 6 |
| External | AesGcmCryptoTest | 5 |
| API | GlobalExceptionHandlerTest | 8 |
| API | PaymentApiIntegrationTest | 17 |
| **합계** | **9개 파일** | **69개** |

---

## 기술 스택

| 구분         | 기술                                  |
| ------------ | ------------------------------------- |
| Language     | Kotlin, JDK 22 (빌드), JDK 21+ (실행) |
| Framework    | Spring Boot 3.4                       |
| Architecture | 헥사고널 (멀티모듈)                   |
| Database     | MariaDB (운영), H2 (테스트)           |
| Migration    | Flyway                                |
| API 문서     | Swagger (springdoc-openapi)           |

## 프로젝트 구조

```
modules/
├── domain/           # 순수 도메인 모델 (프레임워크 의존 없음)
├── application/      # 유스케이스, 포트 정의
├── infrastructure/
│   └── persistence/  # JPA 엔티티, 리포지토리 어댑터
├── external/
│   └── pg-client/    # PG 연동 어댑터 (Mock, TestPG)
└── bootstrap/
    └── api-payment-gateway/  # Spring Boot API 모듈
```

## 실행 방법

### 사전 요구사항

- JDK 22 (빌드용)
- Docker (MariaDB 실행용)

### 환경 설정

```bash
cp .env.example .env
# .env 파일에서 환경변수 수정
```

### 데이터베이스 실행

```bash
docker-compose up -d
```

### 빌드 및 테스트

```bash
./gradlew build          # 컴파일 + 모든 테스트
./gradlew test           # 테스트만
./gradlew ktlintFormat   # 코드 스타일 자동정렬
```

### 애플리케이션 실행

```bash
./gradlew :modules:bootstrap:api-payment-gateway:bootRun
```

기본 포트: **8080**

## 데이터베이스 스키마

| 테이블               | 설명                                                            |
| -------------------- | --------------------------------------------------------------- |
| `partner`            | 제휴사 정보 (id, code, name, active)                            |
| `partner_fee_policy` | 수수료 정책 (partner_id, effective_from, percentage, fixed_fee) |
| `payment`            | 결제 내역 (금액, 수수료, 승인정보, 상태 등)                     |

스키마 상세: `db/migration/V1__init.sql`

---

## 상세 문서

| 문서                                                 | 설명                           |
| ---------------------------------------------------- | ------------------------------ |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md)              | 모듈 구조 및 헥사고널 아키텍처 |
| [PAYMENT_FLOW.md](docs/payment/flow/PAYMENT_FLOW.md) | 결제 생성 흐름 상세            |
| [PG 클라이언트 문서](docs/payment/pg/)               | MockPG, TestPG 연동 가이드     |
| [테스트 문서](docs/testing/)                         | 테스트 코드 문서               |
| [REQUIREMENTS.md](docs/REQUIREMENTS.md)              | 원본 과제 요구사항             |

## 환경 변수

### 데이터베이스 (MariaDB)

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `DB_URL` | JDBC 접속 URL | `jdbc:mariadb://localhost:3306/pgdb` |
| `DB_USERNAME` | DB 사용자명 | `pguser` |
| `DB_PASSWORD` | DB 비밀번호 | `pgpass` |

### TestPG 연동 (선택)

| 변수 | 설명 | 기본값 (테스트용) |
|------|------|-------------------|
| `TEST_PG_BASE_URL` | TestPG API 주소 | `https://api-test-pg.bigs.im` |
| `TEST_PG_API_KEY` | 인증 키 (UUID) | `11111111-1111-4111-8111-111111111111` |
| `TEST_PG_IV` | 암호화 IV (Base64URL) | `AAAAAAAAAAAAAAAA` |

### Docker Compose (선택)

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `MARIADB_ROOT_PASSWORD` | root 비밀번호 | `rootpass` |
| `MARIADB_DATABASE` | 데이터베이스명 | `pgdb` |
| `MARIADB_USER` | 사용자명 | `pguser` |
| `MARIADB_PASSWORD` | 비밀번호 | `pgpass` |
| `MARIADB_PORT` | 포트 | `3306` |

> 💡 `.env.example`을 `.env`로 복사 후 필요에 따라 수정하세요.

## API 문서

Swagger UI: http://localhost:8080/swagger-ui.html

---

## API 사양

### 1. 결제 생성

**엔드포인트:** `POST /api/v1/payments`

**요청 예시 (MockPG - partnerId: 1):**

```json
{
  "partnerId": 1,
  "amount": 10000,
  "pgCardData": {
    "type": "MOCK",
    "cardBin": "123456",
    "cardLast4": "4242"
  }
}
```

**요청 예시 (TestPG - partnerId: 2):**

```json
{
  "partnerId": 2,
  "amount": 10000,
  "pgCardData": {
    "type": "TEST_PG",
    "cardNumber": "1111-1111-1111-1111",
    "birthDate": "19900101",
    "expiry": "1227",
    "cardPassword": "12"
  }
}
```

**성공 응답 (200 OK):**

```json
{
  "id": 99,
  "partnerId": 1,
  "amount": 10000,
  "appliedFeeRate": 0.03,
  "feeAmount": 400,
  "netAmount": 9600,
  "cardLast4": "4242",
  "approvalCode": "10080728",
  "approvedAt": "2025-01-27 10:00:00",
  "status": "APPROVED",
  "failureCode": null,
  "failureMessage": null,
  "failedAt": null,
  "createdAt": "2025-01-27 10:00:00"
}
```

**에러 응답:**

모든 에러 응답은 동일한 `ErrorResponse` 형식을 따릅니다:

```json
{
  "code": "에러_코드",
  "message": "에러 상세 메시지",
  "timestamp": "2025-01-27T10:00:00Z"
}
```

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|-----------|------|
| 400 | `PARTNER_INACTIVE` | 비활성 제휴사로 결제 시도 |
| 400 | `INVALID_PG_CARD_DATA` | 카드 데이터 형식 오류 |
| 400 | `PG_CLIENT_NOT_FOUND` | 제휴사에 맞는 PG 클라이언트 없음 |
| 400 | `VALIDATION_FAILED` | 요청 필드 검증 실패 |
| 404 | `PARTNER_NOT_FOUND` | 존재하지 않는 제휴사 |
| 422 | `PG_REJECTED` | PG에서 결제 거절 (한도 초과 등) |
| 500 | `FEE_POLICY_NOT_FOUND` | 수수료 정책 없음 |
| 500 | `PG_AUTH_FAILED` | PG 인증 실패 (API KEY 오류) |
| 502 | `PG_SERVER_ERROR` | 외부 PG 서버 장애 |

**에러 응답 예시:**

400 Bad Request (비활성 제휴사):
```json
{
  "code": "PARTNER_INACTIVE",
  "message": "제휴사가 비활성 상태입니다: partnerId=3",
  "timestamp": "2025-01-27T10:00:00Z"
}
```

404 Not Found (제휴사 없음):
```json
{
  "code": "PARTNER_NOT_FOUND",
  "message": "제휴사를 찾을 수 없습니다: partnerId=999",
  "timestamp": "2025-01-27T10:00:00Z"
}
```

422 Unprocessable Entity (PG 거절):
```json
{
  "code": "PG_REJECTED",
  "message": "결제가 거절되었습니다: 한도 초과 (ref: ref-123)",
  "timestamp": "2025-01-27T10:00:00Z"
}
```

500 Internal Server Error (수수료 정책 없음):
```json
{
  "code": "FEE_POLICY_NOT_FOUND",
  "message": "수수료 정책을 찾을 수 없습니다: partnerId=1",
  "timestamp": "2025-01-27T10:00:00Z"
}
```

---

### 2. 결제 조회 (통계 + 커서 페이지네이션)

**엔드포인트:** `GET /api/v1/payments`

**쿼리 파라미터:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| partnerId | Long | N | 제휴사 ID |
| status | String | N | 결제 상태 (APPROVED, REJECTED) |
| from | ISO DateTime | N | 조회 시작 시각 |
| to | ISO DateTime | N | 조회 종료 시각 |
| cursor | String | N | 페이지네이션 커서 |
| limit | Int | N | 페이지 크기 (기본 20) |

**요청 예시:**

```
GET /api/v1/payments?partnerId=1&status=APPROVED&from=2025-01-01T00:00:00Z&to=2025-01-31T23:59:59Z&limit=20
```

**응답 (200 OK):**

```json
{
  "items": [
    {
      "id": 99,
      "partnerId": 1,
      "amount": 10000,
      "appliedFeeRate": 0.03,
      "feeAmount": 400,
      "netAmount": 9600,
      "cardLast4": "4242",
      "approvalCode": "10080728",
      "approvedAt": "2025-01-27 10:00:00",
      "status": "APPROVED",
      "createdAt": "2025-01-27 10:00:00"
    }
  ],
  "summary": {
    "count": 35,
    "totalAmount": 350000,
    "totalNetAmount": 339500
  },
  "nextCursor": "eyJjcmVhdGVkQXQiOi4uLn0=",
  "hasNext": true
}
```

**정렬 기준:** `createdAt DESC, id DESC`

---
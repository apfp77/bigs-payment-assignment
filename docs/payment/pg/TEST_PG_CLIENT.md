# TestPgClient

> 파일: `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/TestPgClient.kt`

## 개요

**TestPgClient**는 TestPg API와 실제 연동하는 클라이언트입니다.

| 항목            | 내용                |
| --------------- | ------------------- |
| 지원 partnerId  | `2L`                |
| 필요한 CardData | `TestPgCardDataDto` |
| 네트워크 호출   | ✅ RestTemplate     |
| 암호화          | AES-256-GCM         |

## 설정 프로퍼티

```yaml
pg:
  test:
    base-url: https://testpg.example.com
    api-key: ${PG_API_KEY}
    iv: ${PG_IV}
```

## CardData 형식

```kotlin
data class TestPgCardDataDto(
    val cardNumber: String,   // 카드번호 (하이픈 포함)
    val birthDate: String,    // 생년월일 (YYYYMMDD)
    val expiry: String,       // 유효기간 (MMYY)
    val cardPassword: String  // 비밀번호 앞 2자리
)
```

## API 요청 예시

```json
{
  "partnerId": 2,
  "amount": 10000,
  "pgCardData": {
    "type": "TestPg",
    "cardNumber": "1234-5678-9012-3456",
    "birthDate": "19900101",
    "expiry": "1227",
    "cardPassword": "12"
  }
}
```

## 암호화 흐름

1. `TestPgCardDataDto` → `TestPgPayload` 변환
2. JSON 직렬화 → AES-256-GCM 암호화
3. Base64URL 인코딩 후 `enc` 필드로 전송
4. `API-KEY` 헤더에 인증키 포함

## 에러 처리

| HTTP 상태 | 예외                        | 설명                        |
| --------- | --------------------------- | --------------------------- |
| 401       | `PgAuthenticationException` | API-KEY 인증 실패           |
| 422       | `PgRejectedException`       | 카드 거절 (한도 초과 등)    |
| 500       | `PgAuthenticationException` | PG API-KEY 설정 오류 (서버) |
| 502       | `PgServerException`         | PG 서버 오류 (외부 장애)    |

> [!NOTE]
> 예외 클래스는 `im.bigs.pg.application.pg.port.out` 패키지에 위치합니다.
> 헥사고날 아키텍처 원칙에 따라 Application 포트에 정의되어 있습니다.

### 422 에러 코드 상세

| code | errorCode            | message                         |
| ---- | -------------------- | ------------------------------- |
| 1001 | `STOLEN_OR_LOST`     | 도난 또는 분실된 카드입니다.    |
| 1002 | `INSUFFICIENT_LIMIT` | 한도가 초과되었습니다.          |
| 1003 | `EXPIRED_OR_BLOCKED` | 정지되었거나 만료된 카드입니다. |
| 1004 | `TAMPERED_CARD`      | 위조 또는 변조된 카드입니다.    |
| 1005 | `TAMPERED_CARD`      | 허용되지 않은 카드입니다.       |

## 테스트 방법

WireMock을 사용한 단위 테스트:

- [TEST_PG_CLIENT_TEST.md](../../testing/external/TEST_PG_CLIENT_TEST.md)

## 관련 코드

- `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/TestPgClient.kt`
- `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/crypto/AesGcmCrypto.kt`
- `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/config/TestPgProperties.kt`

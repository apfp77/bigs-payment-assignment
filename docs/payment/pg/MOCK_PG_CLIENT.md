# MockPgClient

> 파일: `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/MockPgClient.kt`

## 개요

**MockPgClient**는 로컬 개발 및 테스트 환경을 위한 더미 PG 클라이언트입니다.

| 항목            | 내용                |
| --------------- | ------------------- |
| 지원 partnerId  | `1L`                |
| 필요한 CardData | `MockPgCardDataDto` |
| 네트워크 호출   | ❌ 없음             |
| 결과            | 항상 `APPROVED`     |

## 동작 방식

1. `partnerId == 1L` 인 경우에만 지원
2. 실제 네트워크 호출 없이 즉시 승인 처리
3. approvalCode = `MMdd` + 랜덤 4자리 (예: `01290728`)
4. CardData에서 `cardBin`, `cardLast4` 추출

## CardData 형식

```kotlin
data class MockPgCardDataDto(
    val cardBin: String,      // 카드 앞 6자리
    val cardLast4: String,    // 카드 뒤 4자리
    val productName: String?  // 상품명 (선택)
)
```

## 입력 검증 규칙

| 필드        | 규칙       | 에러 메시지                               |
| ----------- | ---------- | ----------------------------------------- |
| `cardBin`   | 숫자 6자리 | 카드 BIN은 숫자 6자리여야 합니다          |
| `cardLast4` | 숫자 4자리 | 카드 마지막 4자리는 숫자 4자리여야 합니다 |

검증 실패 시 `400 BAD_REQUEST` 응답:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "pgCardData.cardBin: 카드 BIN은 숫자 6자리여야 합니다",
  "timestamp": "2026-01-29T12:00:00Z"
}
```

## API 요청 예시

```json
{
  "partnerId": 1,
  "amount": 10000,
  "pgCardData": {
    "type": "MockPg",
    "cardBin": "123456",
    "cardLast4": "4242",
    "productName": "테스트 상품"
  }
}
```

## 사용 시나리오

- 로컬 개발 환경에서 결제 흐름 테스트
- 통합 테스트 (`PaymentApiIntegrationTest`)
- PG 연동 없이 비즈니스 로직 검증

## 관련 코드

- `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/MockPgClient.kt`
- `modules/application/src/main/kotlin/im/bigs/pg/application/pg/port/out/PgCardDataDto.kt`

# NewPgClient

> 파일: `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/NewPgClient.kt`

## 개요

**NewPgClient**는 토큰 기반 결제를 지원하는 PG 클라이언트입니다.

| 항목            | 내용               |
| --------------- | ------------------ |
| 지원 partnerId  | `3L`               |
| 필요한 CardData | `NewPgCardDataDto` |
| 결제 방식       | 토큰 기반          |
| 카드정보        | 저장하지 않음      |

## 특징

- **토큰 기반 결제**: 카드번호 대신 사전 발급된 결제 토큰 사용
- **cardBin/cardLast4 없음**: 토큰 결제이므로 카드 식별정보 미반환
- **현재 시뮬레이션**: 실제 외부 API 호출 없이 더미 동작

## CardData 형식

```kotlin
data class NewPgCardDataDto(
    val paymentToken: String,  // 사전 발급된 결제 토큰
    val merchantId: String,    // 가맹점 ID
    val orderId: String        // 주문 ID
)
```

## API 요청 예시

```json
{
  "partnerId": 3,
  "amount": 10000,
  "pgCardData": {
    "type": "NewPg",
    "paymentToken": "tok_xxxxxxxx",
    "merchantId": "M001",
    "orderId": "ORD-2025-0001"
  }
}
```

## 응답 특이사항

토큰 기반 결제이므로 카드 정보가 응답에 포함되지 않습니다:

```json
{
  "approvalCode": "NEW123456",
  "status": "APPROVED",
  "cardBin": null,
  "cardLast4": null
}
```

## 사용 시나리오

- 정기 결제 (Subscription)
- 간편 결제 (저장된 카드)
- 원클릭 결제

## 관련 코드

- `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/NewPgClient.kt`
- `modules/application/src/main/kotlin/im/bigs/pg/application/pg/port/out/PgCardDataDto.kt`

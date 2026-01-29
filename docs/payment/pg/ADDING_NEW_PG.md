# 새로운 PG 추가 가이드

새로운 Payment Gateway를 시스템에 추가하는 체크리스트입니다.

## 체크리스트

### 1. API Layer - 카드 데이터 DTO

**파일**: `modules/bootstrap/api-payment-gateway/src/main/kotlin/im/bigs/pg/api/payment/dto/PgCardData.kt`

```kotlin
/** FooPG (partnerId=4) 결제용 카드 정보. */
@Schema(description = "FooPG 결제용 카드 정보")
data class FooPgCardData(
    @get:Schema(description = "구분자", example = "FOO_PG", allowableValues = ["FOO_PG"])
    val type: String = "FOO_PG",

    @get:Schema(description = "필드1 설명", example = "example1")
    val field1: String,

    @get:Schema(description = "필드2 설명", example = "example2")
    val field2: String,
) : PgCardData
```

**`@JsonSubTypes`에 추가**:

```kotlin
@JsonSubTypes(
    // 기존...
    JsonSubTypes.Type(value = FooPgCardData::class, name = "FOO_PG"),
)
```

**`@Schema discriminatorMapping`에 추가**:

```kotlin
@Schema(
    discriminatorMapping = [
        // 기존...
        DiscriminatorMapping(value = "FOO_PG", schema = FooPgCardData::class),
    ],
)
```

---

### 2. Application Layer - 카드 데이터 DTO

**파일**: `modules/application/src/main/kotlin/im/bigs/pg/application/pg/port/out/PgCardDataDto.kt`

```kotlin
data class FooPgCardDataDto(
    val field1: String,
    val field2: String,
) : PgCardDataDto
```

---

### 3. API → Application 변환 로직

**파일**: `modules/bootstrap/api-payment-gateway/src/main/kotlin/im/bigs/pg/api/payment/PaymentController.kt`

`toPgCardDataDto()` 확장 함수에 케이스 추가:

```kotlin
private fun PgCardData.toPgCardDataDto(): PgCardDataDto = when (this) {
    // 기존...
    is FooPgCardData -> FooPgCardDataDto(field1, field2)
}
```

---

### 4. PG Client 구현

**파일**: `modules/external/pg-client/src/main/kotlin/im/bigs/pg/external/pg/FooPgClient.kt`

```kotlin
@Component
class FooPgClient : PgClientOutPort {
    override fun supports(partnerId: Long) = partnerId == 4L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        val cardData = request.pgCardData as? FooPgCardDataDto
            ?: error("FooPgCardDataDto is required for FooPG")

        // PG 연동 로직...

        return PgApproveResult(
            approvalCode = "...",
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            cardBin = "...",      // 또는 null
            cardLast4 = "...",    // 또는 null
        )
    }
}
```

---

### 5. PaymentService 검증 로직

**파일**: `modules/application/src/main/kotlin/im/bigs/pg/application/payment/service/PaymentService.kt`

`validatePgCardData()` 함수에 케이스 추가:

```kotlin
private fun validatePgCardData(partnerId: Long, command: PaymentCommand) {
    val (expectedType, isValid) = when (partnerId) {
        // 기존...
        4L -> "FooPgCardData" to (command.pgCardData is FooPgCardDataDto)
        else -> return
    }
    // ...
}
```

---

### 6. Swagger 예제 등록

**파일**: `modules/bootstrap/api-payment-gateway/src/main/kotlin/im/bigs/pg/api/config/SwaggerConfig.kt`

`buildPaymentExamples()`에 Triple 추가:

```kotlin
private fun buildPaymentExamples() = listOf(
    // 기존...
    Triple("FOO_PG", FooPgCardData::class, 4L),
)
```

---

### 7. 시드 데이터 (개발/테스트용)

**파일**: `modules/bootstrap/api-payment-gateway/src/main/kotlin/im/bigs/pg/api/config/DataInitializer.kt`

```kotlin
val p4 = partnerRepo.save(PartnerEntity(code = "FOOPG1", name = "FooPG Partner 1", active = true))
feeRepo.save(
    FeePolicyEntity(
        partnerId = p4.id!!,
        effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
        percentage = BigDecimal("0.0200"),
        fixedFee = BigDecimal("30"),
    ),
)
```

---

### 8. 테스트 작성

- **단위 테스트**: `FooPgClientTest.kt`
- **통합 테스트**: `PaymentApiIntegrationTest`에 FOO_PG 케이스 추가

---

### 9. 문서 업데이트

- [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) - PG 클라이언트 전략 표, 시드 데이터 표
- [PG_CARD_DATA_POLYMORPHISM.md](./PG_CARD_DATA_POLYMORPHISM.md) - 새 PG 구현체 설명

---

## 요약

| 단계 | 파일                   | 작업                                      |
| ---- | ---------------------- | ----------------------------------------- |
| 1    | `PgCardData.kt`        | API DTO + `@JsonSubTypes` + discriminator |
| 2    | `PgCardDataDto.kt`     | Application DTO                           |
| 3    | `PaymentController.kt` | 변환 함수                                 |
| 4    | `FooPgClient.kt`       | PG Client 구현 (신규)                     |
| 5    | `PaymentService.kt`    | 검증 로직                                 |
| 6    | `SwaggerConfig.kt`     | 예제 등록                                 |
| 7    | `DataInitializer.kt`   | 시드 데이터                               |
| 8    | 테스트                 | 단위/통합 테스트                          |
| 9    | 문서                   | ARCHITECTURE, PG_CARD_DATA_POLYMORPHISM   |

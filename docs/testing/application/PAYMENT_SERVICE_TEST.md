# PaymentServiceTest

> 파일: `modules/application/src/test/kotlin/im/bigs/pg/application/payment/service/PaymentServiceTest.kt`

## 개요

| 항목        | 내용                |
| ----------- | ------------------- |
| 테스트 대상 | `PaymentService`    |
| 테스트 유형 | 단위 테스트 (MockK) |
| 의존성      | MockK               |

## 테스트 케이스

### 예외 테스트

#### 1. 존재하지 않는 제휴사 ID로 결제 시 PartnerNotFoundException이 발생해야 한다

**시나리오**: partnerId: 999L (존재하지 않음)

**검증 항목**:

- `PartnerNotFoundException` 발생
- errorCode: "PARTNER_NOT_FOUND"

---

#### 2. 비활성 제휴사로 결제 시 PartnerInactiveException이 발생해야 한다

**시나리오**: partnerId: 1L, active: false

**검증 항목**:

- `PartnerInactiveException` 발생
- errorCode: "PARTNER_INACTIVE"

---

#### 3. 수수료 정책이 없으면 FeePolicyNotFoundException이 발생해야 한다

**시나리오**: partnerId: 1L에 정책 없음

**검증 항목**:

- `FeePolicyNotFoundException` 발생
- errorCode: "FEE_POLICY_NOT_FOUND"

---

### 수수료 정책 적용 테스트

#### 4. 수수료 정책에서 퍼센트만 적용되어야 한다

**시나리오**:

- 금액: 10,000원
- 정책: 2.35%, 정액 없음

**검증 항목**:

- feeAmount: 235원
- netAmount: 9,765원
- appliedFeeRate: 0.0235

---

#### 5. 수수료 정책에서 퍼센트와 정액 수수료가 함께 적용되어야 한다

**시나리오**:

- 금액: 10,000원
- 정책: 3.00% + 정액 100원

**검증 항목**:

- feeAmount: 400원 (300 + 100)
- netAmount: 9,600원
- status: APPROVED

---

#### 6. 결제 생성 시 적용된 수수료율이 저장되어야 한다

**시나리오**:

- 금액: 5,000원
- 정책: 2.50% + 정액 50원

**검증 항목**:

- appliedFeeRate: 0.0250
- feeAmount: 175원 (125 + 50)
- netAmount: 4,825원

---

## 테스트 실행

```bash
./gradlew :modules:application:test --tests "PaymentServiceTest"
```

## 관련 코드

- `modules/application/src/main/kotlin/im/bigs/pg/application/payment/service/PaymentService.kt`

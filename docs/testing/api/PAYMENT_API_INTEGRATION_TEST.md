# PaymentApiIntegrationTest

> 파일: `modules/bootstrap/api-payment-gateway/src/test/kotlin/im/bigs/pg/api/payment/PaymentApiIntegrationTest.kt`

## 개요

| 항목        | 내용                                            |
| ----------- | ----------------------------------------------- |
| 테스트 대상 | `POST /api/v1/payments`, `GET /api/v1/payments` |
| 테스트 유형 | 통합 테스트 (@SpringBootTest)                   |
| 의존성      | H2, MockPgClient (partnerId=1)                  |

## 테스트 케이스

### 결제 생성 테스트

#### 1. 결제 생성이 성공해야 한다

**검증 항목**:

- HTTP 200 OK
- id, partnerId, amount, cardLast4 반환
- status: APPROVED

---

#### 2. 결제 생성 시 수수료 정책이 올바르게 적용되어야 한다

**시나리오**: 10,000원 결제, 2.35% 정책

**검증 항목**:

- appliedFeeRate: 0.0235
- feeAmount: 235
- netAmount: 9,765

---

#### 3. 결제 생성 후 DB에 정확히 저장되어야 한다

**검증 항목**:

- DB에서 직접 조회하여 저장 확인
- partnerId, amount, cardLast4 일치

---

### 결제 조회 테스트

#### 4. 결제 조회 시 전체 목록을 반환해야 한다

**시나리오**: 3건 생성 후 조회

**검증 항목**:

- items.size: 3
- summary.count: 3

---

#### 5. 조회 시 summary가 items와 동일한 집합을 집계해야 한다

**시나리오**: 10건 생성, limit=5

**검증 항목**:

- items.size: 5
- summary.count: 10 (전체)
- hasNext: true

---

#### 6. 커서 페이지네이션으로 모든 데이터를 순회할 수 있어야 한다

**시나리오**: 15건 생성, limit=5로 순회

**검증 항목**:

- 3페이지로 순회
- 총 15건 수집
- 중복 없음

---

#### 7. 마지막 페이지에서 hasNext가 false여야 한다

**검증 항목**:

- hasNext: false
- nextCursor: null

---

#### 8. partnerId 필터로 해당 제휴사만 조회해야 한다

**검증 항목**:

- 모든 항목의 partnerId가 일치

---

### E2E 시나리오 테스트

#### 9. 결제 생성 후 바로 조회할 수 있어야 한다

**시나리오**: 결제 생성 → 목록 조회

**검증 항목**:

- 생성한 결제가 목록에 포함

---

## 테스트 실행

```bash
./gradlew :modules:bootstrap:api-payment-gateway:test --tests "PaymentApiIntegrationTest"
```

## 관련 코드

- `modules/bootstrap/api-payment-gateway/src/main/kotlin/im/bigs/pg/api/payment/PaymentController.kt`

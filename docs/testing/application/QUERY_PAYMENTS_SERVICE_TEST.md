# QueryPaymentsServiceTest

> 파일: `modules/application/src/test/kotlin/im/bigs/pg/application/payment/service/QueryPaymentsServiceTest.kt`

## 개요

| 항목        | 내용                   |
| ----------- | ---------------------- |
| 테스트 대상 | `QueryPaymentsService` |
| 테스트 유형 | 단위 테스트 (MockK)    |
| 의존성      | MockK                  |

## 테스트 케이스

### 필터 조회 테스트

#### 1. 필터 없이 전체 조회 시 모든 결제 내역을 반환해야 한다

**검증 항목**:

- 전체 결제 목록 반환
- summary.count 일치
- hasNext: false

---

#### 2. partnerId 필터로 해당 제휴사만 조회해야 한다

**검증 항목**:

- partnerId: 1L인 결제만 반환
- summary.count: 1

---

#### 3. status 필터로 해당 상태만 조회해야 한다

**검증 항목**:

- status: APPROVED인 결제만 반환

---

### 페이지네이션 테스트

#### 4. 커서 없이 첫 페이지 조회 시 최신 데이터를 반환해야 한다

**검증 항목**:

- limit개 반환
- hasNext: true
- nextCursor 존재

---

#### 5. 마지막 페이지에서 hasNext가 false여야 한다

**검증 항목**:

- hasNext: false
- nextCursor: null

---

### 통계 테스트

#### 6. summary는 페이징과 무관하게 전체 집합을 집계해야 한다

**시나리오**:

- 전체 35건
- limit: 10 (10건만 반환)

**검증 항목**:

- items.size: 10
- summary.count: 35 (전체)

---

### 잘못된 입력 처리 테스트

#### 7. 잘못된 status 값은 null로 처리해야 한다

**시나리오**: status: "INVALID_STATUS"

**검증 항목**:

- 필터 무시하고 전체 조회

---

#### 8. 잘못된 커서 형식은 첫 페이지로 처리해야 한다

**시나리오**: cursor: "invalid_cursor_format"

**검증 항목**:

- 첫 페이지부터 조회

---

## 테스트 실행

```bash
./gradlew :modules:application:test --tests "QueryPaymentsServiceTest"
```

## 관련 코드

- `modules/application/src/main/kotlin/im/bigs/pg/application/payment/service/QueryPaymentsService.kt`

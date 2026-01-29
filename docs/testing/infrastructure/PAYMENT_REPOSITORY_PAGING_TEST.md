# PaymentRepositoryPagingTest

> 파일: `modules/infrastructure/persistence/src/test/kotlin/im/bigs/pg/infra/persistence/PaymentRepositoryPagingTest.kt`

## 개요

| 항목        | 내용                       |
| ----------- | -------------------------- |
| 테스트 대상 | `PaymentJpaRepository`     |
| 테스트 유형 | 통합 테스트 (@DataJpaTest) |
| 의존성      | H2 인메모리 DB             |

## 테스트 케이스

### 1. 커서 페이징과 통계가 일관되어야 한다

**시나리오**:

- 35건의 결제 데이터 생성
- partnerId: 1L, status: APPROVED
- 각 금액: 1,000원

**검증 항목**:

1. **첫 페이지 조회** (limit=21):
   - 21건 반환

2. **두 번째 페이지 조회** (커서 기반):
   - 커서 이후 데이터 반환
   - 중복 없음 확인

3. **통계 조회**:
   - 총 건수: 35건
   - 총 금액: 35,000원
   - 총 정산금: 33,950원

---

### 2. 기간 필터로 해당 기간 데이터만 조회해야 한다

**시나리오**:

- 1/1, 1/10, 1/15, 1/20, 2/1 데이터 생성
- from=1/10, to=1/25 필터

**검증 항목**:

- 1/10, 1/15, 1/20 (3건만 조회)
- 통계도 동일 필터 적용

---

### 3. partnerId 필터로 해당 제휴사 데이터만 조회해야 한다

**시나리오**:

- 파트너 1: 3건
- 파트너 2: 2건

**검증 항목**:

- 파트너 1 조회 시 3건
- 파트너 2 조회 시 2건
- 각 통계도 일치

---

### 4. 정렬 순서가 createdAt desc, id desc여야 한다

**시나리오**: 동일 시간에 여러 건 생성

**검증 항목**:

- ID 내림차순 정렬 확인

---

### 5. 다른 시간대 데이터는 createdAt desc로 정렬되어야 한다

**시나리오**: 서로 다른 시간에 데이터 생성

**검증 항목**:

- createdAt 내림차순 정렬 확인

---

### 6. status 필터로 해당 상태 데이터만 조회해야 한다

**시나리오**:

- APPROVED 3건
- CANCELED 2건

**검증 항목**:

- APPROVED 필터 시 3건
- CANCELED 필터 시 2건

---

### 7. 복합 필터로 조회해야 한다

**시나리오**:

- 파트너 1, APPROVED, 1/15 (1건)
- 파트너 1, CANCELED, 1/15 (1건)
- 파트너 2, APPROVED, 1/15 (1건)
- 파트너 1, APPROVED, 1/1 - 범위 밖 (1건)

**검증 항목**:

- 파트너 1 + APPROVED + 1/10~1/20 기간 → 1건
- 통계도 동일 필터 적용

---

### 8. 빈 결과일 때 빈 목록과 0 통계를 반환해야 한다

**시나리오**: 존재하지 않는 partnerId=999L

**검증 항목**:

- items.size: 0
- count: 0
- totalAmount: 0
- totalNetAmount: 0

---

## 커서 페이징 로직

```sql
SELECT * FROM payments
WHERE (created_at < :cursorCreatedAt)
   OR (created_at = :cursorCreatedAt AND id < :cursorId)
ORDER BY created_at DESC, id DESC
LIMIT :limit
```

## 테스트 실행

```bash
./gradlew :modules:infrastructure:persistence:test --tests "PaymentRepositoryPagingTest"
```

## 관련 코드

- `modules/infrastructure/persistence/src/main/kotlin/im/bigs/pg/infra/persistence/payment/repository/PaymentJpaRepository.kt`
- `modules/infrastructure/persistence/src/main/kotlin/im/bigs/pg/infra/persistence/payment/entity/PaymentEntity.kt`

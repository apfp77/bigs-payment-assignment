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

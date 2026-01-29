# FeePolicyPersistenceAdapterTest

> 파일: `modules/infrastructure/persistence/src/test/kotlin/im/bigs/pg/infra/persistence/partner/FeePolicyPersistenceAdapterTest.kt`

## 개요

| 항목        | 내용                          |
| ----------- | ----------------------------- |
| 테스트 대상 | `FeePolicyPersistenceAdapter` |
| 테스트 유형 | 통합 테스트 (@DataJpaTest)    |
| 의존성      | H2 인메모리 DB                |

## 테스트 케이스

### 1. effective_from 기준 최신 정책이 적용되어야 한다

**시나리오**:

- partnerId: 1L에 3개 정책 존재
  - 2020-01-01: 2.00%, 정액 50원
  - 2024-01-01: 3.00%, 정액 100원 ← 적용됨
  - 2030-01-01: 5.00%, 정액 200원 (미래)
- 조회 시점: 2025-06-01

**검증 항목**:

- 2024년 정책 반환 (2025년 이전 중 가장 최신)
- percentage: 0.0300
- fixedFee: 100

---

### 2. 현재 시점 이전 정책이 없으면 null 반환

**시나리오**:

- partnerId: 2L에 미래 정책만 존재
  - 2030-01-01: 5.00%
- 조회 시점: 2025-01-01

**검증 항목**:

- `findEffectivePolicy()` 결과가 `null`

---

### 3. 다른 partnerId의 정책은 조회되지 않아야 한다

**시나리오**:

- partnerId: 1L → 2.00%
- partnerId: 2L → 4.00%
- partnerId: 1L로 조회

**검증 항목**:

- partnerId: 1L의 정책만 반환
- percentage: 0.0200

---

## 테스트 실행

```bash
./gradlew :modules:infrastructure:persistence:test --tests "FeePolicyPersistenceAdapterTest"
```

## 관련 코드

- `modules/infrastructure/persistence/src/main/kotlin/im/bigs/pg/infra/persistence/partner/adapter/FeePolicyPersistenceAdapter.kt`
- `modules/infrastructure/persistence/src/main/kotlin/im/bigs/pg/infra/persistence/partner/repository/FeePolicyJpaRepository.kt`

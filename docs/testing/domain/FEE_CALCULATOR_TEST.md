# FeeCalculatorTest

> 파일: `modules/domain/src/test/kotlin/im/bigs/pg/domain/calculation/FeeCalculatorTest.kt`

## 개요

| 항목        | 내용               |
| ----------- | ------------------ |
| 테스트 대상 | `FeeCalculator`    |
| 테스트 유형 | 단위 테스트        |
| 의존성      | 없음 (순수 Kotlin) |

## 테스트 케이스

### 1. 퍼센트 수수료만 적용 시 반올림 및 정산금이 정확해야 한다

**시나리오**:

- 금액: 10,000원
- 수수료율: 2.35%
- 정액 수수료: 없음

**검증 항목**:

- 수수료: `10000 × 0.0235 = 235원`
- 정산금: `10000 - 235 = 9,765원`

---

### 2. 퍼센트+정액 수수료가 함께 적용되어야 한다

**시나리오**:

- 금액: 10,000원
- 수수료율: 3.00%
- 정액 수수료: 100원

**검증 항목**:

- 수수료: `(10000 × 0.03) + 100 = 400원`
- 정산금: `10000 - 400 = 9,600원`

---

## 테스트 실행

```bash
./gradlew :modules:domain:test --tests "FeeCalculatorTest"
```

## 관련 코드

- `modules/domain/src/main/kotlin/im/bigs/pg/domain/calculation/FeeCalculator.kt`
- `modules/domain/src/main/kotlin/im/bigs/pg/domain/partner/FeePolicy.kt`

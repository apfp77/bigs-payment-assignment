# 테스트 문서 가이드

이 디렉토리는 프로젝트의 모든 테스트 코드에 대한 문서를 포함합니다.

## 레이어별 테스트 개요

| 레이어                              | 문서                   | 테스트 수 |
| ----------------------------------- | ---------------------- | --------- |
| [Domain](./domain/)                 | 수수료 계산기          | 2개       |
| [External](./external/)             | PG 클라이언트, 암호화  | 9개       |
| [Infrastructure](./infrastructure/) | 레포지토리, 어댑터     | 8개       |
| [Application](./application/)       | 결제/조회 서비스       | 19개      |
| [API](./api/)                       | 예외 처리, 통합 테스트 | 19개      |

## 테스트 실행

### 전체 테스트

```bash
./gradlew test
```

### 레이어별 테스트

```bash
# Domain
./gradlew :modules:domain:test

# External
./gradlew :modules:external:pg-client:test

# Infrastructure
./gradlew :modules:infrastructure:persistence:test

# Application
./gradlew :modules:application:test

# API (통합 테스트)
./gradlew :modules:bootstrap:api-payment-gateway:test
```

## 관련 문서

- [ARCHITECTURE.md](../ARCHITECTURE.md) - 전체 아키텍처
- [PG 클라이언트 가이드](../payment/pg/) - PG 연동 상세

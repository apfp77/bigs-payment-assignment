# Test PG API 연동 문서

## Table of Contents
- [Test PG API 연동 문서](#test-pg-api-연동-문서)
  - [Table of Contents](#table-of-contents)
  - [1. 개요](#1-개요)
  - [2. 서버 주소](#2-서버-주소)
  - [3. 빠른 시작](#3-빠른-시작)
    - [테스트 카드 규칙(요약)](#테스트-카드-규칙요약)
  - [4. 암호화 절차](#4-암호화-절차)
    - [암호화 규격](#암호화-규격)
    - [평문 스키마 요약](#평문-스키마-요약)
    - [HTTP 헤더/바디 구성](#http-헤더바디-구성)
  - [5. 성공 요청/응답](#5-성공-요청응답)
    - [요청 예시](#요청-예시)
    - [응답(200)](#응답200)
  - [6. 실패(422)](#6-실패422)
    - [요청 예시](#요청-예시-1)
    - [응답(422)](#응답422)
    - [에러 코드 매핑](#에러-코드-매핑)
  - [7. 인증 실패(401)](#7-인증-실패401)

## 1. 개요
- Test PG 서버의 카드 결제 API 연동 절차를 설명합니다.
- 모든 요청 본문은 AES-256-GCM으로 암호화된 `enc` 필드 하나만 포함합니다.

## 2. 서버 주소
- Base URL: `https://api-test-pg.bigs.im`

## 3. 빠른 시작
1. 화이트리스트 등록을 통해 `API-KEY`, `IV(Base64URL)`를 발급받습니다.
2. 아래 "암호화 절차"에 따라 평문 JSON을 AES-256-GCM으로 암호화해 `enc`를 생성합니다.
3. 다음 HTTP 요청으로 결제를 호출합니다.

```bash
curl -X POST "https://api-test-pg.bigs.im/api/v1/pay/credit-card" \
  -H "API-KEY: <API-KEY>" \
  -H "Content-Type: application/json" \
  -d '{"enc":"<Base64URL(ciphertext||tag)>"}'
```

### 테스트 카드 규칙(요약)
- 성공: `1111-1111-1111-1111` (필수 필드 유효 시 200)
- 실패: `2222-2222-2222-2222` (422, 금액에 따라 사유 상이)

## 4. 암호화 절차
- 요청 본문 형식: `{ "enc": "<Base64URL(ciphertext||tag)>" }`
- 평문 예시:

```json
{
  "cardNumber": "1111-1111-1111-1111",
  "birthDate": "19900101",
  "expiry": "1227",
  "password": "12",
  "amount": 10000
}
```

- 테스트용 키/IV
  - `API-KEY`: `11111111-1111-4111-8111-111111111111`
  - `IV(Base64URL)`: `AAAAAAAAAAAAAAAA`
- 실제 서비스에서는 발급받은 `API-KEY`, `IV_B64URL`을 사용합니다.

### 암호화 규격
| 항목 | 설명 |
| --- | --- |
| 알고리즘 | AES-256-GCM (`AES/GCM/NoPadding`), 태그 128비트 |
| Key | `SHA-256(API-KEY)` → 32바이트 키 |
| IV | 12바이트, 사전 등록(Base64URL 디코딩하여 사용) |
| CipherText | `GCM(key, iv).encrypt(plaintextBytes)` 결과 `ciphertext||tag` |
| enc | 위 결과를 Base64URL(패딩 없음)로 인코딩 |

### 평문 스키마 요약
- `cardNumber`: 숫자 16자리(`-` 허용), 예 `1111-1111-1111-1111`
- `birthDate`: `YYYYMMDD`, 예 `19900101`
- `expiry`: `MMYY`, 예 `1227`
- `password`: 카드 비밀번호 앞 2자리
- `amount`: 원 단위 정수, 1 이상

### HTTP 헤더/바디 구성
```
Header:  API-KEY: <API-KEY>
Body:    { "enc": "<Base64URL(ciphertext||tag)>" }
```

## 5. 성공 요청/응답
### 요청 예시
```
POST /api/v1/pay/credit-card
Content-Type: application/json;charset=UTF-8
API-KEY: 11111111-1111-4111-8111-111111111111

{
  "enc" : "FlrQ_ZFCA9WC7HIkPzKFpnzv1AX0n7zodWtWRo6X6-..."
}
```

| 필드 | 설명 |
| --- | --- |
| `API-KEY` | UUID v4 인증 키 |
| `enc` | AES-256-GCM Base64URL(ciphertext\|\|tag) |

### 응답(200)
```json
{
  "approvalCode": "10080728",
  "approvedAt": "2025-10-08T03:31:34.181568",
  "maskedCardLast4": "1111",
  "amount": 10000,
  "status": "APPROVED"
}
```

| Path | Type | Description |
| --- | --- | --- |
| `approvalCode` | String | 승인 코드 |
| `approvedAt` | String | 승인 시각(UTC) |
| `maskedCardLast4` | String | 카드 마지막 4자리 |
| `amount` | Number | 결제 금액 |
| `status` | String | 승인 상태 (`APPROVED`) |

## 6. 실패(422)
### 요청 예시
```
POST /api/v1/pay/credit-card
Content-Type: application/json;charset=UTF-8
API-KEY: 22222222-2222-4222-8222-222222222222

{
  "enc" : "raqXo5DGiPaEAYiILJYINKaWQDeVKoRvLZiOEhoe4y..."
}
```

| 필드 | 설명 |
| --- | --- |
| `API-KEY` | UUID v4 인증 키 |
| `enc` | AES-256-GCM Base64URL(ciphertext\|\|tag) |

### 응답(422)
```json
{
  "code": 1002,
  "errorCode": "INSUFFICIENT_LIMIT",
  "message": "한도가 초과되었습니다.",
  "referenceId": "b48c79bd-e1b3-416a-a583-efe90d1ee438"
}
```

| Path | Type | Description |
| --- | --- | --- |
| `code` | Number | 에러 코드 |
| `errorCode` | String | 실패 사유 코드 |
| `message` | String | 실패 메시지 |
| `referenceId` | String | 참조 ID |

### 에러 코드 매핑
| code | errorCode | message |
| --- | --- | --- |
| 1001 | STOLEN_OR_LOST | 도난 또는 분실된 카드입니다. |
| 1002 | INSUFFICIENT_LIMIT | 한도가 초과되었습니다. |
| 1003 | EXPIRED_OR_BLOCKED | 정지되었거나 만료된 카드입니다. |
| 1004 | TAMPERED_CARD | 위조 또는 변조된 카드입니다. |
| 1005 | TAMPERED_CARD | 위조 또는 변조된 카드입니다. (허용되지 않은 카드) |

## 7. 인증 실패(401)
모든 경우 `401 Unauthorized`와 보안 헤더가 반환됩니다.

1. **API-KEY 헤더 없음**
   - 요청: API-KEY 없이 `enc`만 전송
   - 응답: 401 Unauthorized
2. **API-KEY 포맷 오류**
   - 요청: `API-KEY: NOT-UUID`
   - 응답: 401 Unauthorized
3. **미등록 API-KEY**
   - 요청: 등록되지 않은 UUID 사용
   - 응답: 401 Unauthorized

---
출처: https://api-test-pg.bigs.im/docs/index.html

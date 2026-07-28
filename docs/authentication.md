# 인증 설계

작성 기준: 2026-07-28

이 문서는 Staff, Hospital, Beauty, User 4개 actor의 인증 구현 기준을 정의한다.

## 1. 인증 방식

1차 구현은 JWT access token 기반 stateless 인증으로 간다.

- 로그인 성공 시 `access_token`을 발급한다.
- API 요청은 `Authorization: Bearer {token}` 헤더를 사용한다.
- 세션은 서버에 저장하지 않는다.
- logout은 1차에서는 클라이언트 토큰 폐기 요청으로 처리한다.
- refresh token, token blacklist, 기기별 세션 관리는 후속 단계에서 추가한다.

## 2. Actor

```text
STAFF
HOSPITAL
BEAUTY
USER
```

토큰에는 actor 타입, 계정 ID, 소유 리소스 ID, 권한 목록을 넣는다.

| actor | 토큰 주요 claim |
| --- | --- |
| `STAFF` | `actor`, `account_id`, `permissions` |
| `HOSPITAL` | `actor`, `account_id`, `hospital_id` |
| `BEAUTY` | `actor`, `account_id`, `beauty_id` |
| `USER` | `actor`, `account_id` |

## 3. API

각 actor는 같은 인증 API 모양을 사용한다.

```text
POST /api/v1/staff/auth/login
GET  /api/v1/staff/auth/me
POST /api/v1/staff/auth/logout

POST /api/v1/hospital/auth/login
GET  /api/v1/hospital/auth/me
POST /api/v1/hospital/auth/logout

POST /api/v1/beauty/auth/login
GET  /api/v1/beauty/auth/me
POST /api/v1/beauty/auth/logout

POST /api/v1/user/auth/login
GET  /api/v1/user/auth/me
POST /api/v1/user/auth/logout
```

로그인 request:

```json
{
  "email": "admin@example.com",
  "password": "password"
}
```

로그인 response:

```json
{
  "success": true,
  "data": {
    "token_type": "Bearer",
    "access_token": "jwt",
    "expires_in": 7200,
    "actor": {
      "actor_type": "STAFF",
      "account_id": 1,
      "hospital_id": null,
      "beauty_id": null,
      "email": "admin@example.com",
      "name": "관리자",
      "nickname": "admin",
      "permissions": ["platform.hospital.show"]
    }
  },
  "meta": null,
  "traceId": "request-trace-id"
}
```

## 4. 계정 상태 기준

로그인은 활성 계정만 허용한다.

| actor | 활성 상태 |
| --- | --- |
| `STAFF` | `ACTIVE` |
| `HOSPITAL` | `ACTIVE` |
| `BEAUTY` | `ACTIVE` |
| `USER` | `ACTIVE` |

`SUSPENDED`, `BLOCKED`, `WITHDRAWN` 계정은 로그인과 API 접근을 막는다.

## 5. SecurityConfig 기준

Spring Security는 URL namespace 단위로 1차 차단한다.

```text
/api/v1/public/**       permitAll
/api/v1/*/auth/login    permitAll
/api/v1/staff/**        ACTOR_STAFF
/api/v1/hospital/**     ACTOR_HOSPITAL
/api/v1/beauty/**       ACTOR_BEAUTY
/api/v1/user/**         ACTOR_USER
```

세부 기능 권한과 소유권은 application service 진입부에서 검사한다.

## 6. 권한/소유권

- Staff: permission code 기반
- Hospital: `hospital_id` 소유권 기반
- Beauty: `beauty_id` 소유권 기반
- User: 본인 계정 ID와 계정 상태 기반

컨트롤러에 권한 문자열을 흩뿌리지 않는다. 반복되는 권한 검사는 `PermissionService`, 소유권 검사는 actor별 policy/service로 분리한다.

## 7. 패키지 구조

```text
adapter/in/web/{actor}/auth/controller
adapter/in/web/auth/request

application/auth
  command
  result

domain/account

infrastructure/persistence/account

common/security
```

Request DTO는 HTTP 입력 모델이므로 adapter 계층에 둔다. 로그인 로직은 application 계층의 `AuthenticationService`가 담당한다.

## 8. 후속 작업

- refresh token
- token blacklist 기반 logout
- 비밀번호 재설정
- 이메일/휴대폰 인증
- OAuth 소셜 로그인
- login attempt rate limit
- 계정 생성/초대/승인 플로우

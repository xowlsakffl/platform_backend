# 인증과 권한

## Actor

| Actor | namespace | 계정 | 현재 인증 방식 |
|---|---|---|---|
| Staff | `/api/v1/staff` | `account_staffs` | 이메일·비밀번호 |
| Hospital | `/api/v1/hospital` | `account_hospitals` | 이메일·비밀번호 |
| Beauty | `/api/v1/beauty` | `account_beauties` | 이메일·비밀번호 |
| User | `/api/v1/user` | `account_users` | 이메일·비밀번호 |

각 Actor는 `/auth/login`, `/auth/me`, `/auth/logout`을 제공한다. 다른 Actor의 토큰으로 namespace를 호출하면 `403 FORBIDDEN`이다.

## JWT 흐름

1. 이메일을 소문자로 정규화하고 활성 계정을 조회한다.
2. BCrypt 비밀번호를 검증한다.
3. `last_login_at`을 갱신한다.
4. Actor, 계정 ID, 소유 리소스 ID, Staff 권한을 담은 HS256 access token을 발급한다.
5. 매 요청에서 서명·issuer·만료·Redis 폐기 여부를 검증한다.
6. DB에서 계정을 다시 조회해 삭제·정지 상태와 최신 Staff 권한을 확인한다.

JWT는 stateless지만 로그아웃은 무효 응답만 반환하지 않는다. 현재 토큰의 `jti`를 `auth:revoked:{jti}` 키로 Redis에 저장하고 원래 만료 시점까지 재사용을 차단한다.

비밀번호 재설정, refresh token, 소셜 로그인은 아직 구현하지 않았다.

## 계정 상태

네 계정 유형은 `ACTIVE`, `SUSPENDED`, `BLOCKED`, `WITHDRAWN`을 사용한다. 로그인과 인증 유지가 가능한 상태는 `ACTIVE`뿐이다. `deleted_at`이 있는 계정도 인증할 수 없다.

## Staff 권한

Staff만 역할 기반 권한을 사용한다.

```text
account_staffs
  -> account_staff_roles
  -> staff_roles
  -> staff_role_permissions
  -> staff_permissions
```

`account_staff_roles`는 내부 운영자와 역할의 다대다 연결 테이블이다. 권한 코드를 Controller에서 직접 비교하지 않고 application의 `PermissionService`를 사용한다.

현재 도메인이 사용하는 권한은 다음과 같다.

| 영역 | 권한 |
|---|---|
| 병원 | `platform.hospital.show/create/update/delete` |
| 의료진 | `platform.doctor.show/create/update/delete` |
| 카테고리 | `platform.category.manage` |

권한 migration에는 향후 도메인 코드도 일부 준비되어 있지만, 해당 API가 구현됐다는 의미는 아니다.

## 소유권

- Hospital Actor는 JWT의 `hospital_id`와 대상 병원 ID가 같아야 한다.
- Beauty Actor는 JWT의 `beauty_id`와 대상 ID가 같아야 한다.
- User Actor는 JWT의 `account_id`와 본인 계정 ID가 같아야 한다.
- Staff는 대상 소유권 대신 권한 코드를 검사한다.

의료진의 Hospital API는 요청에서 병원 ID를 신뢰하지 않고 인증 주체의 `hospital_id`를 사용한다.

## 공개 경로

로그인 없이 허용하는 현재 경로는 다음뿐이다.

- 네 Actor의 `/auth/login`
- `/api/v1/user/media/{id}/content`
- `/actuator/health`

User 미디어 경로는 아무 미디어나 노출하지 않는다. 활성 Category의 `icon`, 승인·노출 상태이고 활성·승인 병원에 속한 Doctor의 `profile_image`만 조회할 수 있다.

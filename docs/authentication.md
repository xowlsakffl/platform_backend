# 인증과 권한

## Actor와 경로

| Actor | namespace | 계정 테이블 | 로그인 방식 |
|---|---|---|---|
| Staff | `/api/v1/staff` | `account_staffs` | 이메일·비밀번호 |
| Partner | `/api/v1/partner` | `account_partners` | 이메일·비밀번호 |
| Beauty | `/api/v1/beauty` | `account_beauties` | 이메일·비밀번호 |
| User | `/api/v1/user` | `account_users` | 이메일·비밀번호 |

각 Actor는 다음 인증 API를 독립적으로 제공한다.

| 메서드 | 경로 | 인증 | 용도 |
|---|---|---|---|
| `POST` | `/auth/login` | 공개 | 로그인과 세션 생성 |
| `POST` | `/auth/refresh` | 리프레시 쿠키 | 액세스 토큰 갱신과 리프레시 토큰 회전 |
| `GET` | `/auth/me` | Bearer | 현재 계정·권한 재조회 |
| `POST` | `/auth/logout` | Bearer | 현재 세션 종료 |
| `POST` | `/auth/logout-all` | Bearer | 해당 Actor 계정의 모든 세션 종료 |

다른 Actor의 토큰으로 namespace를 호출하면 `403 FORBIDDEN`이다.

현재 인증 범위는 로그인, 세션 갱신, 내 정보 조회, 로그아웃, 비밀번호 찾기·재설정까지다. 로그인된 계정의 내 프로필 수정과 현재 비밀번호를 확인하는 비밀번호 변경 API는 아직 구현 범위에 포함하지 않는다.

## 토큰과 세션

- 액세스 토큰은 Spring Security JOSE가 생성·검증하는 HS256 JWT다.
- 기본 액세스 토큰 수명은 15분이다.
- JWT는 `issuer`, `audience`, 서명, `nbf`, `exp`, `jti`, `sid`를 검증한다.
- 리프레시 토큰은 예측 불가능한 opaque 값이며 HttpOnly 쿠키로만 전달한다.
- DB에는 리프레시 토큰 원문이 아니라 SHA-256 해시만 저장한다.
- 리프레시할 때마다 토큰을 회전한다. 이전 토큰은 탭 간 동시 요청을 위해 30초 동안 재시도만 허용하고, 유예 시간이 지난 토큰의 재사용이 감지되면 해당 세션을 폐기한다.
- 세션 만료는 로그인 시점부터 계산하는 절대 만료다. 갱신해도 전체 세션 수명은 늘어나지 않는다.
- 액세스 요청마다 DB의 세션 활성 여부와 계정 상태, 최신 Staff 권한을 다시 확인한다.
- 로그아웃한 액세스 토큰의 `jti`는 원래 만료 시각까지 Redis에도 기록한다.

`keep_logged_in=false`이면 브라우저 종료 시 사라지는 세션 쿠키를 사용한다. `true`이면 쿠키에도 서버 세션 만료와 같은 `Max-Age`를 설정한다.

| Actor | 일반 로그인 | 로그인 유지 |
|---|---:|---:|
| Staff | 8시간 | 7일 |
| Partner | 12시간 | 30일 |
| Beauty | 12시간 | 30일 |
| User | 24시간 | 90일 |

## 웹 저장과 갱신

- 웹은 액세스 토큰과 사용자 정보를 `localStorage`나 브라우저 `sessionStorage`에 저장하지 않는다.
- 액세스 토큰과 확인된 사용자 정보는 현재 페이지 메모리에만 둔다.
- 페이지를 다시 열면 `/auth/refresh`로 액세스 토큰을 받은 뒤 `/auth/me`로 계정과 권한을 확인한다.
- 일반 API가 `401` 또는 `419`를 반환하면 공통 API client가 갱신을 한 번 수행하고 원 요청을 한 번만 재시도한다.
- 갱신 실패 또는 재시도 실패 때만 로컬 상태를 지우고 로그인 화면으로 이동한다.
- 로그아웃은 서버 API가 성공하거나 실패한 뒤 클라이언트 메모리를 정리한다.
- Staff 웹은 `Referrer-Policy: no-referrer`를 적용해 쿼리 문자열의 재설정 토큰이 외부 요청의 `Referer` 헤더로 전달되지 않게 한다.

## 쿠키와 CSRF

Actor별 쿠키 이름과 경로를 분리한다.

```text
medi_refresh_staff     /api/v1/staff/auth
medi_refresh_partner  /api/v1/partner/auth
medi_refresh_beauty    /api/v1/beauty/auth
medi_refresh_user      /api/v1/user/auth
```

쿠키는 `HttpOnly`, `SameSite=Strict`가 기본이며 운영에서는 `Secure=true`가 필수다. 로그인·갱신·로그아웃·비밀번호 재설정 관련 `POST` 요청은 추가로 `X-Auth-Request: medi-web` 헤더를 요구한다. 이 헤더는 인증 비밀값이 아니라 브라우저의 단순 cross-site form 요청을 차단하는 보조 장치다. CORS는 명시된 origin과 credential 요청만 허용한다.

운영 웹과 API는 같은 site의 HTTPS 도메인으로 배포하는 것을 기본으로 한다. 서로 다른 site에 배포해야 하면 `SameSite=None; Secure`로 바꾸고 별도의 CSRF 토큰 방식을 추가 검토해야 한다.

## 로그인 공격 제한

- 실패 횟수는 이메일 원문을 Redis key에 남기지 않고 Actor·이메일·IP 조합을 SHA-256으로 해시해 기록한다.
- 로그인 제한은 성공 요청이 아니라 실패 요청만 집계한다.
- 로그인 성공 시 해당 실패 횟수를 초기화한다.
- 로그인 제한 상태를 Redis에서 확인할 수 없으면 보안상 로그인을 실패 처리한다.

| 기준 | 허용되는 실패 | 차단되는 요청 | 다시 시도 가능한 시점 |
|---|---:|---:|---|
| 동일 Actor·이메일·IP | 10분에 5회 | 6번째 로그인 요청 | 첫 실패로부터 최대 10분 후 |
| 동일 Actor·이메일 전체 | 10분에 15회 | 16번째 로그인 요청 | 첫 실패로부터 최대 10분 후 |

제한 창은 첫 실패 시 시작하는 고정 창이다. 차단된 요청을 반복해도 만료 시각은 뒤로 연장되지 않는다. `429` 응답은 `Retry-After` 헤더와 `error.details.retry_after_seconds`로 실제 남은 시간을 반환하고, 메시지에도 `n분 n초 후`를 표시한다.

리버스 프록시 환경의 실제 접속 IP는 신뢰 프록시 범위와 Spring의 forwarded-header 설정을 배포 환경에서 함께 구성해야 한다. 임의의 `X-Forwarded-For` 헤더를 애플리케이션이 직접 신뢰하지 않는다.

## 비밀번호 찾기와 재설정

네 Actor가 동일한 계약을 사용한다.

| 메서드 | 경로 | 용도 |
|---|---|---|
| `POST` | `/auth/password-reset-link` | 재설정 링크 요청 |
| `POST` | `/auth/password-reset/verify` | 이메일·토큰 유효성 확인 |
| `POST` | `/auth/password-reset` | 새 비밀번호 저장 |

- 링크 요청은 계정 존재 여부와 활성 상태에 관계없이 동일한 성공 문구를 반환한다.
- 토큰은 48바이트 난수로 생성하고 DB에는 SHA-256 해시만 저장한다.
- Actor·이메일별 최신 토큰 하나만 유지하며 기본 60분 후 만료된다.
- 같은 Actor·이메일은 60초 안에 토큰을 다시 만들거나 메일을 중복 발송하지 않는다.
- 비밀번호는 8자 이상, BCrypt 제약에 맞춰 UTF-8 72바이트 이하로 받고 기존 비밀번호와 같으면 거부한다.
- 변경은 토큰 행 잠금, 계정 비밀번호 변경, 토큰 단일 사용 처리, 해당 Actor 계정의 전체 세션 폐기를 하나의 트랜잭션에서 처리한다.
- 유효하지 않거나 만료된 링크는 `419 TOKEN_ERROR`, 요청 제한은 `429 RATE_LIMITED`다.

| 단계 | 제한 기준 | 허용되는 요청 | 차단되는 요청 | 다시 시도 가능한 시점 |
|---|---|---:|---:|---|
| 링크 요청 | 동일 Actor·IP | 1시간에 10회 | 11번째 요청 | 첫 요청으로부터 최대 1시간 후 |
| 링크 요청 | 동일 Actor·이메일 | 1시간에 3회 | 4번째 요청 | 첫 요청으로부터 최대 1시간 후 |
| 링크 검증 | 동일 Actor·이메일·IP | 10분에 30회 | 31번째 요청 | 첫 요청으로부터 최대 10분 후 |
| 비밀번호 변경 제출 | 동일 Actor·이메일·IP | 15분에 10회 | 11번째 요청 | 첫 요청으로부터 최대 15분 후 |

링크 요청의 60초 발급 간격과 시간당 요청 제한은 별도 정책이다. 60초 안의 재요청은 새 토큰이나 메일을 만들지 않지만 요청 횟수에는 포함된다. 재설정 제한도 첫 요청부터 시작하는 고정 창이며 차단 요청으로 만료 시각이 연장되지 않는다. `429` 응답은 로그인 제한과 동일하게 실제 남은 시간을 헤더·상세값·메시지에 반환한다.

운영은 `PASSWORD_RESET_MAIL_MODE=smtp`와 SMTP 환경 변수를 설정한다. 로컬 기본값은 `log`이며 실제 메일 대신 재설정 URL을 애플리케이션 로그에 출력한다. 비밀번호 재설정 URL은 Actor별 프론트 주소를 환경 변수로 지정한다.

## 계정 상태와 권한

직원 관리자, 뷰티 관리자, 일반 사용자 계정은 `ACTIVE`, `SUSPENDED`, `BLOCKED`, `WITHDRAWN`을 사용한다. 파트너 관리자 계정은 파트너 운영상태와 계정 상태의 중복을 피하기 위해 `ACTIVE`, `BLOCKED`만 사용한다.

모든 Actor 계정은 `ACTIVE`일 때만 로그인과 인증을 유지할 수 있으며 `deleted_at`이 있는 계정은 인증할 수 없다. 파트너 관리자 계정의 화면 표기는 `ACTIVE`를 `로그인 가능`, `BLOCKED`를 `로그인 차단`으로 사용한다. 파트너 관리자는 계정이 `ACTIVE`여도 소속 파트너의 운영상태가 `WITHDRAWN`이면 인증할 수 없다. 파트너 관리자 계정을 로그인 차단하거나 파트너이 탈퇴·삭제되면 해당 계정의 인증 세션을 모두 폐기한다.

Staff만 역할 기반 권한을 사용한다.

```text
account_staffs
  -> account_staff_roles
  -> staff_roles
  -> staff_role_permissions
  -> staff_permissions
```

권한 코드는 Controller에서 직접 비교하지 않고 application의 `PermissionService`를 사용한다. Partner, Beauty, User는 인증 계정의 소유 리소스 ID를 기준으로 `OwnershipPolicy`를 적용한다.

파트너 상태 변경 권한은 다음처럼 분리한다.

| 권한 | 변경 대상 |
|---|---|
| `platform.partner.account_status.update` | 파트너 관리자 로그인 가능·로그인 차단 |
| `platform.partner.allow_status.update` | 파트너 신청·승인·반려 |
| `platform.partner.status.update` | 파트너 정상·운영중지·탈퇴 |

`platform.partner.update`는 파트너 기본정보 수정 권한이며 위 상태 변경 권한을 대신하지 않는다. 일반 수정 API에 상태 필드가 포함된 경우에도 해당 전용 권한을 추가로 검사한다. `platform.super_admin` 역할은 마이그레이션 시 등록된 모든 권한을 갖도록 동기화하며, 서비스 코드에서 최고관리자를 별도로 우회 처리하지 않는다.

파트너 담당 직원 정책은 다음과 같다.

- `platform.partner.assign_staff` 권한이 있으면 활성 직원 중 누구든 파트너 담당자로 지정·변경하고 현재 담당자를 해제할 수 있다.
- 해당 권한이 없는 Staff도 담당자가 없는 파트너을 자기 담당으로 등록할 수 있고, 본인에게 배정된 파트너만 담당 해제할 수 있다.
- 다른 직원이 담당 중인 파트너은 권한 없이 변경하거나 해제할 수 없다.
- 담당 직원 선택 목록은 `platform.partner.assign_staff` 권한이 있는 Staff에게만 제공한다.

## 운영 설정

- 운영의 `AUTH_JWT_SECRET`은 필수이며 최소 32바이트의 무작위 값으로 설정한다.
- 운영은 `AUTH_COOKIE_SECURE=true`를 유지한다.
- `CORS_ALLOWED_ORIGINS`에는 실제 HTTPS 프론트 origin만 쉼표로 지정한다. 와일드카드를 사용하지 않는다.
- JWT secret 교체는 기존 액세스 토큰을 즉시 무효화한다. 무중단 key rotation이 필요해지면 `kid` 기반 비대칭 키 방식으로 확장한다.
- 만료 세션, 오래된 폐기 세션, 만료된 비밀번호 재설정 토큰은 기본 매일 04:20에 정리한다.

SMTP 장애가 발생하면 발급한 토큰을 폐기하고 오류를 서버 로그와 모니터링에 남기되, 계정 존재 여부 보호를 위해 API 응답 문구는 바꾸지 않는다.

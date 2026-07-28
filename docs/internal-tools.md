# 내부도구 운영 가이드

작성 기준: 2026-07-27

이 문서는 현재 코드 기준의 Staff 전용 내부도구 로그인, 허브, Horizon / Telescope / API Docs(Scramble) 접근 제어 규칙을 정리한다.

기준 파일:

- `routes/web.php`
- `bootstrap/app.php`
- `config/auth.php`
- `config/horizon.php`
- `config/telescope.php`
- `config/scramble.php`
- `app/Providers/InternalToolServiceProvider.php`
- `app/Providers/HorizonServiceProvider.php`
- `app/Providers/TelescopeServiceProvider.php`
- `app/Common/Http/Middleware/EnsureInternalToolIpAllowed.php`
- `app/Modules/Staff/Http/Controllers/DeveloperTool/DeveloperToolAuthController.php`
- `resources/views/tools/login.blade.php`
- `resources/views/tools/index.blade.php`

## 1) 목적

Staff API 인증은 `staff` guard + Sanctum token 기준이다.

Horizon, Telescope, API Docs 같은 브라우저 기반 운영 도구는 토큰 인증보다 세션 인증이 맞으므로 내부도구 전용 웹 guard를 별도로 둔다.

목적:

- 내부 운영 도구를 하나의 로그인 세션으로 묶는다.
- 도구별 권한 분기를 만들지 않고 `viewTool` Gate로 통일한다.
- IP 제한, 세션 로그인, 역할/이메일 제한을 함께 적용한다.
- Horizon / Telescope / API Docs 진입점을 `/staff/tools` 허브로 모은다.

## 2) 포함 대상

현재 내부도구 허브에 노출되는 도구:

| 도구 | 기본 경로 | 상태 기준 |
|---|---|---|
| Horizon | `/horizon` | 항상 카드 노출 |
| Telescope | `/telescope` | `config('telescope.enabled')` 기준 |
| API Docs(Scramble) | `/docs/api` | Scramble route 존재 또는 `INTERNAL_TOOL_API_DOCS_URL` 설정 기준 |

과거 문서의 Swagger 표현은 현재 코드 기준과 맞지 않는다. 현재 API 문서는 Scramble이 생성한다.

## 3) Guard 구조

`config/auth.php` 기준:

| guard | driver | provider | 용도 |
|---|---|---|---|
| `staff` | `sanctum` | `staff` | Staff API |
| `tool_staff` | `session` | `staff` | 내부도구 웹 화면 |

두 guard 모두 Staff 모델을 사용하지만 인증 방식이 다르다.

운영 규칙:

- API 로그인 상태와 내부도구 로그인 상태는 별개다.
- 내부도구는 반드시 `tool_staff` guard를 사용한다.
- Horizon / Telescope / Scramble에 `staff` Sanctum guard를 붙이지 않는다.

## 4) 주요 경로

내부도구 허브:

| Method | Path | Controller | Middleware |
|---|---|---|---|
| GET | `/staff/tools/login` | `DeveloperToolAuthController@showLoginForm` | `internal_tool.ip` |
| POST | `/staff/tools/login` | `DeveloperToolAuthController@login` | `internal_tool.ip` |
| GET | `/staff/tools` | `DeveloperToolAuthController@index` | `internal_tool.ip`, `auth:tool_staff` |
| GET/POST | `/staff/tools/logout` | `DeveloperToolAuthController@logout` | `internal_tool.ip`, `auth:tool_staff` |

기존 Horizon 전용 로그인 호환 경로:

- `/staff/horizon/login` -> `/staff/tools/login`
- `/staff/horizon/logout` -> `/staff/tools/logout`

개별 도구:

- Horizon: `config('horizon.path', 'horizon')`
- Telescope: `config('telescope.path', 'telescope')`
- API Docs: Scramble route `scramble.docs.ui`, 기본 `/docs/api`

## 5) 접근 제어

내부도구는 아래 단계를 통과해야 한다.

1. IP 제한
2. `tool_staff` 세션 로그인
3. `viewTool` Gate
4. 도구별 패키지 middleware/auth callback

### IP 제한

미들웨어:

- `App\Common\Http\Middleware\EnsureInternalToolIpAllowed`

환경변수:

```env
INTERNAL_TOOL_ALLOWED_IPS=127.0.0.1,::1
```

동작:

- `Request::ip()`가 없으면 차단한다.
- `Symfony\Component\HttpFoundation\IpUtils::checkIp()`로 검사한다.
- IP와 CIDR 표기를 사용할 수 있다.
- 설정이 비어 있으면 기본값은 `127.0.0.1,::1`이다.
- 실패 시 `abort(403, '허용되지 않은 내부도구 접속 IP입니다.')`로 차단한다.

### 세션 로그인

로그인 입력:

- `nickname`
- `password`

로그인 처리:

- `Auth::guard('tool_staff')->attempt($credentials)`
- 성공 시 세션 재생성
- Staff 계정 활성 상태 확인
- `viewTool` Gate 확인
- `last_login_at` 갱신
- 로그인 로그 기록
- `/staff/tools`로 이동

실패 처리:

- 인증 실패: 로그인 화면으로 반환
- 비활성 계정: 로그아웃 후 로그인 화면으로 반환
- 내부도구 권한 없음: 로그아웃 후 로그인 화면으로 반환

로그아웃:

- `tool_staff` guard 로그아웃
- 세션 invalidate
- CSRF token 재생성
- 로그아웃 로그 기록
- `/staff/tools/login`으로 이동

### `viewTool` Gate

정의 위치:

- `app/Providers/InternalToolServiceProvider.php`

허용 조건:

- 사용자가 `AccountStaff` 인스턴스다.
- Staff 상태가 active다.
- 역할이 아래 중 하나다.
  - `beaulab.super_admin`
  - `beaulab.dev`
- `INTERNAL_TOOL_ALLOWED_EMAILS`가 설정되어 있으면 이메일도 허용 목록에 포함되어야 한다.

환경변수:

```env
INTERNAL_TOOL_ALLOWED_EMAILS=
```

비워두면 역할 기준만 적용한다. 값이 있으면 쉼표로 구분하고, 비교는 소문자 trim 기준이다.

## 6) 도구별 보호 방식

### Horizon

`config/horizon.php` middleware:

- `web`
- `internal_tool.ip`
- `auth:tool_staff`

`HorizonServiceProvider`:

- `Horizon::auth()`에서 `tool_staff` 사용자 확인
- `Gate::forUser($user)->allows('viewTool')` 재확인

즉, 라우트 middleware와 Horizon 내부 auth callback을 모두 통과해야 한다.

### Telescope

`config/telescope.php` middleware:

- `web`
- `internal_tool.ip`
- `auth:tool_staff`
- `Laravel\Telescope\Http\Middleware\Authorize`

`TelescopeServiceProvider`:

- `Telescope::auth()`에서 `tool_staff` 사용자 확인
- `Gate::forUser($user)->allows('viewTool')` 재확인

Telescope 저장 정책:

- local 환경은 전체 기록
- local 외 환경은 reportable exception, failed request, failed job, scheduled task, monitored tag 중심으로 필터링
- local 외 환경에서는 `_token`, cookie, CSRF header를 숨긴다.

### API Docs(Scramble)

`config/scramble.php` middleware:

- `web`
- `internal_tool.ip`
- `auth:tool_staff`
- `can:viewTool`

Scramble 설정 위치:

- `InternalToolServiceProvider`

적용 중인 문서 transformer:

- `AddSanctumSecurityScheme`
- `ApplyKoreanSchemaDescriptions`
- `ApplySanctumSecurity`
- `NormalizeQueryArrayParameters`
- `ApplyKoreanOperationDocumentation`

허브의 API Docs 카드 URL 결정 순서:

1. `INTERNAL_TOOL_API_DOCS_URL`이 절대 URL이면 그대로 사용
2. `INTERNAL_TOOL_API_DOCS_URL`이 상대 경로면 `url()`로 변환
3. 설정이 없으면 Scramble route `scramble.docs.ui` 사용
4. route도 없으면 카드 비활성

## 7) 비로그인 redirect

`bootstrap/app.php`의 `redirectGuestsTo`가 내부도구 비로그인 접근을 로그인 화면으로 보낸다.

대상:

- `/staff/tools`
- `/staff/tools/*`
- `/horizon`, `/horizon/*`
- `/telescope`, `/telescope/*`
- `/docs/api`
- `/docs/api.json`

예외:

- `/horizon/api/*`
- `/telescope/telescope-api/*`

API 요청의 JSON 401 처리와 내부도구 웹 redirect는 별개다. `/api/*` 요청은 `./error-handling.md`의 JSON 에러 규칙을 따른다.

## 8) 환경변수

현재 내부도구 관련 환경변수:

```env
INTERNAL_TOOL_ALLOWED_IPS=127.0.0.1,::1
INTERNAL_TOOL_ALLOWED_EMAILS=
INTERNAL_TOOL_API_DOCS_URL=
```

설명:

- `INTERNAL_TOOL_ALLOWED_IPS`: 내부도구 접근 허용 IP/CIDR 목록
- `INTERNAL_TOOL_ALLOWED_EMAILS`: 역할 외 추가 이메일 제한 목록. 비워두면 역할 기준만 적용
- `INTERNAL_TOOL_API_DOCS_URL`: 허브 API Docs 카드 URL override

도구 경로 관련 환경변수:

```env
HORIZON_PATH=horizon
TELESCOPE_PATH=telescope
TELESCOPE_ENABLED=true
```

`INTERNAL_TOOL_SWAGGER_URL`은 현재 코드에서 사용하지 않는다. 신규 설정에는 사용하지 않는다.

## 9) 신규 내부도구 추가 규칙

신규 내부도구를 붙일 때:

- 별도 로그인 페이지를 만들지 않는다.
- `tool_staff` 세션을 재사용한다.
- `internal_tool.ip`를 붙인다.
- `auth:tool_staff`를 붙인다.
- `can:viewTool` 또는 패키지 auth callback에서 `viewTool`을 확인한다.
- 가능하면 `/staff/tools` 허브에 카드로 추가한다.
- 신규 권한 정책은 도구별로 흩뜨리지 말고 `InternalToolServiceProvider`의 `viewTool` 정책을 먼저 검토한다.

권한 확대가 필요하면 Horizon/Telescope/Scramble 각각을 수정하지 말고 `viewTool` Gate 기준을 바꾼다.

## 10) 운영 주의사항

세션:

- 내부도구는 세션 기반이다.
- `SESSION_DRIVER=database`면 `sessions` 테이블이 필요하다.
- 세션 테이블이 없으면 로그인/허브/도구 접근에서 DB 오류가 날 수 있다.

IP:

- 운영 서버가 프록시/로드밸런서 뒤에 있으면 Laravel이 실제 client IP를 올바르게 인식하는지 먼저 확인한다.
- `INTERNAL_TOOL_ALLOWED_IPS`를 너무 넓게 열지 않는다.

Telescope:

- 운영에서 `TELESCOPE_ENABLED=true`로 둘 경우 저장량과 민감정보 마스킹을 같이 봐야 한다.
- 현재 provider는 local 외 환경에서 일부 민감 request 값을 숨기지만, 신규 민감 header/parameter가 생기면 숨김 목록을 추가한다.

API Docs:

- Scramble은 실제 route와 request/DTO 상태에 따라 문서를 생성한다.
- 문서가 이상하면 내부도구 설정보다 API route/request/DTO 주석과 transformer를 먼저 확인한다.

## 11) 사용 흐름

운영자 기준:

1. 허용 IP 환경에서 `/staff/tools/login` 접속
2. Staff 아이디/비밀번호로 로그인
3. 활성 계정, 역할, 이메일 제한 확인
4. `/staff/tools` 허브 진입
5. Horizon / Telescope / API Docs 선택
6. 작업 종료 후 허브에서 로그아웃

## 12) 요약

현재 내부도구는 `tool_staff` 세션 로그인, `internal_tool.ip`, `viewTool` Gate를 공통 기준으로 사용한다.

Horizon / Telescope는 패키지 auth callback에서도 `viewTool`을 재확인하고, API Docs는 Scramble middleware에서 `can:viewTool`을 직접 적용한다.

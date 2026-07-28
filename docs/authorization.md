# Authorization 설계 (Staff / Hospital / Beauty / User)

작성 기준: 2026-07-24

이 문서는 현재 코드 기준의 인증 주체, guard, role, permission, Policy, 프론트 route permission 규칙을 정리한다.

권한의 백엔드 단일 소스는 아래 파일이다.

- `app/Common/Authorization/AccessPermissions.php`
- `app/Common/Authorization/AccessRoles.php`
- `database/seeders/AuthorizationSeeder.php`

Staff 프론트 route 접근 제어는 아래 파일을 기준으로 한다.

- `apps/staff-web/lib/common/routing/route-permissions.ts`

## 1) Guard

- `staff`: 뷰랩 내부 직원
- `hospital`: 병의원 파트너
- `beauty`: 뷰티 파트너
- `user`: 일반 앱 사용자
- `tool_staff`: Horizon, Telescope, Scramble 같은 내부 도구용 웹 세션 guard

Spatie role/permission은 `staff`, `hospital`, `beauty` guard에만 생성한다. `user` guard는 Spatie 권한을 사용하지 않고 인증 여부, 계정 상태, 도메인 Policy/Action 규칙으로 처리한다.

## 2) Role

### Staff (`guard: staff`)

- `beaulab.super_admin`
- `beaulab.admin`
- `beaulab.staff`
- `beaulab.dev`

### Hospital (`guard: hospital`)

- `hospital.owner`

현재 병의원 파트너 계정 구조는 병의원 1개당 병원 계정 1개다. 병원 파트너 role은 `hospital.owner` 단일로 운영한다. 다계정 정책이 필요해지면 `hospital.manager`, `hospital.staff` 같은 role을 새로 정의한다.

### Beauty (`guard: beauty`)

- `beauty.owner`
- `beauty.manager`
- `beauty.staff`

### Agency

`AccessRoles`에 `agency.owner`, `agency.staff` 상수는 있지만 `roleNamesByGuard()`에 포함되어 있지 않다. 현재 Seeder로 생성되는 role이 아니므로 운영 권한으로 취급하지 않는다.

## 3) Permission

### Common

- `common.access`
- `common.dashboard.show`
- `common.profile.show`
- `common.profile.update`

`common.access`는 Staff 보호 라우트 공통 진입 권한이다. 도메인 조회/수정 권한을 대체하지 않는다.
`common.profile.show|update`는 현재 로그인한 자기 프로필 조회/수정과 비밀번호 변경에만 사용한다.

### Staff 전용

- Hospital: `beaulab.hospital.show|create|update|delete`
- Hospital Entry: `beaulab.hospital_entry.show|update`
- Beauty: `beaulab.beauty.show|create|update|delete`
- Agency: `beaulab.agency.show|create|update|delete`
- User: `beaulab.user.show`, `beaulab.user.status.update`
- Staff: `beaulab.staff.show|create|update|delete`
- Doctor: `beaulab.doctor.show|create|update|delete`
- Expert: `beaulab.expert.show|create|update|delete`
- Video: `beaulab.video.show|create|update|delete`
- Hospital Event: `beaulab.hospital_event.show|create|update|delete`
- Hospital Event Ad: `beaulab.hospital_event_ad.show|create|update|delete`
- Hospital Event DB: `beaulab.hospital_event_db.show|update`
- Hospital Event Real Model DB: `beaulab.hospital_event_real_model_db.show|update`
- Hospital Review: `beaulab.hospital_review.show|update`
- Hospital Evaluation: `beaulab.hospital_evaluation.show|update`
- Talk: `beaulab.talk.show|update`
- Reported Talk: `beaulab.reported_talk.show|update`
- Reported Hospital Review: `beaulab.reported_hospital_review.show|update`
- Reported Hospital Evaluation: `beaulab.reported_hospital_evaluation.show|update`
- Reported Chat Message: `beaulab.reported_chat_message.show|update`
- Reported Video: `beaulab.reported_video.show|update`
- Notice: `beaulab.notice.show|create|update|delete`
- FAQ: `beaulab.faq.show|create|update|delete`
- Category: `beaulab.category.manage`
- Hashtag: `beaulab.hashtag.manage`

댓글 리소스는 별도 permission을 두지 않는다.

- 후기 댓글: Hospital Review 권한 사용
- 토크 댓글: Talk 권한 사용
- 신고 댓글: 대상 도메인에 맞는 Reported 권한 사용

### Hospital 전용

- `hospital.profile.show|update|delete`
- `hospital.video.show|update`

병의원 파트너는 동영상 등록 권한이 없다. 자기 동영상의 공개/미공개 같은 상태 변경만 `hospital.video.update`로 처리한다.

### Beauty 전용

- `beauty.profile.show|update|delete`
- `beauty.members.manage`
- `beauty.video.show|create|update|cancel`

## 4) Role -> Permission 매핑

### Staff

- `beaulab.super_admin`
  - staff guard에 생성되는 전체 permission
- `beaulab.admin`
  - common + `AccessPermissions::beaulab()`
- `beaulab.staff`
  - common + 조회 중심 permission
- `beaulab.dev`
  - 현재 `beaulab.staff`와 동일한 조회 중심 permission

`beaulab.staff`, `beaulab.dev`에 포함되는 조회 권한은 아래와 같다.

- `beaulab.hospital.show`
- `beaulab.hospital_entry.show`
- `beaulab.beauty.show`
- `beaulab.agency.show`
- `beaulab.user.show`
- `beaulab.doctor.show`
- `beaulab.expert.show`
- `beaulab.hospital_event_db.show`
- `beaulab.hospital_event_real_model_db.show`
- `beaulab.hospital_review.show`
- `beaulab.hospital_evaluation.show`
- `beaulab.talk.show`
- `beaulab.reported_talk.show`
- `beaulab.reported_hospital_review.show`
- `beaulab.reported_hospital_evaluation.show`
- `beaulab.reported_chat_message.show`
- `beaulab.reported_video.show`
- `beaulab.notice.show`
- `beaulab.faq.show`

현재 `beaulab.admin`, `beaulab.staff`, `beaulab.dev`에는 직원 관리 권한(`beaulab.staff.*`)을 포함하지 않는다.
직원 관리 권한은 최고관리자(`beaulab.super_admin`) 전용이다.
단, 로그인한 자기 계정의 프로필 조회/수정과 비밀번호 변경은 `common.profile.show|update`와 자기 자신 여부로 허용한다.

현재 `beaulab.staff`, `beaulab.dev`에는 이벤트, 광고, 동영상, 카테고리, 해시태그 조회/관리 권한이 포함되어 있지 않다.

현재 코드상 `AccessPermissions::beaulabSuperAdminOnly()`는 `beaulab.category.manage`, `beaulab.hashtag.manage`를 반환하지만, 같은 권한이 `AccessPermissions::beaulab()`에도 포함되어 있다. 따라서 실제로는 `beaulab.admin`도 카테고리/해시태그 권한을 가진다. 진짜 super admin 전용 권한을 만들 경우 해당 permission은 `beaulab()`에 넣지 않는다.

### Hospital

- `hospital.owner`
  - common + Hospital 전용 permission 전체

### Beauty

- `beauty.owner`
  - common + Beauty 전용 permission 전체
- `beauty.manager`
  - common
  - `beauty.profile.show`
  - `beauty.profile.update`
  - `beauty.members.manage`
  - `beauty.video.show|create|update|cancel`
- `beauty.staff`
  - common
  - `beauty.profile.show`
  - `beauty.video.show|create|update|cancel`

## 5) Seeder 동작

`AuthorizationSeeder`는 아래 순서로 동작한다.

1. Spatie permission 캐시 삭제
2. `AccessPermissions::byGuard()` 기준으로 guard별 permission 생성
3. `AccessRoles::roleNamesByGuard()` 기준으로 guard별 role 생성
4. `AccessRoles::mapByGuard()` 기준으로 role permission 동기화
5. Spatie permission 캐시 재삭제

실행 명령:

```bash
php artisan db:seed --class=AuthorizationSeeder
```

Seeder는 현재 코드에 있는 role/permission을 생성하고 role별 permission을 동기화한다. 더 이상 쓰지 않는 role/permission DB row를 자동 삭제하지 않는다. 권한을 폐기할 때는 별도 마이그레이션, 운영 SQL, 정리 커맨드 중 하나로 명시적으로 삭제한다.

## 6) API 라우트 보호

현재 모듈별 공통 라우트 미들웨어는 아래와 같다.

- Staff: `auth:sanctum`, `abilities:actor:staff`, `permission:common.access`
- Hospital: `auth:sanctum`, `abilities:actor:hospital`
- Beauty: `auth:sanctum`, `abilities:actor:beauty`
- User: `auth:sanctum`, `abilities:actor:user`, `EnsureActiveUser`

Staff 라우트에 도메인별 `permission:*` 미들웨어를 직접 붙이는 방식은 현재 기본 패턴이 아니다. Staff 라우트는 `common.access`로 공통 진입을 막고, 실제 도메인 권한은 Action 진입부의 `Gate::authorize()`와 Policy에서 검사한다.

## 7) Policy 규칙

API 진입 Action은 도메인 처리 전에 `Gate::authorize()`를 호출한다.

예시:

```php
Gate::authorize('viewAny', HospitalEventAd::class);
Gate::authorize('update', $hospital);
Gate::authorize('updateStatus', $user);
Gate::authorize('viewAny', [ContentReportState::class, $targetAlias]);
Gate::authorize('viewProfile', $staff);
```

Policy 작성 규칙은 아래와 같다.

- Policy는 HTTP Request를 직접 보지 않는다.
- Policy는 actor와 대상 모델 기준으로 권한만 판단한다.
- Staff 전용 권한은 `AccountStaff` actor에서 `$actor->can(AccessPermissions::...)`로 확인한다.
- 여러 actor가 접근하는 도메인은 상위 Policy에서 actor 타입별 Policy로 위임한다.
- 댓글처럼 별도 permission이 없는 리소스는 부모 도메인 permission을 재사용한다.
- 신고게시물은 `ContentReportStateForStaffPolicy`가 target alias를 기준으로 Reported permission을 매핑한다.
- 직원 계정은 직원관리용 ability(`viewAny|view|create|update|delete`)와 자기 프로필용 ability(`viewProfile|updateProfile`)를 분리한다.
- 인증 진입점(Login, Password Reset), 로그아웃, 히스토리 기록, payload/media 보조 Action처럼 독립 API 권한 판단 지점이 아닌 Action은 `Gate::authorize()` 대상에서 제외한다.

신규 도메인 Policy 추가 시 기본 abilities는 아래 이름을 우선 사용한다.

- 목록/상세 조회: `viewAny`, `view`
- 등록: `create`
- 수정/상태 변경: `update`
- 삭제: `delete`
- 상태 변경이 일반 수정과 명확히 분리되어야 하는 경우에만 `updateStatus`처럼 별도 ability 사용

## 8) Staff 프론트 route permission

Staff 프론트는 `route-permissions.ts`에서 route별 required permission을 정의한다.

규칙:

- 신규 페이지는 반드시 route permission을 추가한다.
- route permission이 없거나 보유 permission과 맞지 않으면 접근 불가로 처리한다.
- 목록/상세는 `*.show`, 등록은 `*.create`, 수정은 `*.update`를 사용한다.
- 프론트 route permission은 UX/내비게이션 가드다. 최종 권한 판단은 백엔드 Policy/Gate가 한다.

예시:

- `/hospital-manage/hospitals` -> `beaulab.hospital.show`
- `/hospital-manage/hospitals/new` -> `beaulab.hospital.create`
- `/hospital-manage/hospitals/[id]/edit` -> `beaulab.hospital.update`
- `/ads-manage/event-ads` -> `beaulab.hospital_event_ad.show`
- `/admin-settings/staff` -> `beaulab.staff.show`
- `/content-manage/hashtags` -> `beaulab.hashtag.manage`

직원 관리와 Agency는 다른 리소스다. 직원 관리 화면은 `beaulab.staff.*`, Agency 관리 기능이 생기면 `beaulab.agency.*`를 사용한다.

## 9) 내부 도구 권한

대상:

- Horizon
- Telescope
- Scramble/OpenAPI 문서

내부 도구는 API용 `staff` 토큰 guard가 아니라 웹 세션 guard인 `tool_staff`를 사용한다.

공통 Gate:

- `viewTool`

허용 기준:

1. 로그인한 `AccountStaff`가 존재해야 한다.
2. 계정 상태가 active여야 한다.
3. role이 `beaulab.super_admin` 또는 `beaulab.dev`여야 한다.
4. `INTERNAL_TOOL_ALLOWED_EMAILS`가 설정되어 있으면 이메일도 허용 목록에 있어야 한다.

상세 구조와 로그인 흐름은 `./internal-tools.md`를 본다.

## 10) 신규 권한 추가 체크리스트

신규 도메인이나 신규 화면을 추가할 때는 아래 순서로 처리한다.

1. `AccessPermissions`에 permission 상수 추가
2. `AccessPermissions::beaulab()`, `hospital()`, `beauty()` 중 맞는 guard permission 목록에 추가
3. 필요한 role에 권한을 부여하도록 `AccessRoles::mapByGuard()` 수정
4. 도메인 Policy 추가 또는 기존 Policy 수정
5. Action 진입부에 `Gate::authorize()` 추가
6. Staff 프론트 화면이면 `route-permissions.ts`에 route permission 추가
7. `AuthorizationSeeder` 실행
8. 권한 캐시가 남아 있으면 `php artisan permission:cache-reset` 또는 Seeder 재실행

권한 이름은 리소스 기준으로 만든다. 같은 메뉴 아래에 있어도 책임이 다르면 권한을 분리한다.

예시:

- 병의원과 입점신청은 분리: `beaulab.hospital.*`, `beaulab.hospital_entry.*`
- 이벤트와 이벤트 DB는 분리: `beaulab.hospital_event.*`, `beaulab.hospital_event_db.*`
- 이벤트와 광고는 분리: `beaulab.hospital_event.*`, `beaulab.hospital_event_ad.*`
- 일반 게시물과 신고게시물은 분리: `beaulab.talk.*`, `beaulab.reported_talk.*`

## 11) 금지/주의

- 권한 문자열을 컨트롤러와 Action에 하드코딩하지 않는다. 백엔드는 `AccessPermissions`, Staff 프론트는 `route-permissions.ts` 매핑을 기준으로 관리한다.
- Staff 라우트의 `common.access`만으로 도메인 접근을 허용하지 않는다.
- 신규 route permission을 누락한 상태로 프론트 메뉴만 추가하지 않는다.
- Seeder가 stale permission을 삭제한다고 가정하지 않는다.
- User API에 Spatie role/permission을 붙이지 않는다.

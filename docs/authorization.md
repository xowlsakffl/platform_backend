# 권한 설계

작성 기준: 2026-07-28

이 문서는 Staff, Hospital, Beauty, User actor의 권한 기준을 정의한다. 로그인과 토큰 발급은 [인증 설계](./authentication.md)를 따른다.

## 1. Actor

```text
STAFF
HOSPITAL
BEAUTY
USER
PUBLIC
SYSTEM
```

- `STAFF`: 내부 운영자
- `HOSPITAL`: 병원 파트너
- `BEAUTY`: 뷰티 파트너
- `USER`: 일반 앱/웹 사용자
- `PUBLIC`: 로그인 전 공개 요청
- `SYSTEM`: 배치, 스케줄러, 내부 자동 처리

## 2. API 보호 기준

| Namespace | 보호 기준 |
| --- | --- |
| `/api/v1/public` | 공개 또는 로그인 전 허용 |
| `/api/v1/staff` | Staff 인증, staff 계정 상태, 권한 코드 |
| `/api/v1/hospital` | Hospital 인증, 병원 소유권 |
| `/api/v1/beauty` | Beauty 인증, 뷰티 소유권 |
| `/api/v1/user` | User 인증, 활성/차단 상태 |

프론트 권한 제어는 UX 보조 수단이다. 최종 권한 검증은 백엔드가 책임진다.

## 3. Role 기준

### Staff

- `platform.super_admin`
- `platform.admin`
- `platform.staff`
- `platform.dev`

### Hospital

- `hospital.owner`

초기 병원 파트너 계정은 병원 1개당 계정 1개 기준이다. 다계정 정책이 필요해지면 `hospital.manager`, `hospital.staff`를 추가한다.

### Beauty

- `beauty.owner`
- `beauty.manager`
- `beauty.staff`

## 4. Permission 기준

### Common

- `common.access`
- `common.dashboard.show`
- `common.profile.show`
- `common.profile.update`

### Staff

- Hospital: `platform.hospital.show|create|update|delete`
- Hospital Entry: `platform.hospital_entry.show|update`
- Beauty: `platform.beauty.show|create|update|delete`
- Agency: `platform.agency.show|create|update|delete`
- User: `platform.user.show`, `platform.user.status.update`
- Staff: `platform.staff.show|create|update|delete`
- Doctor: `platform.doctor.show|create|update|delete`
- Expert: `platform.expert.show|create|update|delete`
- Video: `platform.video.show|create|update|delete`
- Hospital Event: `platform.hospital_event.show|create|update|delete`
- Hospital Event Ad: `platform.hospital_event_ad.show|create|update|delete`
- Hospital Event DB: `platform.hospital_event_db.show|update`
- Hospital Event Real Model DB: `platform.hospital_event_real_model_db.show|update`
- Hospital Review: `platform.hospital_review.show|update`
- Hospital Evaluation: `platform.hospital_evaluation.show|update`
- Reported Talk: `platform.reported_talk.show|update`
- Reported Hospital Review: `platform.reported_hospital_review.show|update`
- Reported Hospital Evaluation: `platform.reported_hospital_evaluation.show|update`
- Reported Chat Message: `platform.reported_chat_message.show|update`
- Reported Video: `platform.reported_video.show|update`
- Category: `platform.category.manage`
- Hashtag: `platform.hashtag.manage`
- Notice: `platform.notice.show|create|update|delete`
- FAQ: `platform.faq.show|create|update|delete`

권한 이름은 리소스 기준으로 만든다. 같은 메뉴 아래에 있어도 책임이 다르면 권한을 분리한다.

## 5. Spring 구현 방향

권장 패키지:

```text
common/security/
  SecurityConfig
  ApiSecurityExceptionHandler

application/account/
  AuthenticationService
  PermissionService

domain/account/
  AccountStaff
  AccountHospital
  AccountBeauty
  AccountUser
```

권한 검증 위치:

- 인증 여부: Spring Security filter chain
- actor 계정 상태: authentication provider 또는 application service 진입부
- Staff permission: application service 진입부
- Hospital/Beauty 소유권: application service 진입부
- User 차단/탈퇴 상태: user service 진입부

Controller에 권한 문자열을 직접 흩뿌리지 않는다.

## 6. Service 진입부 권한 기준

예시:

```java
permissionService.requireStaffPermission(actor, "platform.hospital.update");
hospitalOwnershipPolicy.requireOwner(actor, hospitalId);
userAccountPolicy.requireActive(actor);
```

규칙:

- Controller는 인증/권한의 최종 판단을 하지 않는다.
- Service는 유스케이스 시작 시 필요한 권한과 소유권을 확인한다.
- 같은 권한 검사가 반복되면 application 계층의 policy/service로 분리한다.
- 권한 실패는 `FORBIDDEN`, 인증 실패는 `UNAUTHORIZED`로 응답한다.

## 7. Staff 프론트 route permission

Staff 프론트는 route별 required permission을 관리한다.

예시:

- `/hospital-manage/hospitals` -> `platform.hospital.show`
- `/hospital-manage/hospitals/new` -> `platform.hospital.create`
- `/hospital-manage/hospitals/[id]/edit` -> `platform.hospital.update`
- `/ads-manage/event-ads` -> `platform.hospital_event_ad.show`
- `/admin-settings/staff` -> `platform.staff.show`
- `/content-manage/hashtags` -> `platform.hashtag.manage`

규칙:

- 신규 페이지는 route permission을 추가한다.
- route permission이 없거나 보유 permission과 맞지 않으면 접근 불가로 처리한다.
- 목록/상세는 `*.show`, 등록은 `*.create`, 수정은 `*.update`를 사용한다.
- 프론트 권한은 UX 제어이며, 최종 검증은 백엔드에서 다시 한다.

## 8. 신규 권한 추가 체크리스트

1. permission 문자열을 리소스 기준으로 정한다.
2. role별 기본 권한 매핑을 정한다.
3. seed 또는 migration으로 권한 데이터를 추가한다.
4. application service 진입부에 권한 검증을 추가한다.
5. Staff 프론트 화면이면 route permission을 추가한다.
6. 권한 캐시가 있으면 무효화 기준을 정한다.

## 9. 금지/주의

- Staff API를 인증만으로 열지 않는다.
- 프론트 메뉴 숨김을 백엔드 권한 검증으로 착각하지 않는다.
- User API에 Staff permission 체계를 붙이지 않는다.
- 권한 문자열을 여러 파일에 임의로 하드코딩하지 않는다.
- 삭제된 권한 row가 자동 정리된다고 가정하지 않는다.

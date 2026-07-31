# 미디어

## 저장 모델

Media는 도메인 테이블에 파일 경로 컬럼을 반복하지 않고 다음 키로 연결한다.

```text
owner_type + owner_id + collection
```

| owner_type | collection | 개수 | 제약 |
|---|---|---:|---|
| `PARTNER` | `logo` | 1 | JPG/PNG/WebP, 5MB, 1:1 |
| `PARTNER` | `main_image` | 1 | JPG/PNG/WebP, 10MB, 760x490 |
| `PARTNER` | `interior_image` | 5 | JPG/PNG/WebP, 10MB, 760x490 |
| `PARTNER_BUSINESS_REGISTRATION` | `business_registration_file` | 1 | 이미지/PDF, 10MB |
| `CATEGORY` | `icon` | 1 | JPG/PNG/WebP, 5MB |
| `SPECIALIST` | `profile_image` | 1 | JPG/PNG/WebP, 5MB, 1:1 |
| `SPECIALIST` | `license_image` | 1 | 이미지/PDF, 10MB |
| `SPECIALIST` | `specialist_certificate_image` | 1 | 이미지/PDF, 10MB |

저장 루트는 `app.media.local.root`이며 기본값은 `./storage/media`다. 파일명은 서버가 생성하고 원본 파일명은 메타데이터로 보존한다.

## 쓰기 책임

공통 미디어 CRUD Controller는 두지 않는다. Partner, Category, Specialist의 생성·수정 API가 소유자와 컬렉션 정책을 알고 `MediaCommandService`를 호출한다.

- 단건: 새 파일, 유지할 기존 ID, 명시적 삭제를 구분한다.
- 다건: 유지할 기존 ID와 신규 파일을 합쳐 최대 개수를 검증한다.
- 순서 변경: `existing:{id}`, `new:{index}` 토큰으로 최종 순서를 전달한다.
- 기존 ID는 같은 owner와 collection에 속한 활성 Media만 허용한다.

## 파일 수명주기

DB와 파일 시스템은 하나의 원자적 트랜잭션이 아니므로 다음 순서를 지킨다.

1. 신규 파일 저장
2. 파일 형식·크기·크기 비율 검증
3. Media 메타데이터 저장과 기존 Media soft delete
4. DB 롤백이면 신규 파일 삭제
5. DB 커밋이면 교체·삭제된 기존 파일 삭제

Partner 또는 Specialist soft delete도 연결 Media를 soft delete하고 커밋 후 원본 파일을 정리한다.

## 조회 경로

### Staff

`GET /api/v1/staff/media/{id}/content`

Staff 권한과 소유 도메인 존재를 확인한다. Partner 사업자등록증, Specialist 자격 증빙 파일을 포함한 운영 원본 조회 경로다.

### Partner

`GET /api/v1/partner/specialists/{specialistId}/media/{mediaId}/content`

인증한 Partner 소유의 Specialist 미디어만 조회한다.

### User 앱 공개

`GET /api/v1/user/media/{id}/content`

로그인 없이 호출할 수 있지만 다음만 공개한다.

- `ACTIVE` Category의 `icon`
- `APPROVED + VISIBLE` Specialist의 `profile_image`
- 공개 Specialist의 Partner도 `APPROVED + ACTIVE`

Specialist 자격 증빙 파일과 파트너 사업자등록증은 이 경로에서 항상 `404`다.

## 응답 보안

- 파일은 `inline` Content-Disposition으로 반환한다.
- `X-Content-Type-Options: nosniff`를 설정한다.
- Staff 응답은 private cache, User 앱 응답은 public cache를 1시간 적용한다.
- 파일 시스템 절대 경로는 API 응답에 노출하지 않는다.

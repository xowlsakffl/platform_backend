# 공통 Media 설계

작성 기준: 2026-07-28

## 1. 목적

Media는 병원, 의료진, 이벤트 등 여러 도메인의 파일 메타데이터와 저장 위치를 공통으로 관리한다.

DB에 특정 Java 클래스명이나 JPA 상속 정보를 저장하지 않는다. 연결 대상은 의미가 고정된 `owner_type` enum과 `owner_id` 값으로 식별한다.

```text
owner_type = HOSPITAL
owner_id = 10
collection = gallery
```

현재 연결 가능한 대상은 `HOSPITAL`이다. Doctor 구현 시 `DOCTOR` enum과 대상 존재/권한 검증을 함께 추가한다.

## 2. 저장 구조

핵심 컬럼:

| 컬럼 | 의미 |
|---|---|
| `owner_type` | 연결 도메인 enum |
| `owner_id` | 연결 대상 ID |
| `collection` | 도메인별 파일 용도 |
| `disk` | 저장소 종류. 현재 `LOCAL` |
| `path` | 저장소 내부 상대 경로 |
| `original_name` | 사용자 원본 파일명 |
| `mime_type` | 파일 내용으로 확인한 MIME 타입 |
| `size` | 파일 크기(bytes) |
| `width`, `height` | 확인 가능한 이미지 크기 |
| `sort_order` | 컬렉션 내부 정렬 순서 |
| `is_primary` | 대표 파일 여부 |
| `metadata` | 추가 정보 JSON 객체 |
| `deleted_at` | 소프트 삭제 시각 |

폴리모픽 연결에는 DB 외래키를 걸 수 없다. 대상 존재 여부와 삭제 연동은 application service가 책임진다.

## 3. 파일 저장 규칙

- 기본 저장소는 `MEDIA_STORAGE_ROOT`가 가리키는 로컬 디렉토리다.
- DB에는 저장소 루트가 아닌 `연도/월/UUID.확장자` 상대 경로만 저장한다.
- 원본 파일명은 표시와 응답 헤더에만 사용하고 실제 저장 경로에는 사용하지 않는다.
- 파일 확장자와 MIME 타입은 업로드 파일 내용의 signature를 기준으로 결정한다.
- 허용 형식은 JPEG, PNG, WebP, GIF, PDF다.
- SVG와 실행 가능한 문서 형식은 허용하지 않는다.
- 기본 파일 제한은 20MB, 기본 multipart 요청 제한은 21MB다.
- JPEG, PNG, GIF는 이미지 헤더에서 크기를 읽고 가로 또는 세로 20,000px 초과를 거부한다.

환경 변수:

```text
MEDIA_STORAGE_ROOT=./storage/media
MEDIA_MAX_FILE_SIZE=20MB
MEDIA_MAX_REQUEST_SIZE=21MB
```

## 4. 컬렉션 규칙

`collection`은 영문 소문자로 시작하고 영문 소문자, 숫자, 밑줄만 사용하며 최대 50자다.

```text
logo
thumbnail
gallery
business_registration_file
license_file
```

컬렉션 이름은 도메인 요구사항에서 정의한다. 임의의 Java 클래스명이나 화면 컴포넌트명을 컬렉션으로 사용하지 않는다.

같은 `(owner_type, owner_id, collection)` 범위에서 다음 규칙을 적용한다.

- 첫 번째 업로드 파일은 자동으로 대표 파일이 된다.
- `is_primary=true`로 업로드하거나 수정하면 기존 대표 파일을 해제한다.
- 대표 파일을 삭제하면 남은 파일 중 정렬 순서가 가장 빠른 파일을 대표로 지정한다.
- `sort_order`를 생략하면 현재 마지막 순서 다음 값으로 저장한다.

## 5. 삭제 정책

API 삭제는 DB 레코드의 `deleted_at`만 기록한다. 원본 파일은 즉시 삭제하지 않는다.

- DB 트랜잭션이 롤백된 신규 업로드 파일은 즉시 물리 삭제한다.
- 병원을 soft delete하면 연결된 `HOSPITAL` Media도 같은 트랜잭션에서 soft delete한다.
- 보존기간 이후 원본 파일을 물리 삭제하는 작업은 Scheduler 도메인에서 별도로 구현한다.
- 병원을 hard delete해도 폴리모픽 Media에는 DB cascade가 적용되지 않으므로 application service 연동이 필수다.

## 6. Staff API

기본 경로:

```text
/api/v1/staff/media
```

| Method | Path | 기능 |
|---|---|---|
| `GET` | `/api/v1/staff/media` | 대상 컬렉션 파일 목록 |
| `GET` | `/api/v1/staff/media/{id}` | 미디어 메타데이터 상세 |
| `GET` | `/api/v1/staff/media/{id}/content` | 인증된 원본 파일 조회 |
| `POST` | `/api/v1/staff/media` | multipart 파일 업로드 |
| `PATCH` | `/api/v1/staff/media/{id}` | 정렬, 대표 여부, metadata 수정 |
| `DELETE` | `/api/v1/staff/media/{id}` | 미디어 soft delete |

목록 필수 query:

```text
owner_type=HOSPITAL
owner_id=1
collection=gallery
```

업로드 multipart 필드:

```text
owner_type
owner_id
collection
file
sort_order    선택
is_primary   선택
metadata     선택, JSON 객체 문자열
```

수정 body 예시:

```json
{
  "sort_order": 1,
  "is_primary": true,
  "metadata": {
    "alt": "병원 내부"
  }
}
```

`content_url`은 인증이 필요한 Staff 상대 경로를 반환한다. 공개 앱용 파일 조회는 각 도메인의 공개 상태와 미디어 공개 범위 정책을 확인하는 별도 API로 제공한다.

## 7. 권한

Media는 독립 운영 리소스가 아니라 연결 대상의 일부이므로 대상 도메인 권한을 상속한다.

| 작업 | `HOSPITAL` 대상 권한 |
|---|---|
| 목록, 상세, 원본 조회 | `platform.hospital.show` |
| 업로드, 수정, 삭제 | `platform.hospital.update` |

새 `owner_type`을 추가할 때 대상 존재 검증과 읽기/수정 권한 매핑을 반드시 같이 추가한다.

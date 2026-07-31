# 카테고리

## 목적

파트너, 스페셜리스트, 이벤트, 후기는 하나의 공통 뷰티 카테고리 트리를 사용한다. 대상마다 별도 카테고리나 사용처 매핑을 만들지 않고, 같은 트리에서 선택 가능한 깊이만 다르게 제한한다.

## 모델

```text
categories
  id
  domain
  parent_id
  depth
  group_code
  name
  code
  full_path
  sort_order
  status
  is_menu_visible

category_assignments
  id
  categorizable_type
  categorizable_id
  category_id
  is_primary
```

뷰티 카테고리의 domain은 `BEAUTY`다. `category_usages`는 사용하지 않는다.

`category_assignments`의 대상은 다음 네 종류다.

| 대상 | 코드 | 선택 가능 깊이 | 복수 선택 |
|---|---|---:|---:|
| 파트너 | `PARTNER` | 1뎁스 대카테고리 | 가능 |
| 스페셜리스트 | `SPECIALIST` | 1뎁스 대카테고리 | 가능 |
| 이벤트 | `EVENT` | 3뎁스 소카테고리 | 가능 |
| 후기 | `REVIEW` | 3뎁스 소카테고리 | 가능 |

이벤트와 후기 도메인을 구현할 때도 별도 카테고리 테이블을 만들지 않고 `category_assignments`를 사용한다.

## 그룹과 계층

MVP 기준 파트너 분류는 1뎁스만 seed한다. `group_code`는 확장 호환을 위해 `TREATMENT`를 사용한다.

| 코드 | 표시명 |
|---|---|
| `TREATMENT` | 시술 |

카테고리는 최대 3뎁스다.

```text
대카테고리(depth=1) > 중카테고리(depth=2) > 소카테고리(depth=3)
```

- 뷰티 대카테고리는 `BEAUTY`, `ACTIVE`, `depth=1`이어야 한다.
- 하위 카테고리는 상위 카테고리의 그룹을 상속한다.
- `full_path`는 그룹명을 제외한 `대 > 중 > 소` 형식이다.
- 같은 그룹의 루트 또는 같은 부모 아래에서 이름을 중복할 수 없다.
- 같은 domain에서 code는 중복할 수 없다.
- 상위 이름이나 그룹을 변경하면 하위 `full_path`와 그룹도 같은 트랜잭션에서 변경한다.

## 기준 카테고리

MVP 파트너 분류는 다음 7개다.

```text
반영구
에스테틱
미용실
왁싱
타투
네일아트
마사지
```

전체 명칭, 계층, 순서의 실행 기준은 `V11__reseed_beauty_categories.sql`이다.

## 코드 규칙

카테고리 code는 표시명과 분리된 고정 식별자다.

```text
KB_HAIR_SALON        미용실
KB_NAIL              네일아트
KB_ESTHETIC          에스테틱
```

초기 코드는 정의 순서로 부여하지만, 카테고리 이름이나 정렬 순서가 바뀌어도 기존 코드는 변경하거나 다른 카테고리에 재사용하지 않는다.

## 선택과 검색

- 파트너와 스페셜리스트 저장 시 `BEAUTY`, `ACTIVE`, `depth=1`을 모두 검증한다.
- 이벤트와 후기 저장 시에도 같은 `BEAUTY` 트리를 사용하되, 2~3뎁스 시술 옵션 확장 뒤 `depth=3` 검증을 적용한다.
- 대·중카테고리로 이벤트나 후기를 검색하면 해당 카테고리의 활성 하위 소카테고리 할당까지 포함해야 한다.
- 하위 조회가 생기면 domain, group, full path를 함께 사용한다.

## Staff API

| method | path | 설명 |
|---|---|---|
| GET | `/api/v1/staff/categories` | 관리 목록 |
| GET | `/api/v1/staff/categories/selector` | 선택 UI 목록 |
| GET | `/api/v1/staff/categories/{id}` | 상세 |
| POST | `/api/v1/staff/categories` | 생성, JSON 또는 multipart |
| PATCH | `/api/v1/staff/categories/{id}` | JSON 부분수정 |
| POST | `/api/v1/staff/categories/{id}` | multipart 부분수정 |
| DELETE | `/api/v1/staff/categories/{id}` | 삭제 |

권한은 `platform.category.manage`다.

Selector는 domain, parent, depth, group, 상태, 메뉴 노출 여부와 검색어를 지원한다. 사용처별 `usage` 필터는 지원하지 않는다. parent나 depth가 없고 검색어도 없으면 루트만 반환한다.

## 쓰기와 삭제

- 생성·수정 시 활성 parent, 형제 이름, domain code, 그룹 상속을 검증한다.
- 아이콘은 multipart에서 선택적으로 등록·교체한다.
- 하위 카테고리 또는 `category_assignments`가 남아 있으면 삭제를 거부한다.
- 생성·수정·삭제는 `OperationHistory`에 기록한다.

## 아이콘 공개

카테고리 아이콘은 Staff 원본 조회와 User 앱 공개 조회를 지원한다. User 경로는 카테고리가 `ACTIVE`이고 컬렉션이 `icon`일 때만 파일을 반환한다.

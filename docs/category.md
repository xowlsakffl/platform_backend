# 카테고리

## 목적

업체와 가격 옵션은 하나의 공통 카테고리 트리를 사용한다. 기능마다 별도 카테고리 테이블을 만들지 않고 `usage`로 사용처를 구분한다.

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

category_usages
  id
  usage_code
  category_id
  sort_order
  status

category_assignments
  id
  categorizable_type
  categorizable_id
  category_id
  is_primary
```

- `CategoryUsage`: 특정 기능에서 선택할 수 있는 카테고리와 노출 순서
- `CategoryAssignment`: 업체나 가격 옵션이 실제로 선택한 카테고리 연결

## 단일 정의 파일

업체 카테고리의 유일한 원본은 다음 JSON이다.

```text
src/main/resources/category-definitions/trees/partner.json
```

1단계는 업체 분류, 2단계는 해당 업종의 가격 옵션 분류다. `CategoryUsage`는 별도 파일에 중복 정의하지 않고 깊이에서 자동으로 결정한다.

| 깊이 | Usage | 용도 |
|---:|---|---|
| 1 | `PARTNER_CATEGORY` | 업체 분류 |
| 2 | `PARTNER_OPTION_CATEGORY` | 가격 옵션 분류 |

카테고리를 추가, 수정, 정렬하거나 제거할 때는 `partner.json`만 수정한다. 카테고리 데이터용 Flyway migration은 추가하지 않는다.

## 시작 시 동기화

애플리케이션은 시작할 때 JSON을 검증하고 DB에 트랜잭션으로 동기화한다.

- 키는 `domain + code`다.
- JSON에 있는 카테고리는 생성하거나 이름, 계층, 경로, 순서와 상태를 갱신한다.
- JSON에서 제거된 기존 카테고리와 usage는 참조 보존을 위해 삭제하지 않고 `INACTIVE`로 전환한다.
- 기존 `category_assignments`는 삭제하지 않는다.
- 코드 중복, 같은 부모 아래 이름 중복, 2단계 초과, 빈 값이 있으면 시작에 실패한다.
- 동기화는 기본 활성화이며 `CATEGORY_DEFINITION_SYNC_ENABLED=false`로만 끌 수 있다.

Flyway는 테이블이나 인덱스 같은 스키마 변경에만 사용한다. 카테고리와 usage 데이터는 migration에 중복 정의하지 않는다.

JSON과 운영 DB의 충돌을 막기 위해 Staff API에서는 `PARTNER` 카테고리의 생성, 삭제 및 구조 필드 수정을 허용하지 않는다. 카테고리 아이콘은 공통 미디어이므로 계속 수정할 수 있다.

## 선택 API

Staff는 `usage`와 부모 카테고리로 선택 목록을 제한한다.

```text
GET /api/v1/staff/categories/selector?domain=PARTNER&usage=PARTNER_CATEGORY
GET /api/v1/staff/categories/selector?domain=PARTNER&usage=PARTNER_OPTION_CATEGORY&parent_id={categoryId}
```

Partner는 로그인한 업체의 분류를 기준으로 옵션 카테고리를 제한한다.

```text
GET /api/v1/partner/categories?usage=PARTNER_CATEGORY
GET /api/v1/partner/categories?usage=PARTNER_OPTION_CATEGORY
```

## 연결 규칙

| 대상 | 허용 카테고리 | MVP 규칙 |
|---|---|---|
| `PARTNER` | `PARTNER_CATEGORY`에 등록된 1단계 | 업체당 1개 필수 |
| `PARTNER_OPTION` | 업체 분류 아래 `PARTNER_OPTION_CATEGORY` 2단계 | 옵션당 1개 필수 |

전문가와 이벤트는 카테고리를 중복 연결하지 않는다. 전문가와 이벤트는 가격 옵션을 통해 분류를 확인하고, 후기는 상담·예약 결과를 통해 옵션 정보를 참조한다.

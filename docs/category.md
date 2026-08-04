# 카테고리

## 목적

업체와 가격 옵션은 하나의 공통 카테고리 트리를 사용한다. 기능별로 별도 카테고리 테이블을 만들지 않는다.

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
  usage
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

`CategoryUsage`와 `CategoryAssignment`의 역할은 다르다.

- `CategoryUsage`: 특정 기능에서 선택 가능한 카테고리 목록과 노출 순서
- `CategoryAssignment`: 업체나 옵션에 실제로 선택된 카테고리 연결

## 업체 트리

업체 카테고리 domain은 `PARTNER`다.

```text
업체 분류(depth=1) > 옵션 분류(depth=2)
```

예시:

```text
미용실
├─ 커트
├─ 펌
├─ 염색
└─ 클리닉

네일아트
├─ 젤네일
├─ 네일아트
├─ 연장
└─ 페디큐어
```

## Usage

| 코드 | 선택 대상 | 허용 깊이 |
|---|---|---:|
| `PARTNER_CATEGORY` | 업체 분류 | 1 |
| `PARTNER_OPTION_CATEGORY` | 가격 옵션 분류 | 2 |

Staff selector는 `usage` 필터를 지원한다.

```text
GET /api/v1/staff/categories/selector?domain=PARTNER&usage=PARTNER_CATEGORY
GET /api/v1/staff/categories/selector?domain=PARTNER&usage=PARTNER_OPTION_CATEGORY&parent_id={categoryId}
```

파트너 selector는 로그인한 업체의 분류를 기준으로 옵션 카테고리를 제한한다.

```text
GET /api/v1/partner/categories?usage=PARTNER_CATEGORY
GET /api/v1/partner/categories?usage=PARTNER_OPTION_CATEGORY
```

## Assignment

| 코드 | 연결 대상 | 선택 규칙 |
|---|---|---|
| `PARTNER` | 업체 | `PARTNER_CATEGORY`에 등록된 depth 1 카테고리 |
| `PARTNER_OPTION` | 가격 옵션 | 업체 카테고리 아래의 `PARTNER_OPTION_CATEGORY` 카테고리 |

업체와 옵션은 MVP에서 각각 대표 카테고리 하나를 필수로 사용한다. 저장 시 기존 연결을 교체하고 `is_primary=true`로 기록한다.

기존 가격 옵션은 마이그레이션 시 업체 분류 아래의 `기타` 옵션 분류로 연결한다. 옵션이 존재하는 업체의 분류를 변경하려면 옵션 분류를 먼저 정리해야 한다.

전문가, 이벤트, 후기에는 카테고리를 중복 연결하지 않는다. 전문가와 이벤트는 가격 옵션을 통해 카테고리를 확인하고, 후기는 상담·예약 결과를 통해 옵션 정보를 참조한다.

## 삭제 정책

다음 조건 중 하나라도 해당하면 카테고리를 삭제할 수 없다.

- 활성 하위 카테고리가 존재한다.
- `category_usages`에서 선택 가능 항목으로 사용 중이다.
- `category_assignments`에서 업체나 옵션에 연결돼 있다.

카테고리 이름이나 노출을 중단하려면 삭제 대신 `INACTIVE` 상태를 사용한다.

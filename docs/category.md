# 카테고리 설계

작성 기준: 2026-07-28

카테고리는 병원, 의료진, 후기, 이벤트, 동영상, FAQ 등 여러 도메인에서 공유하는 분류 체계다.

## 1. 설계 원칙

카테고리는 두 가지 책임으로 분리한다.

| 구분 | 기준 | 책임 |
| --- | --- | --- |
| 카테고리 트리 | `categories` | 서비스 분류 체계 원본 |
| 카테고리 그룹 | `categories.group_code` | 같은 domain 안의 성형/쁘띠 같은 성격 구분 |
| 사용처 노출 목록 | `category_usages` 또는 사용처 정책 | 특정 화면/기능에서 보여줄 카테고리 선별 |
| 카테고리 연결 | `category_assignments` | 도메인 객체와 카테고리 연결 |

`categories`에는 화면별 노출 플래그를 무분별하게 추가하지 않는다. 병원 진료과목, 의료진 진료분야, 앱 필터처럼 같은 카테고리 트리에서 서로 다른 노드를 골라 써야 하는 기능은 사용처 정책으로 분리한다.

## 2. 병원 의료 카테고리

병원/의료진/후기/영상에서 쓰는 의료 카테고리는 공통 domain을 사용한다.

```text
HOSPITAL_MEDICAL
```

최상위 구조:

```text
HOSPITAL_MEDICAL
- 눈
- 코
- 지방흡입 / 이식
- 가슴
- 거상
- 안면윤곽 / 양악
- 모발이식
- 기타
- 리프팅
- 필러
- 보톡스
- 지방분해주사
- 피부
- 제모/탈모
- 치과
- 부인과
- 안과
- 한방
```

`성형`, `쁘띠`는 카테고리 노드로 저장하지 않는다. 이 둘은 `categories.group_code`로만 구분한다.

| group_code | 표시명 | 의미 |
| --- | --- | --- |
| `SURGERY` | 성형 | 눈, 코, 지방흡입/이식, 가슴, 거상, 안면윤곽/양악, 모발이식, 기타 |
| `TREATMENT` | 쁘띠 | 리프팅, 필러, 보톡스, 지방분해주사, 피부, 제모/탈모, 치과, 부인과, 안과, 한방 |

하위 카테고리는 부모의 `group_code`를 상속한다. 운영 화면에서 부모와 다른 `group_code`를 허용하지 않는다.

## 3. 병원/의료진 진료과목

병원 진료과목과 의료진 진료분야 셀렉트는 전체 트리를 그대로 보여주지 않는다. 병원/의료진이 진료과목으로 표방할 수 있는 노드만 노출한다.

사용처 코드:

```text
HOSPITAL_DOCTOR_SUBJECT
```

규칙:

- `성형`, `쁘띠` 자체는 선택지에 넣지 않는다.
- 성형/쁘띠 구분 표시는 각 카테고리의 `group_code`를 기준으로 한다.
- 선택 가능한 노드는 운영 정책으로 관리한다.
- Java 구현에서는 selector API가 사용처와 parent 기준으로 children을 조회한다.

## 4. 사용처 목록

병원 의료 카테고리에서 관리할 사용처는 다음과 같다.

| usage | 그룹 | 주요 사용처 |
| --- | --- | --- |
| `HOSPITAL_DOCTOR_SUBJECT` | 성형 + 쁘띠 | 병원 진료과목, 의료진 진료분야 |
| `HOSPITAL_REVIEW_SURGERY` | 성형 | 성형후기 게시판 구분/검증 |
| `HOSPITAL_REVIEW_TREATMENT` | 쁘띠 | 시술후기 게시판 구분/검증 |
| `HOSPITAL_EVENT_SURGERY` | 성형 | 이벤트 카테고리 루트 검증 |
| `HOSPITAL_EVENT_TREATMENT` | 쁘띠 | 이벤트 카테고리 루트 검증 |
| `HOSPITAL_VIDEO_CATEGORY` | 성형 + 쁘띠 | 동영상 카테고리 |
| `HOSPITAL_EVENT_AD_SURGERY` | 성형 | 성형 카테고리별 광고 배너 |
| `HOSPITAL_EVENT_AD_TREATMENT` | 쁘띠 | 쁘띠 카테고리별 광고 배너 |

## 5. 후기 게시판 구분

`HospitalReview.category_domain`은 `categories.domain`이 아니라 성형후기/시술후기 게시판 구분값이다.

| 게시판 | 저장값 | usage |
| --- | --- | --- |
| 성형후기 | `HOSPITAL_REVIEW_SURGERY` | `HOSPITAL_REVIEW_SURGERY` |
| 시술후기 | `HOSPITAL_REVIEW_TREATMENT` | `HOSPITAL_REVIEW_TREATMENT` |

후기 작성 시 선택된 leaf 카테고리가 어떤 후기 usage root 아래에 속하는지 보고 `category_domain`을 계산한다.

검증 기준:

- 카테고리 domain은 `HOSPITAL_MEDICAL`
- 선택 카테고리는 active leaf여야 한다.
- 선택 카테고리들은 같은 후기 usage 그룹에 속해야 한다.
- 성형후기 usage 아래면 성형후기, 시술후기 usage 아래면 시술후기로 저장한다.

## 6. 이벤트/동영상 카테고리

| 기능 | 선택 기준 |
| --- | --- |
| 이벤트 대표 카테고리 | active `HOSPITAL_MEDICAL` leaf 카테고리 |
| 동영상 등록/수정 | `HOSPITAL_VIDEO_CATEGORY` usage에 속한 카테고리 |
| 광고 카테고리별 배너 | `HOSPITAL_EVENT_AD_SURGERY` 또는 `HOSPITAL_EVENT_AD_TREATMENT` usage |

여기서 leaf는 현재 active 자식이 없는 카테고리를 뜻한다. depth 숫자만 보고 소분류를 강제하지 않는다.

## 7. 권장 패키지 구조

```text
domain/category/
  Category
  CategoryAssignment

application/category/
  CategoryService
  query/
  result/

infrastructure/persistence/category/
  CategoryRepository
  CategoryAssignmentRepository

adapter/in/web/staff/category/
adapter/in/web/publicapi/category/
```

카테고리 초기 데이터는 Flyway migration 또는 별도 seed command로 관리한다. 초기 데이터 방식이 확정되면 이 문서와 `schema.dbml`을 같이 갱신한다.

## 8. API 응답 원칙

- selector 응답은 `id`, `name`, `code`, `depth`, `parent_id`, `group_code`, `has_children`을 포함한다.
- 프론트는 `group_code`를 기준으로 성형/쁘띠 표시를 한다.
- 카테고리를 여러 개 가질 수 있는 도메인은 응답에서 `categories` 배열로 내려준다.
- 화면마다 다른 선택지는 usage 기준으로 서버가 제한한다.

## 9. 금지 사항

- `성형`, `쁘띠`를 카테고리 노드로 만들지 않는다.
- 화면별 boolean 컬럼을 `categories`에 계속 추가하지 않는다.
- 프론트에서 임의로 카테고리 그룹을 계산하지 않는다.
- 자식이 있는 카테고리를 leaf 전용 기능에 선택 가능하게 하지 않는다.

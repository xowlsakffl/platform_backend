# Platform Product Plan

## 1. 목표

반영구, 에스테틱, 미용실, 왁싱, 타투, 네일아트, 마사지 업체와 고객을 연결하는 K-뷰티 종합 플랫폼을 만든다.

정식 제품 운영을 목표로 업체·전문가·옵션·이벤트 탐색과 상담/예약 요청 접수 흐름을 완성한다. 결제, 예약금, 정산, 광고 과금은 현재 개발 우선순위에서 제외하되 이후 제품 로드맵에서 별도로 결정한다.

## 2. 액터

### Staff

- 입점 검수
- 업체 운영상태 및 계정상태 관리
- 업체·전문가·이벤트 모니터링
- 상담/예약 요청 모니터링

### Partner

- 자신의 업체정보 작성 및 관리
- 전문가와 가격 옵션 관리
- 이벤트 관리
- 상담/예약 요청 확인 및 처리

`AccountPartner`는 로그인 계정이고 `Partner`는 해당 계정이 운영하는 업체정보다. 파트너와 스태프는 이메일이 아닌 `login_id`로 로그인한다. 파트너 계정에는 담당자명이나 업체명을 중복 저장하지 않고 화면 표시명은 `Partner.name`을 사용한다.

### User

- 업체·전문가·이벤트 탐색
- 상담/예약 요청
- 이용한 전문가에 대한 후기 작성

## 3. 입점 흐름

직접 입점 요청 화면은 계정정보부터 받되 업체명은 계정 단계에서 받지 않는다. 사용자가 첫 업체 기본정보까지 입력한 시점에 `AccountPartner`와 `Partner`를 한 트랜잭션으로 생성한다. 최초 `Partner.allow_status`는 `DRAFT`다.

```text
DRAFT -> PENDING -> APPROVED
                 -> REJECTED -> PENDING
```

- `DRAFT`: 최초 입점정보 작성 및 임시저장
- `PENDING`: 제출 완료, 스태프 검수 중
- `APPROVED`: 입점 승인
- `REJECTED`: 반려 사유 확인 후 수정·재신청 가능

파트너는 `DRAFT` 또는 `REJECTED`에서만 입점정보를 수정한다. `PENDING`에서는 검수 대상이 바뀌지 않도록 수정할 수 없다.

스태프는 `PENDING`을 `APPROVED` 또는 `REJECTED`로만 변경한다. 반려 사유는 필수다. 별도의 입점신청 데이터와 메뉴를 만들지 않고 업체 목록에서 `allow_status`로 구분한다. `DRAFT`는 기본 목록에서 제외하고 명시적인 필터가 있을 때만 조회한다.

상태 역할은 분리한다.

- `Partner.allow_status`: 입점 검수상태
- `Partner.status`: 업체 운영상태
- `AccountPartner.status`: 파트너 계정상태

### 내부관리자 등록

스태프가 업체를 대신 등록할 때는 `Partner`만 먼저 생성하고 `AccountPartner`는 생성하지 않는다. `Partner.registration_source`는 `STAFF_CREATED`로 기록하고 생성한 스태프 ID를 보관한다.

스태프 등록 시 업체의 대표 전화번호와 대표 이메일을 `partner_contacts`에 필수 연락처로 저장한다. 이 연락처는 계정정보나 계정 초대 이메일로 사용하지 않는다. 초대 수신 이메일은 발송 시 별도로 입력하고 해당 초대 이력 행에 기록한다. 재발송할 때만 직전 초대 이메일을 기본값으로 사용할 수 있다.

스태프가 이메일로 계정 초대 링크를 보내면 수신자는 링크에서 로그인 아이디, 전화번호와 비밀번호를 입력한다. 업체명과 담당자명은 받지 않는다. 수락 시점에 `AccountPartner`를 생성해 기존 `Partner`와 연결하며, 계정 이메일은 초대 수신 이메일을 사용한다. 임시 비밀번호나 비밀번호가 설정된 가계정은 발급하지 않는다.

초대 링크는 72시간 동안 유효하고 한 번만 사용할 수 있다. 원본 토큰은 저장하지 않고 SHA-256 해시만 저장한다.

`partner_account_invitations`는 현재 초대상태만 저장하는 테이블이 아니라 초대 이력의 원본이다. 최초 발송과 재발송은 각각 새 행으로 추가한다. 신규 메일 발송에 성공한 뒤 아직 유효한 기존 초대를 `CANCELED`로 전환해 토큰을 무효화하고 이력 화면에는 `무효처리`로 표시한다. 메일 발송이 실패하면 신규 초대만 무효화하며 기존 유효 링크는 유지한다. 이미 만료된 초대는 `PENDING` 상태와 지난 `expires_at`을 그대로 보존하고 이력 화면에서 `초대 만료`로 계산한다. 수동 취소 API는 제공하지 않는다. 향후 추가할 종합 운영 이력은 업체 전반의 변경 추적용으로 분리하며, 초대 상세 이력은 이 테이블을 기준으로 조회한다.

초대 수락은 `Partner.allow_status`를 변경하지 않는다. `DRAFT` 업체는 계정 연결 후 정보를 완성해 제출할 수 있고, 스태프가 검증까지 마친 `APPROVED` 업체는 계정 연결 후 바로 관리할 수 있다.

계정 연결상태는 업체 검수상태와 별도로 다음과 같이 계산한다.

```text
NOT_INVITED
INVITED
CONNECTED
```

`초대 만료`는 별도의 계정 연결상태가 아니라 `INVITED`와 `expires_at`을 기준으로 프론트에서 표시한다.

## 4. 업체정보

### 기본정보

- 업체명(최대 30자), 업체 영문명(최대 90자)
- 상세 설명: 최대 2,000자
- 대표 업체 카테고리 하나
- 도로명 주소, 지번 주소, 상세 주소
- 위도, 경도
- 요일별 운영시간 JSON: 영업·휴게 구간, 24시간 영업, 익일 종료, 예약 접수 마감, 시간대 포함
- 휴무 정책 JSON: 정기 휴무, 법정공휴일, 대체공휴일, 일회·매년·매월 지정 휴무 포함
- 운영시간 추가 문구: `7/29 내부 공사로 휴무`와 같은 특정일 안내
- 오시는 길
- 대표 전화번호, 대표 이메일
- 해시태그: 최대 10개, 업체 내 중복 금지. 공통 `hashtags` 사전과 폴리모픽 `hashtag_relations`로 연결
- 로고, 대표 이미지, 내부 이미지

업체 분류와 옵션 분류는 공통 `Category` 트리를 사용한다.

```text
미용실                     depth=1
├─ 커트                    depth=2
├─ 펌
├─ 염색
└─ 클리닉
```

- `CategoryUsage.PARTNER_CATEGORY`: 업체가 선택할 수 있는 1단계 카테고리
- `CategoryUsage.PARTNER_OPTION_CATEGORY`: 가격 옵션이 선택할 수 있는 2단계 카테고리
- `CategoryAssignment.PARTNER`: 업체와 대표 카테고리 연결
- `CategoryAssignment.PARTNER_OPTION`: 가격 옵션과 옵션 카테고리 연결

`CategoryUsage`는 기능별 선택 가능 목록이고, 실제 데이터 연결은 폴리모픽 `CategoryAssignment`가 담당한다. `Partner`에는 별도 `industry` 컬럼을 두지 않는다.

### 부가정보

편의시설과 업체 특징은 `PartnerFeature` 기준정보에서 선택한다. 업체 테이블에 특징별 boolean 컬럼을 추가하지 않는다.

외부 링크는 `PartnerLink`로 관리하며 종류별 하나만 등록한다.

```text
BLOG
CAFE
HOMEPAGE
RESERVATION
KAKAO
BAND
FACEBOOK
INSTAGRAM
YOUTUBE
SMART_STORE
```

### 검수정보

사업자등록정보와 증빙 파일은 공개 업체정보와 분리한다. 임시저장 단계에서는 일부 값이 비어 있을 수 있지만, 입점 제출 시 필수값과 증빙 파일을 검증한다.

### 미디어

업체 로고, 대표 이미지, 내부 이미지, 사업자등록증은 공통 `Media` 테이블을 사용한다. `ownerType`, `ownerId`, `collection`, `sortOrder`로 구분한다.

### 운영시간과 휴무

운영시간은 요일별 단일 시작·종료값이 아니라 예약 가능시간 계산에 사용할 수 있는 구조로 저장한다.

- 각 요일은 `영업시간 지정`, `24시간`, `휴무` 중 하나다.
- 영업 구간과 휴게 구간은 하루 각각 최대 4개다.
- 자정을 넘기는 영업 구간은 `ends_next_day`로 명시한다.
- `last_booking_time`은 선택값이며 없으면 영업 종료시간을 기준으로 한다.
- 기본 시간대는 `Asia/Seoul`이며 유효한 IANA timezone만 허용한다.
- 휴게 구간은 영업 구간 안에 있어야 하고 같은 종류의 구간끼리 겹칠 수 없다.

휴무 정책은 운영시간과 분리해 반복 규칙과 특정 날짜 예외를 함께 표현한다.

- 정기 휴무: 매주 또는 매월 첫째~다섯째·마지막 주와 요일 조합
- 공휴일 휴무: 공휴일별 선택, 설·추석 당일만 적용, 대체공휴일 포함 여부
- 대체공휴일: 날짜만 추가하며 표시명은 `대체공휴일`로 고정
- 지정 휴무: 일회, 매년, 매월 반복과 시작·종료일
- `enabled=false`이면 휴무 규칙을 노출·적용하지 않는다.

## 5. 가격 옵션

가격 옵션의 소유자는 `Partner`이며 도메인 이름은 `PartnerOption`이다.

필드:

- 옵션명
- 옵션 카테고리
- 설명
- 정상가
- 할인가
- 예상 소요시간
- 노출 여부
- 노출 순서

정상가는 필수이며 할인가를 사용하지 않는 옵션은 할인가를 비워 둔다. 할인가를 입력하면 정상가보다 낮아야 한다. 할인율은 별도 저장하지 않고 `(정상가 - 할인가) / 정상가 × 100`을 정수 내림으로 계산한다.

옵션은 전문가를 연결하지 않아도 유효하다. 사용자는 업체 화면에서 전문가를 지정하지 않고 옵션만 선택해 상담/예약을 요청할 수 있다.

옵션 카테고리는 업체가 선택한 1단계 카테고리의 활성 하위 카테고리만 선택할 수 있다. 파트너가 옵션 카테고리를 자유 입력으로 생성하지는 않는다.

전문가가 수행 가능한 옵션은 `SpecialistOption`으로 연결한다. 전문가별 가격이 다르면 `regular_price_override`와 `sale_price_override`를 사용한다. 전문가 커스텀 정상가가 없으면 업체 옵션의 정상가·할인가를 함께 사용한다.

### 5.1 전문가별 옵션과 가격 노출

`PartnerOption`은 업체가 제공하는 시술의 원본이며, `SpecialistOption`은 해당 전문가의 제공 여부와 가격·소요시간 오버라이드를 관리하는 연결 도메인이다. 전문가마다 옵션명과 설명을 복제하지 않는다.

```text
전문가 커스텀 정상가 없음:
  최종 정상가 = PartnerOption.regular_price
  최종 할인가 = PartnerOption.sale_price

전문가 커스텀 정상가 있음:
  최종 정상가 = SpecialistOption.regular_price_override
  최종 할인가 = SpecialistOption.sale_price_override

최종 적용 가격 = 최종 할인가 ?? 최종 정상가
```

전문가 커스텀 가격은 정상가를 필수로 입력하고 할인가를 선택값으로 둔다. 이를 통해 업체 기본 할인을 상속할지, 전문가가 별도 가격을 사용할지 명확하게 구분한다.

업체 화면의 가격 범위는 해당 옵션을 실제로 제공하는 활성·노출 전문가만 대상으로 계산한다. 연결된 활성 전문가가 없으면 업체 기본 가격을 사용한다.

가격 노출 규칙:

- 전문가를 선택하면 해당 전문가의 최종 정상가·할인가와 할인율을 표시한다.
- 전문가를 선택하지 않았고 전문가별 가격이 다르면 `30,000원~50,000원 · 전문가별 상이` 형식으로 표시한다.
- 전문가를 선택하지 않았고 모든 전문가의 가격이 같으면 단일 가격만 표시한다.

전문가 미지정 요청에서는 가격 범위를 안내값으로 사용하며, 실제 적용 가격은 전문가가 배정된 뒤 해당 전문가의 최종 가격을 기준으로 확정한다. 현재 결제 기능은 포함하지 않는다.

## 6. 전문가

`Specialist`는 사용자가 직접 선택할 수 있는 시술 전문가다.

예시:

- 미용실: 헤어디자이너
- 네일샵: 네일아티스트
- 타투샵: 타투이스트
- 왁싱샵: 왁서
- 반영구: 반영구 아티스트
- 마사지·에스테틱: 테라피스트 또는 관리사

전문가는 업체에 소속되고, 가능한 `PartnerOption`과 전문가별 가격을 가진다. 후기는 실제 서비스를 제공한 전문가에 소속하는 것을 기본으로 한다. 전문가를 지정하지 않은 이용 건의 후기 정책은 후기 도메인 구현 시 별도로 결정한다.

### 6.1 전문가 근무시간과 휴무

업체 영업시간은 매장이 고객을 받을 수 있는 시간이고, 전문가 근무시간은 해당 전문가가 시술 가능한 시간이다. 업체 휴무일에는 모든 전문가의 예약을 받을 수 없으며, 전문가 휴무일은 해당 전문가에게만 적용한다.

전문가 근무시간은 다음 두 방식으로 관리한다.

```text
INHERIT_PARTNER_HOURS
CUSTOM_HOURS
```

- `INHERIT_PARTNER_HOURS`: 업체 영업시간을 기본 근무시간으로 사용한다.
- `CUSTOM_HOURS`: 전문가의 요일별 근무시간과 휴게시간을 직접 설정한다.
- 두 방식 모두 전문가 정기 휴무와 지정 휴무를 추가할 수 있다.
- 초기 정책에서는 전문가 설정으로 업체 휴무일이나 업체 영업시간 밖을 예약 가능하게 만들 수 없다.

실제 예약 가능 시간은 다음 기준으로 계산한다.

```text
업체 영업시간
∩ 전문가 근무시간
- 업체 휴무일
- 전문가 휴무일
- 기존 예약 시간
```

## 7. 상담/예약 요청

상담과 예약을 별도 도메인으로 나누지 않고 하나의 요청 흐름으로 관리한다.

유입경로:

```text
PARTNER
SPECIALIST
EVENT
```

관계 원칙:

- 모든 요청은 `Partner`에 연결한다.
- `PartnerOption` 선택은 가능하다.
- `Specialist` 선택은 선택값이며 필수가 아니다.
- 전문가 화면에서 신청한 경우 해당 전문가를 요청 전문가로 기록한다.
- 업체 화면에서 신청한 경우 요청 전문가 없이 접수하고 업체가 나중에 담당 전문가를 배정할 수 있다.
- 이벤트 화면에서 신청한 경우 이벤트와 유입경로를 함께 기록한다.

향후 예약 요청의 핵심 참조 구조:

```text
partner_id             required
partner_option_id      optional
requested_specialist_id optional
assigned_specialist_id  optional
event_id               optional
inflow_source          required
```

전문가를 지정해서 요청하면 `requested_specialist_id`와 최종 `assigned_specialist_id`가 동일할 수 있다. 미지정 요청은 `requested_specialist_id` 없이 접수하고 배정 시점에 `assigned_specialist_id`를 기록한다.

상태값과 세부 입력 컬럼은 예약 도메인 구현 단계에서 확정한다.

## 8. 이벤트

업체가 노출하는 프로모션 콘텐츠다. 이벤트에서 상담/예약 요청을 만들 수 있고 유입경로는 `EVENT`로 기록한다. 필요하면 `PartnerOption` 또는 `Specialist`를 연결할 수 있다.

## 9. 백엔드 API

파트너 입점:

```text
POST   /api/v1/partner/onboarding/signup
GET    /api/v1/partner/onboarding
PATCH  /api/v1/partner/onboarding
POST   /api/v1/partner/onboarding/submit
```

가격 옵션:

```text
GET    /api/v1/partner/options
POST   /api/v1/partner/options
PATCH  /api/v1/partner/options/{id}
DELETE /api/v1/partner/options/{id}
```

스태프 가격 옵션:

```text
GET    /api/v1/staff/partners/{partnerId}/options
POST   /api/v1/staff/partners/{partnerId}/options
PUT    /api/v1/staff/partners/{partnerId}/options
PATCH  /api/v1/staff/partners/{partnerId}/options/{optionId}
DELETE /api/v1/staff/partners/{partnerId}/options/{optionId}
```

스태프 업체관리:

```text
GET    /api/v1/staff/partners
POST   /api/v1/staff/partners
GET    /api/v1/staff/partners/{id}
PATCH  /api/v1/staff/partners/{id}/fields
PATCH  /api/v1/staff/partners/{id}/allow-status
PATCH  /api/v1/staff/partners/{id}/status
PATCH  /api/v1/staff/partners/{id}/account-status
```

스태프 계정 초대관리:

```text
GET    /api/v1/staff/partner-account-invitations
GET    /api/v1/staff/partners/{partnerId}/account-invitations
POST   /api/v1/staff/partners/{partnerId}/account-invitations
POST   /api/v1/staff/partners/{partnerId}/account-invitations/{id}/resend
```

목록 API는 최신순 초대 이력을 반환한다. 재발송 API 응답의 ID는 기존 초대 ID가 아니라 새로 생성된 초대 이력 ID다.

초대 확인 및 계정 생성:

```text
GET  /api/v1/partner/account-invitations/verify?token={token}
POST /api/v1/partner/account-invitations/accept
```

## 10. 입점 제출 필수조건

- 업체명
- 상세 설명
- 업체 카테고리
- 도로명·지번 주소
- 위도·경도
- 요일별 상세 운영시간과 휴무 정책
- 대표 연락처
- 대표 이미지
- 사업자등록 필수정보와 증빙 파일
- 가격 옵션은 선택 입력이며, 등록하는 옵션에는 카테고리를 지정한다.

부가정보, 외부 링크, 해시태그와 내부 이미지는 선택값이다.

## 11. 현재 미구현 범위

- 결제 및 예약금
- 파트너 정산
- 쿠폰 및 포인트
- 광고 과금
- 외부 캘린더 자동 연동
- 네이버 플레이스 자동 연동
- 카카오톡 알림톡 연동
- 복잡한 좌석·장비·근무표 기반 예약 가능시간 계산

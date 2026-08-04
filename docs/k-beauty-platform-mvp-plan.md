# K-Beauty Platform MVP Plan

## 1. 목표

반영구, 에스테틱, 미용실, 왁싱, 타투, 네일아트, 마사지 업체와 고객을 연결하는 K-뷰티 종합 플랫폼을 만든다.

MVP의 범위는 업체·전문가·이벤트 탐색과 상담/예약 요청 접수까지다. 결제, 예약금, 정산, 광고 과금은 포함하지 않는다.

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

`AccountPartner`는 로그인 계정이고 `Partner`는 해당 계정이 운영하는 업체정보다. 가입할 때 두 데이터를 함께 생성한다.

### User

- 업체·전문가·이벤트 탐색
- 상담/예약 요청
- 이용한 전문가에 대한 후기 작성

## 3. 입점 흐름

직접 입점 요청에서는 `AccountPartner`와 `Partner`를 한 트랜잭션으로 생성한다. 최초 `Partner.allow_status`는 `DRAFT`다.

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

스태프가 업체 담당자 이메일로 계정 초대 링크를 보내면 담당자가 링크에서 이름, 표시명, 전화번호와 비밀번호를 입력한다. 초대 이메일이 로그인 아이디이며 수락 시점에 `AccountPartner`를 생성해 기존 `Partner`와 연결한다. 임시 비밀번호나 비밀번호가 설정된 가계정은 발급하지 않는다.

초대 링크는 72시간 동안 유효하고 한 번만 사용할 수 있다. 재발송하면 기존 토큰은 즉시 무효화한다. 원본 토큰은 저장하지 않고 SHA-256 해시만 저장한다.

초대 수락은 `Partner.allow_status`를 변경하지 않는다. `DRAFT` 업체는 계정 연결 후 정보를 완성해 제출할 수 있고, 스태프가 검증까지 마친 `APPROVED` 업체는 계정 연결 후 바로 관리할 수 있다.

계정 연결상태는 업체 검수상태와 별도로 다음과 같이 계산한다.

```text
NOT_INVITED
INVITED
EXPIRED
CONNECTED
```

## 4. 업체정보

### 기본정보

- 업체명
- 상세 설명: 최대 2,000자
- 대표 업체 카테고리 하나
- 도로명 주소, 지번 주소, 상세 주소
- 위도, 경도
- 요일별 운영시간 JSON
- 운영시간 추가 문구: `7/29 내부 공사로 휴무`와 같은 특정일 안내
- 오시는 길
- 연락처
- 해시태그: 최대 10개, 업체 내 중복 금지
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

## 5. 가격 옵션

가격 옵션의 소유자는 `Partner`이며 도메인 이름은 `PartnerOption`이다.

필드:

- 옵션명
- 옵션 카테고리
- 설명
- 업체 기본 가격
- 가격 표시 방식
- 예상 소요시간
- 노출 여부
- 노출 순서

가격 표시 방식:

```text
FIXED
FROM
INQUIRE
```

옵션은 전문가를 연결하지 않아도 유효하다. 사용자는 업체 화면에서 전문가를 지정하지 않고 옵션만 선택해 상담/예약을 요청할 수 있다.

옵션 카테고리는 업체가 선택한 1단계 카테고리의 활성 하위 카테고리만 선택할 수 있다. 파트너가 옵션 카테고리를 자유 입력으로 생성하지는 않는다.

전문가가 수행 가능한 옵션은 `SpecialistOption`으로 연결한다. 전문가별 가격이 다르면 `price_override`를 사용하고, 가격 표시 방식도 다르면 `price_type_override`를 사용한다. 오버라이드가 없으면 업체 기본 가격을 사용한다.

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
- 전문가 화면에서 신청한 경우 해당 전문가를 연결한다.
- 업체 화면에서 신청한 경우 전문가 없이 요청할 수 있다.
- 이벤트 화면에서 신청한 경우 이벤트와 유입경로를 함께 기록한다.

향후 예약 요청의 핵심 참조 구조:

```text
partner_id             required
partner_option_id      optional
specialist_id          optional
event_id               optional
inflow_source          required
```

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

스태프 업체관리:

```text
GET    /api/v1/staff/partners
POST   /api/v1/staff/partners
GET    /api/v1/staff/partners/{id}
PATCH  /api/v1/staff/partners/{id}/allow-status
PATCH  /api/v1/staff/partners/{id}/status
PATCH  /api/v1/staff/partners/{id}/account-status
```

스태프 계정 초대관리:

```text
GET    /api/v1/staff/partners/{partnerId}/account-invitations
POST   /api/v1/staff/partners/{partnerId}/account-invitations
POST   /api/v1/staff/partners/{partnerId}/account-invitations/{id}/resend
DELETE /api/v1/staff/partners/{partnerId}/account-invitations/{id}
```

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
- 요일별 운영시간
- 대표 연락처
- 대표 이미지
- 사업자등록 필수정보와 증빙 파일
- 카테고리가 지정된 가격 옵션 최소 1개

부가정보, 외부 링크, 해시태그와 내부 이미지는 선택값이다. 가격을 확정할 수 없는 옵션은 `INQUIRE`를 사용한다.

## 11. MVP 제외 범위

- 결제 및 예약금
- 파트너 정산
- 쿠폰 및 포인트
- 광고 과금
- 외부 캘린더 자동 연동
- 네이버 플레이스 자동 연동
- 카카오톡 알림톡 연동
- 복잡한 좌석·장비·근무표 기반 예약 가능시간 계산

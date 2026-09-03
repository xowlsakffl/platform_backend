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

`AccountPartner`는 로그인 계정이고 `Partner`는 업체정보다. 둘은 `PartnerMembership`으로 연결하며 한 계정은 여러 업체를 운영할 수 있다. 파트너와 스태프는 이메일이 아닌 `login_id`로 로그인한다. 계정에는 담당자명이나 업체명을 중복 저장하지 않고, 업체 화면 표시명은 선택한 `Partner.name`을 사용한다.

### User

- 업체·전문가·이벤트 탐색
- 상담/예약 요청
- 이용한 전문가에 대한 후기 작성

## 3. 입점 흐름

직접 입점은 계정을 먼저 만든 뒤 업체를 등록하는 순서다. 계정 가입 단계에서는 업체명을 받지 않고 `AccountPartner`만 생성한다. 로그인 후 기본정보를 제출하면 `Partner`와 소유자 `PartnerMembership`을 한 트랜잭션으로 생성하며, 외부에 노출되는 최초 `Partner.allow_status`는 `REVIEW_REQUESTED`다.

```text
파트너 포털 (인증 불필요)
  |- 공지사항·FAQ 조회
  |- 파트너 가입 -> 로그인 -> 내 업체 목록에서 업체 선택 -> 업체 관리자
  `- 업체 신규 등록 -> 로그인 필요 -> 업체 등록
```

파트너 포털은 인증 없이 접근할 수 있는 공개 진입 화면이며 공지사항과 FAQ를 제공한다. 비로그인 상태의 내 업체 영역에는 로그인 안내를 표시하고, 내 업체 조회·업체 등록·업체 관리자 진입은 인증된 파트너만 사용할 수 있다. 로그인 후에도 등록 업체 수와 관계없이 포털을 먼저 표시한다. 별도의 업체 선택 전용 페이지는 두지 않는다. 업체 관리자에 진입한 뒤 계정에 연결된 업체가 2개 이상일 때만 헤더에 업체 전환 선택기를 표시하고, 1개인 경우에는 전환 UI를 표시하지 않는다.

파트너 포털 상단 배너는 여러 건을 순환하는 슬라이드로 제공한다. Staff가 배너 이미지, 제목, 설명, 연결 URL, 노출 시작·종료 시각, 노출상태와 정렬순서를 관리하며, 파트너 포털은 현재 노출기간에 해당하는 활성 배너만 정렬순서대로 조회한다. 배너가 한 건이면 자동 전환과 페이지 표시를 숨기고, 여러 건이면 자동 전환·이전·다음·페이지 표시를 제공한다. 배너 관리 권한과 변경 이력은 Staff 기준으로 기록한다.

업체별 API는 URL의 `partnerId`와 인증 계정의 활성 `PartnerMembership`을 매 요청마다 검증한다. 프론트에서 선택한 업체 ID를 권한 근거로 사용하지 않는다. 업체의 검수·운영상태, 정보, 옵션과 전문가는 다른 업체와 독립적으로 관리한다.

```text
DRAFT -> REVIEW_REQUESTED -> IN_REVIEW -> APPROVED
                                      -> REJECTED -> REVIEW_REQUESTED
```

- `DRAFT`: 최초 입점정보 작성 및 임시저장
- `REVIEW_REQUESTED`: 제출 완료, 검수 대기열에 등록
- `IN_REVIEW`: Staff가 검수를 시작한 상태
- `APPROVED`: 입점 승인
- `REJECTED`: 반려 사유 확인 후 수정·재신청 가능

파트너는 모든 검수상태에서 업체 정보를 수정할 수 있다. 운영시간, 이미지, 시술 옵션, 전문가처럼 검수 대상이 아닌 노출정보 수정은 검수상태에 영향을 주지 않는다. `IN_REVIEW` 또는 `APPROVED`에서 대표 업체명, 업종, 주소, 대표 연락처 또는 사업자정보를 변경하면 기존 검수 결과를 무효화하고 `REVIEW_REQUESTED`로 되돌린다.

스태프는 `REVIEW_REQUESTED`를 `IN_REVIEW`로 가져온 뒤 `APPROVED` 또는 `REJECTED`로 변경한다. 검수 시작 시 담당 Staff와 시작 시각을 저장하고, 반려 사유는 필수다. 별도의 입점신청 데이터와 메뉴를 만들지 않고 업체 목록에서 `allow_status`로 구분한다. `DRAFT`는 기본 목록에서 제외하고 명시적인 필터가 있을 때만 조회한다.

상태 역할은 분리한다.

- `Partner.allow_status`: 입점 검수상태
- `Partner.status`: 업체 운영상태
- `AccountPartner.status`: 파트너 계정상태

### 업체 소유 계정

업체는 가입된 파트너 계정이 직접 등록한다. Staff는 업체를 대신 생성하지 않으며, 모든 `Partner`는 생성 트랜잭션에서 등록 계정과 `PartnerMembership(OWNER, ACTIVE)`으로 연결된다. 따라서 업체에 계정 미연결 상태나 계정 생성 초대 흐름은 존재하지 않는다.

업체 하나에는 활성 OWNER가 정확히 하나 존재한다. 한 `AccountPartner`는 여러 업체의 OWNER가 될 수 있으며, 업체 관리자 API는 매 요청마다 활성 멤버십을 확인한다.

운영상 소유 계정을 바꿔야 할 때 Staff는 이미 가입된 활성 파트너 계정을 로그인 아이디로 조회해 소유권을 변경한다. 변경 트랜잭션에서 기존 OWNER 멤버십을 `INACTIVE`로 전환하고 대상 계정의 OWNER 멤버십을 `ACTIVE`로 생성하거나 재활성화한다. 업체 정보, 검수상태와 운영상태는 유지하며 변경 전후 로그인 아이디를 업체 운영 이력에 기록한다. 기존 계정이 소유한 다른 업체의 권한에는 영향을 주지 않는다.

### 업체 등록 신청

파트너 업체 등록은 다음 순서로 진행한다.

1. 대표 업체 카테고리 선택
2. 등록 방식 선택 후 사업자등록증 업로드
3. 한 화면에서 업체 기본정보와 사업자정보를 확인·수정한 뒤 검수 신청

업종을 선택하거나 사업자등록증 파일을 선택하면 별도 다음 버튼 없이 다음 화면으로 이동한다. 사업자등록증 OCR 분석은 상단 진행 단계에 포함하지 않고 파일 선택 직후 전용 로딩 화면으로 표시한다. 분석이 끝나면 상호, 사업자등록번호, 대표자명, 주소, 업태와 종목을 자동 입력한 3단계로 이동한다. 인식 실패 시에도 3단계로 이동해 직접 입력할 수 있다.

3단계 검수 신청이 완료되면 `REVIEW_REQUESTED` 상태의 업체와 `PartnerMembership(OWNER, ACTIVE)`를 생성한다. 업체 기본정보에는 대표 업체명, 대표 카테고리, 대표 전화번호, 대표 이메일과 주소를 포함하고, 사업자정보에는 등록한 사업자등록증, 상호, 사업자등록번호, 대표자명, 업태와 종목을 포함한다.

등록 방식 선택 화면에는 `사업자등록증으로 등록`과 `국세청 정보로 등록`을 함께 표시한다. 현재는 `사업자등록증으로 등록`만 선택할 수 있으며, `국세청 정보로 등록`은 비활성 상태와 `추후 예정` 안내만 노출한다. 보류된 방식의 국세청 API, 공공 마이데이터 동의, 추가 데이터 모델은 구현하지 않는다.

OCR은 입력 보조 기능이며 인식 실패가 입점 신청을 막지 않는다. 실패하거나 신뢰도가 낮은 항목은 파트너가 직접 입력·수정한다. 현재 어댑터는 CLOVA OCR의 사업자등록증 Document OCR을 사용하며, 공급자 교체가 가능하도록 `BusinessRegistrationOcrClient` 뒤에 격리한다.

OCR이 유효한 사업자등록번호를 인식하면 기본정보 화면으로 이동하기 전에 기존 업체 중복 여부를 검사한다. 중복 기준은 대표 업체명이나 상호가 아니라 정규화된 사업자등록번호다. 중복이면 등록을 중단하고 내 업체 목록 확인 또는 Staff를 통한 소유 계정 변경을 안내한다. OCR이 번호를 읽지 못한 경우에는 기본정보에서 직접 입력하게 하며, 최종 생성 트랜잭션의 중복 검사와 DB 유니크 키로 다시 차단한다.

검수 신청에는 기본정보와 사업자정보만 필요하다. 운영시간, 휴무일, 업체 이미지, 편의시설, 외부 링크, 시술 옵션과 전문가는 검수 중에도 추가하거나 수정할 수 있다.

## 4. 업체정보

### 기본정보

- 업체명(최대 30자), 업체 영문명(최대 90자)
- 상세 설명: 최대 2,000자
- 대표 업체 카테고리 하나
- 도로명 주소, 상세 주소
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

- 정기 휴무: 매주 또는 매월 첫째~넷째·마지막 주와 요일 조합
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

`PartnerOption`은 업체가 제공하는 시술의 원본이며, `SpecialistOption`은 해당 전문가의 제공 여부와 가격 오버라이드를 관리하는 연결 도메인이다. 전문가마다 옵션명과 설명을 복제하지 않는다.

```text
전문가 커스텀 정상가 없음:
  최종 정상가 = PartnerOption.regular_price
  최종 할인가 = PartnerOption.sale_price

전문가 커스텀 정상가 있음:
  최종 정상가 = SpecialistOption.regular_price_override
  최종 할인가 = SpecialistOption.sale_price_override

최종 적용 가격 = 최종 할인가 ?? 최종 정상가
```

전문가 커스텀 가격은 정상가를 필수로 입력하고 할인가를 선택값으로 둔다. 이를 통해 업체 기본 할인을 상속할지, 전문가가 별도 가격을 사용할지 명확하게 구분한다. 소요시간은 전문가별로 재정의하지 않고 업체 옵션의 소요시간을 공통으로 사용한다.

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

전문가 기본정보는 다음 원칙으로 관리한다.

- 전문가 분야는 한 명당 하나만 선택한다.
- `총 경력 시작일`은 날짜 하나로 관리하고 화면에서 현재까지의 경력을 계산한다.
- 프로필 이미지는 최대 3장이고 첫 번째 이미지를 대표 프로필로 사용한다.
- 별도의 자격번호·학력·경력·활동사항 텍스트 입력은 두지 않는다.
- 자격증·수료증 등은 `전문가 인증 이미지`로 최대 5장까지 관리하며 사용자 앱에는 공개하지 않는다.
- 정렬 순서는 등록·수정 단계에서 입력하지 않는다. 신규 전문가는 목록 마지막에 배치하고 목록의 별도 정렬 기능에서 순서를 변경한다.

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

### 6.2 내부 관리자 화면 배치

전문가 관리는 독립 메뉴로 운영하지 않고 업체 상세 안에서 해당 업체 소속 전문가만 관리한다. 업체 상세 메뉴 순서는 `기본정보 → 운영정보 → 시술·가격 → 전문가정보 → 사업자정보`로 구성한다. 목록은 프로필, 분야, 노출·검수 상태, 제공 옵션 수, 근무시간 방식과 경력을 한 번에 확인할 수 있는 카드형으로 표시한다.

전문가는 로그인 계정 액터가 아니다. 내부 관리자는 전문가 프로필, 제공 가능한 업체 옵션, 전문가별 가격·소요시간 오버라이드, 근무시간과 휴무를 관리한다.

전문가 시술·가격 화면은 업체 시술·가격과 같은 카테고리 탭과 옵션 표시 규칙을 사용한다. 옵션을 선택한 경우에만 전문가별 정상가·할인가와 소요시간을 입력할 수 있고, 소요시간은 시간·분 선택 방식으로 입력한다. 값을 비우면 업체 옵션의 가격과 소요시간을 상속한다.

Staff API는 업체 소유 범위를 URL에서 강제한다.

```text
GET    /api/v1/staff/partners/{partnerId}/specialists
GET    /api/v1/staff/partners/{partnerId}/specialists/{specialistId}
POST   /api/v1/staff/partners/{partnerId}/specialists
PATCH  /api/v1/staff/partners/{partnerId}/specialists/{specialistId}
DELETE /api/v1/staff/partners/{partnerId}/specialists/{specialistId}
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

파트너 계정과 내 업체:

```text
POST   /api/v1/partner/auth/signup
GET    /api/v1/partner/partners
POST   /api/v1/partner/partners
GET    /api/v1/partner/partners/{partnerId}/onboarding
PATCH  /api/v1/partner/partners/{partnerId}/onboarding
POST   /api/v1/partner/partners/{partnerId}/onboarding/submit
```

가격 옵션:

```text
GET    /api/v1/partner/partners/{partnerId}/options
POST   /api/v1/partner/partners/{partnerId}/options
PATCH  /api/v1/partner/partners/{partnerId}/options/{id}
DELETE /api/v1/partner/partners/{partnerId}/options/{id}
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
GET    /api/v1/staff/partners/{id}
PATCH  /api/v1/staff/partners/{id}/fields
PATCH  /api/v1/staff/partners/{id}/allow-status
PATCH  /api/v1/staff/partners/{id}/status
PATCH  /api/v1/staff/partners/{id}/account-status
GET    /api/v1/staff/partners/owner-account-options?q={loginId}
PATCH  /api/v1/staff/partners/{id}/owner-account
```

스태프 파트너 계정 관리:

```text
GET    /api/v1/staff/partner-accounts
GET    /api/v1/staff/partner-accounts/{id}
PATCH  /api/v1/staff/partner-accounts/{id}/status
POST   /api/v1/staff/partner-accounts/{id}/password-reset-link
GET    /api/v1/staff/partner-accounts/{id}/security
POST   /api/v1/staff/partner-accounts/{id}/login-lock/unlock
DELETE /api/v1/staff/partner-accounts/{id}/sessions/{sessionId}
DELETE /api/v1/staff/partner-accounts/{id}/sessions
GET    /api/v1/staff/partner-accounts/{id}/access-events?page={page}&per_page={perPage}
GET    /api/v1/staff/partner-accounts/{id}/management-histories?page={page}&per_page={perPage}
```

계정 상세는 `계정정보`, `관리업체`, `접속·보안`, `히스토리`로 구성한다. 비밀번호 재설정 링크 전송은 계정정보에서 수행한다. 접속이력은 인증 성공·실패 이벤트를 별도 보관하여 페이지네이션한다.

히스토리는 내부관리자의 계정 상태 변경·잠금 해제·개별 세션 종료와 파트너 본인의 업체정보 수정, 옵션 변경, 전문가 변경·순서 변경, 비밀번호 재설정 완료를 포함한다. 업체 변경은 대표키워드·링크·사업자정보의 개업일까지 전후값을 기록하며, 동일한 값 저장과 금액의 소수점 표기 차이만으로는 이력을 만들지 않는다. 대상과 작업자 각각의 조회 인덱스를 사용한다. 내부관리자의 전체 세션 일괄 종료 API는 제공하지 않는다.

Staff·Partner의 업체 수정과 전문가 수정은 업체 행 잠금을 공유한다. 업체 운영시간을 줄일 때 개별 근무시간이 범위를 벗어나는 전문가가 있으면 해당 전문가를 안내하고 저장을 거부한다. 업체 운영시간을 따르는 전문가는 변경된 시간을 그대로 사용한다. 옵션 목록은 양쪽 Actor 모두 일괄 저장 API로 검증·수정·삭제·이력 기록을 한 트랜잭션에서 처리한다.

## 10. 입점 제출 필수조건

- 업체명
- 상세 설명
- 업체 카테고리
- 도로명 주소
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

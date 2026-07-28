# 도메인 상태 정의서

- 작성 기준: 2026-07-27
- 기준 코드: `app/Domains/*/Models`, `database/migrations`, 주요 Staff/User Request
- 목적: 도메인별 상태 컬럼의 저장값, 화면 표시명, 의미를 현재 코드 기준으로 정리한다.

## 1. 상태 정의 원칙

상태값의 기준은 모델 상수다. 프론트는 상태값을 임의로 새로 만들지 않고, 백엔드 DTO에서 내려주는 label 또는 모델의 `statusLabel()` / `allowStatusLabel()` 계열 메서드를 기준으로 표시한다.

상태 컬럼의 의미는 컬럼명별로 구분한다.

| 컬럼/개념 | 의미 |
|---|---|
| `allow_status` | 검수/승인 상태. 운영자가 신청, 검수, 승인, 반려를 판단하는 상태 |
| `status` | 도메인별 운영/계정/노출 상태. 같은 `status`라도 도메인마다 의미가 다르므로 모델 기준으로 해석 |
| `hospital_status` | 병의원 관리자가 직접 설정하는 공개/비공개 상태 |
| `admin_status` | 뷰랩 Staff가 강제중지/정상 전환하는 상태 |
| `report_status` | 신고게시물의 처리 상태. 원본 게시물 상태와 분리 |
| `warning_status` | 신고 대상 작성자에 대한 경고/무시 처리 상태 |
| `ad_status` | 광고 상태. DB 저장 컬럼이 아니라 `allow_status + start_at/end_at`으로 계산 |

공통 검수 상태 색상 정책:

| 저장값 | 표시명 | 색상 기준 |
|---|---|---|
| `PENDING` | 신청 | 파랑 |
| `REVIEWING` | 검수 | 주황 |
| `APPROVED` | 승인 | 초록 |
| `REJECTED` | 반려 | 빨강 |

주의:

- `VALID`, `INVALID`는 현재 신고 상태가 아니다. 신규 코드와 신규 문서에서 사용하지 않는다.
- `HospitalVideo`는 현재 `allow_status`와 `status`를 쓰지 않는다. `hospital_status`, `admin_status`만 쓴다.
- `HospitalEventAd.ad_status`는 DB 컬럼이 아니다. 승인된 광고의 노출기간으로 계산한다.

## 2. 도메인 한눈에

| 분류 | 도메인 | 핵심 상태 |
|---|---|---|
| 계정 | `AccountStaff`, `AccountHospital`, `AccountBeauty`, `AccountUser` | 계정 `status` |
| 병의원 | `Hospital`, `HospitalDoctor`, `HospitalEntry`, `HospitalBusinessRegistration`, `HospitalFeature` | 검수 `allow_status`, 운영 `status` |
| 뷰티 | `Beauty`, `BeautyExpert`, `BeautyBusinessRegistration` | 검수 `allow_status`, 운영 `status` |
| 이벤트/광고/동영상 | `HospitalEvent`, `HospitalEventAd`, `HospitalVideo` | 검수, 공개여부, 강제중지, 광고상태 |
| 고객 DB | `HospitalEventDB`, `HospitalEventRealModelDB` | 상담/승인 상태 |
| 게시물 | `Talk`, `HospitalReview`, `HospitalEvaluation` 계열 | 노출 `status`, 평가 `post_status`, 영수증 `receipt_status` |
| 신고 | `ContentReport`, `ContentReportState` | `report_status`, `warning_status` |
| 공통 | `Category`, `CategoryUsage`, `Hashtag`, `Notice`, `Faq`, `Chat`, `Notification*` | 도메인별 상태/채널/타입 |

## 3. 계정 도메인

### 3.1 `AccountStaff`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | Staff 로그인/사용 가능 |
| `SUSPENDED` | 정지 | 일시 정지 |
| `BLOCKED` | 차단 | 차단 처리 |

기본값: `ACTIVE`

### 3.2 `AccountHospital`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 병원 계정 로그인/사용 가능 |
| `SUSPENDED` | 정지 | 일시 정지 |
| `BLOCKED` | 차단 | 차단 처리 |
| `WITHDRAWN` | 탈퇴 | 병원 계정 탈퇴/종료 |

기본값: `SUSPENDED`

관계 기준:

- `AccountHospital`은 `Hospital`과 1:1이다.
- `account_hospitals.hospital_id`는 unique다.

### 3.3 `AccountBeauty`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 뷰티 계정 로그인/사용 가능 |
| `SUSPENDED` | 정지 | 일시 정지 |
| `BLOCKED` | 차단 | 차단 처리 |

기본값: `SUSPENDED`

### 3.4 `AccountUser`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 정상 | 일반 회원 정상 사용 |
| `SUSPENDED` | 정지 | 일시 정지 |
| `BLOCKED` | 차단 | 신고 경고 누적 등으로 차단 |
| `WITHDRAWN` | 탈퇴 | 탈퇴 회원 |

기본값:

- `status`: `ACTIVE`
- `warning_count`: `0`
- `signup_channel`: `EMAIL`

가입 경로:

| 저장값 | 표시명 |
|---|---|
| `KAKAO` | 카카오톡 |
| `NAVER` | 네이버 |
| `EMAIL` | 이메일 |
| `APPLE` | 애플 |
| `FACEBOOK` | 페이스북 |
| `EMAIL_NO_CONTACT` | 이메일(연락처 x) |
| `UNKNOWN` | 미확인 |

신고 경고 규칙:

- 신고게시물 관리에서 `WARNED` 처리하면 작성자의 `warning_count`가 1 증가한다.
- `warning_count >= 10`이면 `status = BLOCKED`, `blocked_at = now()`로 바뀐다.
- `WARNED` 처리된 건을 `IGNORED`로 바꾸면 경고 수가 1 감소한다.
- 경고 수가 차단 기준 미만이 되면 차단 상태를 `ACTIVE`로 되돌린다.

## 4. 병의원 도메인

### 4.1 공통 검수 상태

다음 검수 상태는 `Hospital`, `HospitalDoctor`, `HospitalEntry`, `HospitalEvent`, `HospitalEventAd`가 사용한다.

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `PENDING` | 신청 | 검수 신청 접수 |
| `REVIEWING` | 검수 | 운영자가 검수 진행 중 |
| `APPROVED` | 승인 | 검수 통과 |
| `REJECTED` | 반려 | 검수 반려 |

### 4.2 `Hospital`

검수 상태: 공통 검수 상태 4종 사용

운영 상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 정상 | 병의원 정상 운영 |
| `SUSPENDED` | 운영중지 | 병의원 운영중지 |
| `WITHDRAWN` | 탈퇴 | 병의원 탈퇴/종료 |

기본값:

- `allow_status`: `PENDING`
- `status`: `ACTIVE`
- `department`: `OTHER`
- `view_count`, `evaluation_count`, `evaluation_average_rating`: `0`

진료과:

| 저장값 | 표시명 |
|---|---|
| `PLASTIC_SURGERY` | 성형외과 |
| `DERMATOLOGY` | 피부과 |
| `CLINIC` | 의원 |
| `DENTISTRY` | 치과 |
| `OPHTHALMOLOGY` | 안과 |
| `KOREAN_MEDICINE` | 한의원 |
| `OTHER` | 기타 |

### 4.3 `HospitalEntry`

입점신청은 검수 상태만 가진다. 운영 상태 `status`는 없다.

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `PENDING` | 신청 | 입점 신청 접수 |
| `REVIEWING` | 검수 | 운영자가 서류 검수 중 |
| `APPROVED` | 승인 | 입점 승인 |
| `REJECTED` | 반려 | 입점 반려 |

기본값: `PENDING`

파일 컬렉션:

| 컬렉션 | 의미 |
|---|---|
| `hospital_entry_business_registration_file` | 사업자등록증 |
| `hospital_entry_license_file` | 의사면허증 |

### 4.4 `HospitalDoctor`

검수 상태: 공통 검수 상태 4종 사용

운영 상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 정상 | 의료진 정상 노출/사용 |
| `SUSPENDED` | 정지 | 의료진 일시 정지 |
| `INACTIVE` | 비활성 | 의료진 비활성 |

기본값:

- `allow_status`: `PENDING`
- `status`: `SUSPENDED`
- `view_count`: `0`

전문의 필드는 `SPECIALIST_FIELD_*` 상수와 `SPECIALIST_FIELD_LABELS`를 기준으로 표시한다.

### 4.5 `HospitalBusinessRegistration`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 유효 | 현재 유효한 사업자등록 정보 |
| `EXPIRED` | 만료 | 유효기간 만료 |
| `REVOKED` | 취소/말소 | 등록 취소 또는 말소 |

기본값: `ACTIVE`

### 4.6 `HospitalFeature`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 선택 가능한 병의원 특징 |
| `INACTIVE` | 비활성 | 선택 불가 |

## 5. 뷰티 도메인

### 5.1 `Beauty`

검수 상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `PENDING` | 신청 | 검수 신청 접수 |
| `APPROVED` | 승인 | 검수 통과 |
| `REJECTED` | 반려 | 검수 반려 |

운영 상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 운영중 | 정상 운영 |
| `SUSPENDED` | 운영중지 | 운영 일시 중단 |
| `WITHDRAWN` | 탈퇴/종료 | 운영 종료 |

기본값:

- `allow_status`: `PENDING`
- `status`: `SUSPENDED`

현재 `Beauty`는 `REVIEWING` 단계를 갖지 않는다.

### 5.2 `BeautyExpert`

검수 상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `PENDING` | 신청 | 검수 신청 접수 |
| `APPROVED` | 승인 | 검수 통과 |
| `REJECTED` | 반려 | 검수 반려 |

운영 상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 정상 노출/사용 |
| `SUSPENDED` | 정지 | 일시 정지 |
| `INACTIVE` | 비활성 | 비활성 |

기본값:

- `allow_status`: `PENDING`
- `status`: `SUSPENDED`

현재 `BeautyExpert`는 `REVIEWING` 단계를 갖지 않는다.

### 5.3 `BeautyBusinessRegistration`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 유효 | 현재 유효한 사업자등록 정보 |
| `EXPIRED` | 만료 | 유효기간 만료 |
| `REVOKED` | 취소/말소 | 등록 취소 또는 말소 |

기본값: `ACTIVE`

## 6. 이벤트 / 광고 / 동영상

### 6.1 `HospitalEvent`

이벤트 타입:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `TEXT` | 텍스트 | 텍스트 기반 이벤트 |
| `IMAGE` | 이미지 | 이미지 기반 이벤트 |

검수 상태: 공통 검수 상태 4종 사용

공개여부 (`hospital_status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `PUBLIC` | 공개 | 병원 관리자가 공개 |
| `PRIVATE` | 비공개 | 병원 관리자가 비공개 |

강제중지 (`admin_status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `NORMAL` | 정상 | Staff 강제중지 없음 |
| `FORCED_STOPPED` | 강제중지 | Staff가 강제중지 |

기본값:

- `event_type`: `IMAGE`
- `allow_status`: `PENDING`
- `hospital_status`: `PUBLIC`
- `admin_status`: `NORMAL`
- `view_count`: `0`

신청 가능 조건(`isApplicationOpen()`):

- `allow_status = APPROVED`
- `hospital_status = PUBLIC`
- `admin_status = NORMAL`
- 삭제되지 않음
- 이벤트 기간이 시작 전이 아니고, 종료되지 않음

### 6.2 `HospitalEventAd`

광고 그룹 key:

| 저장값 | 표시명 |
|---|---|
| `main` | 메인 |
| `surgery` | 성형이벤트 |
| `petit` | 쁘띠이벤트 |
| `etc` | 기타 |

광고 위치:

| 그룹 | 저장값 | 표시명 | 카테고리 필요 |
|---|---|---|---|
| 메인 | `MAIN_POPUP` | 메인 팝업 | 아니오 |
| 메인 | `MAIN_VERTICAL_BANNER` | 메인 세로배너 | 아니오 |
| 메인 | `MAIN_HORIZONTAL_BANNER` | 메인 가로배너 | 아니오 |
| 성형이벤트 | `SURGERY_TOP_BANNER` | 성형 상단배너 | 아니오 |
| 성형이벤트 | `SURGERY_HOT_EVENT` | 성형 HOT이벤트 | 아니오 |
| 성형이벤트 | `SURGERY_CATEGORY_BANNER` | 성형 카테고리별 배너 | 예 |
| 쁘띠이벤트 | `PETIT_TOP_BANNER` | 쁘띠 상단배너 | 아니오 |
| 쁘띠이벤트 | `PETIT_HOT_EVENT` | 쁘띠 HOT이벤트 | 아니오 |
| 쁘띠이벤트 | `PETIT_CATEGORY_BANNER` | 쁘띠 카테고리별 배너 | 예 |
| 기타 | `CONSULT_MEMO` | 상담메모장 | 아니오 |
| 기타 | `SEARCH` | 검색창 | 아니오 |

검수 상태: 공통 검수 상태 4종 사용

광고 상태(`ad_status`)는 저장하지 않고 계산한다.

| 계산값 | 표시명 | 계산 기준 |
|---|---|---|
| `SCHEDULED` | 광고예정 | `allow_status = APPROVED`이고 `start_at > now()` |
| `RUNNING` | 광고중 | `allow_status = APPROVED`이고 현재 시각이 광고기간 안 |
| `ENDED` | 광고종료 | `allow_status = APPROVED`이고 `end_at < now()` |
| `null` | - | 아직 승인되지 않은 광고 |

기본값:

- `allow_status`: `PENDING`
- `cost`: `0`
- 주차별 구좌 제한: `WEEKLY_SLOT_LIMIT = 3`

요일 정책:

- 쁘띠 광고 3종은 목요일 시작
- 그 외 광고는 화요일 시작

### 6.3 `HospitalVideo`

동영상은 검수 상태를 쓰지 않는다.

공개여부 (`hospital_status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `PUBLIC` | 공개 | 병원 관리자가 공개 |
| `PRIVATE` | 미공개 | 병원 관리자가 미공개 |

강제중지 (`admin_status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `NORMAL` | 정상 | Staff 강제중지 없음 |
| `FORCED_STOPPED` | 강제중지 | Staff가 강제중지 |

기본값:

- `hospital_status`: `PUBLIC`
- `admin_status`: `NORMAL`
- `duration_seconds`, `view_count`, `like_count`: `0`

노출 가능 조건(`isVisible()`):

- `hospital_status = PUBLIC`
- `admin_status = NORMAL`
- 삭제되지 않음

summary 필터:

| 저장값 | 의미 |
|---|---|
| `normal` | 정상노출 동영상 |
| `limited` | 노출제한 동영상 |
| `reported` | 신고접수 동영상 |

## 7. 고객 DB

### 7.1 `HospitalEventDB`

상담 여부 상태(`status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `NEW` | 신규 | 신규 신청 |
| `CONFIRMED` | 확인 | 확인 완료 |
| `DUPLICATE` | 중복 | 중복 신청 |

검증 상태(`allow_status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `UNVERIFIED_REPORTED` | 미인증DB 신고 | 미인증 DB로 신고됨 |
| `UNVERIFIED_CONFIRMED` | 미인증DB 확정 | 미인증 DB로 확정 |
| `NORMAL_CONFIRMED` | 정상DB 확정 | 정상 DB로 확정 |

기본값:

- `status`: `NEW`
- `allow_status`: `NORMAL_CONFIRMED`
- `contact_method`: `PHONE`
- `preferred_time`: `ANYTIME`

연락 방법:

| 저장값 | 표시명 |
|---|---|
| `KAKAO` | 카카오톡 |
| `PHONE` | 전화 |
| `SMS` | 문자 |

희망 시간:

| 저장값 | 표시명 |
|---|---|
| `MORNING` | 오전 |
| `AFTERNOON` | 오후 |
| `ANYTIME` | 상시 |

### 7.2 `HospitalEventRealModelDB`

승인상태(`status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `RECEIVED` | 신청 | 리얼모델 신청 접수 |
| `APPROVED` | 승인 | 승인 |
| `REJECTED` | 불가 | 불가/반려 |

기본값: `RECEIVED`

리얼모델 이미지는 필수 정책이며, 컬렉션은 `real_model_db_images`다.

## 8. 게시물 / 후기 / 평가

### 8.1 `Talk` / `TalkComment`

노출 상태(`status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 노출 | 서비스 노출 |
| `INACTIVE` | 미노출 | 운영자 미노출 |

신고 상태는 원본 `status`가 아니라 `ContentReportState.report_status`로 관리한다.

### 8.2 `HospitalReview` / `HospitalReviewComment`

후기 카테고리 도메인:

| 저장값 | 의미 |
|---|---|
| `HOSPITAL_REVIEW_SURGERY` | 성형후기 |
| `HOSPITAL_REVIEW_TREATMENT` | 시술후기 |

노출 상태(`status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 노출 | 서비스 노출 |
| `INACTIVE` | 미노출 | 운영자 미노출 |

신고 상태는 원본 `status`가 아니라 `ContentReportState.report_status`로 관리한다.

### 8.3 `HospitalEvaluation`

평가 카테고리:

| 저장값 | 의미 |
|---|---|
| `HOSPITAL_EVALUATION_SURGERY` | 성형 평가 |
| `HOSPITAL_EVALUATION_TREATMENT` | 시술 평가 |
| `HOSPITAL_EVALUATION_CONSULTATION` | 상담 평가 |

노출 상태(`status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 노출 | 서비스 노출 |
| `INACTIVE` | 미노출 | 운영자 미노출 |

게시 상태(`post_status`)는 기존 평가용 상태다.

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `POST_NORMAL` | 정상 | 정상 게시 |
| `POST_AUTO_BLIND` | 자동차단 | 기존 평가 게시상태 기준 자동차단 |
| `POST_USER_DELETE` | 본인삭제 | 작성자가 삭제 |
| `POST_ADMIN_STOP` | 노출중지 | 운영자가 게시 중지 |

영수증 상태(`receipt_status`):

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `NONE` | 없음 | 영수증 이미지 없음 |
| `UPLOADED` | 영수증 | 사용자가 영수증 이미지 업로드 |
| `VERIFIED` | 영수증 인증 | 운영자가 인증 적합 처리 |
| `REJECTED` | 영수증 부적합 | 운영자가 부적합 처리 |

영수증 부적합 사유:

| 저장값 | 표시명 |
|---|---|
| `IMAGE_MISMATCH` | 영수증 이미지 불일치 |
| `BUSINESS_NAME_MISMATCH` | 상호 불일치 |
| `BUSINESS_NUMBER_MISMATCH` | 사업자번호 불일치 |
| `TRANSACTION_DATE_MISMATCH` | 거래일시 불일치 |
| `SURGERY_COST_MISMATCH` | 수술금액 불일치 |
| `OTHER` | 기타 |

평균 평점:

- 직원친절도, 수술만족도, 병원시설, 사후관리, 비용 5개 항목의 산술 평균이다.
- 저장/갱신 시 소수점 1자리로 계산한다.
- 병원 집계에는 `status = ACTIVE`이고 `post_status = POST_NORMAL`인 평가만 포함한다.

## 9. 신고 도메인

상세 정책은 `content-report.md`를 기준으로 한다.

### 9.1 `ContentReport.reason`

공통 신고 사유:

| 저장값 | 표시명 |
|---|---|
| `ABUSE` | 비방/욕설 |
| `SPAM` | 게시물/댓글 도배 |
| `ILLEGAL_AD` | 불법광고/홍보 |
| `PRIVACY_COPYRIGHT` | 개인정보/저작권 침해 |
| `OTHER` | 기타 |

동영상 신고 사유:

| 저장값 | 표시명 |
|---|---|
| `DELETED_VIDEO` | 삭제된 동영상 |
| `ABUSE` | 비방/욕설 |
| `ILLEGAL_AD` | 불법광고/홍보 |

### 9.2 `ContentReportState.report_status`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `NONE` | 없음 | 신고 접수 전 기본값 |
| `REPORTED` | 신고접수 | 신고 접수 상태 |
| `AUTO_BLOCKED` | 자동차단 | 1시간 내 신고 누적으로 자동 미노출 |
| `ADMIN_HIDDEN` | 노출중지 | 운영자가 신고 인정/노출중지 처리 |
| `NORMAL_VISIBLE` | 정상노출 | 운영자가 정상노출 처리 |
| `REEXPOSED` | 재노출 | 정상노출 처리 3회차부터 자동 전이 잠금 |

동영상 신고 화면 표시:

| 내부 상태 | 동영상 표시 |
|---|---|
| `REPORTED`, `AUTO_BLOCKED` | 신고접수 |
| `ADMIN_HIDDEN` | 삭제처리 |
| `NORMAL_VISIBLE`, `REEXPOSED` | 신고오류 |

### 9.3 `ContentReportState.warning_status`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `NONE` | 미처리 | 경고/무시 미처리 |
| `WARNED` | 경고 | 작성자 경고 반영 |
| `IGNORED` | 무시 | 작성자 경고 없이 처리 |

## 10. 카테고리 / 해시태그

### 10.1 `Category`

도메인:

| 저장값 | 의미 |
|---|---|
| `HOSPITAL_MEDICAL` | 병의원/의료진/이벤트/동영상/광고 의료 카테고리 |
| `HOSPITAL_EVALUATION` | 병의원 평가 카테고리 |
| `TALK` | 토크 카테고리 |
| `BEAUTY` | 뷰티 카테고리 |
| `FAQ` | FAQ 카테고리 |

`HOSPITAL_MEDICAL`은 `group_code`로 성형/쁘띠를 구분한다.

| 저장값 | 표시명 |
|---|---|
| `SURGERY` | 성형 |
| `TREATMENT` | 쁘띠 |

운영 상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 선택/노출 가능 |
| `INACTIVE` | 비활성 | 선택/노출 불가 |

### 10.2 `CategoryUsage`

사용처:

| 저장값 | 의미 |
|---|---|
| `HOSPITAL_DOCTOR_SUBJECT` | 병의원/의료진 진료과목 |
| `HOSPITAL_REVIEW_SURGERY` | 성형후기 |
| `HOSPITAL_REVIEW_TREATMENT` | 시술후기 |
| `HOSPITAL_EVENT_SURGERY` | 성형 이벤트 |
| `HOSPITAL_EVENT_TREATMENT` | 쁘띠 이벤트 |
| `HOSPITAL_VIDEO_CATEGORY` | 동영상 카테고리 |
| `HOSPITAL_EVENT_AD_SURGERY` | 성형 광고 카테고리 |
| `HOSPITAL_EVENT_AD_TREATMENT` | 쁘띠 광고 카테고리 |

상태:

| 저장값 | 표시명 |
|---|---|
| `ACTIVE` | 활성 |
| `INACTIVE` | 비활성 |

### 10.3 `Hashtag`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 사용 가능 |
| `INACTIVE` | 비활성 | 사용 불가 |

주의:

- 과거 `BLOCKED` 값은 `INACTIVE`로 정규화한다.
- 해시태그 이름은 최대 20자이며 한글/영문/숫자/언더스코어만 허용한다.

## 11. 공지 / FAQ

### 11.1 `Notice`

채널:

| 저장값 | 의미 |
|---|---|
| `ALL` | 전체 |
| `APP_WEB` | 앱/웹 |
| `HOSPITAL` | 병원 |
| `BEAUTY` | 뷰티 |

상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 게시 가능 |
| `INACTIVE` | 비활성 | 게시 불가 |

기본값:

- `channel`: `ALL`
- `status`: `ACTIVE`
- `is_publish_period_unlimited`: `true`
- `is_pinned`, `is_important`: `false`

### 11.2 `Faq`

채널:

| 저장값 | 의미 |
|---|---|
| `ALL` | 전체 |
| `APP_WEB` | 앱/웹 |
| `HOSPITAL` | 병원 |
| `BEAUTY` | 뷰티 |

상태:

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 노출 가능 |
| `INACTIVE` | 비활성 | 노출 불가 |

기본값:

- `channel`: `ALL`
- `status`: `ACTIVE`
- `sort_order`, `view_count`: `0`

## 12. 채팅

### 12.1 `Chat`

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `ACTIVE` | 활성 | 정상 채팅방 |
| `SUSPENDED` | 정지 | 일시 정지 |
| `CLOSED` | 종료 | 종료된 채팅방 |

### 12.2 `ChatMessage`

메시지 타입:

| 저장값 | 의미 |
|---|---|
| `TEXT` | 텍스트 메시지 |
| `IMAGE` | 이미지 메시지 |
| `FILE` | 파일 메시지 |

`ChatMessage` 자체에는 신고 처리용 `status`가 없다. 채팅 신고 상태는 `ContentReportState`로 관리한다.

## 13. 알림

### 13.1 `NotificationInbox`

현재 상수 기준:

| 구분 | 저장값 | 의미 |
|---|---|---|
| recipient | `USER` | 일반 사용자 수신자 |
| actor | `USER` | 일반 사용자 행위자 |
| event | `chat.message.created` | 채팅 메시지 생성 |
| target | `chat` | 채팅방 대상 |

읽음 여부는 별도 enum이 아니라 `read_at` 존재 여부로 판단한다.

### 13.2 `NotificationDelivery`

채널:

| 저장값 | 의미 |
|---|---|
| `IN_APP` | 인앱 |
| `PUSH` | 푸시 |
| `EMAIL` | 이메일 |
| `WEB` | 웹 |

발송 상태:

| 저장값 | 의미 |
|---|---|
| `PENDING` | 발송 대기 |
| `SENT` | 발송 성공 |
| `FAILED` | 발송 실패 |

Provider:

| 저장값 | 의미 |
|---|---|
| `REVERB` | Reverb |
| `FCM` | Firebase Cloud Messaging |
| `APNS` | Apple Push Notification Service |
| `MIXED` | 복합 provider |

### 13.3 `NotificationDevice`

| 저장값 | 의미 |
|---|---|
| `IOS` | iOS |
| `ANDROID` | Android |
| `WEB` | Web |

## 14. 공통 미디어 / 메모 / 히스토리

### 14.1 `Media`

별도 상태 enum은 없다.

| 필드 | 의미 |
|---|---|
| `collection` | 도메인별 파일 용도 |
| `is_primary` | 대표 파일 여부 |
| `sort_order` | 정렬 순서 |
| `deleted_at` | 소프트 삭제 여부 |

### 14.2 `AdminNote`

별도 상태 enum은 없다. 대상 도메인별 메모를 폴리모픽으로 저장한다.

### 14.3 `OperationHistory`

Action:

| 저장값 | 의미 |
|---|---|
| `CREATED` | 생성 |
| `UPDATED` | 일반 수정 |
| `STATE_UPDATED` | 상태 변경 |
| `DELETED` | 삭제 |

Actor kind:

| 저장값 | 의미 |
|---|---|
| `STAFF` | Staff 관리자 |
| `HOSPITAL` | 병원 계정 |
| `BEAUTY` | 뷰티 계정 |
| `USER` | 일반 사용자 |
| `SYSTEM` | 시스템 |
| `UNKNOWN` | 알 수 없음 |

상태 변경은 신규 코드에서 `ACTION_STATE_UPDATED`를 사용한다. `status`, `allow_status`, `admin_status`, `report_status`, `warning_status` 같은 상태성 필드의 변경은 운영 히스토리 changes에 field 단위로 남긴다.

## 15. 상태 흐름 요약

병의원/의료진/입점신청/이벤트/광고 검수:

```text
PENDING(신청) -> REVIEWING(검수) -> APPROVED(승인)
                                  -> REJECTED(반려)
```

뷰티/뷰티전문가 검수:

```text
PENDING(신청) -> APPROVED(승인)
              -> REJECTED(반려)
```

병의원 운영:

```text
ACTIVE(정상) <-> SUSPENDED(운영중지)
ACTIVE/SUSPENDED -> WITHDRAWN(탈퇴)
```

이벤트/동영상 노출:

```text
hospital_status: PUBLIC(공개) <-> PRIVATE(비공개/미공개)
admin_status: NORMAL(정상) <-> FORCED_STOPPED(강제중지)
```

광고 상태:

```text
allow_status != APPROVED -> ad_status 없음
allow_status = APPROVED and start_at > now -> SCHEDULED(광고예정)
allow_status = APPROVED and start_at <= now <= end_at -> RUNNING(광고중)
allow_status = APPROVED and end_at < now -> ENDED(광고종료)
```

신고 처리:

```text
NONE -> REPORTED
REPORTED/AUTO_BLOCKED -> ADMIN_HIDDEN(노출중지)
REPORTED/AUTO_BLOCKED -> NORMAL_VISIBLE(정상노출)
NORMAL_VISIBLE 3회차 이상 -> REEXPOSED
```

## 16. 코드 정리 기준

신규 도메인 상태를 추가할 때는 다음을 같이 처리한다.

- 모델 상수 추가
- `statuses()` 또는 `allowStatuses()` 추가
- 화면 표시가 필요한 경우 `statusLabel()` 또는 `allowStatusLabel()` 추가
- Request `Rule::in()` 갱신
- DTO label 필드 갱신
- 프론트 옵션/뱃지 색상 갱신
- 운영 히스토리 field label 갱신
- summary/filter/cache가 있으면 필터 기준 갱신

기존 코드 정리 대상:

- `Beauty`, `BeautyExpert`는 병의원 계열과 달리 `allowStatusLabel()` 메서드가 없다. 뷰티 관리자 화면을 본격 구현할 때 같은 패턴으로 맞춘다.
- `AccountStaff`, `AccountHospital`, `AccountBeauty`는 `statusLabel()` 메서드가 없다. 계정 상태 표시가 반복되면 모델 기준 라벨 메서드로 정리한다.
- `schema.dbml`은 일부 과거 상태값이 남아 있을 수 있으므로 DB 문서 정리 턴에서 별도로 갱신한다.

## 17. 참고 파일

- `app/Domains/*/Models/*.php`
- `database/migrations/0001_01_01_000000_create_account_users_table.php`
- `database/migrations/0001_01_01_000003_create_account_staffs_table.php`
- `database/migrations/0001_01_01_000004_create_hospitals_table.php`
- `database/migrations/2026_02_24_152827_create_hospital_doctors_table.php`
- `database/migrations/2026_02_25_120100_create_hospital_videos_table.php`
- `database/migrations/2026_06_11_090000_create_hospital_events_table.php`
- `database/migrations/2026_06_22_090000_create_hospital_entries_table.php`
- `database/migrations/2026_07_13_090000_create_hospital_event_ads_table.php`
- `./content-report.md`
- `./category.md`

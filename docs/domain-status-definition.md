# 도메인 정책과 상태

이 문서는 현재 이식된 Account, Partner, PartnerFeature, Category, Specialist, Media, OperationHistory의 정책만 정의한다.

## Partner

### 상태

| 필드 | 값 | 의미 |
|---|---|---|
| `allow_status` | `PENDING` | 신청 |
|  | `APPROVED` | 승인 |
|  | `REJECTED` | 반려 |
| `status` | `ACTIVE` | 정상 운영 |
|  | `SUSPENDED` | 운영 중지 |
|  | `WITHDRAWN` | 탈퇴 |

승인 중간 상태는 사용하지 않는다. Staff가 승인상태나 운영상태를 바꾸면 변경 전후 값과 사유를 운영 이력에 남긴다.

`WITHDRAWN`은 최종 상태다. 탈퇴 처리된 파트너은 운영상태를 정상이나 운영중지로 되돌릴 수 없다.

파트너 관리자 계정은 `ACTIVE`, `BLOCKED`만 사용하고 화면에서는 각각 `로그인 가능`, `로그인 차단`으로 표시한다. 파트너의 운영 중지와 탈퇴는 파트너 `status`로 관리하고 계정 상태에 중복 저장하지 않는다. 파트너 관리자 계정을 `BLOCKED`로 바꾸거나 파트너이 `WITHDRAWN` 또는 soft delete되면 기존 인증 세션을 폐기한다. 직원 관리자, 뷰티 관리자, 일반 사용자 계정은 `ACTIVE`, `SUSPENDED`, `BLOCKED`, `WITHDRAWN`을 모두 사용한다.

상태 변경은 기본정보 수정 권한과 분리한다. 파트너 관리자 로그인 상태는 `platform.partner.account_status.update`, 승인상태는 `platform.partner.allow_status.update`, 운영상태는 `platform.partner.status.update` 권한이 필요하다.

### 연락처

| 종류 | 최대 개수 |
|---|---:|
| 대표 번호 | 1, 필수 |
| SMS 발신 번호 | 1 |
| 전화 수신 번호 | 1 |
| 상담 수신 번호 | 3 |
| 이벤트·안내 수신 번호 | 3 |
| 공지·마케팅 수신 이메일 | 3 |

연락처는 `partner_contacts`에 종류와 정렬 순서로 저장한다. 고정 번호 컬럼을 파트너 테이블에 추가하지 않는다.

### 특징

파트너 특징은 활성 기준값만 연결한다.

1. 주차 가능
2. 발렛 가능
3. 역에서 도보 5분 이내
4. 예약제
5. 프라이빗룸
6. 와이파이
7. 개인 락커
8. 샤워실
9. 야간 운영
10. 주말 운영
11. 시술 후 관리
12. 여성 스페셜리스트

### 삭제

Partner API 삭제는 soft delete다. 연결된 Specialist도 soft delete하고, Partner·Specialist·사업자등록증 미디어와 카테고리 할당을 함께 정리한다. DB 외래 키는 실제 hard delete 상황에서 Specialist를 `ON DELETE CASCADE`한다.

## Specialist

### 상태

| 필드 | 값 | 의미 |
|---|---|---|
| `allow_status` | `PENDING` | 신청 |
|  | `APPROVED` | 승인 |
|  | `REJECTED` | 반려 |
| `status` | `VISIBLE` | 노출 |
|  | `HIDDEN` | 미노출 |

Partner Actor가 신규 등록하면 검수 상태는 `PENDING`이다. Partner Actor는 노출 상태만 바꿀 수 있고 검수 상태를 승인할 수 없다. 반려 처리에는 사유가 필요하다.

### 핵심 규칙

- Specialist는 반드시 하나의 Partner에 속한다.
- `license_number`는 선택값이다. 값이 있으면 숫자로 정규화하며 서비스 전체에서 unique다.
- 스페셜리스트 분야는 필수이고 코드 목록은 `SpecialistField` enum이 기준이다.
- 시술 분야 Category는 최대 5개며 첫 항목을 primary로 저장한다.
- 학력·경력·활동사항은 각 20개, 항목당 1,000자까지 JSON 배열로 저장한다.
- soft delete 시 상태를 `HIDDEN`으로 바꾸고 연결 미디어와 Category 할당을 정리한다.

### 미디어 공개 범위

| 컬렉션 | Staff | 소속 Partner | User 앱 공개 |
|---|---:|---:|---:|
| `profile_image` | O | O | 승인+노출일 때 O |
| `license_image` | O | O | X |
| `specialist_certificate_image` | O | O | X |

## Category

- 도메인: `PARTNER_EVALUATION`, `TALK`, `PARTNER`, `FAQ`
- 상태: `ACTIVE`, `INACTIVE`
- 파트너 카테고리는 `PARTNER` domain의 1뎁스 업체 분류다.
- 파트너·스페셜리스트는 1뎁스, 이벤트·후기는 3뎁스 카테고리를 복수 선택한다.
- 파트너에 연결한 1뎁스 카테고리는 화면에서 `파트너 분류`로 부르며 대표 분류를 지정하지 않는다.
- 같은 그룹의 루트와 같은 부모 아래 이름, 도메인 내 code는 중복할 수 없다.
- 하위 Category는 상위 Category의 group code를 상속한다.
- 이름이나 group이 바뀌면 모든 하위 `full_path`와 group을 동기화한다.
- 하위 또는 할당이 남은 Category는 삭제할 수 없다.

현재 업체 분류 기준 데이터는 `반영구`, `에스테틱`, `미용실`, `왁싱`, `타투`, `네일아트`, `마사지`, `속눈썹`, `메이크업`, `두피케어` 10개다. 각 업체 분류 아래 2뎁스 옵션 카테고리를 두고 `PartnerOption`에 연결한다.

## Media

Media는 `owner_type + owner_id + collection`으로 연결한다. DB 외래 키를 둘 수 없는 폴리모픽 구조이므로 application Service가 소유자 존재, 권한, 컬렉션을 검증한다.

교체·삭제된 메타데이터는 soft delete하고 파일은 트랜잭션 커밋 뒤 삭제한다. 신규 업로드 뒤 트랜잭션이 롤백되면 신규 파일을 삭제한다.

## 미이식 집계

Partner 목록의 이벤트 수·후기 수는 관련 도메인이 완성되기 전까지 응답과 화면에 포함하지 않는다. Partner 상세의 신규 이벤트 DB 수와 Specialist 목록의 후기 수처럼 아직 이식되지 않은 집계도 실제 데이터 연결 전에는 운영 지표로 사용하지 않는다. 집계값 필터나 정렬은 관련 도메인과 목록용 집계를 완성한 뒤 제공한다.

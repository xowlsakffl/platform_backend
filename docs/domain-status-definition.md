# 도메인 정책과 상태

이 문서는 현재 이식된 Account, Hospital, HospitalFeature, Category, Doctor, Media, OperationHistory의 정책만 정의한다.

## Hospital

### 상태

| 필드 | 값 | 의미 |
|---|---|---|
| `allow_status` | `PENDING` | 신청 |
|  | `APPROVED` | 승인 |
|  | `REJECTED` | 반려 |
| `status` | `ACTIVE` | 정상 운영 |
|  | `SUSPENDED` | 운영 중지 |
|  | `WITHDRAWN` | 탈퇴 |

검수 중간 상태는 사용하지 않는다. Staff가 검수 상태나 운영 상태를 바꾸면 변경 전후 값과 사유를 운영 이력에 남긴다.

### 연락처

| 종류 | 최대 개수 |
|---|---:|
| 대표 번호 | 1, 필수 |
| SMS 발신 번호 | 1 |
| 전화 수신 번호 | 1 |
| 상담 수신 번호 | 3 |
| 이벤트·안내 수신 번호 | 3 |
| 공지·마케팅 수신 이메일 | 3 |

연락처는 `hospital_contacts`에 종류와 정렬 순서로 저장한다. 고정 번호 컬럼을 병원 테이블에 추가하지 않는다.

### 특징과 통역 언어

병원 특징은 활성 기준값만 연결한다.

1. 마취과 전문의
2. 입원 시설
3. 수술실명제
4. 야간상담/진료
5. 응급 대응 체계
6. 분야별 공동 진료
7. 전용 휴식 공간
8. 시술 후 관리
9. 여성 의사 진료
10. 역에서 도보 5분 이내
11. 성형외과 전문의 진료
12. 주차가능

통역 가능 언어는 `JAPANESE`, `ENGLISH`, `THAI`, `CHINESE`, `TAIWANESE_CHINESE`를 다중 선택한다. 병원 소개 자체의 다국어 필드는 두지 않는다.

### 삭제

Hospital API 삭제는 soft delete다. 연결된 Doctor도 soft delete하고, Hospital·Doctor·사업자등록증 미디어와 카테고리 할당을 함께 정리한다. DB 외래 키는 실제 hard delete 상황에서 Doctor를 `ON DELETE CASCADE`한다.

## Doctor

### 상태

| 필드 | 값 | 의미 |
|---|---|---|
| `allow_status` | `PENDING` | 신청 |
|  | `APPROVED` | 승인 |
|  | `REJECTED` | 반려 |
| `status` | `VISIBLE` | 노출 |
|  | `HIDDEN` | 미노출 |

Hospital Actor가 신규 등록하면 검수 상태는 `PENDING`이다. Hospital Actor는 노출 상태만 바꿀 수 있고 검수 상태를 승인할 수 없다. 반려 처리에는 사유가 필요하다.

### 핵심 규칙

- Doctor는 반드시 하나의 Hospital에 속한다.
- `license_number`는 숫자로 정규화하며 서비스 전체에서 unique다.
- 전문의 분류는 필수이고 코드 목록은 `DoctorSpecialistField` enum이 기준이다.
- 진료 분야 Category는 최대 5개며 첫 항목을 primary로 저장한다.
- 학력·경력·활동사항은 각 20개, 항목당 1,000자까지 JSON 배열로 저장한다.
- soft delete 시 상태를 `HIDDEN`으로 바꾸고 연결 미디어와 Category 할당을 정리한다.

### 미디어 공개 범위

| 컬렉션 | Staff | 소속 Hospital | User 앱 공개 |
|---|---:|---:|---:|
| `profile_image` | O | O | 승인+노출일 때 O |
| `license_image` | O | O | X |
| `specialist_certificate_image` | O | O | X |

## Category

- 도메인: `MEDICAL`, `HOSPITAL_EVALUATION`, `TALK`, `BEAUTY`, `FAQ`
- 상태: `ACTIVE`, `INACTIVE`
- 의료 카테고리는 최대 3단계다.
- 의료 카테고리는 `SURGERY(성형)`, `TREATMENT(시술)` 그룹으로 구분한다.
- 병원·의사는 1뎁스, 이벤트·후기는 3뎁스 카테고리를 복수 선택한다.
- 같은 그룹의 루트와 같은 부모 아래 이름, 도메인 내 code는 중복할 수 없다.
- 하위 Category는 상위 Category의 group code를 상속한다.
- 이름이나 group이 바뀌면 모든 하위 `full_path`와 group을 동기화한다.
- 하위 또는 할당이 남은 Category는 삭제할 수 없다.

현재 기준 데이터는 병원·의사·이벤트·후기가 공유하는 `MEDICAL` 트리 498개다. 사용처별 매핑 테이블은 두지 않는다. 다른 CategoryDomain enum은 확장 식별자이며 모든 도메인 데이터가 seed됐다는 뜻은 아니다.

## Media

Media는 `owner_type + owner_id + collection`으로 연결한다. DB 외래 키를 둘 수 없는 폴리모픽 구조이므로 application Service가 소유자 존재, 권한, 컬렉션을 검증한다.

교체·삭제된 메타데이터는 soft delete하고 파일은 트랜잭션 커밋 뒤 삭제한다. 신규 업로드 뒤 트랜잭션이 롤백되면 신규 파일을 삭제한다.

## 미이식 집계

Hospital 목록의 이벤트 수·후기 수, Hospital 상세의 신규 이벤트 DB 수, Doctor 목록의 후기 수는 관련 이벤트·후기 도메인과 테이블이 아직 없어 현재 `0`이다. 이 값에 필터나 정렬의 완전한 의미를 부여하려면 해당 도메인을 먼저 이식해야 한다.

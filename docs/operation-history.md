# 운영 히스토리 설계

작성 기준: 2026-07-28

운영 히스토리는 Staff 화면에서 사람이 읽는 업무 처리 이력이다. 런타임 로그나 감사 로그와 목적이 다르다.

## 1. 목적

- 누가 처리했는지 기록한다.
- 어떤 대상에 대한 처리인지 기록한다.
- 어떤 업무 액션인지 기록한다.
- 값이 바뀐 경우 변경 전/후를 기록한다.
- 상세 화면과 처리 이력 화면에서 안정적으로 표시한다.

## 2. 테이블

### `operation_histories`

부모 이력 테이블이다.

| 컬럼 | 의미 |
| --- | --- |
| `target_type`, `target_id` | 이력이 붙는 대상 |
| `target_alias` | 화면/API에서 사용하는 대상 alias |
| `actor_type`, `actor_id`, `actor_kind` | 처리자 |
| `action` | 수행 액션 코드 |
| `batch_uuid` | 일괄 처리 묶음 UUID |
| `reason` | 처리 사유 |
| `metadata` | 추가 메타데이터 |

### `operation_history_changes`

필드별 변경 상세 테이블이다.

| 컬럼 | 의미 |
| --- | --- |
| `operation_history_id` | 부모 이력 ID |
| `field_key` | 변경 필드 키 |
| `field_label` | 화면 표시 필드명 |
| `before_value`, `after_value` | 변경 전/후 원본 값 |
| `before_display`, `after_display` | 변경 전/후 표시값 |
| `sort_order` | 표시 순서 |

## 3. 권장 패키지 구조

```text
domain/operationhistory/
  OperationHistory
  OperationHistoryChange

application/operationhistory/
  OperationHistoryService
  command/
  result/

infrastructure/persistence/operationhistory/
  OperationHistoryRepository
```

현재 Hospital 1차 구현에서는 Hospital 유스케이스 안에서 이력 조립을 먼저 처리한다. 도메인이 늘어나면 공통 `OperationHistoryService`로 분리한다.

## 4. 대상 기준

운영 히스토리 대상은 alias로 식별한다.

예시:

| alias | 대상 |
| --- | --- |
| `hospital` | 병원 |
| `hospital_doctor` | 의료진 |
| `hospital_entry` | 입점신청 |
| `hospital_event` | 병원 이벤트 |
| `hospital_event_db` | 이벤트 상담 DB |
| `hospital_event_real_model_db` | 리얼모델 신청 |
| `hospital_video` | 병원 동영상 |
| `account_user` | 일반회원 |
| `talk` | 토크 |
| `hospital_review` | 병의원 후기 |
| `hospital_evaluation` | 병의원 평가 |
| `notice` | 공지 |
| `faq` | FAQ |
| `category` | 카테고리 |

신고 처리 이력은 신고 상태 행이 아니라 실제 신고 대상에 기록한다.

## 5. 수행자 기준

`actor_kind`는 아래 값을 기준으로 한다.

- `STAFF`
- `HOSPITAL`
- `BEAUTY`
- `USER`
- `SYSTEM`
- `UNKNOWN`

Staff API에서 처리한 이력은 기본적으로 `STAFF`로 기록한다. 로그인 계정 도메인이 구현되기 전에는 시스템 또는 임시 actor 정보를 사용할 수 있다.

## 6. 액션 기준

권장 액션:

| action | 의미 |
| --- | --- |
| `CREATED` | 생성 |
| `UPDATED` | 일반 수정 |
| `DELETED` | 삭제 |
| `STATE_UPDATED` | 상태/검수 상태 변경 |
| `APPROVED` | 승인 |
| `REJECTED` | 반려 |
| `RESTORED` | 복구 |

액션은 업무 의미를 나타내고, 필드명은 change에 둔다. 예를 들어 `검수상태 변경`을 action으로 만들지 않고 `STATE_UPDATED` action과 `allow_status` change로 기록한다.

## 7. 변경 상세 기준

`field_label`은 순수 필드명만 사용한다.

좋은 예:

- `검수상태`
- `노출상태`
- `병원명`
- `사업자등록번호`

나쁜 예:

- `검수상태 변경`
- `노출상태 수정`
- `처리 완료`

화면 표시 문구가 필요한 값은 `before_display`, `after_display`를 같이 저장한다. enum label 계산이 바뀌어도 과거 이력 화면이 흔들리지 않게 하기 위함이다.

## 8. 기록 흐름

단건 상태 변경:

```java
OperationHistory history = OperationHistory.stateUpdated(
    "hospital",
    hospital.getId(),
    actor,
    reason
);

history.addChange(
    "allow_status",
    "검수상태",
    before.name(),
    after.name(),
    "신청",
    "승인"
);
```

여러 필드 변경:

```java
OperationHistory history = OperationHistory.updated("hospital", hospital.getId(), actor);

history.addChange("name", "병원명", beforeName, afterName, beforeName, afterName);
history.addChange("representative_phone", "대표번호", beforePhone, afterPhone, beforePhone, afterPhone);
```

변경 전/후 값이 같으면 change를 만들지 않는다. 변경값이 없으면 일반 수정 이력도 남기지 않는다.

## 9. API 응답 기준

```json
{
  "id": 1,
  "target_alias": "hospital",
  "target_id": 10,
  "actor_kind": "STAFF",
  "action": "STATE_UPDATED",
  "action_label": "상태 변경",
  "reason": "서류 확인 완료",
  "changes": [
    {
      "field_key": "allow_status",
      "field_label": "검수상태",
      "before_value": "PENDING",
      "after_value": "APPROVED",
      "before_display": "신청",
      "after_display": "승인"
    }
  ],
  "created_at": "2026-07-28T09:00:00"
}
```

신규 화면은 `changes` 배열을 기준으로 렌더링한다.

## 10. 조회 API 기준

상세 화면에서 최근 이력만 필요하면 상세 응답에 일부 포함할 수 있다. 별도 페이지네이션이 필요하면 도메인별 이력 조회 API를 둔다.

예시:

```text
GET /api/v1/staff/hospitals/{id}/operation-histories
```

조회는 반드시 `target_alias + target_id` 기준으로 제한한다.

## 11. 신규 도메인 적용 체크리스트

- target alias를 정했는가?
- 생성/수정/삭제/상태 변경 중 어떤 액션을 기록할지 정했는가?
- 상태값 label을 `before_display`, `after_display`로 저장하는가?
- 변경값이 없을 때 불필요한 이력을 만들지 않는가?
- 일괄 처리는 `batch_uuid`로 묶는가?
- 상세 화면에서 최근 이력만 필요한지, 별도 페이지네이션이 필요한지 정했는가?

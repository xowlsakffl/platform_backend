# 운영 히스토리 가이드

작성 기준: 2026-07-27

이 문서는 Staff 운영 화면에 노출되는 처리 이력 구조를 정리한다.

운영 히스토리와 로그는 목적이 다르다.

- `operation_histories`: 관리자 화면에서 사람이 읽는 업무 처리 이력
- `operation_history_changes`: 처리 이력에 붙는 필드별 변경 전/후 값
- 앱 로그(`Log::*`): 인증/보안 이벤트, 외부 연동 실패, 예외 분석용 운영 로그
- 모델 감사 로그(`activity_log` 등): 모델 변경 감사 추적용 로그

운영 히스토리는 “누가, 어떤 대상에, 어떤 업무 처리를 했고, 어떤 값이 바뀌었는지”를 화면에 안정적으로 보여주는 것이 목적이다.

## 1) 테이블 구조

| 테이블 | 역할 |
|---|---|
| `operation_histories` | 이력 부모 행. 대상, 수행자, 액션, 사유, 메타데이터를 저장 |
| `operation_history_changes` | 이력 상세 변경 행. 변경 필드별 전/후 값을 저장 |

`operation_histories` 주요 컬럼:

| 컬럼 | 의미 |
|---|---|
| `target_type`, `target_id` | 이력이 붙는 대상 모델 |
| `actor_type`, `actor_id`, `actor_kind` | 처리한 관리자/사용자/시스템 |
| `action` | 수행 액션 코드 |
| `batch_uuid` | 일괄 처리 묶음 UUID |
| `reason` | 처리 사유 |
| `metadata` | 추가 메타데이터 |

`operation_history_changes` 주요 컬럼:

| 컬럼 | 의미 |
|---|---|
| `operation_history_id` | 부모 이력 ID |
| `field_key` | 변경 필드 키 |
| `field_label` | 화면 표시 필드명 |
| `before_value`, `after_value` | 변경 전/후 원본 값 |
| `before_display`, `after_display` | 변경 전/후 표시값 |
| `sort_order` | 표시 순서 |

## 2) 대상 레지스트리

운영 히스토리 대상은 반드시 `OperationHistoryTargetRegistry`에 등록되어 있어야 한다.

등록되지 않은 모델에 이력을 기록하면 `OperationHistoryCreateAction`에서 `지원하지 않는 히스토리 대상입니다.` 예외가 발생한다. 신규 도메인에 운영 히스토리를 붙일 때는 먼저 레지스트리에 alias와 모델 class를 추가한다.

현재 지원 대상:

| alias | 모델 |
|---|---|
| `hospital` | `Hospital` |
| `hospital_doctor` | `HospitalDoctor` |
| `hospital_entry` | `HospitalEntry` |
| `beauty` | `Beauty` |
| `beauty_expert` | `BeautyExpert` |
| `hospital_event` | `HospitalEvent` |
| `hospital_event_db` | `HospitalEventDB` |
| `hospital_event_real_model_db` | `HospitalEventRealModelDB` |
| `hospital_event_ad` | `HospitalEventAd` |
| `hospital_video` | `HospitalVideo` |
| `account_user` | `AccountUser` |
| `hospital_review` | `HospitalReview` |
| `hospital_review_comment` | `HospitalReviewComment` |
| `hospital_evaluation` | `HospitalEvaluation` |
| `talk` | `Talk` |
| `talk_comment` | `TalkComment` |
| `chat_message` | `ChatMessage` |
| `notice` | `Notice` |
| `faq` | `Faq` |
| `category` | `Category` |
| `hashtag` | `Hashtag` |

`ContentReport`와 `ContentReportState` 자체는 히스토리 대상이 아니다. 신고 처리 이력은 신고 상태 행이 아니라 실제 신고 대상 모델(`Talk`, `TalkComment`, `HospitalReview`, `HospitalReviewComment`, `HospitalEvaluation`, `ChatMessage`, `HospitalVideo` 등)에 기록한다.

## 3) 수행자 레지스트리

수행자는 `OperationHistoryActorRegistry` 기준으로 alias/kind를 판정한다.

현재 actor kind:

- `STAFF`
- `HOSPITAL`
- `BEAUTY`
- `USER`
- `SYSTEM`
- `UNKNOWN`

규칙:

- `actor`가 있으면 모델 class 기준으로 kind를 판정한다.
- `actor`가 없으면 기본적으로 `SYSTEM`이다.
- 자동 처리나 시스템성 처리에서는 `actorKind`로 명시 override할 수 있다.
- DTO는 actor relation이 로드되어 있으면 이름/이메일을 내려주고, 없으면 `actor_kind`를 fallback label로 사용한다.

## 4) 액션 정책

`operation_histories.action`은 큰 행위 분류만 담당한다. 필드별 의미는 `operation_history_changes.field_key`, `field_label`로 구분한다.

| action | 화면 라벨 | 사용 기준 |
|---|---|---|
| `CREATED` | 생성 | 대상 생성 |
| `UPDATED` | 수정 | 일반 정보 수정 |
| `STATE_UPDATED` | 상태 변경 | 상태성 값 변경 |
| `DELETED` | 삭제 | 실제 삭제 이력 |

상태성 변경은 전부 `STATE_UPDATED`를 사용한다.

예:

- `status`
- `allow_status`
- `admin_status`
- `hospital_status`
- `report_status`
- `warning_status`
- `receipt_status`

`ACTION_STATUS_UPDATED`는 사용하지 않는다. 과거 상태 변경 표현은 `STATE_UPDATED`로 통일한다.

삭제처럼 보이는 처리라도 실제로는 상태값을 바꿔 화면 노출을 제어하는 플로우라면 `DELETED`가 아니라 `STATE_UPDATED`를 사용한다.

예:

- 신고 처리로 게시물이 미노출됨: `STATE_UPDATED`
- 유저가 댓글을 삭제해서 상태가 삭제/미노출로 바뀜: `STATE_UPDATED`
- 운영상 실제 삭제 행위를 이력으로 남겨야 함: `DELETED`

## 5) 변경값 정책

신규 운영 히스토리는 반드시 `changes` 배열을 기준으로 기록한다.

필드 규칙:

- `field_key`: 실제 컬럼명 또는 도메인 상태 키
- `field_label`: 화면에 보여줄 순수 필드명
- `before_value`, `after_value`: 원본 값
- `before_display`, `after_display`: 사람이 읽는 표시값

`field_label`에는 `변경`, `수정`, `처리` 같은 동사를 붙이지 않는다.

올바른 예:

- `노출여부`
- `검수상태`
- `조치유형`
- `신고상태`
- `경고여부`
- `영수증 상태`
- `강제중지`

잘못된 예:

- `노출여부 변경`
- `검수상태 변경`
- `조치유형 변경`
- `경고여부 변경`

`OperationHistoryCreateAction`과 `OperationHistoryDto`는 호환 방어용으로 `field_label` 끝의 ` 변경`을 제거한다. 그래도 신규 호출부에서는 처음부터 순수 필드명을 넘긴다.

화면 표시 문구가 필요한 값은 반드시 `before_display`, `after_display`를 함께 저장한다. 라벨 계산 로직이 나중에 바뀌어도 과거 이력 화면이 흔들리지 않게 하기 위함이다.

## 6) 기록 방식

운영 이력 생성은 `OperationHistoryCreateAction`만 사용한다. 도메인 Action에서 직접 `operation_histories`나 `operation_history_changes`를 생성하지 않는다.

단건 변경:

```php
$this->historyCreateAction->execute(
    target: $target,
    action: OperationHistory::ACTION_STATE_UPDATED,
    actor: $staff,
    reason: $reason,
    changes: OperationHistoryChangeSetBuilder::single(
        key: 'allow_status',
        label: '검수상태',
        before: $before,
        after: $after,
        beforeDisplay: '신청',
        afterDisplay: '승인',
    ),
);
```

여러 필드 변경:

```php
$changes = OperationHistoryChangeSetBuilder::make()
    ->compare(
        key: 'name',
        label: '병의원명',
        before: $beforeName,
        after: $afterName,
    )
    ->compare(
        key: 'tel',
        label: '전화번호',
        before: $beforeTel,
        after: $afterTel,
    )
    ->toArray();
```

수정 전/후 스냅샷이 있으면 `OperationHistoryChangeSetBuilder::fromSnapshots($before, $after)`를 사용한다.

`OperationHistoryChangeSetBuilder`는 JSON 기준으로 변경 전/후가 같으면 change를 만들지 않는다. 도메인별 `*UpdateHistoryRecordAction`은 변경값이 없으면 생성 이력을 제외하고 히스토리를 남기지 않는 흐름으로 작성한다.

생성 이력은 최초 입력값 전체를 `changes`에 남기지 않는다. 부모 이력 1건만 남기고 `reason`은 `null`로 둔다.

## 7) 도메인별 기록 위치

일반 수정 이력은 도메인별 `*UpdateHistoryRecordAction`에서 담당한다.

현재 기록 액션이 있는 주요 도메인:

- `AccountUserUpdateHistoryRecordAction`
- `BeautyUpdateHistoryRecordAction`
- `BeautyExpertUpdateHistoryRecordAction`
- `CategoryUpdateHistoryRecordAction`
- `HashtagUpdateHistoryRecordAction`
- `FaqUpdateHistoryRecordAction`
- `HospitalUpdateHistoryRecordAction`
- `HospitalDoctorUpdateHistoryRecordAction`
- `HospitalEntryUpdateHistoryRecordAction`
- `HospitalEventUpdateHistoryRecordAction`
- `HospitalEventDBUpdateHistoryRecordAction`
- `HospitalEventRealModelDBUpdateHistoryRecordAction`
- `HospitalEventAdUpdateHistoryRecordAction`
- `HospitalVideoUpdateHistoryRecordAction`
- `NoticeUpdateHistoryRecordAction`

신고 게시물 처리는 공통 `ContentReport` Action에서 처리하되, 히스토리 대상은 신고 대상 모델이다.

상태 변경 전용 Action은 직접 `OperationHistoryCreateAction`을 호출해도 된다. 단, action은 `STATE_UPDATED`, label은 순수 필드명, 값 라벨은 `before_display`/`after_display` 원칙을 지킨다.

## 8) API 응답 기준

`OperationHistoryDto`는 다음 구조를 내려준다.

```json
{
  "id": 1,
  "target_type": "App\\Domains\\Hospital\\Models\\Hospital",
  "target_id": 10,
  "target_alias": "hospital",
  "actor_kind": "STAFF",
  "actor_type": "account_staff",
  "actor_alias": "staff",
  "actor_id": 3,
  "actor": {
    "id": 3,
    "name": "관리자",
    "email": "admin@example.com"
  },
  "actor_label": "관리자",
  "action": "STATE_UPDATED",
  "action_label": "상태 변경",
  "batch_uuid": null,
  "field": "allow_status",
  "before_value": "PENDING",
  "after_value": "APPROVED",
  "reason": null,
  "metadata": null,
  "changes": [
    {
      "id": 1,
      "field_key": "allow_status",
      "field_label": "검수상태",
      "before_value": "PENDING",
      "after_value": "APPROVED",
      "before_display": "신청",
      "after_display": "승인",
      "sort_order": 0
    }
  ],
  "created_at": "2026-07-27T09:00:00.000000Z",
  "updated_at": "2026-07-27T09:00:00.000000Z"
}
```

`field`, `before_value`, `after_value`는 기존 프론트 호환용으로 첫 번째 change를 내려주는 필드다. 신규 화면은 `changes` 배열을 기준으로 렌더링한다.

## 9) 조회 API 기준

별도 페이지네이션 히스토리 API가 필요한 도메인은 도메인별 `*OperationHistoriesForStaffAction`을 둔다.

현재 별도 조회 Action이 있는 도메인:

- `Hospital`
- `HospitalEvent`
- `HospitalEventAd`
- `HospitalVideo`
- `HospitalEventDB`
- `HospitalEventRealModelDB`
- `HospitalReview`
- `HospitalReviewComment`
- `HospitalEvaluation`
- `Talk`
- `TalkComment`

그 외 도메인은 상세 DTO에서 최근 이력을 함께 내려주거나, 화면 요구가 생길 때 같은 패턴으로 별도 조회 Action을 추가한다.

조회 Query는 target model 기준으로 제한해야 한다. 신고 처리 이력처럼 공통 처리 Action에서 남긴 이력도 조회는 실제 target alias 기준으로 한다.

## 10) 화면 표시 규칙

프론트 신규 히스토리 UI는 `changes` 배열을 기준으로 렌더링한다.

- 작업 컬럼은 `action_label`만 표시한다.
- 변경내용은 `changes`를 공통 규칙으로 요약한다.
- `UPDATED` 단일 필드: `병의원명 변경`
- `UPDATED` 다중 필드: `병의원명 외 3개 변경`
- `STATE_UPDATED`: `검수상태: 신청 → 승인`
- 상태 변경에서 `검수상태 변경`, `조치유형 변경` 같은 문구를 action label로 만들지 않는다.
- JSON 원본 값을 그대로 노출하지 않는다. 사람이 읽을 수 있는 값은 도메인 기록 액션에서 `before_display`, `after_display`로 저장한다.
- 히스토리 페이지네이션 중 기존 목록 영역을 비우지 않는다. 일반 목록과 같은 loading state를 사용해 스크롤 튐을 막는다.

## 11) 신규 도메인 적용 체크리스트

신규 도메인에 운영 히스토리를 붙일 때 확인한다.

- `OperationHistoryTargetRegistry`에 alias와 모델 class를 등록했는가?
- 모델에 `operationHistories()` morphMany 관계가 필요한가?
- 일반 수정은 도메인별 `*UpdateHistoryRecordAction`으로 분리했는가?
- 상태 변경은 `STATE_UPDATED`로 기록했는가?
- `field_label`에 `변경`, `수정`, `처리`를 붙이지 않았는가?
- 상태값 라벨을 `before_display`, `after_display`로 저장했는가?
- 변경값이 없을 때 불필요한 이력이 생기지 않는가?
- 상세 화면에서 별도 페이지네이션이 필요하면 `*OperationHistoriesForStaffAction`을 추가했는가?

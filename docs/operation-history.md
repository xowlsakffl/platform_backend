# 운영 이력

## 목적

운영 이력은 Staff 또는 Hospital이 관리 데이터를 언제 어떻게 바꿨는지 추적한다. 애플리케이션 로그와 달리 비즈니스 변경 내역을 DB에 보존한다.

```text
operation_histories
  target_type, target_id
  actor_type, actor_id
  action, reason, memo

operation_history_changes
  operation_history_id
  field_key
  before_value
  after_value
```

현재 target은 `HOSPITAL`, `DOCTOR`, `CATEGORY`다.

## 기록 기준

- 생성, 상태 변경, 검수 변경, 실제 값이 바뀐 수정, 삭제를 기록한다.
- 수정 요청이 들어와도 전후 값이 같으면 빈 수정 이력을 만들지 않는다.
- 반려 등 사유가 필요한 정책은 Request와 Service에서 검증하고 `reason`에 저장한다.
- 복합 값은 안정적인 JSON 문자열로 저장한다.
- 비밀번호, JWT, 원본 파일 내용 같은 비밀 데이터는 기록하지 않는다.

Hospital 상세의 `latest_status_history`는 현재 status로 바뀐 가장 최근 이력이다. Staff는 `GET /api/v1/staff/hospitals/{id}/operation-histories`로 병원 이력을 페이지 조회한다.

이력 테이블은 폴리모픽 target을 사용하므로 대상 테이블 외래 키를 두지 않는다. 대상이 soft delete되어도 이력은 남긴다.

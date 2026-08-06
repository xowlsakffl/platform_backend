# 업체 특징

## 목적

업체가 제공하는 편의시설과 이용 특징을 공통 마스터로 관리한다. 업체에는 `partner_feature_assignments`로 여러 특징을 연결한다.

## 단일 정의 파일

업체 특징의 유일한 원본은 다음 JSON이다.

```text
src/main/resources/partner-feature-definitions/features.json
```

특징을 추가하거나 이름과 순서를 수정할 때는 이 파일만 변경한다. 특징 데이터용 Flyway migration은 추가하지 않는다.

## 시작 시 동기화

애플리케이션은 시작할 때 JSON을 검증하고 DB에 트랜잭션으로 동기화한다.

- `code`가 식별자다.
- JSON에 있는 특징은 생성하거나 이름, 순서와 상태를 갱신한다.
- JSON에서 제거된 특징은 기존 업체 연결 보존을 위해 삭제하지 않고 `INACTIVE`로 전환한다.
- 기존 `partner_feature_assignments`는 삭제하지 않는다.
- 코드 또는 순서 중복, 잘못된 코드 형식, 빈 이름이 있으면 시작에 실패한다.
- 동기화는 기본 활성화이며 `PARTNER_FEATURE_DEFINITION_SYNC_ENABLED=false`로만 끌 수 있다.

Flyway는 `partner_features`와 연결 테이블의 스키마만 관리한다. 특징 기준 데이터는 migration에 중복 정의하지 않는다.

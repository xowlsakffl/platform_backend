# Scheduler 운영 가이드

작성 기준: 2026-07-28

이 문서는 Medi 백엔드의 시간 기반 작업 실행 기준을 정리한다.

## 1. 기본 원칙

스케줄러는 시간 기반 트리거만 담당한다. 오래 걸리는 작업, 외부 API 반복 호출, 대량 데이터 처리는 worker 또는 비동기 작업으로 넘긴다.

Spring Boot에서는 `@Scheduled`와 전용 service를 기준으로 시작한다.

## 2. 실행 기준

- 서버 timezone은 `Asia/Seoul` 기준으로 맞춘다.
- 스케줄 주기는 코드와 문서에 같이 남긴다.
- 중복 실행되면 안 되는 작업은 lock을 사용한다.
- 다중 서버 배포 전에는 한 서버에서만 실행되도록 lock 전략을 먼저 확정한다.
- 실패와 소요 시간을 로그/metric으로 남긴다.

## 3. 현재 후보 작업

| 작업 | 주기 | 목적 |
| --- | --- | --- |
| 임시 에디터 이미지 정리 | 매시간 | 오래된 임시 업로드 제거 |
| pending Push 재처리 | 5분마다 | 누락된 알림 발송 재시도 |
| 이미지 variant 생성 | 필요 시 | 썸네일/medium 이미지 백필 |
| 병원 평가 집계 보정 | 매일 새벽 | 평가 수/평균 평점 재계산 |
| 오래된 실패 작업 정리 | 매일 새벽 | 운영 테이블 크기 관리 |

후보 작업은 실제 구현 시 command/service 이름, lock key, timeout, metric 이름을 문서에 추가한다.

## 4. 구현 형태

권장 구조:

```text
domain/{domain}/application
  -> 실제 비즈니스 처리

infrastructure/scheduler
  -> @Scheduled trigger
  -> lock 획득
  -> application service 호출
```

스케줄러 클래스에 비즈니스 로직을 직접 넣지 않는다.

## 5. Lock 기준

중복 실행 위험이 있는 작업은 아래 중 하나를 사용한다.

- DB lock 테이블
- Redis lock
- ShedLock 같은 scheduler lock 라이브러리

lock key는 작업 목적이 드러나게 정한다.

```text
scheduler:hospital-evaluation-rating-refresh
scheduler:notification-pending-push-retry
```

## 6. 배포 체크리스트

1. 스케줄 주기와 timezone 확인
2. lock 적용 여부 확인
3. 작업 timeout과 batch size 확인
4. 실패 로그와 metric 확인
5. 운영 재실행 방법 문서화
6. 관련 비동기 작업 queue 영향 확인

## 7. 장애 대응

### 스케줄이 실행되지 않음

1. 애플리케이션 프로필과 scheduler 활성 설정 확인
2. 서버 timezone 확인
3. 로그에서 scheduler trigger 확인
4. lock이 풀리지 않았는지 확인
5. 수동 실행 가능한 service 또는 운영 명령으로 단건 확인

### 특정 작업만 실패

1. 실패 stack trace와 입력 범위 확인
2. 외부 API 장애인지 DB 데이터 문제인지 분리
3. 재실행 가능 여부 확인
4. batch size를 줄여 재처리
5. 반복 실패면 상태 테이블에 실패 사유를 남긴다.

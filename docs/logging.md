# 로깅 전략

작성 기준: 2026-07-28

이 문서는 Medi 백엔드의 앱 로그, traceId, 감사 로그, 운영 히스토리, 비동기 작업 로그 기준을 정리한다.

## 1. 로그 구분

| 구분 | 목적 |
| --- | --- |
| 앱 로그 | 장애 분석, 성능 병목 확인, 예외 추적 |
| 요청 로그 | API 호출 흐름과 latency 확인 |
| 감사 로그 | 로그인, 권한 변경, 민감 데이터 접근 추적 |
| 운영 히스토리 | 관리자 화면에서 확인할 도메인 변경 이력 |
| 비동기 작업 로그 | 작업 처리/재시도/실패 원인 분석 |
| 스케줄 로그 | 시간 기반 작업 실행/누락/실패 확인 |

감사 로그와 운영 히스토리는 목적이 다르다. 감사 로그는 시스템 추적용이고, 운영 히스토리는 운영자가 화면에서 업무 처리 이력을 확인하기 위한 데이터다.

## 2. Trace ID

모든 요청은 `RequestTraceFilter`에서 trace ID를 확정한다.

- 요청 header `X-Request-Id`가 있으면 재사용한다.
- 없으면 UUID를 생성한다.
- 응답 header `X-Request-Id`에도 같은 값을 내려준다.
- MDC key는 `traceId`를 사용한다.

로그 패턴에는 `traceId`를 포함한다.

```text
%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{traceId}] %logger{36} - %msg%n
```

## 3. 앱 로그 기준

권장 로그 레벨:

- `INFO`: 주요 상태 전이, 외부 연동 성공/실패 요약, 스케줄 시작/종료
- `WARN`: 재시도 가능한 외부 장애, 비정상 입력 반복, 임계치 근접
- `ERROR`: 요청 실패, 작업 영구 실패, 데이터 정합성 위험
- `DEBUG`: 로컬 개발/임시 분석 전용

금지:

- 비밀번호, 토큰, 인증 코드, 주민번호 등 민감 정보를 로그에 남기지 않는다.
- 파일 본문, 이미지 base64, 대용량 payload를 로그에 남기지 않는다.
- 예외를 삼키고 로그만 남긴 뒤 성공 처리하지 않는다.

## 4. 요청 로그

요청 로그에는 아래 정보를 남긴다.

- traceId
- method
- path
- actor
- account id
- status
- latency
- client IP
- user agent

요청 body 전체를 기본 로그로 남기지 않는다. 필요한 경우 필드 단위 allowlist를 만든다.

## 5. 감사 로그

감사 로그 대상:

- 로그인 성공/실패
- 로그아웃
- 비밀번호 변경/초기화
- 권한/역할 변경
- 내부도구 접근
- 민감 데이터 조회
- 대량 다운로드

감사 로그 필드:

- actor type
- actor id
- action
- target type
- target id
- IP
- user agent
- traceId
- result
- created at

## 6. 운영 히스토리

운영 히스토리는 관리자 화면에 노출할 도메인 변경 이력이다.

기준:

- 도메인 변경 전/후 값을 저장한다.
- 단건 변경도 변경 상세 1건으로 저장한다.
- 다중 변경은 부모 이력 1건에 여러 변경 상세를 붙인다.
- 화면 표시용 label은 백엔드에서 일관되게 내려준다.

상세 구조는 [operation-history.md](./operation-history.md)를 따른다.

## 7. 비동기 작업 로그

비동기 작업은 시작/성공/실패를 구분해 남긴다.

필수 정보:

- job type
- job id 또는 대상 id
- queue name
- attempt count
- status
- elapsed time
- error code/message
- traceId 또는 parent traceId

작업 실패는 로그만 남기지 말고 상태 테이블에도 반영한다.

## 8. 스케줄 로그

스케줄 작업은 실행 단위마다 아래 정보를 남긴다.

- schedule name
- lock key
- started at
- finished at
- elapsed time
- processed count
- skipped count
- failed count

중복 실행 방지 lock을 사용하는 경우 lock 획득 실패도 `INFO`로 남긴다. 같은 시간대에 같은 작업이 반복 실패하면 `WARN` 이상으로 올린다.

## 9. 외부 연동 로그

외부 연동은 요청 단위로 아래 정보를 남긴다.

- provider
- endpoint 또는 operation name
- timeout
- retry count
- response status
- latency
- error code

외부 API 응답 원문은 저장하지 않는다. 장애 분석에 필요한 값만 선별해 남긴다.

## 10. 장애 대응 체크

API 장애:

1. traceId로 요청 로그와 앱 로그를 묶어 확인한다.
2. 예외 코드와 HTTP status를 확인한다.
3. 같은 actor/account에서 반복되는지 확인한다.
4. DB query, 외부 API, validation 문제를 분리한다.

비동기 작업 장애:

1. 실패 job type과 대상 id를 확인한다.
2. 재시도 가능한 실패인지 판단한다.
3. 상태 테이블의 attempt count와 last error를 확인한다.
4. 재처리 범위를 제한해서 실행한다.

스케줄 장애:

1. scheduler 활성 여부를 확인한다.
2. lock이 풀리지 않았는지 확인한다.
3. 최근 배포 이후 주기/조건이 바뀌었는지 확인한다.
4. 수동 실행 가능한 service로 단건 재현한다.

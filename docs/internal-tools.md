# 내부도구 운영 가이드

작성 기준: 2026-07-28

이 문서는 Staff 전용 내부 운영 도구의 접근 제어 기준을 정리한다.

## 1. 목적

내부도구는 운영자만 접근해야 하는 관리성 화면과 진단 엔드포인트를 뜻한다.

대상 예시:

- Spring Boot Actuator
- OpenAPI 문서 UI
- 비동기 작업 상태 화면
- 운영 진단/정리 도구

현재 구현되지 않은 도구는 문서에 확정 기능처럼 적지 않는다. 도구를 추가할 때 이 문서와 보안 설정을 함께 갱신한다.

## 2. 접근 제어 원칙

내부도구는 아래 조건을 모두 통과해야 한다.

1. 내부망 또는 IP allowlist
2. Staff 인증
3. 내부도구 접근 권한
4. 필요한 경우 2차 확인 또는 별도 audit log

권한 기준:

- `platform.super_admin`
- `platform.dev`

일반 Staff 권한과 내부도구 접근 권한은 분리한다. 운영 데이터 조회 권한이 있어도 내부 진단 도구 접근 권한이 자동으로 생기면 안 된다.

## 3. 경로 기준

권장 경로:

| 용도 | 경로 |
| --- | --- |
| 내부도구 허브 | `/staff/tools` |
| OpenAPI 문서 | `/staff/tools/api-docs` |
| 작업 상태 화면 | `/staff/tools/jobs` |
| 운영 진단 | `/staff/tools/diagnostics` |

Actuator는 직접 노출하지 않는다. 공개 가능한 health check만 `/actuator/health`로 열고, 상세 actuator endpoint는 인증/네트워크 제한 뒤에 둔다.

## 4. 설정 기준

환경변수:

```env
INTERNAL_TOOL_ALLOWED_IPS=127.0.0.1,::1
INTERNAL_TOOL_ALLOWED_EMAILS=
```

규칙:

- IP allowlist가 비어 있으면 로컬 접근만 허용한다.
- 운영 환경에서는 allowlist를 명시한다.
- 이메일 allowlist가 있으면 role 조건과 이메일 조건을 모두 통과해야 한다.
- 프록시/로드밸런서 뒤에서는 실제 client IP 계산 방식을 먼저 확정한다.

## 5. 감사 로그

아래 행위는 audit log 또는 operation history에 남긴다.

- 내부도구 로그인 성공/실패
- 운영 진단 명령 실행
- 데이터 정리/백필/재처리 실행
- 권한 변경
- 민감 데이터 조회

로그에는 `traceId`, staff id, IP, user agent, 실행 대상, 결과를 포함한다.

## 6. 금지 사항

- 내부도구를 public API namespace 아래에 두지 않는다.
- 단순히 메뉴를 숨기는 방식으로 접근 제어를 대체하지 않는다.
- 내부도구 권한을 일반 Staff 조회 권한에 섞지 않는다.
- 운영 데이터 변경 도구를 audit log 없이 만들지 않는다.

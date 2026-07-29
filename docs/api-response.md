# API 응답과 예외

## 성공 응답

모든 JSON API 성공 응답은 `ApiResponse.Success`를 사용한다.

```json
{
  "success": true,
  "data": {},
  "meta": null,
  "traceId": "request-trace-id"
}
```

목록 페이지는 `data`에 배열, `meta`에 페이지 정보를 둔다.

```json
{
  "success": true,
  "data": [],
  "meta": {
    "current_page": 1,
    "per_page": 15,
    "total": 0,
    "last_page": 0,
    "from": null,
    "to": null
  },
  "traceId": "request-trace-id"
}
```

Controller는 `RequestTrace.traceId(request)`를 응답에 전달한다. 클라이언트의 정상적인 `X-Request-Id`가 있으면 재사용하고, 없으면 서버가 생성한다.

## 실패 응답

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "details": null
  },
  "traceId": "request-trace-id"
}
```

Bean Validation 오류는 `details.errors`에 필드별 메시지를 넣는다.

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "details": {
      "errors": {
        "email": ["올바른 형식의 이메일 주소여야 합니다"]
      }
    }
  },
  "traceId": "request-trace-id"
}
```

## ErrorCode

| 코드 | HTTP | 사용 기준 |
|---|---:|---|
| `INVALID_REQUEST` | 422 | 검증, 중복, 허용되지 않은 상태 변경 |
| `UNAUTHORIZED` | 401 | 로그인 또는 유효한 토큰 필요 |
| `FORBIDDEN` | 403 | Actor, 권한, 소유권 불일치 |
| `NOT_FOUND` | 404 | 리소스 또는 공개 미디어 없음 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP method |
| `TOKEN_ERROR` | 419 | 별도 토큰 절차 오류 |
| `PAYLOAD_TOO_LARGE` | 413 | 요청 또는 업로드 용량 초과 |
| `RATE_LIMITED` | 429 | 요청 제한 초과 |
| `DB_ERROR` | 500 | 데이터베이스 처리 실패 |
| `INTERNAL_ERROR` | 500 | 내부 처리 실패 |

## 예외 사용

- Request의 타입·형식·길이·범위는 Bean Validation으로 처리한다.
- 조회 실패, 중복, 상태 규칙, 소유권 실패는 `ApiException`을 사용한다.
- 저장 데이터 JSON 손상, 미디어 I/O 등 클라이언트가 고칠 수 없는 실패는 `InternalApplicationException`을 사용한다.
- 인증·인가 실패는 Spring Security 진입점과 공통 예외 응답을 사용한다.
- 디버그가 꺼진 환경에서는 내부 예외명과 메시지를 응답에 노출하지 않는다.

## Request 사용 기준

- 단일 PathVariable 조회·삭제: 별도 Request 없음
- 필터·정렬·페이지·include가 있는 조회: `*ListForActorRequest`, `*DetailForActorRequest`
- 생성·수정·상태 변경: `*CreateForActorRequest`, `*UpdateForActorRequest`, `*StatusUpdateForActorRequest`
- multipart 부분수정: 실제 전달된 필드 집합을 Command에 넘겨 미전달과 명시적 삭제를 구분

Request는 application의 Command/Query로 변환하며 Service가 HTTP Request 클래스를 직접 참조하지 않는다.

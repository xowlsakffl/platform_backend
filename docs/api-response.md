# API 응답 / 페이지네이션 규칙

작성 기준: 2026-07-24

이 문서는 현재 코드 기준의 API 응답 포맷과 페이지네이션 사용 규칙을 정리한다.

## 1) 적용 범위

일반 JSON API는 `App\Common\Http\Responses\ApiResponse`를 통해 응답한다.

- `/api/*` 요청과 `expectsJson()` 요청의 예외 응답은 `bootstrap/app.php`에서 `ApiResponse`로 변환한다.
- 컨트롤러의 일반 성공 응답은 `ApiResponse::success()`를 사용한다.
- 파일 다운로드, 스트리밍 다운로드처럼 JSON이 아닌 응답은 예외다. 현재 `TalkExcelDownloadForStaffAction`은 `response()->streamDownload()`를 사용한다.

## 2) 성공 응답

`ApiResponse::success()`는 항상 아래 필드를 포함한다.

```json
{
  "success": true,
  "data": {},
  "meta": null,
  "traceId": "request-trace-id"
}
```

규칙:

- `success`: 항상 `true`
- `data`: 응답 본문. 값이 없으면 `null`
- `meta`: 페이지네이션/부가 정보. 값이 없으면 `null`
- `traceId`: `RequestId` 미들웨어가 확정한 요청 추적 ID

`ApiResponse::success($data, $meta, $traceId, $status)`는 커스텀 HTTP status를 받을 수 있지만, 신규 코드에서는 특별한 이유가 없으면 기본 `200`을 사용한다.

목록 응답은 일반적으로 `data`에 row 배열, `meta`에 페이지 정보를 둔다.

```json
{
  "success": true,
  "data": [],
  "meta": {
    "current_page": 1,
    "per_page": 15,
    "total": 0,
    "last_page": 1
  },
  "traceId": "request-trace-id"
}
```

## 3) 에러 응답

에러 응답은 `ApiResponse::errorCode()` 또는 `bootstrap/app.php`의 공통 예외 핸들러가 만든다.

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다."
  },
  "traceId": "request-trace-id"
}
```

규칙:

- `success`: 항상 `false`
- `error.code`: `App\Common\Exceptions\ErrorCode` 값
- `error.message`: 클라이언트에 노출 가능한 메시지
- `error.details`: 상세 정보가 있을 때만 포함
- `traceId`: 요청 추적 ID

Validation 실패는 `error.details.errors`에 필드별 오류를 담는다.

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "details": {
      "errors": {
        "name": ["이름은 필수 항목입니다."]
      }
    }
  },
  "traceId": "request-trace-id"
}
```

주요 예외 매핑은 `./error-handling.md`를 따른다. 현재 코드 기준으로 `PostTooLargeException`은 `PAYLOAD_TOO_LARGE` / HTTP 413으로 응답한다.

## 4) Controller / Action 응답 책임

Controller는 검증된 입력을 Action에 넘기고, Action 결과를 `ApiResponse`에 연결한다.

목록 Action은 보통 아래 구조를 반환한다.

```php
return [
    'items' => [...],
    'meta' => [...],
];
```

Controller는 이를 아래처럼 연결한다.

```php
$result = $action->execute($request->filters());

return ApiResponse::success($result['items'], $result['meta'] ?? null);
```

단건 조회 Action은 도메인 키를 고정해서 반환한다.

```php
return [
    'hospital' => HospitalForStaffDetailDto::fromModel($hospital)->toArray(),
];
```

## 5) User API 응답 정책

User API는 조회 API와 행위 처리 API의 응답 책임을 분리한다.

### 조회 API

리소스를 화면에 표시하는 API는 DTO를 사용한다.

- `GET list`
- `GET detail`
- 채팅방 목록, 채팅 메시지 목록
- 알림 목록, 차단 회원 목록
- 토크/후기 상세처럼 앱 화면을 구성하는 조회 응답

조회 응답은 Eloquent 모델을 그대로 노출하지 않고 `Dto\User` 계열에서 필요한 필드만 변환한다.

```php
return [
    'talk' => TalkForUserDetailDto::fromModel($talk)->toArray(),
];
```

### 생성 API

생성 직후 클라이언트가 화면에 바로 붙여야 하는 리소스는 DTO를 반환할 수 있다.

- 토크 생성
- 토크 댓글 생성
- 병의원 후기 생성
- 병의원 후기 댓글 생성
- 채팅 메시지 전송

단, 클라이언트가 생성 결과 전체를 즉시 사용하지 않는 경우에는 `id` 또는 `message` 같은 최소 응답만 내려준다.

### Command API

단순 행위 처리 API는 내부 상태나 DTO를 내려주지 않는다.

- 신고
- 삭제
- 읽음 처리
- 차단/차단 해제
- 알림 전체 읽음
- 토글/상태 변경성 API

기본 응답은 컨트롤러에서 직접 명시한다.

```php
return ApiResponse::success([
    'message' => '신고가 완료되었습니다.',
]);
```

Action은 처리만 담당하고, 단순 완료 메시지를 만들지 않는다. 단, command 이후 클라이언트 동기화에 최소 상태가 필요한 경우에만 `message + 최소 필드`를 허용한다.

```php
return ApiResponse::success([
    'message' => '알림 설정이 변경되었습니다.',
    'notifications_enabled' => true,
]);
```

## 6) LengthAware pagination

Laravel `LengthAwarePaginator`를 쓰는 목록 응답은 `App\Common\Support\PaginatedResponse::fromPaginator()`를 사용한다.

```php
return PaginatedResponse::fromPaginator(
    $paginator,
    fn (Model $model): array => Dto::fromModel($model)->toArray(),
);
```

`fromPaginator()`는 아래 구조를 만든다.

```php
[
    'items' => [...],
    'meta' => [
        'current_page' => 1,
        'per_page' => 15,
        'total' => 0,
        'last_page' => 1,
    ],
]
```

추가 meta가 필요하면 세 번째 인자로 전달한다.

```php
return PaginatedResponse::fromPaginator(
    $paginator,
    $mapper,
    ['summary' => $summary],
);
```

상세 보조 목록도 같은 방식으로 paginator를 만든 뒤 `fromPaginator()`에 태운다. 페이지 보정이 필요하면 해당 화면/액션의 정책으로 명시적으로 처리한다.

## 7) Cursor pagination 예외

채팅 메시지 목록은 `ChatMessageListForUserQuery`에서 cursor 방식으로 조회한다.

```json
{
  "per_page": 30,
  "has_more": true,
  "after_id": null,
  "before_id": 123,
  "order": "desc"
}
```

이 응답은 `current_page`, `total`, `last_page` 개념이 없으므로 `PaginatedResponse`에 태우지 않는다. cursor 방식 목록을 추가할 때는 이 구조를 명시적으로 유지한다.

## 8) 현재 통일된 목록

다음 계열은 `PaginatedResponse::fromPaginator()`를 사용한다.

- Staff 일반 목록: 회원, 병원, 입점신청, 뷰티, 의사, 전문가, 이벤트, 광고, 영상, 공지, FAQ, 카테고리, 해시태그
- Staff 게시물 목록: 토크, 토크 댓글, 병의원 후기, 후기 댓글, 병의원 평가
- Staff 상세 보조 목록: 댓글, 히스토리, 신고내역
- Staff 신고게시물 목록
- User 목록: 채팅방, 알림, 차단 회원

직접 `current_page`, `per_page`, `total`, `last_page`를 조립하지 않는다. 예외가 필요하면 먼저 cursor pagination인지, 파일/스트림 응답인지, 외부 API 응답인지 명확히 구분한다.

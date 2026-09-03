# 공지사항

## 정책

- Staff가 제목(100자), HTML 본문, 첨부파일 최대 5개, 노출 설정, 게시기간, 상단 고정, 대시보드 팝업 사용 여부를 관리한다.
- 게시기간은 한국시간이며 시작 포함, 종료 제외다. 기간이 없으면 `무기한 게시`로 표시한다. 수동 미노출 설정은 날짜보다 우선하며 목록·상세는 최종 게시상태를 표시한다.
- `status`는 `PUBLIC`/`PRIVATE`다. 응답의 `publication_status`는 `HIDDEN`/`SCHEDULED`/`PUBLISHED`/`ENDED`로 계산하며 따로 저장하지 않는다.
- 파트너 대시보드 홈 팝업은 로그인한 Partner에게만 제공한다. 공개 포털 메인에는 팝업을 띄우지 않는다.
- 게시중인 공지만 Partner 목록, 상세, 팝업, 파일 조회에 제공한다. Staff는 권한에 따라 비공개 공지도 조회한다.
- 본문 이미지와 첨부파일은 공통 `media`에 저장한다. 첨부파일은 JPG/PNG/WebP/PDF/DOCX/XLSX/PPTX, 각 20MB 이하를 허용한다. 본문 이미지는 JPG/PNG/WebP 각 5MB, 최대 30개다.
- 빈 첨부파일과 허용되지 않은 확장자는 저장 전에 거부한다. 첨부파일 검증에 실패하면 본문 이미지 귀속과 공지 변경까지 함께 롤백한다.
- 본문은 HTML 파서의 허용 목록으로 정제한다. 외부 이미지, 스크립트, 임의 스타일과 위험한 링크를 허용하지 않는다. 이미지 참조는 `/notice-media/{id}`로 저장한다.
- 본문 이미지 업로드는 Staff 소유 임시 미디어이며 저장 시 해당 공지로 귀속한다. 다른 직원의 임시 파일 또는 다른 공지 파일을 사용할 수 없다. 취소 시 제거하고 24시간 경과한 미사용 파일도 정리한다.
- 등록/수정/삭제 이력은 공통 운영 이력의 `NOTICE` 대상으로 남긴다. 파일 롤백/커밋 후 정리 정책은 기존 미디어와 같다.
- 등록/수정/삭제 요청의 IP·User-Agent와 수행자 스냅샷을 계정 히스토리와 같은 방식으로 기록한다. 과거에 기록되지 않은 IP는 추정하여 채우지 않는다.
- 수정 시 `version`으로 동시 수정 덮어쓰기를 방지한다. 삭제는 soft delete다.
- 본문 HTML은 최대 100,000자다. 공통 이력의 전후 값은 `mediumtext`로 저장해 한글 본문도 잘리지 않는다. 본문 변경 전후 HTML은 보존하지만 삭제한 이미지 파일 자체는 보존하지 않는다.
- API 데이터는 기존 계약과 동일하게 `snake_case`다. 본문 파일은 다운로드 시에도 게시상태를 다시 검사한다.

## API

- Staff: `GET/POST /api/v1/staff/notices`, `GET/PATCH/DELETE /api/v1/staff/notices/{id}`
- Staff 이력: `GET /api/v1/staff/notices/{id}/histories`
- Staff 이력에는 대상 종류·ID, 수행자 종류·ID·이름·로그인 아이디 및 IP 스냅샷을 포함한다. 변경 전후 값은 축약하지 않고 반환하며 화면에서 요약과 상세 보기를 구분한다.
- Staff 본문 이미지: `POST/DELETE /api/v1/staff/notices/editor-images`
- Staff 파일: `GET /api/v1/staff/notices/media/{mediaId}/content`
- Partner: `GET /api/v1/partner/notices`, `GET /api/v1/partner/notices/{id}`, `GET /api/v1/partner/notices/popups`
- Partner 파일: `GET /api/v1/partner/notices/{id}/media/{mediaId}/content`

등록/수정은 multipart의 JSON `data`와 신규 파일 배열 `attachments`를 받는다. 유지할 파일은 `attachment_ids`로 명시한다.
권한은 기존 `platform.notice.show/create/update/delete`를 사용한다.

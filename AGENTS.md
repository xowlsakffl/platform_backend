# Medi Backend Agent Rules

이 파일은 `medi_backend` 작업 시 우선 확인할 에이전트 진입점입니다.

## 기준 문서

- [백엔드 문서 인덱스](./docs/README.md)
- [백엔드 아키텍처](./docs/architecture.md)
- [Beaulab에서 Medi로 이관할 때의 기준](./docs/migration-from-beaulab.md)

## 최소 강제 규칙

- 새 문서는 한국어로 작성합니다.
- 구조와 구현 규칙이 바뀌면 관련 문서를 함께 갱신합니다.
- 백엔드는 Spring Boot 단일 애플리케이션으로 시작합니다.
- 패키지는 Actor 기준이 아니라 도메인 기준으로 나눕니다.
- API URL namespace는 `/api/v1/public`, `/api/v1/user`, `/api/v1/staff`, `/api/v1/hospital`, `/api/v1/beauty`를 기준으로 합니다.
- DB 테이블명과 API 도메인 경로에는 임시 브랜드명 `medi`를 넣지 않습니다.
- `docs/`의 세부 도메인 문서는 Beaulab 비즈니스 규칙을 Medi 초안으로 승격한 문서입니다.
- 세부 문서에 남아 있는 Laravel 경로와 클래스명은 구현 참고 맥락이며, Spring Boot 패키지/클래스로 그대로 복제하지 않습니다.
- 개인정보 동의, 마케팅 동의, 신청 IP, user agent, 운영 이력은 초기 설계에서 누락하지 않습니다.

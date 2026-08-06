# 백엔드 문서

이 문서는 현재 구현된 코드와 확정된 정책만 설명한다. 아직 구현하지 않은 도메인은 미리 상세 설계하지 않고, 이식 작업을 시작할 때 정책을 대조한 뒤 문서와 코드를 함께 추가한다.

## 읽는 순서

1. [아키텍처와 디렉터리](./architecture.md)
2. [API 응답과 예외](./api-response.md)
3. [인증과 권한](./authentication.md)
4. [도메인 정책과 상태](./domain-status-definition.md)
5. 변경 대상 세부 문서

## 세부 문서

- [카테고리](./category.md)
- [업체 특징](./partner-feature.md)
- [미디어](./media.md)
- [운영 이력](./operation-history.md)
- [Redis 캐시](./cache.md)
- [현재 DB 스키마](./schema.dbml)

## 단일 소스 원칙

- DB 구조의 실행 기준은 `src/main/resources/db/migration`이다.
- 상태와 공개 범위의 기준은 도메인 enum과 [도메인 정책](./domain-status-definition.md)이다.
- API 입력·응답의 기준은 Request, Command/Query, Result 코드다.
- 문서와 코드가 다르면 정책 우선순위를 확인해 같은 변경에서 둘을 맞춘다.
- 임시 서비스명은 설정과 화면 표시에서만 사용하고 테이블명이나 리소스 경로에 넣지 않는다.

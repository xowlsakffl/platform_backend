# Redis 캐시

## 현재 사용처

Redis는 두 기능에 사용한다.

| 기능 | key | TTL |
|---|---|---:|
| Staff Hospital summary | `staff:summary:hospital:default` | 기본 300초 |
| 로그아웃 JWT 차단 | `auth:revoked:{jti}` | JWT 남은 만료 시간 |

## Hospital summary

`GET /api/v1/staff/hospitals/summary` 결과를 캐시한다. 병원 생성·삭제·상태·검수 변경과 Hospital 로그인으로 휴면 수치가 바뀌면 DB 커밋 후 무효화한다.

캐시 조회·저장 실패 시 summary는 DB 조회로 대체한다. 캐시는 원본 데이터가 아니므로 Redis 장애 때문에 병원 관리 조회가 틀린 값을 확정해서는 안 된다.

## JWT 차단 목록

로그아웃 토큰 차단은 보안 상태이므로 일반 조회 캐시와 다르다. JWT 검증 시 Redis에서 `jti`를 확인하며, Redis 키는 원래 토큰 만료 뒤 자동 삭제된다.

현재 로컬 개발에서는 Redis가 필수 의존성이다. `application-local.yml`과 `.env.example`의 비밀번호를 일치시킨다.

새 캐시를 추가할 때는 key 규칙, TTL, 원본, 무효화 이벤트, Redis 장애 시 동작을 문서와 코드에 함께 명시한다.

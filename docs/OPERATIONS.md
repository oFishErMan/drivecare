# 로그와 운영 가이드

## 변경 의도

별도 로그 수집 서버나 메시지 큐를 추가하지 않고, 현재 Docker Compose 환경에서 오류를 찾고
수정하기 쉽게 정리했다. 요청 추적, 안전한 오류 응답, 커밋 후 변경 로그, 알림 작업의 실패 격리가 핵심이다.

## 오류 한 건 추적하기

1. 서버 오류 안내에 표시되는 `문의 코드`를 복사한다. 개발자 도구에서는 응답의 `X-Request-ID`로 확인할 수 있다.
2. 프로젝트 루트에서 다음 명령으로 최근 로그를 검색한다.

```powershell
docker compose logs --since 30m backend frontend | Select-String '복사한-문의-코드'
```

Nginx → 백엔드 → 오류 JSON → 화면에서 같은 요청 ID를 사용한다. Nginx 자체의 502 응답에도 ID가 붙는다.
네트워크 단절처럼 서버 응답을 받지 못한 경우에는 문의 코드가 없을 수 있다.
인증/CSRF 오류도 `{status, title, detail, code, requestId}` 형태로 응답하며, 입력 검증 오류에는
기존 `fieldErrors`가 추가된다. 비밀번호 오류는 401, 권한/CSRF 오류는 403, 데이터 충돌은 409로 구분한다.

## 기록하는 로그

| 이벤트 | 내용 | 확인할 상황 |
| --- | --- | --- |
| `request_completed` | HTTP 메서드, 라우트 템플릿, 상태, 처리 시간 | 느린 요청과 실패 요청 |
| `unexpected_error` / `request_unhandled` | 예외 타입과 제한된 호출 위치 | 예상하지 못한 서버 오류 |
| `data_conflict` | 충돌 예외 타입 | 동시 수정, 제약 조건 위반 |
| `business_change` | 작업 종류, 사용자 ID, 대상 ID | 차량/소모품/교체 이력 등의 변경 |
| `notification_job_started/completed` | 작업 ID, 스캔·생성·건너뜀·실패 수, 소요 시간 | 정기 알림 작업 |
| `notification_item_failed` / `notification_job_aborted` | 대상 ID 또는 중단 위치의 진단 정보 | 알림 일부 실패 또는 작업 중단 |

변경 로그는 **DB 트랜잭션 커밋 후** 기록한다. 롤백된 요청을 성공으로 기록하지 않는다.
단, 로그와 DB가 원자적으로 저장되는 감사 시스템은 아니다. 장애 시 로그 유실 가능성이 있으며,
법적 감사·장기 이력 보존이 필요하다면 별도 저장·수집 정책이 필요하다. 정비 교체 이력 자체는 DB에 저장한다.

정상 GET 요청은 DEBUG, 변경 요청과 일반 4xx는 INFO, 느린 요청은 WARN, 5xx는 ERROR로 기록한다.
Docker 백엔드는 ECS JSON 형식이며 `requestId`/`jobId`가 별도 필드로 들어간다.
업무 이벤트와 HTTP 세부 항목은 JSON의 `message` 안에 `key=value` 형태로 남는다.
로컬 개발은 읽기 쉬운 텍스트 형식을 유지한다.

## 민감 정보와 로그 보관

직접 작성한 요청·업무 로그에는 비밀번호, 이메일, 요청 본문, 쿠키, 구매 URL과 쿼리 문자열을 남기지 않는다.
예외 메시지에도 SQL 입력값이 포함될 수 있어, 예상하지 못한 오류는 예외 종류와 호출 위치만 제한적으로 기록한다.
같은 값을 별도로 출력할 수 있는 Hibernate/MariaDB의 일부 진단 로거도 비활성화했다.
이 때문에 원인 분석에는 재현 테스트가 필요할 수 있다. 운영에서 SQL 바인딩 TRACE나 본문 로그를 켜지 않는다.
프레임워크 전체의 디버그 로그나 Nginx 자체 오류 로그까지 완전히 비식별화된다는 보장은 없다.
사용자/대상 ID도 접근을 제한해야 하는 운영 정보다.

네 컨테이너 모두 Docker `local` 드라이버와 `max-size=10m`, `max-file=3` 회전을 사용한다.
각 컨테이너의 로그 파일 보관을 약 30MB 단위로 제한하는 설정이며, 압축·메타데이터 등 실제 디스크 사용량은 다를 수 있다.
DB/Redis 데이터 볼륨 크기를 제한하는 설정은 아니다. 컨테이너 재생성 시 이전 로그는 사라질 수 있으므로
장애 로그를 보관해야 한다면 **재배포 전에** 안전한 별도 위치로 내보낸다.

## 알림 작업 구조

- 시작 시 최대 소모품 ID를 고정하고, 기본 100개씩 ID 커서로 조회한다. 전체 엔티티를 한 번에 메모리에 올리지 않는다.
- 소모품 한 건마다 별도 서비스 트랜잭션에서 처리한다. 한 건 실패해도 나머지 처리를 계속한다.
- 소모품 행 잠금과 기존 중복 키를 함께 사용한다. 여러 작업이 겹쳐도 같은 알림이 중복 생성되지 않게 한다.
- 개별 실패는 `failed` 수와 대상 ID로 남는다. 조회 단계 실패는 작업을 중단하고 `notification_job_aborted`를 기록한다.
- 자동 즉시 재시도 큐는 없다. 실패한 항목은 다음 정기 실행에서 다시 평가한다.
- 날짜 기준 만료와 거리 기준 초과를 구분하며, 0km는 '교체 시점 도달', 음수는 양수의 '초과'로 안내한다.
- 계산·입력 날짜 검증·스케줄 모두 같은 `Clock` 시간대를 사용한다. 기본은 `Asia/Seoul`이다.

## 환경 설정

`.env.example`을 참고해 필요한 값만 `.env`에서 변경한다. 설정 변경 후에는 해당 컨테이너를 재생성해야 한다.
로그 드라이버 변경은 네 서비스에 모두 적용해야 하며 DB/Redis 재시작 동안 잠시 접속이 끊길 수 있다.
기존 데이터 볼륨은 유지한다. 로그 설정을 바꾸려고 `down -v`를 실행하지 않는다.

| 변수 | 기본값 | 의미 |
| --- | --- | --- |
| `APP_LOG_FORMAT` | `ecs` | Docker 백엔드 JSON 로그 형식; 다른 프로그램의 `LOG_FORMAT`과 분리 |
| `APP_LOG_LEVEL` | `INFO` | 애플리케이션 로그 수준; 상세 GET 추적은 `DEBUG` |
| `SLOW_REQUEST_MS` | `1000` | 느린 요청 WARN 기준, 밀리초 |
| `APP_TIME_ZONE` | `Asia/Seoul` | 업무 날짜와 스케줄 기준 |
| `NOTIFICATION_BATCH_SIZE` | `100` | 한 번에 조회할 ID 수, 1~1000 |
| `NOTIFICATION_CRON` | `0 0 9 * * *` | 매일 오전 9시; `-`로 비활성화 가능 |

```powershell
docker compose config --quiet
docker compose up -d --build --wait
docker compose ps
docker compose logs --since 10m backend
```

프록시 설정은 API·Swagger에 공통으로 적용한다. 업스트림 연결 재사용(keepalive)과 Docker DNS 재조회로
연결 생성 비용을 줄이고 백엔드 컨테이너 IP 변경에 대응한다.
이는 이전 연결 고갈 문제를 완화하기 위한 설정이며, 100명 규모의 성능 목표 달성을 증명하지는 않는다.
`scripts/load-test.mjs`는 데이터를 새로 생성하므로 운영 DB에서 무심코 반복 실행하지 않는다.

## 회귀 검증

```powershell
pnpm --dir frontend lint
pnpm --dir frontend test
pnpm --dir frontend build
cd backend
.\gradlew.bat test integrationTest --console=plain
```

인증·권한·교체 이력 테스트 외에 요청 ID 정합성, 민감 정보 노출 방지, 커밋 후 로그,
알림 실패 격리와 날짜/주행거리 경계값을 검증한다. 통합 테스트는 별도 MariaDB·Redis 컨테이너에서
동시 교체 처리와 알림 중복 방지를 검사하며, 개발용 서비스의 기존 데이터를 지우지 않는다.

### 2026-10-11 검증 결과

- 백엔드 회귀 테스트 27개와 실제 MariaDB·Redis 통합 테스트 3개 통과.
- 프런트엔드 테스트 13개, 린트와 프로덕션 빌드 통과.
- Docker 이미지 빌드, Compose 설정 검사, Nginx 설정 검사 통과 후 재배포.
- 배포 환경에서 홈·로그인·차량/소모품/알림 조회·잘못된 입력·Swagger 등 HTTP 검사 13건 통과.
- 401/403/400/404 응답의 헤더와 JSON 요청 ID 일치 확인. Nginx와 백엔드 로그에서도 같은 ID 확인.
- 테스트용 비밀번호·이메일·쿼리 값이 해당 요청의 실제 로그에 기록되지 않는 것을 확인.
- 네 서비스의 로그 드라이버/회전 설정 적용과 백엔드 헬스 `UP` 확인.
- 기존 DB 볼륨 유지. 100명 동시접속 부하 테스트는 이번 검증에 포함하지 않았다.

배포 중 PC에 설정되어 있던 `LOG_FORMAT=json`이 Spring Boot 로그 형식과 충돌하는 문제를 발견해
프로젝트 설정을 `APP_LOG_FORMAT`으로 분리했다. 전역 환경 변수는 변경하지 않았다.

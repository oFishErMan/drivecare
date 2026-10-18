# DriveCare

차량 주행거리와 소모품 교체주기를 관리하는 셀프호스팅 웹 애플리케이션입니다.

소스 구조와 기능 확장 방법은 [유지보수 가이드](docs/ARCHITECTURE.md)를 참고하세요.
화면별 문제와 개선 이유는 [화면 검토 기획서](docs/PRODUCT_REVIEW.md), [디자인·개발 인계서](docs/DESIGN_HANDOFF.md)에 정리했습니다.
요청 추적, 로그 확인, 알림 작업과 운영 설정은 [로그·운영 가이드](docs/OPERATIONS.md)를 참고하세요.
구매 링크·교체 이력·사용 지표와 수익화 검증 범위는 [BM 구현 가이드](docs/BUSINESS_MODEL.md)에 정리했습니다.

## 주요 기능

- 세션 기반 회원 인증과 Redis 세션 저장
- 차량 및 현재 계기판 주행거리 관리
- 엔진오일 등 소모품의 거리·기간 교체주기 관리
- 남은 주행거리와 예상 교체일 계산
- 정기 작업을 통한 교체 임박·초과 알림 생성
- 브라우저 알림 표시
- Docker Compose 기반 단일 명령 배포

## 기술 구성

- Backend: Java 17, Spring Boot 4.1.1, Spring Security, Spring Session Data Redis, JPA, Flyway, OpenAPI
- Frontend: React 19.3, Vite 8.3, Nginx
- Data: MariaDB 11.8, Redis 8.2
- Test/Delivery: JUnit 6, MockMvc, Testcontainers, GitHub Actions, Docker Compose

## 포트폴리오 핵심 기술과 선택 이유

### 1. Redis 기반 서버 세션

인증 정보는 브라우저에 직접 담지 않고 `HttpOnly` 세션 쿠키와 Redis에 저장합니다.
애플리케이션 인스턴스를 재시작하거나 여러 대로 늘려도 같은 세션 저장소를 사용할 수 있습니다.
JWT보다 즉시 로그아웃과 세션 만료 처리가 단순하다는 장점이 있어, 관리자 화면이나 일반 웹 서비스에
자주 사용하는 방식입니다.

### 2. JPA 트랜잭션과 비관적 잠금

계기판 주행거리는 이전 값보다 작아질 수 없습니다. 동시에 두 요청이 들어왔을 때 두 요청 모두 이전 값을
읽는 경쟁 상태를 막기 위해 차량 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 검증하고 저장합니다. 전체 테이블이
아닌 수정 대상 차량 한 행에만 잠금을 적용해 정합성과 처리량 사이의 균형을 잡았습니다.

### 3. Flyway 스키마 버전 관리

테이블과 인덱스를 애플리케이션 시작 시 임의로 생성하지 않고 SQL 마이그레이션 이력으로 관리합니다.
로컬·테스트·운영 환경이 같은 스키마 변경 순서를 따르며, 이미 실행된 변경은 다시 실행되지 않습니다.

### 4. RFC 9457 Problem Details 오류 응답

잘못된 주행거리, 존재하지 않는 차량, 입력값 검증 실패를 HTTP 상태 코드와 `ProblemDetail` JSON으로
일관되게 반환합니다. 검증 실패에는 필드별 오류를 포함하므로 프런트엔드가 문자열을 분석하지 않고
사용자에게 정확한 안내를 보여줄 수 있습니다.

### 5. OpenAPI와 Swagger UI

실행 중인 컨트롤러를 기준으로 API 명세를 생성합니다. 프런트엔드 개발자나 리뷰어가 별도 문서 없이
요청·응답 구조를 확인할 수 있습니다.

- Swagger UI: `http://localhost:3000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:3000/v3/api-docs`

### 6. 요청 추적 ID

모든 응답에 `X-Request-ID`를 발급하고 같은 값을 서버 로그의 MDC에 기록합니다. 사용자가 전달한 오류
응답과 서버 로그를 연결해 장애 요청 한 건을 빠르게 찾을 수 있으며, 외부 모니터링 제품 없이도 활용할 수
있는 가벼운 운영 기능입니다.

### 7. 실제 인프라 통합 테스트

빠른 API 회귀 테스트는 H2로 실행하고, 별도 `integrationTest`에서는 Testcontainers가 MariaDB와 Redis를
일회성으로 실행합니다. 실제 DB에서 Flyway SQL과 Redis 연결을 확인해 H2와 MariaDB의 문법·동작 차이로
생길 수 있는 배포 오류를 줄입니다.

### 8. CI와 컨테이너 배포

GitHub Actions가 백엔드 테스트, 실제 인프라 통합 테스트, 프런트엔드 린트·빌드, Docker Compose 문법을
검증합니다. 실행 환경은 Docker Compose로 통일하고 각 서비스의 헬스체크가 통과한 뒤 의존 서비스를
시작합니다.

이 프로젝트는 현재 규모에서 Kafka, Kubernetes, 마이크로서비스를 사용하지 않습니다. 단일 애플리케이션과
하나의 데이터베이스로 충분한 문제에 복잡한 운영 요소를 추가하지 않는 것도 기술 선택의 일부입니다.

## Docker 실행

1. 환경 변수 파일을 준비합니다.

```powershell
Copy-Item .env.example .env
```

2. `.env`의 비밀번호를 변경합니다.

3. 전체 애플리케이션을 실행합니다.

```powershell
docker compose up -d --build
```

4. 브라우저에서 `http://localhost:3000`을 엽니다.

```powershell
docker compose ps
docker compose logs -f backend
docker compose down
```

데이터까지 완전히 제거하려는 경우에만 `docker compose down -v`를 사용합니다.

## 로컬 개발

프로젝트의 Docker MariaDB와 Redis를 실행합니다. Windows에 별도로 설치된 MariaDB(3306)와 충돌하지 않도록 DB는 `127.0.0.1:13306`, Redis는 `127.0.0.1:16379`로 접속합니다. 두 포트는 로컬 PC에만 공개됩니다.

```powershell
docker compose up -d mariadb redis
```

VS Code에서 F5를 누르고 백엔드 실행 구성을 선택하면 `local` 프로필이 적용됩니다. 프로젝트 전체 폴더와 `backend` 폴더 양쪽에 실행 구성이 있습니다. 확장 프로그램이 생성한 기본 Run 대신 이 구성을 선택하세요. `.env`에서 비밀번호를 변경했다면 동일한 값을 로컬 실행 환경에도 전달해야 합니다.

터미널에서 실행할 때도 `local` 프로필을 지정합니다.

```powershell
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

프런트엔드는 별도의 터미널에서 실행합니다.

```powershell
cd frontend
pnpm install
pnpm dev
```

Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 전달합니다.

`localhost:3000`은 Docker 백엔드에 연결된 배포 화면입니다. VS Code에서 수정한 백엔드와 함께 확인하려면 `pnpm dev`로 실행한 `localhost:5173`을 사용하세요.

## 테스트

빠른 백엔드 회귀 테스트는 Docker 없이 실행할 수 있습니다.

```powershell
cd backend
.\gradlew.bat test
```

Docker가 실행 중이면 실제 MariaDB와 Redis를 사용하는 통합 테스트도 실행할 수 있습니다.

```powershell
.\gradlew.bat integrationTest
```

프런트엔드 정적 검사와 프로덕션 빌드는 다음과 같이 실행합니다.

```powershell
cd frontend
pnpm lint
pnpm build
```

## 세션 구조

로그인 성공 시 `DRIVECARE_SESSION` 쿠키가 발급되고 실제 세션 데이터는 Redis의
`drivecare:session` 네임스페이스에 저장됩니다. 세션 기본 만료 시간은 30분이며
`SESSION_TIMEOUT` 환경 변수로 변경할 수 있습니다.

## 알림 계산

- 거리 기준: `교체 당시 주행거리 + 교체주기(km) - 현재 주행거리`
- 날짜 기준: `마지막 교체일 + 교체주기(개월)`
- 기본적으로 매일 오전 9시(Asia/Seoul)에 교체 임박 또는 초과 알림을 생성합니다.
- 브라우저 알림은 사용자가 알림 권한을 허용하고 웹 페이지가 열려 있을 때 표시됩니다.

서비스 워커 기반의 백그라운드 Web Push는 별도의 VAPID 키와 HTTPS 구성이 필요하므로 현재 범위에는 포함하지 않았습니다.

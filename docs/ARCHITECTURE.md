# DriveCare 유지보수 가이드

## 설계 원칙

현재 규모에 맞게 기능별 패키지와 작은 서비스 계층을 사용한다. 별도 마이크로서비스,
불필요한 서비스 인터페이스, 범용 추상 클래스는 도입하지 않았다.
리팩터링은 API URL·JSON 형식·DB 스키마·정비 계산 기준을 유지한다.

## 백엔드

`HTTP → Controller → Service → Repository / Entity`

- `*Controller`: 요청 검증(`@Valid`), 로그인 사용자 식별, HTTP 상태/응답 전달.
- `*Service`: 소유권 확인, 등록/수정 업무 규칙, 트랜잭션 경계. 컨트롤러에 의존하지 않는다.
- `*Dtos`: 기능별 요청/응답 record. 엔티티를 그대로 API에 노출하지 않는다.
- `*Repository`: 데이터 조회와 잠금. 주행거리 갱신의 기존 비관적 잠금을 유지한다.
- `MaintenanceStatusCalculator`: 정비 시점 계산. 날짜를 인자로 받아 경계값을 독립적으로 테스트한다.

`auth`, `vehicle`, `maintenance`, `notification` 패키지가 각각 해당 기능을 담당한다.
로그인 성공 시 세션 ID 변경과 SecurityContext 저장은 HTTP 요청에 결합된 처리이므로
`AuthController`에 유지하고, 계정 등록·비밀번호 검증·사용자 조회는 `AuthService`가 맡는다.
로그아웃은 기존 Spring Security 설정에서 처리한다.

조회 서비스에는 `@Transactional(readOnly = true)`, 변경 서비스에는 `@Transactional`을 둔다.
차량 생성과 최초 주행거리 기록은 한 트랜잭션이며, 응답 DTO 변환도 트랜잭션 내부에서 수행한다.
다른 사용자의 차량/알림은 소유자 조건이 포함된 조회로 제한한다.

### 수정 위치 예시

- 정비 임박 기준 변경: `maintenance/MaintenanceStatusCalculator.java`와 계산기 테스트.
- 차량 수정 API 추가: `vehicle/VehicleDtos.java` → `VehicleService` → `VehicleController`.
- 데이터 구조 변경: 엔티티 변경과 함께 새 Flyway migration을 추가. 기존 migration은 수정하지 않는다.
- 권한/세션 정책 변경: `security/SecurityConfig.java` 및 인증 회귀 테스트.

## 프런트엔드

- `App.jsx`: 화면 조합과 로그인/로딩/대시보드 분기.
- `features/dashboard/DashboardTabs.jsx`: 정비 현황·차량 관리·소모품 등록·알림 탭과 키보드 이동.
- `hooks/useDashboardTabs.js`: URL 해시 기반 화면 전환, 기존 바로가기와 뒤로/앞으로 가기 지원.
- `features/`: 로그인, 차량, 소모품, 알림, 대시보드별 화면과 동작.
- `components/`: 아이콘, 입력 필드, 안내 메시지 등 공통 UI.
- `hooks/useDashboard.js`: 세션 복원, 주기적 조회, 로그아웃, 대시보드 상태.
- `hooks/useSaveFeedback.js`: 등록 중 상태와 성공/실패 피드백.
- `domain/maintenance.js`: 소모품 종류·아이콘·상태 문구의 단일 정의 및 표시 함수.
- `domain/forms.js`: 폼 문자열을 API용 숫자/null로 명시적으로 변환. 필드가 늘면 해당 payload 함수를 수정한다.
- `api.js`: HTTP 통신, CSRF, 타임아웃, 오류 안내, 세션 만료 이벤트.
- `.prettierrc.json`: 코드 스타일 기준. VS Code의 Prettier에서도 같은 기준을 사용한다.

소모품 종류를 추가하면 백엔드의 `MaintenanceCategory`와 프런트엔드의
`MAINTENANCE_CATEGORIES`를 함께 수정한다. 등록 옵션·카드·알림의 이름과 아이콘은
이 공통 메타데이터를 사용하므로 각각 따로 수정하지 않아도 된다.

## 검증

프로젝트 루트에서:

```powershell
pnpm --dir frontend lint
pnpm --dir frontend test
pnpm --dir frontend build
cd backend
.\gradlew.bat test integrationTest --console=plain
```

프런트엔드 테스트는 HTTP 오류 처리, 폼 타입 변환, 초과 주행거리 표시를 검증한다.
백엔드 테스트는 인증/입력 검증/소유권 격리/주행거리 감소 금지/정비 계산 경계값을 검증한다.
`integrationTest`는 Docker 실행이 필요하며 별도 MariaDB·Redis 컨테이너를 사용한다.
운영 개발 DB의 테스트 계정이나 기록을 삭제하지 않는다.

Nginx 업스트림 연결 재사용과 Docker DNS 재조회 설정을 적용했다. 기존 부하 테스트에서 발견한
연결 고갈 문제의 완화 조치이며, 100명 규모 성능 목표 달성 여부는 별도 부하 재검증이 필요하다.
미래 교체일 검증은 구매 링크·교체 이력 기능을 추가하면서 등록/교체 API에 적용했다.

구매 링크, 교체 이력, 방문·클릭 지표는 [BM 구현 가이드](BUSINESS_MODEL.md)를 참고한다.

## 운영 기능의 책임 분리

- `web/RequestIdFilter`: 보안 필터 이전에 요청 ID 발급, 요청 결과와 소요 시간 기록, MDC 정리.
- `web/ApiProblems`: 보안 계층과 컨트롤러가 공유하는 오류 형식과 추적 코드.
- `web/ApiExceptionHandler`: 예외별 상태 코드, 안전한 사용자 메시지와 서버 진단 로그.
- `observability/OperationLog`: 실제 커밋된 변경만 로그로 남기는 트랜잭션 콜백.
- `observability/ExceptionDiagnostics`: 입력값이 섞일 수 있는 예외 메시지 대신 종류와 호출 위치 기록.
- `config/TimeConfig`: 업무 계산·입력 검증·스케줄에 일관된 시간대 제공.
- `notification/MaintenanceNotificationScheduler`: ID 단위 분할 조회와 실행 통계. 업무 트랜잭션을 소유하지 않는다.
- `notification/MaintenanceNotificationService`: 한 건의 잠금·중복 확인·알림 저장 트랜잭션.
- `notification/NotificationMessage`: 알림 문구 구성과 경계값 처리. 기간 임박 기준은 계산기 상수를 공유한다.

로그 조회 명령, 회전 정책, 환경 변수와 제약은 [운영 가이드](OPERATIONS.md)를 참고한다.

## 소모품 삭제 정책

`DELETE /api/maintenance-items/{id}?expectedRevision={revision}`은 소유권·CSRF·변경 버전을 확인하고
대상 행을 잠근 뒤 `active=false`로 처리한다. DB의 교체 이력과 클릭 기록은 보존한다.
활성 목록/상태 요약/알림 목록에서 제외하고 미확인 알림을 읽음 처리하며, 정기 작업도 비활성 항목을 건너뛴다.
삭제 후 링크 수정·교체 기록 추가·이력 조회는 404로 거절한다. UI에는 영향 범위를 설명하는 확인창을 제공한다.
현재 사용자용 복구 화면이나 영구 파기 기능은 제공하지 않는다.
프런트엔드 `SelectField`는 입력 필드와 같은 라벨 구조를 사용해 필수 표시가 별도 그리드 행으로 분리되지 않게 한다.

검증: 백엔드 32개(실제 인프라 3개 포함), 프런트엔드 14개 테스트 통과. 배포 후 임시 소모품 한 건으로
등록 → 변경 → 오래된 버전 삭제 409 → 정상 삭제 204 → 재삭제/이력 조회 404를 확인했다.
기존 사용자 소모품은 삭제하지 않았다. 데스크톱 선택란 높이·상단 정렬, 390px 모바일 넘침 여부,
삭제 확인창의 취소 버튼·Esc 동작도 확인했다.

## 탭형 화면

기본 화면은 정비 현황(`#maintenance-status`)이며 차량 관리(`#vehicles`), 소모품 등록(`#maintenance-form`),
알림(`#notifications`)을 각각 분리했다. 기존 `#vehicle-form` 주소는 차량 관리 탭의 등록 패널을 연다.
`VehicleRegistrationDrawer`는 native dialog의 포커스·모달 동작과 열기/닫기 모션, 배경 스크롤 복구를 담당한다.
`VehicleForm`은 닫아도 마운트를 유지해 작성 중인 값을 보존하며, 입력·검증·저장은 기존 폼 책임으로 둔다.
탭 패널은 숨기되 마운트를 유지하므로 탭 전환/차량 폼 접기로 입력 중인 내용이 사라지지 않는다.
페이지 새로고침이나 로그아웃 시에는 임시 입력을 보존하지 않으며 브라우저 저장소에 개인정보를 저장하지 않는다.
차량 등록 후 차량 관리, 소모품 등록 후 정비 현황으로 이동하며 성공 메시지는 공통 영역에서 표시한다.
탭은 `tablist`/`tab`/`tabpanel`, 선택 상태, 방향키·Home·End 이동을 제공한다.
탭 클릭은 History API를 사용해 기본 앵커 이동이 키보드 포커스를 빼앗지 않게 한다.

## 기획에 따른 화면 개선

현재 화면 검토와 판단 근거는 [화면 기획서](PRODUCT_REVIEW.md), 시각·상호작용 기준은
[디자인·개발 인계서](DESIGN_HANDOFF.md), 반영 위치와 검수 범위는 [반영 결과](UI_REVIEW_DELIVERY.md)를 참고한다.
조회·정렬·날짜 표현은 `domain/maintenanceView.js`에 두고, 각 탭의 복잡한 렌더링은
`MaintenanceOverview`와 `NotificationCenter`로 분리했다. 폼 입력 오류는 공통 필드와 오류 전달 계층을 공유한다.

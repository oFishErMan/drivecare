# 기획·디자인·개발 반영 및 검수 결과

기준일: 2026-10-15 · 대상: 로컬 Docker DriveCare · 문서 상태: 구현/검수 완료

## 역할별 산출물

기획자 역할로 현재 화면과 상태를 조사하고 문제·영향·우선순위·완료 기준을
[PRODUCT_REVIEW.md](PRODUCT_REVIEW.md)의 UX-01~18에 기록했다.
디자이너 역할로 정보 순서, 버튼 위계, 상태 표현, 모바일 배치와 인터랙션을
[DESIGN_HANDOFF.md](DESIGN_HANDOFF.md)에 명세했다.
개발자 역할로 해당 명세를 React 컴포넌트와 순수 조회/표시 함수에 반영하고 프런트엔드 이미지를 재배포했다.
외부 사람에게 문서를 발송하거나 실제 사용자 조사를 수행한 것은 아니다.

## 요구사항 추적

| 기획 ID | 반영 위치 | 결과 |
| --- | --- | --- |
| UX-01~03 | MaintenanceOverview, domain/maintenanceView | 필요/임박/정상 우선 정렬, 차량·상태·검색 교집합, 모수와 결과 수 구분 |
| UX-04~05 | MaintenanceCard, maintenanceTiming | 기간 전용·만료 일수, 거리 기준 보조 표시, 기간일/주행 추정일 출처 구분 |
| UX-06~07 | MaintenanceCard, MaintenanceActions | 차량명·현재 주행거리·최근 교체 표시, 차량 이동, 기록/이력/관리 행동 위계 |
| UX-08 | EmptyState, MaintenanceOverview, NotificationCenter | 차량/소모품 없음, 조회 결과 없음, 미확인 알림 없음의 다음 행동 |
| UX-09 | App의 차량 등록 핸들러, VehicleCard, MaintenanceForm | 차량 카드에서 선택 전달, 다른 차량의 작성 내용을 바꾸기 전 확인 |
| UX-10 | MaintenanceForm | 빈 교체주기, 예시와 권장값 구분, 구매 링크 접기, 현재 주행거리 상한 |
| UX-11~12 | NotificationCenter, NotificationRow | 권한 상태와 열림 조건 안내, 전체/미확인, 관련 현재 소모품 이동 |
| UX-13 | api.js, useSaveFeedback, 공통 Field/SelectField, 각 폼 | fieldErrors 전달, 오류 연결과 첫 필드 포커스, 입력 유지 |
| UX-14 | AuthScreen | 모드별 폼 이름, 입력 키/자동완성, 오류/처리 중 안내와 표시 전환 유지 |
| UX-15 | useDashboard, DashboardHero | 최근 성공 갱신 시각, 수동 갱신, 오래된 조회 결과/오류의 덮어쓰기 방지 |
| UX-16 | experience.css, DashboardTabs/패널 이동 | 주요 터치 영역, 모바일 배치, 본문 바로가기·포커스, 가로 넘침 방지 |
| UX-17 | MaintenanceDelete | 기존 확인 문구·취소·소프트 삭제 정책 검토/유지; 새 복구 기능은 추가하지 않음 |
| UX-18 | domain/maintenance | 서버 enum 12종과 등록 옵션·카드·검색·알림 이름 일치 |

## 검증 결과와 범위

- `pnpm lint`: 경고/오류 없이 통과.
- `pnpm test`: 26개 통과. 조회 교집합, 정렬 원본 보존, 알림 대상 ID, 기간·거리 경계, 필드 오류 전달 포함.
- `pnpm build`, Docker 프런트엔드 이미지 빌드·배포 통과.
- Vite SSR로 차량 없음/소모품 없음 분기를 렌더링해 각각의 시작 버튼을 확인. 실제 새 계정 생성은 하지 않음.
- 실행 화면에서 교체 필요 항목 우선, 기간 전용 배터리 일수, 차량 표시, 필터+검색과 초기화 확인.
- 실행 화면에서 알림 미확인 없음→전체 보기, 관련 소모품으로 이동, 차량 카드→등록과 작성 보존 확인.
- 필수 주기를 비운 제출에서 오류 문구, `aria-invalid`/`aria-describedby`, 첫 오류 필드 포커스 확인.
- 교체 이력 실제 조회, 교체/링크 폼 열기·취소, 주행거리 갱신 폼, 삭제 확인창 취소·Esc 확인.
- 인증 화면의 로그인/가입 전환과 이름·이메일·비밀번호 필드 배치/이름을 확인. 실제 신규 가입/비밀번호 변경은 하지 않음.
- 360px에서 페이지 너비 345px로 가로 넘침 없음, 탭 높이 66px·상태 필터 높이 70px 확인.
- 작은 화면에서 제목·필터를 압축하는 추가 디자인 검수를 반영. 기본 데이터의 첫 카드 상단이 약 599px에 위치.
- 390px에서도 등록 폼 12종 옵션·선택 차량·가로 넘침 없음, 주행거리 폼 취소, 관련 차량 이동 확인.
- Home/End 이동 후 선택 탭과 포커스 일치, 최종 실행 화면의 브라우저 오류 로그 없음 확인.

백엔드 API와 DB 스키마는 변경하지 않았다. 이번에는 정비 기록을 새로 저장하거나 기존 소모품을 삭제하지 않았다.
OS 알림 허용/차단 전환은 실제로 변경하지 않았으며 해당 분기는 소스 검토 범위다.
대규모 데이터의 성능·100명 부하·실제 이용자 사용성 개선 수치는 이번 결과에 포함하지 않는다.

## 인수 후 남은 별도 정책

차량 수정/매각, 정비 이력 정정/복구, 계정 재설정/탈퇴, 페이지 종료 후 Web Push는 별도 정책과
API 작업이 필요하다. 이번 화면 개선 완료와 구분해 기획서의 후속 과제에 남겼다.

## 구현 유지보수

- `domain/maintenanceView.js`: 조회·정렬·표시 근거. 서버 상태 계산을 대체하지 않는다.
- `domain/forms.js`: 새 폼 초기값과 API payload 형 변환.
- `MaintenanceOverview`, `NotificationCenter`: 화면별 조회 상태와 안내.
- `experience.css`: 이번 기획의 시각 규칙. 기존 App.css를 추가로 확장하지 않고 분리했다.
- 로그인 사용자 ID별 DashboardContent 마운트: 탭 이동 시 작성은 보존하고 로그아웃/다른 사용자에서는 초기화.
- 스타일만 수정한 경우에는 전체 DB 테스트를 반복하기보다 빌드와 실제 화면 검수를 우선한다.

## 최종 화면

- [데스크톱 검수 화면](screenshots/ux-review-desktop-20261015.jpg)
- [모바일 검수 화면](screenshots/ux-review-mobile-20261015.jpg)

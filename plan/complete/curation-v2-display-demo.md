# Plan: Curation V2 Display Demo

## Overview

Bottlenote repository 안에 curation demo 프로젝트의 `display` 정적 데모 화면을 이식한다. 목적은 spec 기반 curation v2의 Admin/Product API 계약이 실제 화면 흐름에서 올바르게 동작하는지 확인하는 것이다.

데모는 애플리케이션 빌드/배포 산출물에 포함하지 않고, `.example/display` 아래에 둔다. 원본 curation demo의 화면 구조와 UX는 가능한 유지하되, 더 나은 표현이 필요한 부분은 현재 Bottlenote curation v2 API 계약에 맞춰 조정한다. API adapter는 데모 서버 계약이 아니라 현재 Bottlenote 프로젝트의 Admin/Product endpoint와 응답 형식을 기준으로 작성한다.

## Assumptions

- `.exmale/display`는 오타이며 최종 대상 경로는 `.example/display`이다.
- 구현 시작 시 Bottlenote repository는 `main` 기준 최신으로 전환하고 `git pull`, submodule update를 먼저 수행한다.
- `git.environment-variables` submodule도 최신 main revision으로 맞춘 뒤 작업한다.
- 원본 curation demo의 디자인과 화면 흐름은 가능한 유지한다. 다만 Bottlenote 응답 구조를 더 명확하게 보여주기 위한 UI 조정은 허용한다.
- 환경 전환은 화면 선택 UI를 만들지 않고 config 파일 하나의 active 값으로 제어한다.
- 지원 환경은 `local`, `development` 두 개다.
- 각 환경 config에는 product/admin base URL, admin login credential, active flag를 둔다.
- admin token은 데모 페이지가 자동 로그인으로 로딩해 보관하고 이후 admin API 요청에 자동으로 붙인다.
- local/development 서버 기동 여부는 데모가 책임지지 않는다. 데모는 API 호출 실패를 화면에 명확히 표시한다.
- 데모는 정적 파일이며 Spring Boot runtime, Gradle build artifact, API 문서 산출물에는 포함하지 않는다.
- 현재 `.gitignore`에는 `.example/`가 없으므로, 구현 단계에서 `.example/`를 ignore 대상으로 추가해야 한다.

## Success Criteria

- `.example/display` 아래에서 정적 데모 페이지가 열리고 원본 display의 주요 화면을 제공한다.
- config 파일에서 `active` 환경을 `local` 또는 `development`로 바꾸면 같은 화면이 해당 API base URL을 사용한다.
- development 환경에서 admin login이 자동 수행되고 access token이 이후 Admin API 요청의 `Authorization: Bearer` 헤더에 자동 반영된다.
- 스펙 목록 화면에서 `GET /admin/api/v2/curation-specs` 응답의 3개 spec이 표시된다.
- 스펙 상세 또는 카드 확장 영역에서 `requestSpec`, `responseSpec`, `hydratorKey`, `version`, `isActive`가 확인 가능하다.
- 큐레이션 목록 화면에서 Admin `GET /admin/api/v2/curations`와 Product `GET /api/v2/curations`의 결과 차이를 구분해 확인할 수 있다.
- 큐레이션 생성 화면은 현재 Bottlenote Admin create contract에 맞는 payload를 생성한다.
- 생성 요청 실패 시 HTTP status, 서버 error code/message, validation 실패 위치를 화면에 표시한다.
- 생성 성공 후 상세 화면에서 Admin detail payload와 Product detail materialized payload를 비교해 볼 수 있다.
- Product 응답 payload가 `responseSpec`의 의도와 맞는지 사람이 확인할 수 있도록 raw JSON과 모바일 미리보기를 함께 제공한다.
- local 서버가 꺼져 있거나 CORS/auth/API 오류가 발생하면 빈 화면이 아니라 원인 진단 메시지를 표시한다.
- `.example/` 경로는 git에 추적되지 않도록 ignore 처리된다. 단, 필요하면 별도 문서에 실행 방법만 추적한다.

## Impact Scope

- Repository state:
  - 작업 시작 전 `main` 최신화와 submodule 최신화가 필요하다.
  - 기존 `feat/curation` 브랜치 상태에서 바로 구현하지 않는다.

- Static demo files:
  - 새 경로: `.example/display/**`
  - 원본 참조: `/Users/hgkim/workspace/etc/curation_demo/curation_demo/display/**`
  - 주요 구성: HTML, CSS, JS modules, widgets, environment config, API adapter

- API surfaces:
  - Admin login: `/admin/api/v1/auth/login`
  - Admin specs: `/admin/api/v2/curation-specs`, `/admin/api/v2/curation-specs/{specId}`
  - Admin spec-based curation: `/admin/api/v2/curations`, `/admin/api/v2/curations/{curationId}`
  - Product curation v2: `/api/v2/curations`, `/api/v2/curations/{curationId}`

- Security:
  - credential은 static config에 들어가므로 이 데모는 local/development 전용이다.
  - production credential이나 production base URL은 포함하지 않는다.
  - token은 브라우저 메모리 또는 sessionStorage 수준으로만 보관하고 파일에 쓰지 않는다.

- Persistence/schema:
  - DB schema 변경 없음.
  - seed/migration 변경 없음.
  - 데모를 통한 생성 요청은 development/local DB에 실제 데이터를 만들 수 있으므로 화면에서 대상 환경을 명확히 표시해야 한다.

- Build/deploy:
  - Gradle module 변경 없음.
  - Spring Boot app packaging 변경 없음.
  - 배포 workflow 변경 없음.

- Tests/verification:
  - 정적 파일 문법 검증 또는 간단한 smoke 실행이 필요하다.
  - 가능하면 local static server로 페이지를 열고 development API 대상 스펙 목록 조회, 로그인, curation 목록 조회를 확인한다.
  - 브라우저 기반 전체 검증은 필요 시 별도 단계로 수행한다.

- Documentation:
  - `.example/`가 ignore되면 repo에 데모 실행 문서를 남길 별도 위치가 필요할 수 있다.
  - 실행 문서에는 config 전환 방법, local/development base URL, 자동 로그인 동작, 주의사항을 적는다.

## Tasks

### Task 1: Repository baseline sync
- Acceptance: 작업 브랜치가 최신 `main` 기준에서 시작되고, `git.environment-variables` submodule도 최신 main revision을 가리킨다.
- Acceptance: 동기화 후 기존 curation v2 API 파일과 계획 문서가 사라지지 않는다.
- Verification: `git status --short --branch`, `git rev-parse --short HEAD`, `git submodule status --recursive`
- Files: repository state only; submodule gitlink only if upstream revision changed
- Size: S
- Status: [x] done

### Task 2: Ignored display workspace
- Acceptance: `.example/`가 git ignore 대상이 되고, `.example/display` 정적 페이지 기본 구조가 준비된다.
- Acceptance: `display.config.js` 하나로 `active: "local" | "development"`를 제어하며 production 환경은 포함하지 않는다.
- Acceptance: config에는 product/admin base URL과 admin login credential이 들어가고, token은 브라우저 메모리 또는 `sessionStorage`에만 저장된다.
- Verification: `git status --ignored --short .example .gitignore`, static server root에서 `index.html` 로드 확인
- Files: `.gitignore` tracked; `.example/display/index.html`, `.example/display/js/config.js`, `.example/display/js/api.js` ignored
- Size: M
- Status: [x] done

### Task 3: Spec browsing screens
- Acceptance: 원본 display의 navigation, common style, specs/curations 목록 화면이 Bottlenote API 응답 구조에 맞게 동작한다.
- Acceptance: development 환경에서 자동 로그인 후 `GET /admin/api/v2/curation-specs`가 3개 spec을 표시한다.
- Acceptance: spec 카드에서 `requestSpec`, `responseSpec`, `hydratorKey`, `version`, `isActive`를 확인할 수 있다.
- Verification: static server + development config로 specs 화면 조회, browser console error 없음
- Files: `.example/display/specs.html`, `.example/display/curations.html`, `.example/display/css/**`, `.example/display/js/nav.js`, `.example/display/js/specs.js`, `.example/display/js/curations.js` ignored
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-3
- [x] `main` and submodule baseline confirmed
- [x] `.example/` is ignored
- [x] development specs list returns HTTP 200 and renders 3 specs
- [x] no credential or token is tracked by git

### Task 4: Curation creation flow
- Acceptance: 원본 `curation-new.html`의 동적 form UX를 가능한 유지하면서 Bottlenote Admin `POST /admin/api/v2/curations` request body를 생성한다.
- Acceptance: request payload는 선택된 spec의 `requestSpec` 기준으로 구성되고, 기본 정보와 payload가 현재 `CurationCreateRequest` 계약에 맞게 전송된다.
- Acceptance: 실패 시 HTTP status, server error code/message, validation 실패 위치를 화면에 표시한다.
- Verification: development config에서 intentionally invalid payload 요청 후 validation error 표시 확인, valid payload 요청은 사용자 승인 데이터로 smoke 가능
- Files: `.example/display/curation-new.html`, `.example/display/js/curation-new.js`, `.example/display/js/widgets/**`, `.example/display/js/styles.js` ignored
- Size: M
- Status: [x] done

### Task 5: Detail comparison screen
- Acceptance: 상세 화면에서 Admin detail 응답과 Product detail 응답을 같은 curation id 기준으로 조회해 비교한다.
- Acceptance: Admin raw payload, Product materialized payload, `responseSpec`를 동시에 확인할 수 있다.
- Acceptance: 모바일 미리보기는 Product 응답을 기준으로 표시하고, hydration 결과가 없거나 null인 항목을 구분해 보여준다.
- Verification: existing or newly created curation id로 admin/product detail HTTP 200 확인, raw JSON과 preview 렌더 확인
- Files: `.example/display/curation-detail.html`, `.example/display/js/curation-detail.js`, `.example/display/js/mobile-preview.js`, `.example/display/js/widgets/**` ignored
- Size: M
- Status: [x] done

### Task 6: Demo verification notes
- Acceptance: ignored demo 실행 방법, config 전환 방법, 자동 로그인 동작, local/development 주의사항이 tracked 문서에 남는다.
- Acceptance: development API 기준 specs list, admin curations list, product curations list, login token injection smoke 결과가 기록된다.
- Acceptance: `.example/` 아래 credential/token이 git에 추적되지 않는다는 증거가 남는다.
- Verification: `git status --short --ignored .example`, `curl` 또는 browser smoke 결과 기록, `git diff --check`
- Files: tracked documentation under `plan/` or `docs/`; ignored `.example/display/**` remains untracked
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 4-6
- [x] create flow maps to Admin request contract
- [x] detail screen compares Admin and Product responses
- [x] development smoke completed with token auto-loading
- [x] tracked diff excludes `.example/display/**`

## Progress Log

- 2026-05-17 Task 1 완료: `main`으로 전환 후 `git pull`로 `dc9cc851`까지 fast-forward했고, `git.environment-variables` submodule은 `8dd662e15c098c2e57c36a2604a1ba42fceed9f1` 상태임을 확인했다. 최신 main 기준 작업 브랜치 `codex/curation-v2-display-demo`를 생성했다. 검증: `git status --short --branch`, `git rev-parse --short HEAD`, `git submodule status --recursive`.
- 2026-05-17 Task 2 완료: `.gitignore`에 `.example/`를 추가했고 원본 curation demo display를 `.example/display`로 복사했다. `js/config.js`는 `active` 기반 local/development 전환을 제공하고, `js/api.js`는 Bottlenote Admin/Product base URL, admin 자동 로그인, sessionStorage token 저장, GlobalResponse unwrap을 담당한다. 검증: `git status --short --ignored .example .gitignore plan/curation-v2-display-demo.md`, `node --check .example/display/js/api.js`, `node --check .example/display/js/config.js`, `node --check .example/display/js/nav.js`.
- 2026-05-17 Task 3 완료: specs 화면은 Admin `/curation-specs` 응답의 `requestSpec`, `responseSpec`, `hydratorKey`, `version`, `isActive`를 표시하고, curations 화면은 Admin `/spec-based-curations`와 Product `/api/v2/curations`를 나란히 조회하도록 조정했다. 정적 서버 `localhost:8097`에서 `index.html`, `specs.html`, `curations.html` HTTP 200을 확인했다. development API smoke는 specs=200 count=3, admin_curations=200 count=0, product_curations=200 count=0.
- 2026-05-18 정정: admin-api context-path를 `/admin/api`로 낮추고 spec 기반 curation surface를 `/v2`로 이동했다. 현재 코드베이스 기준 Admin specs는 `/admin/api/v2/curation-specs`, Admin spec-based curation은 `/admin/api/v2/curations`가 canonical endpoint다. 기존 `/admin/api/v1/curation-specs`, `/admin/api/v1/spec-based-curations` alias는 제공하지 않는다.
- 2026-05-17 Task 4 완료: `curation-new.html` 기본 노출 기간을 현재 dev smoke에 맞게 2026-05-17~2026-12-31로 바꾸고, submit body에서 DTO에 없는 `coverImageUrl`을 제거했다. 오류 표시는 `HTTP status`, server `code/status/message`를 노출하도록 조정했다. development Admin create invalid smoke는 HTTP 400, `CURATION_PAYLOAD_REQUIRED`를 확인했다.
- 2026-05-17 Task 5 완료: 상세 화면을 Admin raw detail과 Product materialized detail 비교 화면으로 단순화했다. development에 `CODEX_DISPLAY_SMOKE_20260517` 큐레이션을 생성했고 `targetId=2`를 받았다. Admin detail HTTP 200, Product detail HTTP 200, Admin payload array count=1, Product payload array count=1, MANUAL 항목 `stats=null`을 확인했다. 정적 상세 페이지 `curation-detail.html?id=2`도 HTTP 200으로 서빙됐다.
- 2026-05-17 Task 6 완료: `plan/curation-v2-display-demo-runbook.md`에 ignored demo 실행 방법, config 전환, 자동 로그인, development smoke 결과, git tracking policy를 기록했다. `.example/`는 `!! .example/`로 ignored 상태이고, tracked diff에는 `.example/display/**`가 포함되지 않는다.

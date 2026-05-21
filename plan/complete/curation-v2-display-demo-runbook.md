# Curation V2 Display Demo Runbook

## Purpose

이 문서는 ignored workspace인 `.example/display`에 구성한 Bottlenote curation v2 정적 데모 실행 방법과 검증 결과를 기록한다. 데모 파일은 local/development 전용 credential을 포함할 수 있으므로 git에 추적하지 않는다.

## Workspace

- Demo root: `.example/display`
- Entry page: `.example/display/index.html`
- Environment config: `.example/display/js/config.js`
- API adapter: `.example/display/js/api.js`
- Git tracking policy: `.example/` is ignored by `.gitignore`

## Environment Switching

`js/config.js`의 `displayConfig.active` 값으로 대상 환경을 바꾼다.

```js
export const displayConfig = {
  active: 'development',
  environments: {
    local: { /* local admin/product base URL */ },
    development: { /* development admin/product base URL */ },
  },
};
```

화면에서 환경 선택 UI는 제공하지 않는다. 데모를 새 환경으로 확인하려면 config 파일을 수정한 뒤 브라우저를 새로고침한다.

## Authentication

- Admin API 요청은 `api.login()`이 먼저 `/admin/api/v1/auth/login`을 호출한다.
- access token은 `sessionStorage`에 저장한다.
- token은 파일에 쓰지 않는다.
- 401 응답을 받으면 저장된 token을 지우고 한 번 재로그인한다.

## Current API Contract

현재 코드베이스 기준 데모가 호출해야 하는 canonical endpoint는 다음과 같다.

- Admin login: `/admin/api/v1/auth/login`
- Admin specs: `/admin/api/v2/curation-specs`, `/admin/api/v2/curation-specs/{specId}`
- Admin spec-based curation: `/admin/api/v2/curations`, `/admin/api/v2/curations/{curationId}`
- Product curation v2: `/api/v2/curations`, `/api/v2/curations/{curationId}`

기존 `/admin/api/v1/curation-specs`, `/admin/api/v1/spec-based-curations`는 2026-05-18 Admin API versioning 변경 이후 canonical endpoint가 아니며 alias도 제공하지 않는다.

## Local Static Server

```bash
python3 -m http.server 8097 --directory .example/display
```

확인 URL:

- `http://127.0.0.1:8097/index.html`
- `http://127.0.0.1:8097/specs.html`
- `http://127.0.0.1:8097/curations.html`
- `http://127.0.0.1:8097/curation-new.html`
- `http://127.0.0.1:8097/curation-detail.html?id=2`

## Development Smoke Result

Executed at: 2026-05-17

이 결과는 2026-05-18 Admin API v2 path remap 이전 실행 기록이다. 현재 코드베이스로 재검증할 때는 위 Current API Contract의 `/admin/api/v2/**` 경로를 사용한다.

- Static pages:
  - `index.html`: HTTP 200
  - `specs.html`: HTTP 200
  - `curations.html`: HTTP 200
  - `curation-detail.html?id=2`: HTTP 200
- Admin login:
  - `/admin/api/v1/auth/login`: HTTP 200
  - access token present: yes
- Admin specs:
  - historical path: `/admin/api/v1/curation-specs`
  - current path: `/admin/api/v2/curation-specs`
  - historical result: HTTP 200
  - count: 3
  - first spec keys: `id`, `code`, `name`, `description`, `hydratorKey`, `version`, `isActive`, `requestSpec`, `responseSpec`
- Admin curation list before demo create:
  - historical path: `/admin/api/v1/spec-based-curations?keyword=&isActive=&page=0&size=50`
  - current path: `/admin/api/v2/curations?keyword=&isActive=&page=0&size=50`
  - historical result: HTTP 200
  - count: 0
- Product curation list before demo create:
  - `/api/v2/curations`: HTTP 200
  - count: 0
- Invalid create smoke:
  - historical path: `/admin/api/v1/spec-based-curations`
  - current path: `/admin/api/v2/curations`
  - historical result: HTTP 400
  - error code: `CURATION_PAYLOAD_REQUIRED`
- Valid create smoke:
  - created name: `CODEX_DISPLAY_SMOKE_20260517`
  - targetId: 2
- Admin detail:
  - historical path: `/admin/api/v1/spec-based-curations/2`
  - current path: `/admin/api/v2/curations/2`
  - historical result: HTTP 200
  - spec: `RECOMMENDED_WHISKY`
  - payload type: array
  - payload count: 1
- Product detail:
  - `/api/v2/curations/2`: HTTP 200
  - spec: `RECOMMENDED_WHISKY`
  - payload type: array
  - payload count: 1
  - first payload `stats`: `null` because the smoke item uses `source=MANUAL`

## Git Tracking Check

Expected `git status --short --ignored .example`:

```text
!! .example/
```

Only `.gitignore`, this plan document, and this runbook should be tracked for the demo workflow. `.example/display/**` must remain ignored.

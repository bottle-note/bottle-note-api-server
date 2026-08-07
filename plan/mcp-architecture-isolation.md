# Admin MCP 아키텍처 — 격리 게이트웨이

- 작성: 2026-08-08
- 이슈: bottle-note/workspace#370
- 결정: **별도 앱 `bottlenote-mcp`** + **백엔드 MCP 전용 API**

## Topology

```
관리자 / Agent 클라이언트 (Claude, Codex, Cursor)
        |  Streamable HTTP  https://mcp.bottlenote.com/mcp
        |  Authorization: Bearer bn_agent_*
        v
 bottlenote-mcp  (/Users/hgkim/workspace/bottlenote/mcp)
        |  내부 HTTP allowlist only
        |  1) POST /admin/api/v1/auth/agent
        |  2) GET  /admin/api/v1/mcp/...
        |  Admin JWT 클라이언트 미노출
        v
 bottlenote-admin-api
```

## 왜 API 서버 엔드포인트만으로 부족한가

- MCP는 REST가 아니라 **JSON-RPC tools 프로토콜** (Streamable HTTP).
- 게이트웨이를 격리해야 권한 폭발 반경·배포·스케일을 Admin UI API와 분리 가능.

## 왜 TS 별도 앱인가 (이번 결정)

| 옵션 | 결과 |
|------|------|
| admin-api 내장 MCP | 비채택 — 프로세스/배포 커플링 |
| Java monorepo 모듈 | 가능했으나 사용자 지정 경로 `bottlenote/mcp` 별도 앱 |
| **TS `bottlenote-mcp`** | **채택** — Codex/Claude MCP SDK, multi-arch Node 이미지, k9s 독립 배포 |

## 서브모듈

- 동일: `git.environment-variables` → `https://github.com/bottle-note/environment-variables.git`
- 시크릿/배포 매니페스트 원천은 서브모듈. 앱 레포에 키 커밋 금지.

## Multi-arch

- Dockerfile: `node:22-alpine` base (amd64/arm64)
- buildx: `--platform linux/amd64,linux/arm64`

## 백엔드 MCP 최적화 API (admin-api)

| Method | Path | 용도 |
|--------|------|------|
| GET | `/admin/api/v1/mcp/whiskies` | 요약 검색 (size≤50) |
| GET | `/admin/api/v1/mcp/whiskies/{id}` | MCP용 상세 |

일반 Admin UI 계약(`/alcohols`)과 분리. 필드 축소·페이지 클램프·agent 친화 페이로드.

## 보안

1. 클라이언트 → MCP: Agent Key만
2. MCP → Admin: Agent Key 교환 후 JWT (요청 스코프)
3. 아웃바운드 allowlist: `/auth/agent`, `/mcp/*`
4. 로그 스크럽: `bn_agent_*`, JWT
5. 삭제/bulk 툴 미등록

## 로컬 경로

- MCP app: `/Users/hgkim/workspace/bottlenote/mcp`
- Backend worktree: `bottle-note-api-server/feat-issues-mcp`

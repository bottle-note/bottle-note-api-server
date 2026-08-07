# Admin MCP — 아키텍처 SSOT

- 이슈: bottle-note/workspace#370 · 관련 #340(Agent Key) · #341(감사, open)
- 작성/정리: 2026-08-08
- 이 문서가 plan 내 **유일한** MCP 설계 문서다. 조사 원본(research/brief)은 폐기했다.

## Topology

```
관리자 / Agent 클라이언트 (Claude, Codex, Cursor)
        |  Streamable HTTP  https://mcp.bottlenote.com/mcp
        |  Authorization: Bearer bn_agent_*
        v
 bottlenote-mcp  (/Users/hgkim/workspace/bottlenote/mcp, TS)
        |  내부 HTTP allowlist only
        |  1) POST /admin/api/v1/auth/agent
        |  2) GET  /admin/api/v1/mcp/...
        |  Admin JWT 클라이언트 미노출
        v
 bottlenote-admin-api
```

## 결정

| 항목 | 값 |
|------|-----|
| 게이트웨이 | 별도 TS 앱 `bottlenote-mcp` (admin-api 내장·Java monorepo MCP 모듈 비채택) |
| Transport | Streamable HTTP `/mcp`, **stateless** (sticky 없음, multi-pod OK) |
| Wire | 구현은 SDK 현실에 맞춤; 비즈니스 상태 세션 금지 |
| 인증 | Agent Key만 수신 → 서버 내부 #340 교환 → Admin JWT(요청 스코프) |
| 배포 | multi-arch Node 22 (`linux/amd64`, `linux/arm64`), k9s/GitOps |
| 서브모듈 | `git.environment-variables` (api-server와 동일). 키 커밋 금지 |

## 백엔드 MCP API (admin-api)

일반 `/alcohols` UI 계약과 분리. 필드 축소·size≤50.

| Method | Path | 용도 |
|--------|------|------|
| GET | `/admin/api/v1/mcp/whiskies` | 요약 검색 |
| GET | `/admin/api/v1/mcp/whiskies/{id}` | MCP용 상세 |

## Tools

### v0.1 (구현 중/게이트웨이 스캐폴드)

| tool | backend |
|------|---------|
| `bottlenote_whisky_search` | `GET /mcp/whiskies` |
| `bottlenote_whisky_get` | `GET /mcp/whiskies/{id}` |

### 이후 (미구현)

조회: lookup, category reference, distillery/region list·get, tasting_tag list  
쓰기: whisky create/update(`confirm=true`), image presign, preview_diff  
감사: #341 연계

### NEVER (미등록 + 아웃바운드 거부)

- delete / bulk / 무페이징 list_all
- 토큰 발급·통과 프록시
- 웹검색·외부 출처 판단·태그 자동생성
- curation / banner / user / review 전면

## 보안 (필수)

1. 클라이언트 → MCP: Agent Key만 (Admin JWT 수신·통과 금지)
2. MCP → Admin: allowlist `/auth/agent`, `/mcp/*` 만
3. 로그·메트릭·예외에 `bn_agent_*` / JWT 원문 금지
4. 쓰기 툴은 서버 `confirm=true` 강제 (annotation은 UX 힌트일 뿐)
5. Rate limit 키: agentId 우선, fallback XFF (게이트웨이 XFF 신뢰)

## 로컬

- MCP app: `/Users/hgkim/workspace/bottlenote/mcp` (푸시·본구현은 별도)
- Backend: 이 저장소 `feat-issues-mcp` 등

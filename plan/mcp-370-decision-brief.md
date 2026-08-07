# Admin MCP #370 — Decision Brief

- 작성일: 2026-08-08
- 이슈: `bottle-note/workspace#370`
- 관련: #340 Agent Key 완료, #341 감사 로그 open
- 입력: `mcp-research-spec-trends` · `mcp-research-codebase-readiness` · `mcp-research-deploy-clients` · `mcp-research-tool-catalog` · `mcp-research-security-checklist`
- 성격: **의사결정 브리프**. 프로덕션 코드·배포·PR 없음. 확정 요구사항 전 define/plan 입력.

---

## Executive Summary

| 결정 항목 | 권장 |
|---|---|
| 모듈 | 신규 bootJar **`bottlenote-admin-mcp`** (admin-api 내장·product 합류 비권장) |
| 트랜스포트 | **Streamable HTTP only**, 경로 `/mcp`, public `https://mcp.bottlenote.com` |
| 와이어 스펙 | 구현 **`2025-11-25`** (Java SDK v2 + Spring AI) / 설계 **stateless** (2026-07-28 전제) |
| 인증 1차 | **안 B**: `Authorization: Bearer bn_agent_*` → 서버 내부 `#340` 교환 → Admin JWT |
| 토큰 정책 | 클라이언트 Admin JWT **수신·통과 금지**. Admin JWT는 MCP 프로세스 내부만 |
| 첫 PR | 모듈 스캐폴드 + 키 교환 + whisky search/get + allowlist + 구조화 로그 |
| #370 DoD 후속 | 참조 조회 → 쓰기(+confirm)/presign/preview → #341 → prod HA → OAuth(안 A) |

---

## 1. Recommended Architecture

### 1.1 토폴로지

```
[MCP Client: Claude Code / Cursor / Codex]
    |  Streamable HTTP POST https://mcp.../mcp
    |  Authorization: Bearer bn_agent_*   (MCP 전용 자격만)
    v
[bottlenote-admin-mcp]   stateless, multi-pod OK, sticky 없음
    |  1) POST /admin/api/v1/auth/agent  (서버 내부, 키 원문 미로그)
    |  2) Admin JWT (요청 스코프 / 짧은 TTL 캐시, 클라이언트 미노출)
    |  3) 화이트리스트 Admin API만 호출 (NEVER 경로 코드 경로 0)
    v
[bottlenote-admin-api] --> mono --> MySQL / Redis / S3
```

### 1.2 모듈 배치

| 옵션 | 판정 | 이유 |
|---|---|---|
| **A. 신규 `bottlenote-admin-mcp` bootJar** | **채택** | 프로세스 격리, MCP 전용 게이트웨이·스케일, Admin 계약 HTTP 소비, blast radius 분리 |
| B. admin-api 내장 | 스파이크만 | 배포 단순하나 포트·인증·재시작 커플링, 레이어 혼선 |
| C. mono에 프로토콜 | 거부 | mono는 도메인 라이브러리 |
| D. product-api 합류 | 거부 | Admin 권한 경계 붕괴 |

- mono Facade/Service **직접 주입 금지**. Admin HTTP 클라이언트(WebClient/Feign)만 사용.
- settings: Gradle multi-module include, Java 21, Spring Boot 3.4.x, Spring AI MCP Server Boot Starter (`protocol=STREAMABLE`).
- 의존: `observability`(선택), Redis(rate limit·선택 handle), Admin 내부 base URL.

### 1.3 Transport · Endpoint

| 항목 | 값 |
|---|---|
| Public URL (prod) | `https://mcp.bottlenote.com` (DNS 표기 `bottlenote` vs `bottle-note` 인프라 확정 필요) |
| Dev URL (가칭) | `https://mcp.development.bottle-note.com` |
| Path | `/mcp` |
| Protocol wire | `2025-11-25` (SDK 한계) |
| Design constraint | **stateless**: `Mcp-Session-Id`/커넥션 스코프 비즈니스 상태 금지. 필요 시 Redis handle + `<agentId>:<handle>` 바인딩 |
| Health | `/actuator/health/liveness`, `/actuator/health/readiness` (context-path 없음) |
| TLS | Gateway 종단. 앱 plain `:8080` |
| Session affinity | **없음** (`sessionAffinity` 미설정) |
| CORS 1차 | CLI/native 전제 → allow-origins 비움/최소. 브라우저 커넥터는 이후 |

### 1.4 Agent Key → Admin JWT (인증 흐름)

```
Client  --Bearer bn_agent_*-->  MCP
MCP     --POST /admin/api/v1/auth/agent { agentKey }-->  Admin API (#340)
Admin   --TokenItem(access, refresh)-->  MCP (메모리만)
MCP     --Bearer access-->  화이트리스트 Admin API
MCP     --툴 결과(토큰 필드 없음)-->  Client
```

| 규칙 | 내용 |
|---|---|
| 클라이언트 제시 | Agent Key(또는 이후 안 A의 MCP audience JWT)만 |
| 거부 | 사람 Admin JWT, Product JWT, audience 불일치 토큰 제시 → 401 |
| 교환 주체 | MCP 서버만. 교환 API는 내부 네트워크/서비스 호출 |
| Admin JWT 수명 | access 위주. **refresh last-writer-wins** → 파드 간 refresh 공유·재사용 금지. 만료 시 교환 재시도 |
| 캐시 | 요청 단위 또는 Redis `agentId` 바인딩 + TTL ≤ access 잔여. JVM static 금지 |
| 로그 | `bn_agent_*`·JWT 원문 0건. agentId/profileCode만 |
| 시드 주의 | 현재 에이전트 매핑 admin = `ROOT_ADMIN` → 툴/아웃바운드 allowlist로 폭발 반경 축소 (역할 하향은 후속 이슈) |

**1차 인증 안**: **B (정적 Bearer)**. 소수 내부 에이전트 + Claude/Cursor/Codex 네이티브 헤더 지원.  
**이후 안 A**: OAuth 2.1 RS + PRM, audience=`https://mcp.bottlenote.com`, client_credentials.

### 1.5 인가 · Rate limit (아키텍처 수준)

- 연결 인증 ≠ 전 툴 허용. scope: `admin:read` 기본 / `admin:whisky:write` / `admin:image:presign`.
- annotation(`readOnlyHint` 등)은 UX만. 인가·confirm은 **서버 디스패처**.
- Rate limit: Redis. 키=`agentId` 또는 XFF IP. write/presign/교환 > read. 로컬 카운터 금지.
- 아웃바운드: **URL allowlist** (툴 미등록 + 호출 경로 이중).

---

## 2. First PR Vertical Slice vs Later Phases (#370 DoD)

### 2.1 #370 DoD 해석 (브리프 기준)

| DoD 축 | 완료 조건(합의 초안) | 1차 PR | 이후 |
|---|---|---|---|
| 원격 MCP 서버 | Streamable HTTP + 내부 에이전트 연결 | dev 호스트 | prod 호스트 |
| Agent Key 인증 | #340 교환, 토큰 통과 0 | 안 B | 안 A 선택 |
| 위스키 조회 | search/get (+lookup·참조) | search/get | 나머지 read |
| 위스키 단건 생성·수정 | confirm + 감사 | 제외 | P2 |
| 이미지 준비 | presign | 제외 | P2 |
| 변경 전후 검증 | preview_diff | 제외 | P2 |
| 삭제·bulk 비제공 | 미등록 + allowlist | 강제 | 유지 |
| 감사 | 툴 호출 단위 추적 | 구조화 로그 훅 | #341 풀 |
| 다중 인스턴스 | sticky 없이 동작 | 설계+dev 스모크 | prod replicas≥2 |

### 2.2 First PR — 최소 수직 슬라이스

**목표**: “에이전트가 키로 인증하고, 위스키를 검색·조회하며, 호출이 추적 가능한가?”를 최소 코드로 증명.

| Step | 내용 | 완료 조건 |
|---|---|---|
| S0 | `bottlenote-admin-mcp` 모듈 + Streamable HTTP `/mcp` + health | bootRun, sticky 불필요 |
| S1 | Agent Key → `POST .../auth/agent` → access 요청 스코프 | 유효/무효 키 테스트, 원문 미로그 |
| S2 | 툴 2개: `bottlenote_whisky_search`, `bottlenote_whisky_get` | Admin 왕복, `size` ≤50 클램프 |
| S3 | 구조화 감사 로그(임시): agentId/profileCode, tool, targetIds?, status, durationMs, correlationId | 스크러버 검증. #341 이식 필드 정렬 |
| S4 | 아웃바운드 allowlist: agent login + alcohols GET만 | DELETE/bulk 코드 경로 0 |
| S5 (배포, 서브모듈) | dev Deployment + Service + HTTPRoute `/mcp`·`/actuator` | 401 무키, 200 tools/list |

**1차 PR 의도적 제외**: create/update, presign, region/distillery write, preview_diff, OAuth/PRM, #341 스키마 본구현, production multi-replica, Gateway global RL.

**1차 수락 기준**:

1. `POST .../mcp` + 유효 Bearer → `tools/list`에 등록 툴
2. 무효/누락 Bearer → 401, body에 키 미포함
3. search/get 실 Admin 왕복 성공
4. 커밋·로그·트레이스에 `bn_agent_` 원문 0건
5. sticky 없이 list/call 가능 (dev replicas=2 스모크 권장)

### 2.3 Later Phases (DoD 완성 순서)

| Phase | 내용 | DoD 기여 |
|---|---|---|
| **P1** (본 문서 First PR) | 모듈 + 교환 + whisky search/get + allowlist + 구조화 로그 + dev 배포 | 연결·조회 증명 |
| **P2** | 참조 조회 6툴 (lookup, category, distillery list/get, region list/get, tasting_tag_list) | 생성 워크플로 입력 |
| **P3** | write: create/update(`confirm=true`), image_presign 어댑터, preview_diff; write rate limit | 단건 변경 워크플로 |
| **P4** | #341 감사 테이블/이벤트 연계 (before/after, agent 차원, 성공·deny 1행) | 운영 감사 DoD |
| **P5** | production 호스트, replicas≥2, PDB, 릴리스 워크플로 편입 | HA |
| **P6** | 안 A OAuth RS + PRM (필요 시) | 범용 클라이언트 |
| **P7** | Gateway RL / `Mcp-Method` 정책, 에이전트 역할 ROOT_ADMIN 하향 | 보안 하드닝 |
| **P8** | Java SDK `2026-07-28` 마이그레이션 (트랜스포트만 교체 가정) | 스펙 정합 |

**쓰기 슬라이스(P3) 전 게이트**: 최소 구조화 감사 훅 또는 #341 최소 스키마 없이 운영 write 금지 권고.

---

## 3. Tool Inventory — Include vs Never-expose

예산: **등록 13개** (조회 8 + 변경 5 계열). 명명: `bottlenote_{domain}_{action}`.

### 3.1 INCLUDE (서버 등록)

| # | tool | R/W | 1차 PR | Phase | Admin API |
|---|---|---|---|---|---|
| 1 | `bottlenote_whisky_search` | read | **Y** | P1 | `GET /admin/api/v1/alcohols` |
| 2 | `bottlenote_whisky_get` | read | **Y** | P1 | `GET /admin/api/v1/alcohols/{id}` |
| 3 | `bottlenote_whisky_lookup` | read | N | P2 | `GET .../alcohols/lookup` |
| 4 | `bottlenote_category_reference_get` | read | N | P2 | `GET .../alcohols/categories/reference` |
| 5 | `bottlenote_distillery_list` | read | N | P2 | `GET .../distilleries` |
| 6 | `bottlenote_distillery_get` | read | N | P2 | `GET .../distilleries/{id}` |
| 7 | `bottlenote_region_list` | read | N | P2 | `GET .../regions` |
| 8 | `bottlenote_region_get` | read | N | P2 | `GET .../regions/{id}` |
| 9 | `bottlenote_tasting_tag_list` | read | N | P2 | `GET .../tasting-tags` |
| 10 | `bottlenote_whisky_create` | write + **confirm** | N | P3 | `POST .../alcohols` |
| 11 | `bottlenote_whisky_update` | write + **confirm** | N | P3 | `PUT .../alcohols/{id}` |
| 12 | `bottlenote_image_presign` | write (URL만) | N | P3 | `GET .../s3/presign-url` (스키마 어댑터) |
| 13 | `bottlenote_whisky_preview_diff` | read | N | P3 | 로컬 get+diff (전용 API 불필요) |

공통 규칙: page size 기본 20·최대 50, `additionalProperties: false`, write는 서버 `confirm=true` 강제, 목록은 요약 필드.

### 3.2 NEVER-EXPOSE (미등록 + 아웃바운드 거부)

| 금지 | 이유 | 대응 Admin API (존재해도 비노출) |
|---|---|---|
| `*_delete` / 소프트삭제 일괄 | #370 자동 삭제 비제공 | `DELETE /alcohols|distilleries|regions|tasting-tags/{id}` |
| bulk reorder / 대량 수정 단일 툴 | 폭발 반경 | `PATCH .../bulk/reorder` 등 |
| 무페이징 `list_all_*` | 컨텍스트 폭증 | — |
| Agent Key / JWT 발급·조회 툴 | 시크릿 노출 | #340 교환은 MCP 내부 전용 |
| 토큰 통과 프록시 툴 | 스펙 금지 | — |
| region/distillery/tag CUD 툴 | 1차 범위 밖 (참조 조회만) | POST/PUT/DELETE 해당 리소스 |
| tag↔alcohol 전용 attach/detach 툴 | whisky create/update `tastingTagIds`로 흡수 | `POST/DELETE .../tasting-tags/{id}/alcohols` |
| curation / banner / user / review 전면 | #370 범위 밖 | 별도 카탈로그 |
| 웹검색·Whiskybase·출처 판정·태그 자동생성 | 명시 배제 | 리서치 MCP 몫 |

NEVER의 본방어 = **툴 미등록 + Admin HTTP allowlist**. JWT가 ROOT_ADMIN이어도 MCP가 DELETE를 호출하지 못하게 한다.

### 3.3 Scope 맵 (구현 시)

| scope | 툴 |
|---|---|
| `admin:read` | #1–#9, #13 |
| `admin:whisky:write` | #10–#11 (+ read 권장) |
| `admin:image:presign` | #12 |

초기 에이전트: `admin:read` only → write 필요 시 별 에이전트 또는 step-up.

---

## 4. Risks

### 4.1 Java SDK gap (스펙 2026-07-28 vs SDK 2025-11-25)

| 항목 | 내용 |
|---|---|
| 사실 | MCP 최신 스펙 `2026-07-28` = stateless, session 제거, `server/discover` 등. Java SDK GA v2.0.0은 **`2025-11-25`** 트래킹. 2026-07-28 대응 릴리스 조사 시점 없음 |
| 리스크 | 클라이언트가 2026-07-28 only 요구 시 연결 실패; 세션 API에 비즈니스 상태를 묶으면 마이그레이션 재작성 |
| 완화 | **와이어는 2025-11-25, 설계는 stateless**. 세션 미의존. Assumption으로 plan에 고정. SDK 업그레이드 시 트랜스포트 계층만 교체 |
| 차단 가능 | Spring AI ↔ Boot 3.4.11 의존성 해석 실패 → **착수 전 스파이크(A1)** |

### 4.2 Multi-pod

| 항목 | 내용 |
|---|---|
| 사실 | AGENTS.md: 다중 인스턴스, JVM 로컬로 카운트·락 금지. product prod replicas 2. **admin-api는 현재 replicas 1**. MCP 스펙도 sticky 불필요 방향 |
| 리스크 | 세션/static rate limit/파드 로컬 JWT 캐시 → 불일치·우회. 동일 Agent Key로 다 파드가 **refresh 경쟁** 시 last-writer-wins로 상호 무효 |
| 완화 | MCP **stateless** + Redis rate limit/handle. Admin JWT **access 위주**, refresh 공유 금지. Service `sessionAffinity` 미사용. 설계부터 multi 전제 (admin 스케일 전에도) |

### 4.3 #341 Audit gap

| 항목 | 내용 |
|---|---|
| 사실 | 운영 감사 테이블/툴 호출 로그 **없음**. 있는 것은 JPA `AuditPrincipal`(V5) + `AGENT` enum. `AuditorAwareImpl`은 항상 **ADMIN**. 에이전트 토큰 호출도 ADMIN 주체 계약(통합 테스트) |
| 리스크 | 쓰기 후 “누가 무엇을” 추적 불가. 교환 직후 profileCode 유실. 감사 실패 삼키고 write 성공 시 컴플라이언스 붕괴 |
| 완화 | **1차**: 구조화 앱 로그 + 스크러버 + 필드 세트 정렬(보안 체크리스트 5.1). **JPA Auditor를 AGENT로 바꾸지 않음**(계약 유지). MCP 감사는 별도 경로. **P3 write 전** #341 최소 연동 또는 감사 실패 시 write 실패 정책 합의 |
| 권장 필드 | timestamp, traceId, agentId, toolName, rw, decision, denyReason?, argsRedacted, targetIds, before/after(write), resultCode, durationMs, clientIp |

### 4.4 기타 상위 리스크 (요약)

| ID | 리스크 | 완화 |
|---|---|---|
| R1 | 시드 Agent = ROOT_ADMIN → 탈취 JWT = 전 Admin API | MCP allowlist; 역할 하향 후속 이슈 |
| R2 | Envoy Streamable HTTP 버퍼링/타임아웃 | 인프라 실측 후 툴 타임아웃 정합 |
| R3 | presign 스키마 vs Admin `rootPath/uploadSize` 불일치 | P3 어댑터 |
| R4 | 호스트명 `mcp.bottlenote.com` vs `bottle-note.com` | DNS 팀 확정 |
| R5 | Agent Key 로그 유출 | K1–K8 체크리스트 + CI secret scan |

---

## 5. Exact File / Module List — First Implementation PR

프로덕션 비즈니스 로직은 아래 **신규 MCP 모듈 + 루트 빌드 연결**에 한정. Admin 도메인 서비스 수정 없음(교환 API 기존 사용).

### 5.1 이 저장소 (API 서버) — First PR 예상 경로

| 경로 | 작업 |
|---|---|
| `settings.gradle` (또는 `settings.gradle.kts`) | `bottlenote-admin-mcp` include |
| `build.gradle` / version catalog (`gradle/libs.versions.toml`) | Spring AI MCP starter, WebClient 등 버전 핀 (스파이크 후) |
| `bottlenote-admin-mcp/build.gradle` | bootJar 모듈 정의, mono 비의존 또는 최소 공통만 |
| `bottlenote-admin-mcp/src/main/resources/application.yml` | port, MCP STREAMABLE `/mcp`, Admin base URL, Redis, actuator |
| `bottlenote-admin-mcp/src/main/java/.../AdminMcpApplication.java` | Spring Boot entry |
| `.../config/McpServerConfig.java` (가칭) | Streamable HTTP / tool registration |
| `.../config/SecurityConfig.java` | Bearer Agent Key 필터, 공개 health, 그 외 인증 |
| `.../auth/AgentKeyAuthenticationFilter.java` (가칭) | `bn_agent_*` 검증 흐름 진입 |
| `.../auth/AdminTokenExchangeClient.java` | `POST /admin/api/v1/auth/agent` |
| `.../auth/AdminAccessTokenHolder.java` | 요청 스코프/짧은 캐시 (static 금지) |
| `.../client/AdminApiClient.java` | WebClient + **경로 allowlist** |
| `.../client/AdminApiPaths.java` | 허용 상수: agent, `GET /alcohols`, `GET /alcohols/{id}` |
| `.../tool/WhiskySearchTool.java` | `bottlenote_whisky_search` |
| `.../tool/WhiskyGetTool.java` | `bottlenote_whisky_get` |
| `.../tool/ToolSchemaSupport.java` (선택) | size 클램프, schema 공통 |
| `.../audit/McpAuditLogger.java` | 구조화 로그 (키/JWT 스크러빙) |
| `.../audit/SecretScrubber.java` | Authorization / bn_agent_ / JWT 마스킹 |
| `.../ratelimit/RedisRateLimiter.java` (최소) | agentId + IP |
| `bottlenote-admin-mcp/src/test/java/...` | 단위: scrubber, size clamp, allowlist deny; 통합: Fake Admin / WireMock 교환+alcohols |

**패키지 루트 권장**: `app.bottlenote.mcp` 또는 프로젝트 기존 `app.bottlenote` 규칙에 맞춤 (구현 시 기존 모듈 패키지 관례 확인).

**First PR에서 건드리지 않음**:

- `bottlenote-admin-api` 컨트롤러/서비스 (교환·alcohols 이미 존재)
- `bottlenote-mono` 도메인
- JPA Auditor / V5 principal (AGENT로 변경 금지)
- Flyway (1차 불필요; #341은 P4)
- product-api

### 5.2 배포 서브모듈 (`git.environment-variables`) — 동일 이슈 연계 PR 가능, 이 브리프 범위는 목록만

| 경로 | 작업 |
|---|---|
| `deploy/base/mcp-server.yaml` | Deployment + Service ClusterIP 80→8080 |
| `deploy/overlays/development/*-patch.yaml` | replicas 1, profile, image |
| `deploy/overlays/development/http-route.yaml` | host `mcp.development...`, `/mcp` + `/actuator`, 나머지 403 |
| `deploy/overlays/development/kustomization.yaml` | resources + images |
| (선택) `deploy/overlays/development/secrets/mcp-server-secret.sops.yaml` | `ADMIN_API_BASE_URL`, Redis, OTel — **Agent Key 원문 넣지 않음** |
| (이후) production overlay + DNS `mcp.bottlenote.com` | P5 |
| `.github/workflows/deploy_development_applications.yml` (API 레포) | 이미지 빌드·태그 슬롯 (1차는 수동 태그 가능) |

### 5.3 문서 (선택, First PR 또는 직후)

| 경로 | 내용 |
|---|---|
| `docs/mcp-client-setup.md` 또는 plan 절 승격 | Claude/Cursor/Codex placeholder (`bn_agent_<YOUR_KEY>`, env `BOTTLENOTE_AGENT_KEY`) |

### 5.4 First PR 의존성 스파이크 (PR 전 또는 PR 0)

| 항목 | 산출 |
|---|---|
| Spring AI MCP starter + Boot 3.4.11 해석 | 호환 버전 핀 표 |
| `McpStateless*` / STREAMABLE 기동 스모크 | `/mcp` initialize 또는 tools/list |

---

## 6. Go / No-Go (배포·확장 전)

보안 체크리스트 요약 — First PR 완료 시 최소 증명:

1. [ ] Agent Key 원문 로그/트레이스/응답/커밋 0
2. [ ] 클라이언트 Admin JWT 제시 → 401, Admin API 전달 0
3. [ ] DELETE/bulk 툴 미등록 + allowlist 외 호출 0
4. [ ] tools/call(또는 list) 성공·실패 감사/구조화 로그 1행
5. [ ] Redis 또는 동등 공유 저장소 전제 설계 (로컬 RL 없음)
6. [ ] sticky 없이 동작

Write 확장 전 추가:

7. [ ] confirm 없이 write → 거부 + deny 감사  
8. [ ] before/after (update)  
9. [ ] write rate limit + 429  
10. [ ] #341 연계 정책 합의  

---

## 7. Decisions Locked vs Open

### Locked (이 브리프 권장 = 구현 기본값)

1. 모듈: **독립 `bottlenote-admin-mcp`**
2. Transport: **Streamable HTTP `/mcp` only**
3. 자격: **Agent Key → 서버 내부 Admin JWT (#340)**, 토큰 통과 금지
4. 1차 인증 표면: **안 B**
5. 설계: **stateless / multi-pod / Redis for RL**
6. First tools: **whisky_search + whisky_get only**
7. NEVER: delete, bulk, token tools, 범위 외 도메인
8. #341: 1차는 구조화 로그; JPA AGENT 전환 안 함

### Open (구현 전 확인)

| # | 항목 | 담당 |
|---|---|---|
| O1 | prod 호스트 최종 문자열 | 인프라/DNS |
| O2 | Envoy 스트리밍·timeout 실측 | 인프라 |
| O3 | Spring AI 버전 핀 결과 | 백엔드 스파이크 |
| O4 | 감사 실패 시 write 실패 여부 | #341 합의 |
| O5 | 안 A 도입 시점 | 제품/보안 |
| O6 | Rate limit 수치 운영 합의 | 운영 |

---

## 8. Anti-patterns (즉시 거부)

- product-api에 Admin MCP 부착
- sticky session / 파드 로컬 rate limit
- 클라이언트 JWT를 Admin Authorization에 패스스루
- 세션·static에 비즈니스 상태
- 삭제 방지 = description/annotation만
- `admin:*` 단일 스코프
- 1차 PR에 OAuth + 전체 13툴 + prod HA 일괄
- 테스트 fixture에 실키 `bn_agent_` 커밋
- 레거시 HTTP+SSE 채택

---

## Source (내부)

- `plan/mcp-research-spec-trends.md`
- `plan/mcp-research-codebase-readiness.md`
- `plan/mcp-research-deploy-clients.md`
- `plan/mcp-research-tool-catalog.md`
- `plan/mcp-research-security-checklist.md`
- 관련: `plan/agent-key-token-exchange.md` (있다면)

---

**산출물 성격**: #370 define/plan 의사결정 입력. 이 문서만으로 구현·배포 승인으로 간주하지 않는다.  
**다음 권장 단계** (실행하지 않음): `/define` 또는 plan Tasks 분해 → First PR 스파이크(O3) → S0–S5 구현.

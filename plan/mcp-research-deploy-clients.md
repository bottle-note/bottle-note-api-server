# MCP 배포·클라이언트 설정 리서치 — Admin MCP #370

- 조사일: 2026-08-08 / 이슈: `bottle-note/workspace#370`
- 관련: #340 Agent Key 완료, #341 감사 로그 open
- 근거: `git.environment-variables/deploy/**` 실측(시크릿 값 미열람) + `plan/mcp-research-spec-trends.md` + 클라이언트 설정 공개 문서
- 성격: 읽기 전용 설계 입력. 프로덕션 코드 변경 없음. 확정 요구사항 아님.

## Summary

- **제품 형태**: `https://mcp.bottlenote.com` (Streamable HTTP, 엔드포인트 `/mcp`) 원격 MCP 서버. Agent Key(`bn_agent_*`)를 클라이언트가 보내고, MCP 서버가 내부에서 Admin JWT로 교환(#340) — **토큰 통과 금지**.
- **배포 형태**: 기존 product/admin과 **동일 GitOps 패턴**을 복제한다. `deploy/base` + `overlays/{development,production}` + Envoy Gateway `HTTPRoute` + KSOPS Secret + Argo CD auto-sync. 매니페스트 원본은 서브모듈 `git.environment-variables`(원격 `bottle-note/environment-variables`).
- **TLS**: 클러스터 밖 TLS 종단은 `main-gateway`(Envoy Gateway, `envoy-gateway-system`). 앱 컨테이너는 HTTP `:8080`만 노출. 앱에서 TLS 종료하지 않음.
- **CORS**: MCP 클라이언트는 브라우저 Origin이 아니라 **네이티브 앱/CLI 헤더 호출**이 기본. 서버 CORS allowlist는 비우거나 최소 유지. 브라우저 커넥터(ChatGPT 등 OAuth)를 나중에 열 때만 별도 정책.
- **Health**: product/admin과 동일하게 Actuator liveness/readiness. 게이트웨이는 `/actuator` prefix 허용 패턴 유지.
- **Rate limit / XFF**: 게이트웨이가 XFF를 재작성하므로 앱이 받는 XFF는 신뢰 가능. **Rate limit 키 = Agent Key 주체(agentId) 우선, fallback = XFF 클라이언트 IP**. Redis 공유 카운터. **sticky session 불필요**(stateless MCP).
- **클라이언트**: Claude Code / Cursor / Codex(CLI)는 Streamable HTTP + `Authorization: Bearer` 네이티브 지원. Claude Desktop은 `mcp-remote` 브릿지 또는 stdio. 문서·온보딩에는 **원문 키 placeholder만** (`bn_agent_<YOUR_KEY>` / env var).
- **1차 PR vs 이후**: 1차는 앱 모듈(또는 admin-api 내장 옵션 결정) + dev HTTPRoute + 읽기 툴 + 정적 Bearer + 클라이언트 문서. 이후 OAuth/PRM, prod multi-replica, 게이트웨이 rate limit, 쓰기 툴, 감사(#341).

## 1. 제품 형태 (`mcp.bottlenote.com`)

| 항목 | 권장 값 | 근거 |
|---|---|---|
| Public URL | `https://mcp.bottlenote.com` | 스펙 트렌드 문서 audience 예시, #370 원격 서버 전제 |
| Dev URL | `https://mcp.development.bottle-note.com` (가칭) | 기존 호스트 규칙: `*.development.bottle-note.com` / `api.development...` |
| Transport | **Streamable HTTP only** | 레거시 HTTP+SSE Deprecated. Spring AI `protocol=STREAMABLE` |
| Path | `/mcp` | Spring AI MCP Server Boot Starter 기본 경로 |
| Protocol wire | 구현: `2025-11-25` (Java SDK v2), 설계: stateless(2026-07-28 전제) | 스펙 트렌드 문서 |
| Auth (1차) | `Authorization: Bearer bn_agent_...` | 안 B 최소 비용. 소수 내부 에이전트 |
| Auth (이후) | OAuth 2.1 RS + PRM, audience=`https://mcp.bottlenote.com` | 안 A. 범용 클라이언트 호환 |
| Upstream | Admin API (내부 ClusterIP 또는 동일 프로세스) | 툴 카탈로그 13개 → Admin 엔드포인트 |
| Session affinity | **없음** | sticky session 금지. handle 필요 시 Redis |

### 모듈 배치 옵션

| 옵션 | 설명 | 1차 추천 |
|---|---|---|
| **A. 독립 Deployment `mcp-server`** | 별도 jar/이미지, admin-api를 HTTP 클라이언트로 호출 | **권장** — blast radius·스케일·게이트웨이 정책을 MCP 전용으로 분리 |
| B. admin-api 내장 | 같은 프로세스에 `/mcp` 노출 | 배포 단순. 단 admin 부하·재시작 커플링 |
| C. product-api 내장 | 비권장 | Admin 권한 툴을 product에 넣으면 경계 붕괴 |

옵션 A를 전제로 아래 배포 패턴을 기술한다. B로 가면 HTTPRoute만 admin 호스트에 `/mcp`를 추가하는 축소판이 된다.

## 2. 기존 deploy 패턴 실측 (시크릿 미포함)

### 2.1 디렉터리 구조

```
git.environment-variables/deploy/
  base/                         # 공통 Deployment + Service
    product-api.yaml
    admin-api.yaml
    admin-dashboard.yaml
    frontend.yaml
    batch-module.yaml
    kustomization.yaml
  overlays/
    development/                # ns: bottlenote-development
      kustomization.yaml        # resources: ../../base + http-route + redis
      *-patch.yaml              # replicas, profile, nodeSelector
      http-route.yaml           # Envoy Gateway HTTPRoute
      secrets-generator.yaml    # KSOPS
      secrets/*.sops.yaml
      redis-replication.yaml
    production/                 # ns: bottlenote-production
      (동일 + product-api-pdb.yaml)
  argocd/bottlenote/applications/
    bottlenote-development.yaml
    bottlenote-production.yaml  # source path = deploy/overlays/{env}
```

### 2.2 Deployment / Service 관례 (product-api 기준)

| 항목 | 값 |
|---|---|
| labels | `app: product-api` (+ overlay `env: dev|prod`) |
| container port | `8080` name `http` |
| Service | ClusterIP `port: 80` → `targetPort: 8080` |
| envFrom | `secretRef: product-api-env` (KSOPS 생성 Secret) |
| env 고정 | `TZ=Asia/Seoul`, overlay에서 `SPRING_PROFILES_ACTIVE`, `SERVER_NAME`=pod name |
| imagePullSecrets | `private-registry-secret` |
| registry | `docker-registry.bottle-note.com/...` |
| image tag | overlay `kustomization.yaml` `images[].newTag` — GH Actions가 갱신 |
| nodeSelector | `kubernetes.io/arch: arm64` |
| replicas | product prod **2**, admin/dev **1** |
| PDB | product prod only: `minAvailable: 1` |
| resources (product base) | req 500m/2Gi, lim 1500m/6Gi |
| resources (admin base) | req 250m/1Gi, lim 750m/3Gi |

**MCP 1차 제안 리소스** (admin과 유사, 트래픽 소수):

- req: cpu 250m / mem 512Mi–1Gi
- lim: cpu 500m–750m / mem 1–2Gi
- replicas: dev 1 / prod 1(1차) → 2 + PDB(이후)

### 2.3 Health probe 관례

| 서비스 | liveness / readiness path |
|---|---|
| product-api | `/actuator/health/liveness`, `/actuator/health/readiness` |
| admin-api | `/admin/api/actuator/health/liveness` (context-path 포함), readiness 동일 |

공통 probe 타이밍(base):

- startup: initialDelay 90s, period 10s, failureThreshold 25, timeout 5s
- liveness: period 10s, timeout 5s, failureThreshold 3
- readiness: period 5s, timeout 3s, failureThreshold 3

**MCP 서버**: context-path 없이 `/actuator/health/{liveness,readiness}` 권장. 게이트웨이 허용 prefix에 `/actuator` 포함.

### 2.4 HTTPRoute / 게이트웨이 관례

- Gateway: `main-gateway` in `envoy-gateway-system` (Gateway API)
- API 종류: `gateway.networking.k8s.io/v1` `HTTPRoute`
- 호스트별 라우트 분리 (prod 예):
  - `api.product.bottle-note.com` → product-api
  - `admin-api.bottle-note.com` → admin-api (`/admin/api`, `/actuator`)
  - `bottle-note.com` → frontend
  - `admin.bottle-note.com` → admin-dashboard
- **경로 화이트리스트 + 나머지 403**: `HTTPRouteFilter` `block-unknown-paths` (DirectResponse JSON `{"error":"Forbidden"}`)
- product 허용: `/api/v1`, `/api/v2`, `/actuator` — 그 외 `/` → 403
- admin 허용: `/admin/api`, `/actuator` — 그 외 `/` → 403

**MCP용 HTTPRoute 초안 (prod, 매니페스트 예시 — 아직 미적용)**

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: mcp-server-route
  namespace: bottlenote-production
spec:
  parentRefs:
  - group: gateway.networking.k8s.io
    kind: Gateway
    name: main-gateway
    namespace: envoy-gateway-system
  hostnames:
  - mcp.bottlenote.com
  rules:
  - matches:
    - path:
        type: PathPrefix
        value: /mcp
    backendRefs:
    - name: mcp-server
      port: 80
  - matches:
    - path:
        type: PathPrefix
        value: /actuator
    backendRefs:
    - name: mcp-server
      port: 80
  - matches:
    - path:
        type: PathPrefix
        value: /
    filters:
    - type: ExtensionRef
      extensionRef:
        group: gateway.envoyproxy.io
        kind: HTTPRouteFilter
        name: block-unknown-paths
```

dev는 hostname만 `mcp.development.bottle-note.com`(가칭)으로 교체.

**Streamable HTTP 게이트웨이 주의** (스펙 트렌드 미확인 항목 승격):

- POST `/mcp` 응답 스트리밍을 버퍼링하면 장기 툴 호출이 끊긴다.
- 타임아웃: 툴 호출 상한(예: 30–60s)보다 게이트웨이/Envoy idle timeout이 짧으면 실패.
- 바디 크기 제한: 목록 응답·diff 스냅샷을 고려해 기본 제한 확인.
- 확인 담당: 인프라. 앱 착수 전 체크리스트에 포함.

### 2.5 TLS

- 공개 호스트 TLS는 Gateway 레이어에서 종단 (이 저장소 매니페스트에 Certificate 리소스는 없음 — 클러스터/Gateway 공통 설정 추정).
- 앱: plain HTTP 8080. 컨테이너 간 mTLS 강제 흔적 없음.
- MCP 클라이언트 → `https://mcp.bottlenote.com` 만 문서화. HTTP 평문 공개 금지.

### 2.6 Secret / 설정 (키 이름만)

KSOPS `secrets-generator.yaml` → `product-api-secret.sops.yaml` 등이 Secret `product-api-env`로 주입.

관측된 **키 이름**(값 미열람): `SERVER_PORT`, `DB_*`, `REDIS_*`, `JWT_SECRET_KEY`, `AWS_*`, OTel, Discord, OAuth 쿠키 관련, Root Admin 등.

Agent Key 원문 보관:

- 서브모듈 루트 `agent/api-keys.sops.yaml` — 프로필 `0001`–`0006`, 필드 `alias` / `api_key` (SOPS age)
- DB V7: `agents.api_key_hash` SHA-256만 저장, 원문 없음
- **배포 Secret에 Agent Key 원문을 넣지 않는다.** 원문은 사람/에이전트 클라이언트 로컬 설정 전용. MCP 서버는 해시 검증 또는 Admin 교환 API만 사용.

MCP 서버 전용 env 후보(신규, 값 설계만):

| 키 | 용도 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | dev/prod |
| `ADMIN_API_BASE_URL` | 클러스터 내부 `http://admin-api` (+ context `/admin/api`) |
| `MCP_SERVER_PUBLIC_URL` | `https://mcp.bottlenote.com` (OAuth audience / 문서) |
| `REDIS_*` | rate limit·(선택) handle 저장 — 기존 redis 공유 가능 |
| OTel 계열 | 기존 product/admin과 동일 패턴 |
| `JWT`/`AGENT` 교환 관련 | 서버가 Admin 교환 호출 시 필요한 내부 설정 (시크릿은 별도 SOPS) |

### 2.7 GitOps / 배포 파이프라인

| 환경 | 트리거 | 동작 |
|---|---|---|
| development | main CI 성공 또는 `deploy_development_applications.yml` 수동 | 이미지 빌드 → overlay `kustomization.yaml` 이미지 태그 커밋 → Argo sync |
| production | `backend/vX.Y.Z` release published → `deploy_release_applications.yml` | 동일, production overlay |
| batch | `deploy_batch.yml` 수동 | 별도 |

Argo Application:

- source: `https://github.com/bottle-note/environment-variables`, path `deploy/overlays/{env}`
- automated prune + selfHeal
- Secret `/data` ignoreDifferences

**MCP 추가 시 작업 위치** (이 API 서버 레포가 아닌 environment-variables 서브모듈/원격):

1. `deploy/base/mcp-server.yaml` (Deployment+Service)
2. overlays patch + images 항목
3. `http-route.yaml`에 호스트/경로 규칙
4. (선택) `mcp-server-secret.sops.yaml` + secrets-generator
5. GH workflow에 이미지 빌드·태그 갱신 job (1차 수동 태그도 가능)
6. DNS: `mcp.bottlenote.com` / dev 호스트 → Gateway

로컬에서 이미지 빌드·푸시·매니페스트 직접 운영 배포 금지(AGENTS.md).

### 2.8 Redis

- Opstree `RedisReplication` (prod clusterSize 3, AOF, maxmemory 1gb LRU)
- MCP rate limit·짧은 TTL handle에 **기존 Redis 공유** 가능. 별도 Redis는 불필요(1차).

## 3. Streamable HTTP · CORS · Health 상세

### 3.1 Streamable HTTP

| 항목 | 권장 |
|---|---|
| Method/Path | MCP JSON-RPC over HTTP, 주로 `POST /mcp` (+ 스펙이 요구하는 GET 스트림이 있으면 동일 path) |
| Content-Type | `application/json` (및 스트림 시 스펙 규정 MIME) |
| 필수 헤더(미래 스펙) | `Mcp-Method` / `Mcp-Name` — 게이트웨이 정책 승격 여지. 지금은 앱 레이어에서 툴명 기준 인가 |
| Session | `Mcp-Session-Id`에 서버 상태 묶지 않음. 2025-11-25 SDK가 세션을 쓰더라도 **비즈니스 상태를 세션에 두지 않음** |
| 압축 | product/admin은 Tomcat compression on (json 등). 스트리밍 응답은 압축 off 또는 스트리밍 MIME 제외 확인 |

### 3.2 CORS

현재 product/admin:

- allowlist origin (localhost, github.io 문서 등)
- methods: GET/POST/PUT/DELETE/PATCH/OPTIONS
- headers: `Authorization`, `Content-Type`
- credentials: false
- OpenAPI 문서는 별도 빈/최소 docs CORS

**MCP 권장**:

| 클라이언트 유형 | CORS 필요? |
|---|---|
| Claude Code / Cursor / Codex CLI / Desktop(mcp-remote) | **아니오** (Origin 없는 서버-사이드 또는 네이티브 HTTP) |
| 브라우저 기반 커넥터 (ChatGPT custom connector 등) | **예** — OAuth + 제한적 Origin. 1차 범위 밖 |

1차 서버 설정:

- 일반 MCP 경로: allow-origins **비움** 또는 미사용. preflight가 오면 403이어도 CLI는 무관.
- 허용 헤더에 최소한 `Authorization`, `Content-Type` (및 스펙 추가 헤더 `Mcp-Method`, `Mcp-Name`을 쓸 계획이면 포함).
- `Access-Control-Allow-Origin: *` + credentials 조합 금지.
- Agent Key를 쿼리스트링에 싣는 방식 금지 (로그·Referer 유출).

### 3.3 Health · 관측

- `/actuator/health/liveness`: 프로세스 생존만 (의존성 제외)
- `/actuator/health/readiness`: Admin API 또는 DB/Redis 중 MCP 동작에 필수인 의존성만
- 게이트웨이 `/actuator` 공개: 현재 product/admin과 동일하게 **클러스터 밖에서도 경로 허용**. 민감 엔드포인트(`env`, `heapdump` 등)는 Spring 노출 제한으로 막혀 있어야 함 — MCP도 동일 정책 강제.
- OTel: product 시크릿에 이미 OTLP 엔드포인트 키 존재. MCP도 동일 exporter. 툴 호출 span에 `gen_ai.tool.name`, agentId(해시/ID만).

## 4. Rate limit · XFF · stickiness

### 4.1 XFF 신뢰 모델

AGENTS.md / CLAUDE.md:

> 앞단 게이트웨이가 클라이언트를 통해 들어온 `X-Forwarded-For`를 제거하고 실제 접속 주소로 다시 채운다. 따라서 앱이 받는 XFF는 신뢰할 수 있다.

코드 실측(product `SecurityConfig` visitor telemetry): XFF 첫 유효 IP 사용. 동일 해석기를 MCP rate limit·감사 IP 필드에 재사용 가능.

### 4.2 Rate limit 설계

현재 코드베이스에 Bucket4j/전역 rate limit 구현 **없음**. MCP에서 신규 도입.

| 차원 | 키 | 권장 한도(초안) | 비고 |
|---|---|---|---|
| 인증 주체 | `agentId` (검증 후) | 분당 60 req / 에이전트 | 툴 호출·initialize·tools/list 포함 여부 명시 |
| 미인증/실패 | XFF IP | 분당 20 req | 브루트포스·키 스캔 완화 |
| 쓰기 툴 | `agentId` + tool name | 분당 10 (create/update) | confirm 필수와 병행 |
| 응답 | HTTP 429 + `Retry-After` | 클라이언트 재시도 가이드 | |

저장소: **Redis** (다중 파드 공유). 로컬 메모리 카운터 금지.

구현 위치:

1. 1차: Spring Filter / Interceptor in MCP 앱 (빠름)
2. 이후: Envoy Gateway global rate limit (인프라) — 앱 한도와 이중화 가능

### 4.3 Stickiness

| 대상 | sticky 필요? |
|---|---|
| MCP 세션 → 파드 | **아니오** — stateless 설계 |
| Rate limit | Redis 공유로 sticky 불필요 |
| Admin JWT 캐시(서버 내부) | 파드 로컬 캐시 TTL 짧게 또는 Redis. sticky로 해결하지 않음 |
| Service `sessionAffinity` | **설정하지 않음** (기본 None) |

Gateway HTTPRoute에 cookie affinity / consistent hash 파드 고정 넣지 않는다.

## 5. 클라이언트 설정 (Agent Key placeholder)

원칙:

- 문서·레포·커밋에 **실키 금지**. placeholder: `bn_agent_<YOUR_AGENT_KEY>` 또는 env `BOTTLENOTE_AGENT_KEY`.
- 원문 키는 `agent/api-keys.sops.yaml` 복호화 권한이 있는 운영자만 로컬에 설정.
- 서버는 키를 툴 인자로 받지 않음. 헤더(또는 OAuth)만.

Public endpoint (placeholder 호스트):

```text
https://mcp.bottlenote.com/mcp
```

dev:

```text
https://mcp.development.bottle-note.com/mcp
```

### 5.1 Claude Code

네이티브 Streamable HTTP.

```bash
claude mcp add --scope user --transport http bottlenote-admin \
  https://mcp.bottlenote.com/mcp \
  --header "Authorization: Bearer ${BOTTLENOTE_AGENT_KEY}"
```

또는 프로젝트 `.mcp.json` (키가 파일에 남지 않게 env 치환 지원 시 문서화):

```json
{
  "mcpServers": {
    "bottlenote-admin": {
      "type": "http",
      "url": "https://mcp.bottlenote.com/mcp",
      "headers": {
        "Authorization": "Bearer bn_agent_<YOUR_AGENT_KEY>"
      }
    }
  }
}
```

검증: `/mcp` 또는 툴 목록에 `bottlenote_whisky_search` 등 노출.

### 5.2 Cursor

- Global: `~/.cursor/mcp.json`
- Project: `.cursor/mcp.json`

```json
{
  "mcpServers": {
    "bottlenote-admin": {
      "url": "https://mcp.bottlenote.com/mcp",
      "headers": {
        "Authorization": "Bearer bn_agent_<YOUR_AGENT_KEY>"
      }
    }
  }
}
```

Settings → MCP에서 토글/재연결. 프로젝트 레포에 실키 커밋 금지 — 팀 공유 시 env 기반 또는 개인 global 설정.

### 5.3 Codex CLI

`~/.codex/config.toml` — Streamable HTTP + bearer env 권장(파일에 키 미기록).

```toml
[mcp_servers.bottlenote_admin]
url = "https://mcp.bottlenote.com/mcp"
bearer_token_env_var = "BOTTLENOTE_AGENT_KEY"
# 대안: 정적 헤더 (비권장 — 파일에 키 잔존)
# http_headers = { "Authorization" = "Bearer bn_agent_<YOUR_AGENT_KEY>" }
```

CLI:

```bash
export BOTTLENOTE_AGENT_KEY='bn_agent_<YOUR_AGENT_KEY>'
codex mcp add bottlenote_admin \
  --url https://mcp.bottlenote.com/mcp \
  --bearer-token-env-var BOTTLENOTE_AGENT_KEY
codex mcp get bottlenote_admin --json   # transport.type == streamable_http 확인
```

### 5.4 Claude Desktop (부록)

원격 HTTP 네이티브 미흡 시 `mcp-remote` 브릿지:

```json
{
  "mcpServers": {
    "bottlenote-admin": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "https://mcp.bottlenote.com/mcp",
        "--header",
        "Authorization:Bearer bn_agent_<YOUR_AGENT_KEY>"
      ]
    }
  }
}
```

경로: macOS `~/Library/Application Support/Claude/claude_desktop_config.json`. Node.js PATH 필요.

### 5.5 클라이언트 호환 매트릭스

| 클라이언트 | Transport | Auth 설정 | 1차 지원 |
|---|---|---|---|
| Claude Code | Streamable HTTP native | `--header` / `.mcp.json` | Yes |
| Cursor | Streamable HTTP native | `url` + `headers` | Yes |
| Codex CLI | Streamable HTTP native | `url` + `bearer_token_env_var` | Yes |
| Claude Desktop | stdio bridge (`mcp-remote`) | `--header` | Yes (문서화) |
| ChatGPT connector | OAuth 위주 | 정적 Bearer 비적합 | **이후** (안 A) |

### 5.6 온보딩 체크리스트 (운영자)

1. SOPS로 `agent/api-keys.sops.yaml`에서 본인 프로필 키 확인 (원문 재표시 정책은 운영 규약 따름)
2. `export BOTTLENOTE_AGENT_KEY=...` (shell profile 또는 secret manager)
3. 위 클라이언트 중 하나로 연결
4. 읽기 툴 1회 호출 (`bottlenote_whisky_search`)
5. 401 → 키/헤더 공백, 403 → 에이전트 status, 429 → rate limit 대기

## 6. First PR vs Later

### 6.1 1차 PR 범위 (최소 동작 수직 슬라이스)

**목표**: 내부 에이전트 1–2명이 dev에서 읽기 툴을 호출할 수 있다.

| 영역 | 포함 | 제외 |
|---|---|---|
| 앱 | MCP 모듈/서버 스캐폴드, Streamable HTTP `/mcp`, 정적 Bearer Agent Key 검증, Admin 교환 내부 호출 | OAuth/PRM, EMA |
| 툴 | 조회 툴 서브셋 (예: search/get/lookup 3–5개) | 쓰기·presign·preview 전체, 삭제 |
| 배포 | `deploy/base/mcp-server.yaml` + **development** overlay + HTTPRoute + 이미지 파이프라인(또는 임시 수동 태그) | production 다중 레플리카, PDB |
| DNS/TLS | dev 호스트만 | prod `mcp.bottlenote.com` (준비만) |
| Rate limit | 앱 레벨 agentId + XFF, Redis | Gateway global RL |
| CORS | 기본 거부/미사용 | 브라우저 커넥터 |
| 문서 | 이 plan 기반 클라이언트 설정 절 (placeholder) | 공개 레지스트리 등재 |
| 감사 | 기존 로그에 agentId·툴명 최소 필드 | #341 풀 모델 |
| 테스트 | 단위(Fake Admin)·계약 테스트 | 풀 e2e 부하 |

**1차 수락 기준 초안**:

1. `POST https://mcp.development.../mcp` + 유효 Bearer → `tools/list`에 등록 툴
2. 무효/누락 Bearer → 401
3. 파드 2개로 올려도 sticky 없이 list/call 성공 (dev에서 replicas=2 스모크 가능)
4. 시크릿·커밋에 `bn_agent_` 원문 0건
5. HTTPRoute: `/mcp`, `/actuator`만 백엔드, 기타 403

### 6.2 이후 PR / 단계

| 단계 | 내용 |
|---|---|
| P2 | 쓰기 툴 + `confirm=true` 서버 가드, presign, preview_diff |
| P3 | production 호스트, replicas≥2, PDB, 릴리스 워크플로 정식 편입 |
| P4 | OAuth 2.1 + Protected Resource Metadata (안 A), audience 검증 |
| P5 | #341 감사 로그 풀 연동 (before/after, traceId) |
| P6 | Gateway 단 rate limit / `Mcp-Method` 헤더 정책 (스펙·SDK 지원 시) |
| P7 | 브라우저 커넥터·ChatGPT 등 — CORS/OAuth 별도 설계 |
| P8 | Java SDK `2026-07-28` 마이그레이션 (트랜스포트만 교체 가정) |

### 6.3 의사결정이 필요한 항목 (구현 전 확인)

1. **모듈 배치**: 독립 `mcp-server` vs admin-api 내장 (이 문서 권장: 독립)
2. **prod 호스트 최종 문자열**: `mcp.bottlenote.com` vs `mcp.bottle-note.com` (기존 도메인은 `bottle-note.com` 하이픈 패턴 — **DNS 팀과 확정 필요**. 스펙 트렌드 문서는 `mcp.bottlenote.com` 표기)
3. **게이트웨이 스트리밍/타임아웃** 실측
4. **Rate limit 수치** (분당 60 등) 운영 합의
5. 1차 인증 안 B 유지 기간 — OAuth 이전이라도 Claude/Cursor/Codex는 Bearer로 충분

## 7. 안티패턴 (배포·클라이언트)

- product-api에 Admin MCP 툴을 붙여 권한 경계 붕괴
- sticky session / `sessionAffinity: ClientIP`로 상태 숨기기
- Agent Key 원문을 k8s Secret·환경변수 서브모듈 plain·CI 로그에 저장
- 클라이언트 설정 예시에 실키 하드코딩 후 레포 커밋
- 게이트웨이에서 `/` 전체 개방 (스캐닝 노출) — 반드시 path allowlist
- MCP 경로에 `Access-Control-Allow-Origin: *`
- rate limit을 파드 로컬 메모리로만 구현
- 레거시 SSE URL을 클라이언트 문서에 병기
- 1차 PR에 OAuth+전체 툴+prod HA를 한 번에 넣기

## 8. 미확인 / 후속 확인

- [ ] Envoy Gateway가 Streamable HTTP 응답 스트리밍을 버퍼링하는지, idle/request timeout 기본값
- [ ] 호스트명 표기: `bottlenote.com` vs `bottle-note.com` 최종 DNS
- [ ] 신규 MCP 이미지용 GH workflow 슬롯 (development/release 워크플로 확장 vs 독립)
- [ ] admin-api 내부 ClusterIP 호출 시 인증: 교환 API 경로·네트워크 정책
- [ ] Actuator 공개 범위 재검토 (MCP·admin·product 공통 보안 하드닝)
- [ ] Claude Desktop `mcp-remote`와 서버 401 challenge 호환성 실기기 테스트

## Source / 근거

### 레포 실측

- `git.environment-variables/deploy/base/{product-api,admin-api}.yaml`
- `git.environment-variables/deploy/overlays/{development,production}/{http-route,kustomization,*-patch,product-api-pdb,secrets-generator,redis-replication}.yaml`
- `git.environment-variables/deploy/argocd/bottlenote/applications/*.yaml`
- `git.environment-variables/storage/db/migration/V7__add_agent_key_auth.sql`
- `git.environment-variables/agent/api-keys.sops.yaml` (구조만, 값 미열람)
- `.github/workflows/deploy_{development,release}_applications.yml`
- `bottlenote-product-api/.../SecurityConfig.java` (XFF·CORS)
- `plan/mcp-research-spec-trends.md`, `plan/mcp-research-tool-catalog.md`, `plan/agent-key-token-exchange.md`
- AGENTS.md / CLAUDE.md 다중 인스턴스·XFF·배포 규칙

### 외부 (클라이언트 설정 패턴)

- MCP Streamable HTTP 원격 + Bearer: Cursor `url`/`headers`, Claude Code `claude mcp add --transport http`, Codex `url` + `bearer_token_env_var`
- Claude Desktop: `mcp-remote` stdio 브릿지 관례
- 스펙·Spring AI Streamable HTTP: 스펙 트렌드 문서 Source links 참조

---

**산출물 성격**: #370 define/plan 입력. 이 문서만으로 구현·배포 승인으로 간주하지 않는다.

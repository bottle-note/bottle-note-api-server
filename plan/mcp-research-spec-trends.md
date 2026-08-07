# MCP 스펙·트렌드 리서치 (2025-2026) — Admin MCP #370 설계 입력

- 조사일: 2026-08-07 / 대상 이슈: `bottle-note/workspace#370`
- 성격: 읽기 전용 리서치. 코드 변경 없음. 결론은 설계 판단의 근거이며 확정된 요구사항이 아니다.

## Summary

- **최신 스펙은 `2026-07-28`** (2026-07-28 릴리스, 직전은 `2025-11-25`). 출범 이후 최대 개정이며 **프로토콜이 stateless로 전환**됐다.
  - `initialize`/`notifications/initialized` 핸드셰이크 제거, `Mcp-Session-Id` 헤더 제거. 매 요청이 `_meta`에 프로토콜 버전과 클라이언트 capability를 싣는다 (SEP-2567, SEP-2575).
  - `server/discover` RPC 신설(서버 MUST 구현), 서버 간 sticky session 불필요 → 평범한 round-robin LB 뒤에서 다중 파드 운영 가능.
  - `tools/list` 등 목록 응답에 `ttlMs`/`cacheScope` 필수(SEP-2549), `Mcp-Method`/`Mcp-Name` 요청 헤더 필수(SEP-2243) → 게이트웨이가 body 파싱 없이 라우팅·인가·레이트리밋 가능.
  - Roots / Sampling / Logging **deprecated**(SEP-2577, 최소 12개월 유예). HTTP+SSE 레거시 트랜스포트도 Deprecated로 재분류(SEP-2596).
  - 서버 주도 요청(sampling/elicitation/roots)은 **MRTR 패턴**으로 교체: 서버가 `resultType: "input_required"` 반환 → 클라이언트가 재시도에 답을 실어 보냄(SEP-2322).
- **그런데 Java 생태계는 아직 `2025-11-25`에 머물러 있다.** MCP Java SDK 최신 GA는 **v2.0.0 (2026-06-11)**, 명시적으로 `2025-11-25` 스펙을 트래킹한다. 2026-07-28 대응 릴리스는 조사 시점(2026-08-07) 기준 없음. 공식 Tier 1 SDK는 TS/Python/Go/C#이고 Java는 여기에 없다.
- **결론(#370 관점)**: 지금 착수하면 **와이어는 `2025-11-25`(Java SDK v2.0.0 + Spring AI 2.x)로 구현하되, 설계는 stateless 전제로** 해야 한다. 세션에 의존하는 서버 상태를 만들면 2026-07-28 마이그레이션 때 통째로 다시 짜야 한다. 이건 이 저장소의 "인스턴스는 다중이다" 제약과도 정확히 일치한다.
- 인증은 스펙상 OAuth 2.1 리소스 서버가 정도(正道)지만, #370의 Agent Key(`bn_agent_*`, SHA-256 해시 저장, 에이전트당 1개)는 사실상 client_credentials용 정적 시크릿이다. 아래 "Must-adopt" 3번에서 두 안을 비교한다.

## Must-adopt for #370

### 1. Stateless 서버로 설계 (최우선)

- MCP 세션/커넥션에 어떤 서버 상태도 붙이지 않는다. 툴 호출 간 상태가 필요하면 **서버가 발급한 명시적 handle을 툴 인자로 주고받는다**(스펙이 지정한 유일한 방식).
- Java SDK의 `McpStatelessSyncServer` / Spring AI `mcp-stateless-server-boot-starter` 계열을 기본값으로 잡는다. Spring AI에서 Streamable HTTP는 `spring.ai.mcp.server.protocol=STREAMABLE`, 엔드포인트 기본 `/mcp`.
- 트랜스포트는 **Streamable HTTP만**. 레거시 HTTP+SSE는 채택하지 않는다(주요 클라이언트가 2026 중반 sunset 공지, 스펙상 Deprecated).
- k8s 다중 파드 전제이므로 sticky session 설정을 만들지 않는다. 상태가 필요하면 Redis.

### 2. State handle 하이재킹 방어 (변경 전후 검증 워크플로에 직결)

- #370의 "변경 전후 데이터 조회 및 검증"은 draft/change-set handle을 낳기 쉽다. 스펙 보안 문서가 이걸 **State Handle Hijacking** 공격면으로 명시한다.
- 필수: handle은 `SecureRandom` 기반 비순차 값, **서버 측에서 `<agentId>:<handle>` 형태로 인증 주체에 바인딩**, 다른 principal이 제시하면 거부, TTL 만료. **handle 소지를 인증으로 취급 금지.**

### 3. 인증: Agent Key를 어떻게 노출할 것인가

스펙 요구(HTTP 트랜스포트에서 authorization을 지원한다면): MCP 서버는 OAuth 2.1 리소스 서버로 동작하고 **RFC 9728 Protected Resource Metadata를 MUST 구현**, 토큰의 **audience가 자기 자신인지 MUST 검증**, 401에 `WWW-Authenticate: Bearer resource_metadata=..., scope=...`를 실어야 한다.

- **안 A (권장) — OAuth2 리소스 서버 + client_credentials**: Agent Key를 `client_id/client_secret`으로 매핑해 자체 AS(또는 admin-api)가 audience `https://mcp.bottlenote.com`인 JWT를 발급. `spring-ai-community/mcp-security`의 `McpServerOAuth2Configurer` + `spring-boot-starter-oauth2-resource-server`로 붙는다. 범용 MCP 클라이언트 호환성과 감사 추적이 가장 좋다.
- **안 B (최소 비용) — 정적 Bearer**: `Authorization: Bearer bn_agent_...`를 그대로 받고 해시 조회로 검증. 클라이언트에 수동 헤더 설정이 필요하고, OAuth 디스커버리를 요구하는 클라이언트와는 붙지 않는다. 내부 소수 에이전트 한정이면 실용적.
- 어느 안이든 **토큰 통과(token passthrough) 금지**: 클라이언트가 보낸 Admin JWT를 그대로 받아 Admin API로 넘기면 안 된다. MCP 서버는 자기 앞으로 발급된 자격만 받고, Admin JWT는 **서버가 Agent Key로 직접 교환해 내부에서만** 쓴다. #370 설계가 이미 이 형태이므로 유지하면 된다.
- 스코프 최소화: `scopes_supported`에 전체 카탈로그를 싣지 말고 읽기 기본(`admin:read`) → 쓰기(`admin:whisky:write`)를 `insufficient_scope` 403 챌린지로 승격시킨다.
- 참고 수치: 2026-05 기준 원격 MCP 서버 중 OAuth 2.1을 실제 구현한 비율은 **8.5%**. 즉 여기서 제대로 하면 상위 10%다.

### 4. 툴 설계 — 파괴적 동작은 서버가 막는다

- **명명**: `bottlenote_whisky_search`처럼 `{서비스}_{도메인}_{동작}` 스네이크. 클라이언트가 여러 MCP를 동시에 물기 때문에 접두사가 충돌 방지가 된다.
- **파라미터 8개 이하**, 멀티 목적 툴은 쪼갠다. `inputSchema`는 JSON Schema 2020-12. 날짜/enum은 포맷을 명시하지 않으면 모델이 추측한다 — ISO-8601, enum 값 목록을 스키마에 박는다.
- **annotation**: 조회 툴에 `readOnlyHint: true`, 수정 툴에 `destructiveHint`/`idempotentHint`를 정확히 단다. 단 **annotation은 클라이언트 UX 힌트일 뿐 보안 경계가 아니다**(스펙: 서버 어노테이션은 신뢰 대상 아님).
- **#370의 "비제공" 항목은 툴 설명이 아니라 서버 로직으로 강제한다**: 자동 삭제 툴은 아예 등록하지 않음, 대량 수정은 건수 상한 + 승인 handle 없으면 거부. 프롬프트로 막는 것은 방어가 아니다.
- **페이지네이션**: 조회 툴은 반드시 커서 기반 + 상한(예: 기본 20, 최대 50). 프로젝트의 `PageResponse`/`CursorPageable`을 그대로 매핑한다. 응답에 위스키 전체 필드를 덤프하지 말고 목록/상세를 분리한다.
- **툴 개수 예산**: 툴 정의 1개가 100~500 토큰. 58개 툴 세팅이 55K 토큰을 먹은 측정치가 있다. #370 범위는 조회 7~9 + 변경 4~6 정도로 **15개 내외에 묶는다**. 넘어가면 클라이언트가 tool search로 lazy-load하기 시작해 발견율이 떨어진다.
- **`tools/list`는 결정적 순서로 반환**(2026-07-28 SHOULD). LLM 프롬프트 캐시 적중률에 직접 영향.

### 5. 감사 로그·관측 (이슈 완료 기준에 포함된 항목)

- MCP의 `logging` 기능은 deprecated다. **OpenTelemetry로 간다.** 2026-07-28은 `_meta`의 `traceparent`/`tracestate`/`baggage` 전파 규약을 문서화했다(SEP-414) — 클라이언트 trace가 MCP 서버를 지나 Admin API까지 한 trace로 이어진다.
- 툴 호출당 감사 레코드에 최소: `agentId`(검증된 토큰에서 도출, 클라이언트 입력 금지), 툴 이름, 인자(민감값 마스킹), 대상 리소스 ID, **변경 전/후 스냅샷**, 결과 코드, traceId, 소요시간.
- 시맨틱 컨벤션은 OTel GenAI 규약(`gen_ai.tool.name`, `mcp.server.name`)에 맞춘다.
- 로그에 Agent Key 원문·JWT가 절대 남지 않도록 스크러빙. `agent/api-keys.sops.yaml` 정책과 일관되게.
- 이 저장소는 `#341` 감사 로그 모델과 연계하도록 되어 있으므로, MCP 전용 로그를 새로 만들지 말고 기존 감사 모델에 "요청 주체 = 에이전트" 차원을 추가하는 방향이 맞다.

### 6. Java/Spring 스택 선택

| 옵션 | 상태 (2026-08) | 판단 |
|---|---|---|
| **MCP Java SDK v2.0.0** (`io.modelcontextprotocol.sdk:mcp`) | GA 2026-06-11, `2025-11-25` 트래킹. STDIO/SSE/Streamable HTTP 내장, 웹 프레임워크 불필요 | 저수준 제어가 필요할 때. Spring AI가 이걸 감쌈 |
| **Spring AI MCP Server Boot Starter** (`spring-ai-starter-mcp-server-webmvc`) | Spring AI 2.x. `@McpTool`/`@McpToolParam` 어노테이션 API가 코어에 편입 | **권장.** 이 저장소가 Spring Boot 3.4.11/Java 21이므로 Spring AI 버전이 요구하는 Boot 하한만 확인하면 됨 |
| **spring-ai-community/mcp-security** | `McpServerOAuth2Configurer` + 자동설정. PRM/DCR/SSRF 가드 포함 | 안 A 채택 시 필수 |
| Quarkus MCP (quarkus-mcp-server) | 성숙하지만 런타임이 다름 | **부적합** — 이 저장소는 Spring 단일 스택 |

- **버전 리스크를 plan 문서에 Assumption으로 못 박을 것**: "Java SDK는 `2026-07-28`을 아직 지원하지 않으며, 지원 릴리스가 나오면 트랜스포트 계층만 교체한다." 이 가정이 깨지면(예: 클라이언트가 2026-07-28만 요구) 재개봉 대상이다.

## Nice-to-have

- **`ttlMs`/`cacheScope`**: 2026-07-28 필수 필드. 지금 SDK가 안 내보내도, 툴 카탈로그를 "정적이며 캐시 가능"하게 설계해두면 나중에 값만 채우면 된다. Admin 툴 목록은 배포 단위로만 바뀌므로 긴 TTL이 가능하다.
- **`Mcp-Method`/`Mcp-Name` 헤더 기반 게이트웨이 정책**: 앞단에서 body 파싱 없이 "쓰기 툴은 특정 에이전트만" 같은 정책을 걸 수 있다. 지금은 애플리케이션 레이어에서 같은 판정을 하되, 판정 로직을 툴 이름 기준으로 짜두면 나중에 게이트웨이로 승격 가능.
- **EMA (Enterprise-Managed Authorization) 확장**: 2026-06-18 stable, Anthropic/Microsoft/Okta 채택. 조직 IdP SSO로 MCP 접근을 통제한다. Bottle Note는 에이전트 수가 적어 지금은 과하지만, 사람 관리자가 MCP를 직접 쓰게 되면 재검토 대상.
- **MCP Registry 등재**: 공식 레지스트리에 2026-05 기준 9,652개 서버 등록. Admin MCP는 비공개라 등재 대상이 아니지만, 사내 카탈로그를 같은 스키마로 두면 나중에 확장이 쉽다.
- **Tasks 확장** (`io.modelcontextprotocol/tasks`): 이미지 업로드·대량 검증처럼 오래 걸리는 작업을 폴링형 task handle로 넘기는 공식 확장. 지금은 동기 처리로 충분하지만 이미지 업로드가 커지면 후보.

## Avoid / anti-patterns

- **레거시 HTTP+SSE 트랜스포트 채택** — Deprecated. 새로 만들면서 이걸 고를 이유가 없다.
- **세션 기반 상태**(`Mcp-Session-Id`, 커넥션 스코프 캐시, static 필드) — 스펙에서 제거됐고, 이 저장소의 다중 인스턴스 제약과도 정면 충돌.
- **토큰 통과** — 클라이언트가 준 토큰을 검증 없이 Admin API로 전달. 스펙이 명시적으로 금지. audience 검증 없이 토큰을 받는 것도 같은 범주.
- **파괴적 동작을 툴 description/annotation으로만 막기** — 서버 어노테이션은 신뢰 대상이 아니다. "승인 없는 대량 수정 금지"는 서버 코드의 건수 상한과 승인 handle로 강제한다.
- **옴니버스 스코프**(`admin:*`, `full-access`) — 탈취 시 폭발 반경이 전체가 되고 감사 로그에서 의도를 구분할 수 없다.
- **전체 카탈로그 툴 폭증** — Admin API 엔드포인트를 1:1로 툴에 매핑하는 것. 토큰 예산이 먼저 터지고 모델의 툴 선택 정확도가 떨어진다. 업무 단위로 묶는다.
- **인자 없는 무제한 조회 툴** — 페이징/상한 없는 `list_all_whiskies`류. 컨텍스트를 날린다.
- **Roots / Sampling / Logging 신규 채택** — 전부 deprecated. MCP 서버가 LLM 호출이 필요하면 provider API를 직접 쓴다.
- **외부 조사 기능을 슬쩍 넣기** — #370이 명시적으로 배제한 범위(웹 검색, Whiskybase 탐색, 출처 신뢰도 판단, 테이스팅 태그 자동 생성). 별도 리서치 MCP의 몫이다.

## 미확인 / 후속 확인 필요

- Java SDK / Spring AI의 `2026-07-28` 지원 릴리스 일정 — 조사 시점에 공개 로드맵 없음. 착수 전 java-sdk 릴리스 노트 재확인 권장.
- Spring AI 2.x가 요구하는 Spring Boot 최소 버전과 현재 3.4.11의 호환성 — 실제 의존성 해석으로 검증 필요.
- `mcp.bottlenote.com` 앞단 게이트웨이가 Streamable HTTP의 장기 응답 스트림(POST 응답 스트리밍)을 버퍼링 없이 통과시키는지 — 인프라 확인 항목.

## Source links

- [MCP Specification (latest, 2026-07-28)](https://modelcontextprotocol.io/specification/latest)
- [Key Changes — 2026-07-28 changelog](https://modelcontextprotocol.io/specification/2026-07-28/changelog)
- [The 2026-07-28 Specification (blog)](https://blog.modelcontextprotocol.io/posts/2026-07-28/)
- [Authorization — 2026-07-28](https://modelcontextprotocol.io/specification/2026-07-28/basic/authorization)
- [Security Best Practices — 2026-07-28](https://modelcontextprotocol.io/docs/2026-07-28/tutorials/security/security_best_practices)
- [Enterprise-Managed Authorization: Zero-touch OAuth for MCP](https://blog.modelcontextprotocol.io/posts/enterprise-managed-auth/)
- [The 2026 MCP Roadmap](https://blog.modelcontextprotocol.io/posts/2026-mcp-roadmap/)
- [MCP Java SDK — releases (v2.0.0, 2026-06-11)](https://github.com/modelcontextprotocol/java-sdk/releases)
- [MCP Java SDK — Server docs](https://java.sdk.modelcontextprotocol.io/latest/server/)
- [Spring AI — MCP overview](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
- [Spring AI — Streamable-HTTP MCP Servers](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-streamable-http-server-boot-starter-docs.html)
- [Spring AI — MCP Security](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-security.html)
- [spring-ai-community/mcp-security](https://github.com/spring-ai-community/mcp-security)
- [AWS — MCP tool design: practical approaches and tradeoffs](https://aws.amazon.com/blogs/machine-learning/mcp-tool-design-practical-approaches-and-tradeoffs/)
- [MCP Tool Schema Bloat: The Hidden Token Tax](https://layered.dev/mcp-tool-schema-bloat-the-hidden-token-tax-and-how-to-fix-it/)
- [Progressive Tool Discovery for Token Efficiency (discussion #1923)](https://github.com/modelcontextprotocol/modelcontextprotocol/discussions/1923)
- [MCP Observability: From Tool Call to Full-Stack Trace](https://www.groundcover.com/blog/mcp-spec-update-2026-07-28)
- [MCP Observability — monitoring AI agent tool access (Obot)](https://obot.ai/blog/mcp-observability-how-to-monitor-ai-agent-activity-in-the-enterprise/)
- [MCP Ecosystem H1 2026 Retrospective: Adoption Data Points](https://www.digitalapplied.com/blog/mcp-ecosystem-h1-2026-retrospective-adoption-data-points)

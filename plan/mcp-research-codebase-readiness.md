# Admin MCP #370 — Codebase Readiness

- 조사일: 2026-08-08 / 이슈: `bottle-note/workspace#370`
- task_id: `task_15b6beb24a16` / dispatch_id: `ctx_21b5039f2484`
- 근거: Admin/mono 실코드, Flyway V5·V7, deploy overlay, `plan/mcp-research-spec-trends.md`, `plan/mcp-research-tool-catalog.md`
- 성격: 읽기 전용 readiness 매핑. 프로덕션 코드·PR 없음. 확정 요구사항 아님.

## Summary

- **Agent 로그인(#340)은 Admin에 이미 존재**한다. 공개 경로 `POST /admin/api/v1/auth/agent` → 매핑 Admin JWT(`TokenItem`) 발급. MCP 서버가 키를 들고 내부에서만 교환하면 토큰 통과를 피할 수 있다.
- **#370 대상 Admin API(위스키·지역·증류소·카테고리·테이스팅태그·이미지)는 컨트롤러 기준으로 전부 실재**한다. 1차 툴 카탈로그(13개) 중 `preview_diff`만 전용 API 없이 서버 로컬 diff로 충분.
- **#341 운영 감사 로그는 미구현**. 있는 것은 JPA 엔티티 `create/last_modify_principal_*` 컬럼(V5)과 `AuditPrincipalType.AGENT` 열거값뿐이다. 에이전트 토큰으로 호출해도 감사 주체는 **ADMIN**으로 찍힌다(통합 테스트가 명시적으로 고정).
- **다중 파드 규칙과 현재 배포 상태가 어긋나 있다.** 프로젝트 규칙·product-api(prod replicas 2)는 multi-pod 전제지만 **admin-api는 prod/dev 모두 replicas: 1**. MCP는 처음부터 stateless로 설계해야 하며, Admin JWT 교환 시 refresh last-writer-wins와 `agents.last_used_at` 미갱신 공백을 인지해야 한다.
- **첫 수직 슬라이스 권장**: 인증 교환 + `whisky_search`/`whisky_get` 읽기 2툴. 모듈은 **신규 `bottlenote-admin-mcp` bootJar**(Admin HTTP 클라이언트)를 1순위.

## 경로·인증 실측

### 글로벌 prefix

| 레이어 | 값 | 근거 |
|---|---|---|
| servlet context-path | `/admin/api` | `bottlenote-admin-api/.../application.yml` |
| API version prefix | `/v1` (presentation Controller 자동) | `AdminApiVersionConfig` (`/v2/curation*` 제외) |
| 세션 | `STATELESS` | `SecurityConfig` |
| 기본 인가 | `@SecurityPolicy` 없으면 `REQUIRED_AUTH` | `SecurityPolicyConfig.FALLBACK_AUTH_TYPE` |
| PUBLIC | login / refresh / agent / actuator / error / openapi | 어노테이션 + explicit routes |

### Agent login

| 항목 | 실측 |
|---|---|
| 메서드·경로 | `POST /admin/api/v1/auth/agent` |
| 컨트롤러 | `AuthController.loginWithAgent` (`@PostMapping("/agent")`, `PUBLIC`) |
| 요청 DTO | `AgentLoginRequest(agentKey)` `@NotBlank` |
| 키 형식 | `^bn_agent_[A-Za-z0-9_-]{43}$` → SHA-256 (`AgentKeyHasher`) |
| 조회 | `AgentFacade.findActiveAgentAccount` → `agents` ACTIVE + 매핑 admin ACTIVE |
| 응답 | `GlobalResponse` 래핑 `TokenItem(accessToken, refreshToken)` |
| JWT | `tokenProvider.generateAdminToken(email, roles, adminId)` — **사람 관리자와 동일 클레임** |
| 부수효과 | admin `refreshToken` 덮어쓰기, `lastLoginAt` 갱신 |
| 오류 | 형식 오류 400(`AGENT_KEY_INVALID_FORMAT`), 그 외 통합 401(`AGENT_AUTHENTICATION_FAILED`) |
| 시드 | V7: 에이전트 6프로필 a–f, 매핑 `admin_users` roles=`ROOT_ADMIN` |
| 미구현 | `agents.last_used_at` 컬럼만 있고 **코드에서 갱신 없음** |

시드 에이전트가 `ROOT_ADMIN`이라는 점은 MCP 권한 최소화와 충돌한다. MCP 스코프를 툴 레이어에서 강제해도, 탈취된 Admin JWT 자체는 전체 Admin API를 열 수 있다.

### Security·토큰 소비

- Access JWT는 `AdminJwtAuthenticationFilter`가 보호 경로에 적용.
- 에이전트 발급 토큰으로 보호 API 호출 가능(통합 테스트: `GET /v1/users`).
- 재로그인 시 **이전 refresh 무효(last-writer-wins)**. 동일 에이전트 키로 MCP 파드 여러 개가 각자 refresh를 돌리면 서로 쫓아낸다 → MCP는 **access 위주 + 필요 시 교환 재시도**, refresh를 공유 상태로 쓰지 말 것.

## 대상 Admin API 맵 (컨트롤러 실측)

전체 외부 경로 = `/admin/api` + `/v1` + 컨트롤러 매핑. 인가 기본 `REQUIRED_AUTH`(Bearer Admin JWT).

### Whisky (alcohols)

| Method | Path | 핸들러 | 서비스 | MCP 카탈로그 |
|---|---|---|---|---|
| GET | `/alcohols` | `searchAlcohols` | `AlcoholQueryService.searchAdminAlcohols` | `bottlenote_whisky_search` |
| GET | `/alcohols/{alcoholId}` | `getAlcoholDetail` | `findAdminAlcoholDetailById` | `bottlenote_whisky_get` |
| GET | `/alcohols/lookup` | `getAlcoholLookups` | `AlcoholLookupService.lookup` | `bottlenote_whisky_lookup` |
| GET | `/alcohols/categories/reference` | `getCategoryReference` | `findAllCategoryReferenceMap` | `bottlenote_category_reference_get` |
| POST | `/alcohols` | `createAlcohol` | `AdminAlcoholCommandService.createAlcohol` | `bottlenote_whisky_create` (+confirm) |
| PUT | `/alcohols/{alcoholId}` | `updateAlcohol` | `updateAlcohol` | `bottlenote_whisky_update` (+confirm) |
| DELETE | `/alcohols/{alcoholId}` | `deleteAlcohol` | `deleteAlcohol` | **NEVER 노출** |

- 검색 DTO: `AdminAlcoholSearchRequest` — keyword, category, regionId, sort, page(default 0), size(default 20), includeDeleted.
- Upsert DTO: `AdminAlcoholUpsertRequest` — kor/eng name, abv, type, categories, regionId, distilleryId, age, cask, imageUrl, description, volume, tastingTagIds?.
- Lookup: Redis 스냅샷 우선 + local-cache(버전 키로 invalidation). multi-pod 허용 패턴.

### Region

| Method | Path | MCP |
|---|---|---|
| GET | `/regions`, `/regions/{id}` | list/get (1차) |
| POST/PUT/DELETE | create/update/delete | 1차 비노출 |
| PATCH | `/{id}/sort-order`, `/bulk/reorder`, `/{parentId}/children/bulk/reorder` | **NEVER bulk** |

### Distillery

| Method | Path | MCP |
|---|---|---|
| GET | `/distilleries`, `/distilleries/{id}` | list/get (1차) |
| POST/PUT/DELETE | CUD | 1차 비노출 |
| PATCH | sort-order, bulk/reorder | **NEVER bulk** |

### Category

- 전용 `/categories` CRUD 없음.
- 참조만: `GET /alcohols/categories/reference` → 그룹 맵. 카탈로그 툴 1개로 충분.

### Tasting tag

| Method | Path | MCP |
|---|---|---|
| GET | `/tasting-tags`, `/tasting-tags/{id}` | list (1차), detail 선택 |
| POST/PUT/DELETE | 태그 CUD | 1차 비노출(사람 Admin 전제) |
| POST/DELETE | `/{tagId}/alcohols` | whisky create/update의 `tastingTagIds`로 흡수, 툴 비노출 |

참조 검색 DTO 공통: `AdminReferenceSearchRequest` page/size default 0/20.

### Image (presign)

| Method | Path | 서비스 | MCP |
|---|---|---|---|
| GET | `/s3/presign-url` | `ImageUploadService.getPreSignUrlForAdmin` | `bottlenote_image_presign` |

- 쿼리: `rootPath`, `uploadSize`(default 1), `contentType`(default `image/jpeg`).
- 툴 카탈로그의 `fileName` 중심 스키마와 **필드명이 불일치** → 어댑터 필요.
- 바이너리 업로드는 클라이언트→S3 PUT(PreSign). MCP 서버는 URL만 중계.
- expiry 5분. adminId는 SecurityContext에서 강제.

## Audit vs #341

### 현재 있는 것 (entity principal audit)

- `AuditPrincipal` embeddable: `principal_id`, `principal_type`, `principal_email`.
- `AuditPrincipalType`: `USER | ADMIN | AGENT | SYSTEM | ANONYMOUS`.
- `AuditorAwareImpl`: `CustomAdminUserContext` → **항상 ADMIN**. `AGENT` 분기 없음.
- V5: alcohols/distilleries/regions 등 다수 테이블에 principal 컬럼 표준화.
- 에이전트 토큰 회원가입 통합 테스트: **감사 주체 ADMIN 유지가 계약**.

### 없는 것 (#341 / MCP 완료 기준 대비)

| 필요 항목 (#370 감사 기대) | 코드베이스 상태 |
|---|---|
| 툴 호출 단위 append-only 로그 | **없음** (audit_log 테이블/서비스 0) |
| toolName, args(마스킹), 결과 코드, 소요시간 | **없음** |
| 변경 전/후 스냅샷 | 엔티티 last_modify만 존재, before JSON 없음 |
| agentId / profileCode 차원 | JWT·Auditor에 없음. Facade payload에 profileCode만 있고 로그인 후 유실 |
| traceId 전파 | admin `management.tracing.enabled: false` |
| Agent Key / JWT 스크러빙 정책 코드화 | 로그 관행만, 전용 필터 미확인 |

### 해석

- **#341이 열림(open)인 상태**에서 #370이 “#341 연계”를 전제하면, MCP 1차 슬라이스는 (a) 구조화 애플리케이션 로그 + 후속 #341 스키마 수용 훅, 또는 (b) #341 최소 스키마를 선행 슬라이스로 묶는 선택이 필요하다.
- 엔티티 principal을 AGENT로 바꾸면 **기존 계약(통합 테스트)이 깨진다**. MCP 감사는 별도 테이블/이벤트가 맞고, JPA Auditor는 당분간 ADMIN 유지가 안전하다.
- 다만 MCP 감사 레코드에는 **반드시** `agentId`/`profileCode`(교환 시점에 Facade에서 확보)를 남겨 “어느 에이전트가 어느 Admin으로 위장했는지” 추적 가능해야 한다. 지금은 교환 직후 profileCode를 버리는 구조다.

## Multi-pod / 운영 규칙

| 규칙·사실 | 함의 |
|---|---|
| AGENTS.md: 인스턴스 다중, JVM 로컬 상태로 카운트·락·스케줄 금지 | MCP 세션/툴 상태 sticky 금지. handle·rate limit은 Redis 등 공유 저장소 |
| MCP 스펙 방향(2026-07-28 stateless) | 세션 없는 Streamable HTTP, round-robin OK (`mcp-research-spec-trends`) |
| admin-api deploy replicas: **1** (dev/prod) | 지금 Admin 자체는 single-pod. MCP 붙여도 당장 sticky 이슈는 적지만 **설계는 multi 전제** |
| product-api prod replicas: **2** | 플랫폼 운영 원칙은 multi. admin/MCP도 확장 시 동일 규칙 |
| JWT access + DB refresh | access는 무상태 검증 가능. refresh는 row last-writer-wins → MCP 파드 간 공유 refresh 금지 |
| Alcohol lookup local-cache + Redis version | 허용된 “짧은 로컬 캐시 + 공유 버전” 패턴. MCP 툴 결과 캐시도 동일 패턴만 |
| OTEL off | 분산 trace 연계는 인프라 켜기 전까지 불완전. 로그에 자체 correlation id 권장 |
| k8s health | `/admin/api/actuator/health/{liveness,readiness}` — MCP 모듈 분리 시 자체 probe 필요 |

## capability | API | gap | risk

| capability | API (실측) | gap | risk |
|---|---|---|---|
| Agent 자격 교환 | `POST /admin/api/v1/auth/agent` | 교환 후 profileCode/agentId 유실; last_used_at 미갱신 | 감사·키 사용 추적 공백. 시드 ROOT_ADMIN JWT 탈취 시 전 Admin 권한 |
| Admin JWT 사용 | Bearer on REQUIRED_AUTH routes | MCP 전용 audience/scope 없음 | 토큰이 범용 Admin 키. 스코프 최소화 불가(앱 레이어 가드 필수) |
| Whisky 검색 | `GET /alcohols` | size 상한 서버 강제 여부 툴 어댑터 확인 필요 | 큰 size로 컨텍스트 폭증 |
| Whisky 상세 | `GET /alcohols/{id}` | 없음(준비됨) | 민감 필드 과다 노출 가능 → 툴 응답 축소 권장 |
| Whisky lookup | `GET /alcohols/lookup` | Redis/local-cache 의존 | Redis 장애 시 DB 폴백 지연 |
| Category 참조 | `GET /alcohols/categories/reference` | 없음 | 낮음 |
| Region list/get | `GET /regions`, `/{id}` | 없음 | 낮음 |
| Distillery list/get | `GET /distilleries`, `/{id}` | 없음 | 낮음 |
| Tasting tag list | `GET /tasting-tags` | 없음 | 낮음 |
| Whisky create | `POST /alcohols` | confirm 가드·payload nested 스키마는 MCP 측 신규 | 오생성; 필수 필드 다수 → 모델 실수율 |
| Whisky update | `PUT /alcohols/{id}` | before 스냅샷 자동 감사 없음 | 조용한 덮어쓰기; #341 전 추적 공백 **고** |
| Image presign | `GET /s3/presign-url` | 툴 스키마 vs `rootPath/uploadSize/contentType` 불일치 | 잘못된 경로/MIME; 업로드 남용 |
| preview_diff | 없음(로컬 get+diff) | 전용 API 불필요 | 구현 누락 시 검증 워크플로 약화 |
| Delete / bulk reorder | DELETE·PATCH bulk 존재 | MCP 미등록만으로는 우회 호출 가능(Admin JWT 보유 시) | **고** — JWT 권한 과다 + NEVER 툴만으로는 부족, MCP 서버가 Admin 호출 화이트리스트 강제 필요 |
| 운영 감사 로그 | entity principal only | #341 미구현; AGENT 타입 미사용 | 컴플라이언스·사고 대응 공백 **고** |
| Multi-pod MCP | (미존재) | 모듈·배포 없음; OTEL off | 세션 상태 설계 시 실패; 관측 공백 |
| Spring AI / MCP SDK | 의존성 0 | 버전 핀·Boot 3.4.11 호환 검증 필요 | 착수 차단 가능(가정) |

## 모듈 배치 권장

### 후보 비교

| 옵션 | 구조 | 장점 | 단점 | 판정 |
|---|---|---|---|---|
| **A. 신규 `bottlenote-admin-mcp` bootJar** | settings.gradle include, WebClient/Feign → admin-api 내부 URL | 프로세스 격리, Admin 표면과 MCP 표면 분리, 독립 스케일·배포, Admin API 계약을 그대로 소비 | HTTP 홉, 배포 파이프라인 추가, DTO 이중 매핑 | **1순위** |
| B. admin-api 내부 패키지 | Kotlin presentation + Spring AI in-process | 빠른 프로토타입, 서비스 직접 호출 | MCP 포트·인증 혼재, Admin 장애 도메인 공유, 레이어 표준(Controller→Service)과 MCP 툴 계층 혼선 | 스파이크만 |
| C. mono 라이브러리 only | 툴 구현을 mono에 | 공유 로직 | mono는 도메인 라이브러리 — 프로토콜/트랜스포트 넣으면 경계 붕괴 | **비권장** |
| D. product-api 합류 | — | — | Admin 데이터 경로와 무관 | 탈락 |

### 권장 토폴로지 (A)

```
[MCP Client]
    | Streamable HTTP + Agent Key (Bearer bn_agent_* 또는 OAuth 안 A)
    v
[bottlenote-admin-mcp]  --stateless--
    | 1) POST /admin/api/v1/auth/agent  (서버 내부만, 키 저장소/ENV)
    | 2) Admin JWT로 화이트리스트 Admin API만 호출
    v
[bottlenote-admin-api] --> [mono] --> MySQL/Redis/S3
```

- mono 도메인 서비스 직접 주입 금지(옵션 A). Facade/Service를 MCP가 우회하면 트랜잭션·인가·OpenAPI 계약을 두 번 유지하게 된다.
- 의존성: Spring Boot 3.4.x, Spring AI MCP server starter, WebClient, 기존 observability 모듈 선택.
- 시크릿: Agent Key는 MCP 파드 Secret(또는 호출 클라이언트 제공 키를 요청마다 해시 검증). **Admin JWT는 프로세스 메모리·요청 스코프만**, Redis에 넣더라도 TTL·agent 바인딩.
- NEVER 목록은 **아웃바운드 HTTP 화이트리스트**로 강제(툴 미등록 + URL allowlist 이중).

## 첫 수직 슬라이스 (권장)

목표: “에이전트가 키로 인증하고, 위스키를 검색·조회하며, 호출이 추적 가능한가?”를 최소 코드로 증명.

| Step | 내용 | 완료 조건 |
|---|---|---|
| S0 | 모듈 스캐폴드 `bottlenote-admin-mcp` + Streamable HTTP `/mcp` + health | bootRun, multi-pod sticky 불필요 확인 |
| S1 | Agent Key 수신 → `POST .../auth/agent` → access 캐시(요청 단위) | 통합 테스트: 유효/무효 키, 형식 오류 |
| S2 | 툴 2개: `bottlenote_whisky_search`, `bottlenote_whisky_get` | Admin 실 API 왕복, page size ≤50 클램프 |
| S3 | 구조화 감사 로그(임시): agentProfileCode, tool, alcoholId?, status, durationMs, correlationId | 키/JWT 로그 미출력 검증. #341 테이블 생기면 동일 필드 이식 |
| S4 | 아웃바운드 allowlist: agent + alcohols GET만 | DELETE/ bulk 경로 호출 코드 경로 0 |

**슬라이스에서 의도적으로 제외**: create/update, presign, region/distillery write, #341 스키마 본구현, OAuth AS(안 A), preview_diff.

### 후속 슬라이스 순서 (참고)

1. 참조 조회(category/region/distillery/tasting-tag list) → 생성 워크플로 입력 완비  
2. image presign 어댑터 + whisky create/update(+confirm) + preview_diff  
3. #341 감사 테이블 연계 + (선택) JWT에 agent claim 또는 교환 응답 확장  
4. 에이전트 Admin 역할을 ROOT_ADMIN에서 최소 권한 역할로 하향(별 이슈)

## 차단·가정

| ID | 항목 | 조치 |
|---|---|---|
| A1 | Spring AI MCP ↔ Boot 3.4.11 호환 | 착수 전 의존성 해석 스파이크 |
| A2 | Java SDK는 2025-11-25 트래킹, 설계는 stateless | 스펙 트렌드 문서와 동일 Assumption |
| A3 | #341 open | 1차는 구조화 로그 훅; 스키마 확정 시 이식 |
| A4 | 시드 Agent = ROOT_ADMIN | 권한 축소는 보안 후속; MCP allowlist로 완화 |
| A5 | admin-api 단일 레플리카 | MCP multi 배포 시 Admin 스케일·rate limit 재검토 |
| A6 | Gitea/workspace 이슈 본문은 이 조사에서 직접 fetch 안 함 | 요구는 카탈로그·스펙 리서치·코드 실측으로 대체 |

## 관련 파일 (앵커)

- Auth: `bottlenote-admin-api/.../auth/presentation/AuthController.kt`, `bottlenote-mono/.../user/service/AdminAuthService.java`, `.../agent/**`
- Alcohols: `.../alcohols/presentation/AdminAlcoholsController.kt`, `AdminAlcoholCommandService.java`, `AdminAlcoholUpsertRequest.java`
- Region/Distillery/Tag: `AdminRegionController.kt`, `AdminDistilleryController.kt`, `AdminTastingTagController.kt`
- Image: `AdminImageUploadController.kt`, `ImageUploadService.java`
- Audit: `AuditPrincipal.java`, `AuditorAwareImpl.java`, `AuditPrincipalType.java`, V5 SQL
- Agent seed: `V7__add_agent_key_auth.sql`, `agent/api-keys.sops.yaml`(원문 비커밋 정책)
- Deploy: `git.environment-variables/deploy/base/admin-api.yaml`, overlays `replicas: 1`
- 선행 리서치: `plan/mcp-research-spec-trends.md`, `plan/mcp-research-tool-catalog.md`, `plan/agent-key-token-exchange.md`

## 결론

1. **코드베이스 readiness: 인증·대상 CRUD/조회 API는 준비됨. 감사·권한 세분화·MCP 모듈은 공백.**  
2. **#340으로 자격 교환 경로가 닫혀 있어 #370 착수 가능.** #341 없이도 읽기 슬라이스는 가능하나, 쓰기 슬라이스 전에 감사 훅(최소 로그 또는 #341)이 필요하다.  
3. **첫 수직 슬라이스 = 신규 MCP 모듈 + agent 교환 + whisky search/get + allowlist + 구조화 로그.**  
4. **삭제·bulk·토큰 통과·세션 상태·Admin JWT 스코프 과다는 최상위 리스크**이며 서버 allowlist와 무상태 설계로 막는다.

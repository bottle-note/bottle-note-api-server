# Remote Admin MCP 보안 체크리스트 — #370 설계 입력

- 조사일: 2026-08-08 / 이슈: `bottle-note/workspace#370`
- 관련: #340 Agent Key 완료, #341 감사 로그 open
- 근거: MCP Security Best Practices(Token Passthrough 금지·State Handle Hijacking), Authorization(OAuth 2.1 RS·RFC 8707 audience), `plan/mcp-research-spec-trends.md`, `plan/mcp-research-tool-catalog.md`, `plan/agent-key-token-exchange.md`
- 성격: 읽기 전용 설계 입력. 프로덕션 코드 변경 없음. 확정 요구사항 아님.
- 대상: 원격 Admin MCP (`https://mcp.bottlenote.com`, Streamable HTTP)

## Summary

| 영역 | 한 줄 결론 | 구현 위치(권장) |
|---|---|---|
| Agent Key 원문 | **로그·메트릭·트레이스·예외·응답에 절대 기록 금지**. DB는 SHA-256 해시만 (#340) | 필터/스크러버 + 감사 직렬화 |
| JWT 재사용·통과 | 클라이언트 Admin JWT **수신·전달 금지**. MCP 전용 자격만 수락, Admin JWT는 서버 내부 교환 | 인증 필터 + Admin API 클라이언트 |
| 툴 인가 | 인증(identity) ≠ 툴 권한. **툴별 scope** + 서버 가드(`confirm`, 건수 상한, 삭제 미등록) | 툴 디스패처 |
| 프롬프트 인젝션(쓰기) | description/annotation은 보안 경계가 **아님**. 쓰기 툴은 **서버 로직 + confirm + 감사**로 강제 | write 툴 핸들러 |
| 호출당 감사 | 툴 호출마다 고정 필드 세트 기록 (#341 연계). 민감값 마스킹 | 감사 인터셉터 |
| Rate limit | `mcp.bottlenote.com` 게이트웨이 + 앱 이중 제한. 쓰기 툴·presign·교환 API 우선 | Ingress/Gateway + Redis |

---

## 1. Agent Key 원문 비노출 (MUST)

### 1.1 정책

- 형식: `bn_agent_*` (에이전트당 활성 키 1개).
- 저장: **SHA-256 해시만** DB/`agents` 테이블. 원문은 `agent/api-keys.sops.yaml`(SOPS)에만 존재 (#340).
- 전송: `Authorization: Bearer bn_agent_...` 또는 OAuth client_credentials의 secret으로만. 쿼리스트링·툴 인자·본문에 키 금지.

### 1.2 체크리스트 (구현 시 전부 통과)

| # | 항목 | 검증 방법 |
|---|---|---|
| K1 | 액세스/앱/감사 로그에 `bn_agent_` 원문 0건 | 통합 테스트: 요청 후 로그 캡처 후 정규식 `bn_agent_[A-Za-z0-9_-]{8,}` 매칭 0 |
| K2 | 예외 메시지·`toString()`·Jackson 직렬화에 원문 없음 | 인증 실패 응답 body에 Bearer 값 미포함 |
| K3 | OTel span attribute / baggage에 원문 없음 | 스팬 덤프에서 `authorization`, `api_key`, `bn_agent_` 부재 |
| K4 | 메트릭 라벨에 키·토큰 값 금지 (agentId, outcome만) | Prometheus 라벨 화이트리스트 |
| K5 | 교환 실패 시 "invalid credentials" 단일 메시지 (키 일부 노출 금지) | 401 body 고정 문자열 |
| K6 | 디버그 로그 레벨에서도 헤더 덤프 금지 | `HttpLogging`/`CommonsRequestLoggingFilter` 비활성 또는 스크러빙 |
| K7 | 툴 응답·에러 payload에 키/JWT 미포함 | schema 단위 테스트 |
| K8 | CI 시크릿 스캔: 커밋·SQL·테스트 픽스처에 `bn_agent_` 원문 금지 | gitleaks / custom grep |

### 1.3 허용 기록 형태

| 허용 | 금지 |
|---|---|
| `agentId` (예: `0001`~`0006`) | `bn_agent_xxxx...` 전체/부분 |
| 키 지문 앞 4자 + `***` (운영 디버그 한시, 기본 off) | Authorization 헤더 전체 |
| 해시 조회 성공/실패 boolean | JWT access/refresh 원문 |
| `keyVersion` / `rotatedAt` | SOPS 복호화 평문 로그 |

### 1.4 스크러빙 규칙 (권장 구현)

요청 로그·감사 인자 직렬화 직전 공통 스크러버:

1. 헤더 `Authorization` → `[REDACTED]`
2. 값 정규식 `bn_agent_[A-Za-z0-9_-]+` → `[REDACTED_AGENT_KEY]`
3. JWT형 `eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+` → `[REDACTED_JWT]`
4. 필드명 화이트리스트 외 `password`, `secret`, `token`, `apiKey`, `api_key`, `refreshToken` → 마스킹

---

## 2. JWT 재사용·토큰 통과 금지 (MUST)

스펙: MCP 서버는 **자기에게 발급된 토큰만** 수락. 다운스트림으로 클라이언트 토큰을 통과(passthrough)하면 안 된다.

### 2.1 신뢰 경계

```
[MCP Client]
    |  Bearer: MCP 전용 자격 (Agent Key 또는 audience=https://mcp.bottlenote.com JWT)
    v
[MCP Server @ mcp.bottlenote.com]  --(내부)-->  Agent Key로 Admin JWT 교환 (#340)
    |  Bearer: Admin JWT (서버 메모리/짧은 TTL 캐시, 클라이언트 미노출)
    v
[Admin API]
```

### 2.2 체크리스트

| # | 항목 | 동작 |
|---|---|---|
| J1 | 클라이언트가 보낸 `admin` audience JWT 거부 | 401. audience MUST = MCP 리소스 (`https://mcp.bottlenote.com` 또는 합의 canonical URI) |
| J2 | 클라이언트가 보낸 Product/Admin 사람 JWT 거부 | 동일 401. "토큰 통과 프록시" 툴 미등록 (카탈로그 NEVER) |
| J3 | Admin API 호출용 JWT는 **MCP 서버만** #340 교환으로 획득 | 교환 엔드포인트는 MCP 내부 전용 네트워크 또는 서비스 계정 |
| J4 | 교환으로 얻은 Admin JWT를 클라이언트 응답/툴 결과에 넣지 않음 | 응답 스키마에 token 필드 없음 |
| J5 | 교환 JWT 캐시 시 Redis 키 = `agentId` 바인딩, TTL ≤ access token 잔여, 평문 로그 금지 | 다중 파드 전제(로컬 static 금지) |
| J6 | 탈취 대비: access token 짧은 TTL + 키 로테이션 절차 | 키 유출 시 해시 교체 + 기존 캐시 무효 |
| J7 | (안 A 채택 시) RFC 8707 `resource` / aud 검증 필수 | `aud` 불일치 즉시 401 |
| J8 | 토큰을 URI 쿼리에 싣지 않음 | 로깅 프록시 유출 방지 |

### 2.3 안 A vs 안 B (인증 표면)

| | 안 A OAuth RS + client_credentials | 안 B 정적 Bearer Agent Key |
|---|---|---|
| 클라이언트 제시 | MCP audience JWT | `bn_agent_*` |
| 스펙 정합 | 높음 (PRM·WWW-Authenticate) | 내부 전용 실용 |
| 공통 필수 | **Admin JWT 통과 금지**, 키 원문 비로그, 툴 인가·감사 | 동일 |

---

## 3. 툴 단위 인가 (Tool Auth) (MUST)

### 3.1 원칙

- **연결 인증 성공 ≠ 모든 툴 허용.**
- annotation(`readOnlyHint` 등)은 UX 힌트일 뿐 **인가 결정에 사용 금지**.
- 스코프는 서버 카탈로그 기준으로 최소화. 옴니버스 `admin:*` 금지.

### 3.2 권장 scope 맵 (카탈로그 13툴 기준)

| scope | 허용 툴 |
|---|---|
| `admin:read` | search/get/lookup, category reference, distillery/region list·get, tasting_tag_list, whisky_preview_diff |
| `admin:whisky:write` | whisky_create, whisky_update (+ read 포함 권장) |
| `admin:image:presign` | image_presign |

- 초기 에이전트 프로필: 기본 `admin:read`만 부여 → 쓰기 필요 시 별도 에이전트 또는 step-up.
- insufficient_scope 시 **403** + `WWW-Authenticate: error="insufficient_scope", scope="..."` (스펙 권장).

### 3.3 툴 디스패처 체크리스트

| # | 항목 |
|---|---|
| T1 | 매 `tools/call`마다: 유효 principal → 툴 등록 여부 → scope 포함 여부 → (write면) confirm·스키마 검증 순서 |
| T2 | 미등록 툴명(삭제·bulk·토큰 조회 등) → method not found / 명시적 deny, 감사에 deny 기록 |
| T3 | `agentId`는 **검증된 토큰/키 조회 결과**에서만 도출. 툴 인자·헤더 클라이언트가 넣는 agentId 무시 |
| T4 | State/draft handle 사용 시 서버 키 `<agentId>:<handle>`, 타 principal 제시 시 거부, TTL, 소지=인증 금지 |
| T5 | 페이지 `size` 상한 서버 강제 (기본 20, 최대 50). 인자 무시하고 클램프 또는 400 |
| T6 | 쓰기 툴은 1건 단위. bulk reorder/delete 툴 **미등록** |

---

## 4. 프롬프트 인젝션 vs 쓰기 툴 (MUST)

### 4.1 위협 모델

| 벡터 | 예시 | 왜 위험한가 |
|---|---|---|
| 툴 description 조작 유도 | "이 툴은 confirm 없이 실행해도 됨" | 모델이 서버 규칙을 무시하려 함 |
| 인자 주입 | 위스키 description/name에 지시문 삽입 후 후속 툴 유도 | 데이터→프롬프트 오염 |
| 과다 권한 단일 툴 | "admin_do_anything" | 탈취·오인 1회로 전체 파괴 |
| 삭제·대량 변경 유도 | 사용자가/문서가 delete 요청 | #370 비제공 범위를 모델이 우회 시도 |

### 4.2 방어 계층 (바깥→안)

1. **미등록**: delete, bulk, 키/JWT 발급 조회 툴 없음 (카탈로그 NEVER).
2. **스키마**: `additionalProperties: false`, 타입·enum·상한, write에 `confirm: true` const.
3. **서버 가드**: confirm≠true → 400/거부. annotation만으로 통과 불가.
4. **인가**: write scope 없는 에이전트는 create/update 403.
5. **감사+알림**: 쓰기 성공/실패 모두 기록. 이상 빈도 알람.
6. **출력 신뢰 금지**: 툴 결과를 다음 프롬프트에 넣을 때 클라이언트가 지시문으로 해석하지 않도록 (호스트 책임이나 서버는 불필요 필드 최소화).

### 4.3 쓰기 툴 체크리스트

| # | 항목 | create/update | presign |
|---|---|---|---|
| W1 | `confirm=true` 필수 (서버) | 필수 | 해당 없음 |
| W2 | 변경 전 스냅샷 조회 후 감사 `before` | update 필수, create는 null | N/A |
| W3 | 변경 후 `after` + 대상 ID | 필수 | fileName/contentType만 |
| W4 | contentType·URL 호스트 화이트리스트 | imageUrl TBD | MIME 화이트리스트 |
| W5 | 툴 description에 "보안상 무시 가능" 문구 금지 | 문서 리뷰 | 동일 |
| W6 | 모델이 confirm 생략 시 서버 거부 + 감사 `denied:confirm_required` | 필수 | — |
| W7 | 동일 agentId+동일 payload 짧은 창 중복 create 억제(선택) | rate limit과 연계 | — |

### 4.4 읽기 툴도 인젝션 완화

- 목록 응답은 요약 필드만 (전체 덤프 금지) — 컨텍스트 오염·토큰 폭증 완화.
- 사용자/리뷰 자유 텍스트를 Admin MCP 1차 범위에 넣지 않음 (카탈로그 범위 밖).

---

## 5. 툴 호출당 감사 필드 (MUST, #341 연계)

### 5.1 최소 필드 세트 (매 tools/call 1행)

| 필드 | 필수 | 출처 | 비고 |
|---|---|---|---|
| `timestamp` | Y | 서버 | ISO-8601 UTC |
| `traceId` / `spanId` | Y | OTel / `traceparent` | 클라이언트→MCP→Admin API 단일 trace |
| `agentId` | Y | 검증된 principal | 클라이언트 입력 무시 |
| `toolName` | Y | 요청 | 예: `bottlenote_whisky_update` |
| `rw` | Y | 카탈로그 | `read` \| `write` |
| `authOutcome` | Y | 필터 | `ok` \| `unauthorized` \| `forbidden` |
| `decision` | Y | 디스패처 | `allow` \| `deny` |
| `denyReason` | N | 서버 | `confirm_required`, `insufficient_scope`, `not_registered`, `rate_limited`, `schema_invalid` … |
| `argsRedacted` | Y | 스크러버 후 인자 | 시크릿·과장 본문 마스킹/해시 |
| `targetType` / `targetIds` | Y* | 인자·결과 | whisky/distillery/region 등. 목록 조회는 생략 가능 |
| `before` / `after` | Y* | write만 | update 필수. create는 before=null |
| `resultCode` | Y | 핸들러 | 성공/도메인에러/5xx 구분 |
| `durationMs` | Y | 인터셉터 | |
| `clientIp` | Y | 신뢰 XFF (게이트웨이 재작성 전제) | |
| `protocolVersion` | N | 요청 `_meta`/헤더 | |
| `mcpMethod` | N | `tools/call` 등 | 게이트웨이 정책 승격용 |
| `requestId` | N | 서버 생성 UUID | 멱등·지원용 |

\* write 또는 단건 get에서 필수에 가깝게 취급.

### 5.2 기록 금지

- Agent Key 원문, Admin/Product JWT, refresh token
- S3 presigned URL 쿼리 시그니처 전체 (path·bucket·만료만)
- 불필요 PII 확대 수집 (1차 카탈로그에 user/review 없음)

### 5.3 운영

- 보관: 최소 **90일** 권장 (CSA agentic MCP 가이드 수준). 조직 정책에 맞춤.
- 전송: 중앙 SIEM/로그 파이프. 앱 로컬 디스크만으로 끝내지 않음.
- #341: MCP 전용 테이블 신설보다 **기존 감사 모델에 주체 차원=에이전트** 추가 우선.
- 시맨틱: OTel GenAI/`gen_ai.tool.name`, `mcp.server.name=bottlenote-admin-mcp`.

### 5.4 감사 체크리스트

| # | 항목 |
|---|---|
| A1 | 성공·실패·deny 모두 1레코드 (실패 시 무로그 금지) |
| A2 | write 누락 `before`/`after` 시 배포 게이트 실패 (테스트) |
| A3 | 스크러버 유닛 테스트: 키·JWT 샘플 입력 → 출력에 원문 0 |
| A4 | traceId 없으면 서버가 생성해 응답/로그에 상관 ID 유지 |
| A5 | 감사 기록 실패 시 write는 **실패 처리**(감사 없는 변경 금지) 또는 동기 outbox — 정책 확정 필요 |

---

## 6. Rate limit — `mcp.bottlenote.com` (MUST)

다중 인스턴스 전제 → **Redis(또는 동등 공유 저장소)** 토큰 버킷. JVM 로컬 카운터 금지.

### 6.1 계층

| 계층 | 대상 | 목적 |
|---|---|---|
| L1 게이트웨이/Ingress | IP, TLS 종료 호스트 | 볼류메트릭 DDoS·비인증 폭주 |
| L2 앱 (인증 후) | `agentId` + 툴 클래스 | 에이전트 오남용·인젝션 루프 |
| L3 다운스트림 | Admin API·S3 presign 기존 한도 | 폭주 전파 차단 |

### 6.2 권장 초기 한도 (조정 가능 수치, 설계 출발점)

| 키 | 한도 | 비고 |
|---|---|---|
| 비인증 IP → `/mcp` | 30 req/min | 401 폭풍 완화 |
| agentId 전체 | 120 req/min | 읽기 위주 에이전트 |
| agentId + read 툴 | 100 req/min | |
| agentId + write 툴 | **10 req/min**, burst 3 | create/update |
| agentId + `image_presign` | **20 req/min** | 남용 업로드 URL 발급 |
| agentId + 토큰 교환 | **5 req/min** | 키 스터핑 완화 |
| 전역 write (클러스터) | 60 req/min | 사고 시 상한 |

초과 시: **HTTP 429** + `Retry-After`. 감사 `denyReason=rate_limited`. 본문에 키/토큰 미포함.

### 6.3 체크리스트

| # | 항목 |
|---|---|
| R1 | 한도 키에 원문 키/JWT 사용 금지 → `agentId` 또는 IP 해시 |
| R2 | sticky session 없이 동작 (stateless MCP + Redis) |
| R3 | write·presign·교환이 read보다 엄격 |
| R4 | 429도 감사·메트릭 기록 (`mcp_rate_limited_total{tool,agentId}`) |
| R5 | 운영 런북: 특정 agentId 즉시 차단(킬 스위치) Redis flag |
| R6 | Streamable HTTP 장기 스트림: 게이트웨이가 바디 버퍼로 타임아웃 내지 않는지 인프라 확인 (스펙 트렌드 미확인 항목) |

---

## 7. 배포·네트워크 가드 (SHOULD)

| # | 항목 |
|---|---|
| N1 | 외부 노출은 `https://mcp.bottlenote.com` only. Admin API는 클러스터 내부 |
| N2 | TLS 필수. HSTS 게이트웨이 |
| N3 | CORS: 브라우저 일반 사용 없다면 최소/비허용. 자격 쿠키 사용 안 함 (`Bearer` only) |
| N4 | 헬스/ready는 인증 없이 가능하되 내부 정보·키 미노출 |
| N5 | 의존 Admin API·교환 API 타임아웃·재시도 상한 (재시도 폭풍=쓰기 중복 주의, idempotency 키 검토) |

---

## 8. 구현 전 Go / No-Go 게이트

배포 전 아래가 모두 문서·테스트로 증명되어야 한다.

1. [ ] Agent Key 원문이 로그/트레이스/응답/커밋에 0건
2. [ ] 클라이언트 Admin JWT 제시 → 401, Admin API로 전달 0건
3. [ ] 삭제·bulk·키 조회 툴 미등록 + 호출 시도 deny 감사
4. [ ] write 툴 confirm 없이 호출 → 거부 + 감사
5. [ ] 툴 호출 1회 = 감사 1행 (5.1 필드)
6. [ ] write 시 before/after 존재
7. [ ] agentId·write 클래스 rate limit + 429
8. [ ] scope 없는 write → 403 insufficient_scope
9. [ ] handle 사용 시 agent 바인딩 검증 (해당 시)
10. [ ] 다중 파드에서 한도·JWT 캐시 공유 저장소 사용

---

## 9. Anti-patterns (즉시 거부)

- 클라이언트가 준 JWT를 Admin API Authorization에 그대로 설정
- `log.debug(request.headers)` 무스크러빙
- 툴 description/“AI 안전 수칙”만으로 삭제 방지
- `admin:*` 단일 스코프
- 로컬 static Map rate limit (다중 파드에서 무력)
- 감사 실패를 삼키고 write 성공 처리
- 테스트 fixture에 실키 `bn_agent_` 커밋
- handle 소지자 = 인증된 사용자로 간주

---

## 10. 후속·미확정

| 항목 | 상태 |
|---|---|
| 안 A(OAuth RS) vs 안 B(정적 Bearer) 최종 선택 | 스펙 트렌드 문서 권장 A, 비용 시 B |
| 감사 기록 실패 시 write 트랜잭션 정책 | #341과 합의 필요 |
| imageUrl 허용 호스트 화이트리스트 | TBD |
| 게이트웨이 Streamable HTTP 스트리밍 버퍼링 | 인프라 확인 |
| 사람 관리자 SSO(EMA) | 에이전트 수 적을 때 비우선 |

---

## Source links

- [MCP Security Best Practices (Token Passthrough, State Handle)](https://modelcontextprotocol.io/specification/draft/basic/security_best_practices)
- [MCP Authorization (OAuth 2.1 RS, audience, no transit tokens)](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization)
- [CSA Agentic MCP Security Best Practices](https://labs.cloudsecurityalliance.org/agentic/agentic-mcp-security-best-practices-v1/)
- [OWASP MCP Top 10](https://owasp.org/www-project-mcp-top-10/)
- 내부: `plan/mcp-research-spec-trends.md`, `plan/mcp-research-tool-catalog.md`, `plan/agent-key-token-exchange.md`

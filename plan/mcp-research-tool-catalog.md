# MCP Tool Catalog 리서치 — Admin MCP #370

- 조사일: 2026-08-08 / 이슈: `bottle-note/workspace#370`
- 관련: #340 Agent Key 완료, #341 감사 로그 open
- 근거: Admin API 컨트롤러·DTO 실측 + `plan/mcp-research-spec-trends.md`
- 성격: 읽기 전용 설계 입력. 프로덕션 코드 변경 없음. 확정 요구사항 아님.

## Summary

- 예산: **등록 툴 13개** (조회 8 + 변경 5). 스펙 트렌드 권장 15 이내.
- 명명: `bottlenote_{domain}_{action}` 스네이크. 접두사로 다중 MCP 충돌 방지.
- 인증: Agent Key(`bn_agent_*`) → 서버 내부 Admin JWT 교환(#340). 클라이언트 토큰 통과 금지.
- 파괴 정책: **삭제 툴 미등록**. 생성/수정은 서버 가드 + 클라이언트 `destructiveHint`/확인 UX.
- 페이지: 조회 기본 20·최대 50. 무제한 list 금지. `inputSchema`는 JSON Schema 2020-12 축약.
- 감사: 툴 호출마다 agentId·툴명·대상 ID·변경 전/후·traceId (#341 연계 예정).

## 공통 규칙

| 항목 | 규칙 |
|---|---|
| R/W | `read` = GET 계열, `write` = POST/PUT/PATCH |
| confirm | 파괴·비가역·다건 변경 시 `confirm=true` 필수(서버 거부). annotation만으로 막지 않음 |
| hints | read: `readOnlyHint`; write: `destructiveHint`/`idempotentHint` (UX 힌트, 보안 경계 아님) |
| 파라미터 | 툴당 8개 이하. enum·ISO 날짜 스키마 명시 |
| handle | draft/승인 handle 사용 시 `<agentId>:<handle>` 바인딩·TTL. 소지=인증 금지 |

## 등록 카탈로그 (13)

### A. 조회 (read, confirm 불필요)

| # | tool | inputSchema (필수·주요) | Admin API | 비고 |
|---|---|---|---|---|
| 1 | `bottlenote_whisky_search` | `keyword?`, `category?` enum, `regionId?`, `sortType?`, `sortOrder?`, `page?`≥0, `size?`1–50, `includeDeleted?` | `GET /alcohols` | 목록 요약만 |
| 2 | `bottlenote_whisky_get` | `alcoholId` integer **req** | `GET /alcohols/{alcoholId}` | 상세 전 필드 |
| 3 | `bottlenote_whisky_lookup` | `keyword?`, `category?`, `regionId?`, `distilleryId?`, `cursor?`≥0, `pageSize?`1–50 | `GET /alcohols/lookup` | 커서 검색 |
| 4 | `bottlenote_category_reference_get` | _(없음)_ | `GET /alcohols/categories/reference` | 생성 전 참조 |
| 5 | `bottlenote_distillery_list` | `keyword?`, `page?`, `size?`1–50 | `GET /distilleries` | 목록 |
| 6 | `bottlenote_distillery_get` | `distilleryId` **req** | `GET /distilleries/{id}` | 상세 |
| 7 | `bottlenote_region_list` | `keyword?`, `page?`, `size?`1–50 | `GET /regions` | 계층 포함 시 요약 |
| 8 | `bottlenote_region_get` | `regionId` **req** | `GET /regions/{id}` | 상세 |

### B. 변경 (write)

| # | tool | inputSchema | R/W · confirm | Admin API | 비고 |
|---|---|---|---|---|---|
| 9 | `bottlenote_whisky_create` | `korName`, `engName`, `abv`, `type`, `korCategory`, `engCategory`, `categoryGroup`, `regionId`, `distilleryId`, `age`, `cask`, `imageUrl`, `description`, `volume`, `tastingTagIds?` + `confirm` **req true** | write · **confirm** | `POST /alcohols` | Upsert DTO 1:1. 파라미터 8초과 → 구현 시 nested `payload` object 1개로 축소 권장 |
| 10 | `bottlenote_whisky_update` | `alcoholId` **req** + create와 동일 본문 + `confirm` **req true** | write · **confirm** | `PUT /alcohols/{id}` | 호출 전 서버가 현재 스냅샷 조회해 감사 로그에 before 기록 |
| 11 | `bottlenote_image_presign` | `fileName` **req**, `contentType` **req** enum 이미지 MIME | write · 불필요 | `GET /s3/presign-url` | URL만 발급. 바이너리 업로드는 MCP 밖(클라이언트→S3) |
| 12 | `bottlenote_tasting_tag_list` | `keyword?`, `page?`, `size?`1–50 | read · 불필요 | `GET /tasting-tags` | 위스키 태깅 참조 |
| 13 | `bottlenote_whisky_preview_diff` | `alcoholId` **req**, `payload` object(변경 후보 필드) | read · 불필요 | **TBD** (로컬 get+diff 또는 전용 preview API) | 변경 전후 검증 워크플로. 서버 state handle 불필요(무상태 diff) |

> 9·10 스키마 축소안: `payload: AdminAlcoholUpsert` 단일 object + `alcoholId?` + `confirm`. 토큰·파라미터 예산 준수.

### C. 입력 스키마 예시 (축약)

```json
// bottlenote_whisky_search
{ "type":"object", "properties": {
  "keyword":{"type":"string"}, "category":{"type":"string"},
  "regionId":{"type":"integer"}, "page":{"type":"integer","minimum":0},
  "size":{"type":"integer","minimum":1,"maximum":50}
}, "additionalProperties": false }

// bottlenote_whisky_update
{ "type":"object", "required":["alcoholId","payload","confirm"], "properties": {
  "alcoholId":{"type":"integer"},
  "payload":{"type":"object", "required":["korName","engName","abv","type","korCategory",
    "engCategory","categoryGroup","regionId","distilleryId","age","cask","imageUrl",
    "description","volume"],
    "properties":{ "korName":{"type":"string"}, "engName":{"type":"string"},
      "abv":{"type":"string"}, "type":{"type":"string"}, "regionId":{"type":"integer"},
      "distilleryId":{"type":"integer"}, "tastingTagIds":{"type":"array","items":{"type":"integer"}} }},
  "confirm":{"type":"boolean", "const": true}
}, "additionalProperties": false }
```

## NEVER 목록 (서버 미등록 + 로직 거부)

| 금지 | 이유 | 대응 Admin API (존재해도 MCP 비노출) |
|---|---|---|
| `*_delete` / 소프트삭제 일괄 | #370 자동 삭제 비제공 | `DELETE /alcohols/{id}`, distillery/region/tag delete |
| 무페이징 `list_all_*` | 컨텍스트 폭증 | 없음 — 검색·커서만 |
| bulk reorder / 대량 수정 단일 툴 | 폭발 반경 | `PATCH .../bulk/reorder` (region·distillery) |
| 웹검색·Whiskybase·출처 판정 | #370 범위 외 | 없음 (리서치 MCP) |
| 테이스팅 태그 자동 생성·추천 | #370 비제공 | tag create는 사람 Admin UI 전제 |
| Agent Key/JWT 발급·조회 툴 | 시크릿 노출 | #340 교환 API는 MCP 서버 내부 전용 |
| 토큰 통과 프록시 툴 | 스펙 금지 | — |
| curation/banner/user/review 전면 | 1차 #370 범위 밖 | 별도 카탈로그 확장 시 |

## confirm 정책 (파괴·쓰기)

| 동작 | confirm | 서버 가드 |
|---|---|---|
| create whisky | `confirm=true` 필수 | 필수 필드 검증, 이미지 URL 허용 호스트 검사(TBD) |
| update whisky | `confirm=true` 필수 | before 스냅샷 + after 감사; 1건만 |
| image presign | 불필요 | contentType 화이트리스트, rate limit |
| preview_diff | 불필요 | 읽기 전용, 부작용 없음 |
| 삭제·bulk | N/A | **툴 없음** — 호출 시도 시 method not found |

## Admin API 매핑 요약

```
READ  GET  /alcohols                    → bottlenote_whisky_search
      GET  /alcohols/{id}               → bottlenote_whisky_get
      GET  /alcohols/lookup             → bottlenote_whisky_lookup
      GET  /alcohols/categories/reference → bottlenote_category_reference_get
      GET  /distilleries[/{id}]         → bottlenote_distillery_list|get
      GET  /regions[/{id}]              → bottlenote_region_list|get
      GET  /tasting-tags                → bottlenote_tasting_tag_list
      GET  /s3/presign-url              → bottlenote_image_presign
WRITE POST /alcohols                    → bottlenote_whisky_create (+confirm)
      PUT  /alcohols/{id}               → bottlenote_whisky_update (+confirm)
TBD   (로컬 diff)                       → bottlenote_whisky_preview_diff
NEVER DELETE /alcohols|distilleries|regions|tasting-tags/{id}
      PATCH  /**/bulk/reorder
```

## 구현 메모 (합성 입력)

1. create/update는 `payload` 객체로 파라미터 예산 준수.
2. preview_diff는 신규 Admin API 없이 `get` + 서버측 필드 diff로 충분 → **TBD API 최소화**.
3. distillery/region **쓰기·삭제·reorder** 1차 제외(참조 조회만). 필요 시 2차 카탈로그.
4. tasting tag: list만. alcohol↔tag 연결은 whisky create/update의 `tastingTagIds`로 흡수 (`POST/DELETE /tasting-tags/{id}/alcohols` 비노출).
5. `tools/list` 결정적 순서: 위 표 #1→#13.
6. 스코프 제안: `admin:read` 기본 / `admin:whisky:write` 로 create·update 승격.

## 미확인

- Admin API 글로벌 prefix(`/api/v1` 등) — 배포 설정 확인 후 문서 경로 보정.
- presign query 파라미터 정식 이름·MIME 화이트리스트.
- #341 감사 스키마에 toolName·before/after 컬럼 존재 여부.
- nested `payload` vs flat 필드 중 클라이언트( Claude/Cursor ) 스키마 호환 실측.

## 결론 (Key findings)

- **13툴 / 삭제 0 / bulk 0** 으로 #370 위스키 조회·단건 생성·수정·이미지 준비·변경 diff에 충분.
- Admin 매핑은 alcohols·distillery·region·tasting-tags·s3 실경로 기준. preview_diff만 TBD.
- confirm은 스키마 `const:true` + 서버 검증 이중. NEVER는 미등록이 본방어.
- #340 자격 교환·#341 감사 차원을 전제로 하면 토큰 통과·고아 감사 로그를 피할 수 있다.

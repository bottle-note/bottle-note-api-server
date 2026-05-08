# Plan: Distillery 이미지 컬럼 rename (logo_img_url → image_url)

## Overview

PR #578 (증류소 어드민 CRUD) 머지 시점에 자바 필드는 `imageUrl` 로 변경했지만, DB 컬럼은 `logo_img_url` 로 남겨두고 `@Column(name = "logo_img_url")` 매핑 어노테이션으로 우회한 상태다. 자바·DB 명명 일관성을 회복하기 위해 컬럼도 `image_url` 로 rename 한다. 동시에 `Distillery.java` 의 `@Column` 매핑을 갱신하고 TestContainers init-script 4개의 INSERT 문도 새 컬럼명으로 보정한다.

### Assumptions

#### 확정 가정 (팩트체크 완료)

1. **자바 영향 범위 1곳**: `bottlenote-mono/.../alcohols/domain/Distillery.java:43` `@Column(name = "logo_img_url")` 만. 다른 자바/Kotlin 코드에서는 `logoImgPath`/`logoImgUrl` 명칭 잔존 없음 (PR #578 의 `21846ae4` 에서 모두 정리됨).
2. **schema.mysql.sql 영향 1곳 + 신규 changeset 1건**:
   - 기존 `CREATE TABLE distilleries (... logo_img_url ...)` 라인은 **수정하지 않는다** (Liquibase changeset checksum 정책상 이미 적용된 changeset 의 SQL 변경은 운영 DB 에서 거부됨).
   - 새 changeset 1건 추가: `ALTER TABLE distilleries RENAME COLUMN logo_img_url TO image_url`
3. **TestContainers init-script 4건 INSERT 문 수정 필수**:
   - `bottlenote-mono/src/test/resources/init-script/init-alcohol.sql:29`
   - `bottlenote-mono/src/test/resources/init-script/init-user-history.sql:125`
   - `bottlenote-mono/src/test/resources/init-script/init-user-mybottle-query.sql:83`
   - `bottlenote-mono/src/test/resources/init-script/init-user-mypage-query.sql:88`
   - schema 변경(ALTER) 적용 후 INSERT 가 `logo_img_url` 컬럼을 가리키면 통합 테스트 실패.
4. **서브모듈 back-up/ 디렉토리는 건드리지 않는다** — 과거 백업 SQL 들이라 변경 무관.
5. **MySQL 8.x 가정** — `RENAME COLUMN` 구문 사용 가능 (8.0.0 이상).
6. **운영 영향**: Distillery 는 위스키 원산지 메타데이터로 변경 빈도 낮은 도메인. 그러나 ApplicationContext 부팅 시 Hibernate `validate` 가 컬럼 매핑 검증하므로, schema 변경과 자바 배포가 **반드시 동기화** 되어야 한다.
7. **코드/서브모듈 변경 단위**: 서브모듈 `git.environment-variables` 에 changeset commit + push, main 레포에서 서브모듈 포인터 bump + Distillery.java + init-script 4개 함께 같은 PR 또는 같은 머지 단위로 진행해야 schema/자바 시점 어긋남이 없다.

#### 정정/확인 필요 가정

8. **운영 DB 적용 절차** — 결정 필요:
   - (a) Liquibase CLI 를 사용자가 로컬에서 dev → prod 순차 실행
   - (b) 배포 파이프라인 (Helm/Argo/Flyway 류) 이 자동 적용
   - (c) SRE 위임
   - 본 세션에서 자동 모드여도 **운영 DB 변경은 explicit 사용자 승인** 필요.
9. **무중단 vs 점검 윈도우** — 결정 필요:
   - 단발 `RENAME COLUMN` 은 schema 변경 직후 ~ 자바 배포 전 사이에 기존 자바 인스턴스가 `logo_img_url` 을 찾다가 SchemaManagementException → 부팅 실패. 이미 부팅된 인스턴스도 지속 트래픽이 컬럼을 찾으면 실패.
   - 안전한 무중단 절차: **expand-and-contract**
     1. expand: `image_url` 컬럼 추가 + `UPDATE distilleries SET image_url = logo_img_url`
     2. deploy: 자바를 `@Column(name = "image_url")` 로 배포 (이때까지 두 컬럼 모두 존재)
     3. contract: 후속 changeset 으로 `logo_img_url` DROP
   - Distillery 트래픽이 적으면 **점검 윈도우 내 단발 RENAME** 으로도 충분.
10. **PR 단위** — 결정 필요:
    - (a) 본 plan 한 PR 로 schema + Distillery.java + init-script 한꺼번에
    - (b) schema 만 먼저 PR (서브모듈) → dev 적용 후 자바 PR 분리
    - (c) expand 후 deploy 후 contract 3 단계로 PR 3 개

### Success Criteria

| # | 기준 | 검증 |
|---|------|------|
| SC1 | 서브모듈 `schema.mysql.sql` 에 RENAME COLUMN changeset 1건 추가, rollback 라인 포함 | grep |
| SC2 | `Distillery.java` `@Column(name = "image_url")` 로 변경 (또는 default 적용 가능 시 어노테이션 제거) | 코드 검사 |
| SC3 | 4개 init-script 의 INSERT 컬럼명 `logo_img_url` → `image_url` | grep 0건 |
| SC4 | 메인 레포 서브모듈 포인터를 새 changeset 포함 SHA 로 bump | git submodule status |
| SC5 | `./gradlew unit_test integration_test admin_integration_test` 모두 BUILD SUCCESSFUL, 회귀 0 | gradle |
| SC6 | dev DB 에 changeset 적용 후 `SHOW COLUMNS FROM distilleries` 결과 `image_url` 존재 / `logo_img_url` 부재 | mysql client |
| SC7 | prod DB 동일 검증 + 자바 배포 시점과 동기화 (점검 윈도우 또는 expand-and-contract) | 운영 절차 기록 |
| SC8 | 자바 부팅 시 Hibernate validate 통과 (schema-validation 에러 0) | 운영 로그 |

### Impact Scope

**서브모듈 (`git.environment-variables`)**
- `storage/mysql/changelog/schema.mysql.sql` — 새 changeset 1건 append (기존 라인 보존)

**메인 레포 (`bottle-note-api-server`)**
- `bottlenote-mono/src/main/java/app/bottlenote/alcohols/domain/Distillery.java` — `@Column(name = "image_url")`
- `bottlenote-mono/src/test/resources/init-script/init-alcohol.sql`
- `bottlenote-mono/src/test/resources/init-script/init-user-history.sql`
- `bottlenote-mono/src/test/resources/init-script/init-user-mybottle-query.sql`
- `bottlenote-mono/src/test/resources/init-script/init-user-mypage-query.sql`
- `git.environment-variables` 서브모듈 포인터 bump

**비영향**: 도메인 이벤트 / 캐시 / 다른 도메인 / Liquibase 가 아닌 형식 / Spotless / API 응답 (응답 필드명은 이미 imageUrl).

### 운영 적용 절차 (D8/D9 결정 후 확정)

**A안: 점검 윈도우 단발 RENAME (권장 — Distillery 트래픽 낮음)**
1. dev MySQL 백업
2. Liquibase update 실행 → `image_url` 컬럼 적용 확인
3. 자바 dev 배포 (Distillery.java 변경 포함)
4. 부팅·통합 테스트 검증
5. prod MySQL 백업 → Liquibase update → 자바 배포 (점검 윈도우 내)

**B안: Expand-and-contract**
- 단계별로 PR 3개. dev 부터 충분히 검증한 후 prod 적용.

### Tasks (D8/D9/D10 결정 후 분해)

진행 전 다음 결정 부탁드립니다:

| # | 결정 | 옵션 | 권장 |
|---|------|------|------|
| D8 | Liquibase CLI 실행 주체 | (a) 로컬 / (b) 파이프라인 / (c) SRE | 사용자 환경에 따름 |
| D9 | 무중단 형태 | (a) 점검 윈도우 단발 RENAME / (b) expand-and-contract | **(a)** — Distillery 트래픽 낮음, 운영 부담 적음 |
| D10 | PR 단위 | (a) 한 PR / (b) schema 우선 / (c) 3단계 분리 | **(a)** — D9 (a) 선택 시 가장 단순 |

기본 진행안 (D8 미정·D9-a·D10-a) 으로 plan 만 먼저 채워두고, 코드 변경(서브모듈 changeset + Distillery.java + init-script) 은 D8/D9/D10 확정 후 `/implement` 단계에서 일괄 처리.

## Progress Log

(implement 단계에서 채워짐)

# Plan: Admin OpenAPI 전환과 레거시 문서 제거

## Overview

**무엇을**: admin-api의 13개 컨트롤러 65개 operation을 런타임 OpenAPI 3.1 문서로 제공하고, Product/Admin의 RestDocs·AsciiDoc·Antora 문서 체계를 제거한다. Product/Admin OpenAPI JSON은 인증 없이 공개하되 일반 API CORS와 분리된 문서 전용 CORS 정책을 사용한다.

**왜**: product-api는 이미 springdoc 기반 문서를 제공하지만 admin-api는 RestDocs 기반 문서만 남아 있어 문서 생성 방식이 이원화되어 있다. 두 API를 OpenAPI로 통일하고 수동 동기화가 필요한 레거시 문서 파이프라인을 제거한다.

### Assumptions

1. Admin OpenAPI의 외부 계약은 `GET https://admin-api.development.bottle-note.com/admin/api/v1/openapi.admin.json`이다.
2. Admin OpenAPI JSON은 JWT 등 인증 정보 없이 조회할 수 있는 PUBLIC 리소스다. Swagger UI는 제공하지 않는다.
3. Product와 Admin OpenAPI JSON은 일반 API CORS 정책에서 분리된 문서 전용 CORS 정책 집합을 사용한다.
4. 문서 전용 CORS 정책의 초기 허용 origin은 비어 있다. `Origin` 없는 무인증 GET은 정상 처리하지만, `Origin`이 있는 cross-origin 요청은 Spring CORS 처리 단계에서 403으로 거부하며 permissive CORS 헤더를 반환하지 않는다. 전체 origin 허용은 후속 보안 작업에서 별도로 결정한다.
5. product-api의 기존 springdoc 문서와 품질 검증은 유지한다. admin-api에는 같은 수준의 OpenAPI 문서 계약을 추가한다.
6. Admin의 65개 operation 모두 한국어 태그·summary·정상 응답 스키마를 가지며, 실제 런타임 성공 응답은 기존과 동일한 `GlobalResponse` 형식을 유지한다.
7. Admin 컨트롤러의 `ResponseEntity<*>` 58건은 명시적인 본문 타입으로 바꾸되 직렬화 결과, HTTP 상태, 헤더 및 비즈니스 동작은 변경하지 않는다. 이미 명시적인 7건과 기존에 `GlobalResponse`를 반환하는 호출을 다시 감싸지 않는다.
8. 에러 응답 문서는 product-api의 기존 전역 오류 응답 문서화 정책과 일관되게 제공하되, 런타임 예외 처리 계약은 변경하지 않는다.
9. Admin OpenAPI가 기존 문서를 대체할 수 있음이 검증된 뒤 Product/Admin의 RestDocs 테스트, AsciiDoc 원본, Antora 사이트, 관련 빌드·CI·프로젝트 지침을 제거한다.
10. DB 스키마, 도메인 로직, 인증 정책(OpenAPI JSON 공개 예외 제외), API 요청·응답의 런타임 계약은 변경하지 않는다.

### Success Criteria

1. Admin OpenAPI 외부 URL을 무인증으로 호출하면 HTTP 200과 OpenAPI 3.1 JSON을 반환한다.
2. Admin 문서에는 현재 13개 컨트롤러의 65개 operation이 누락 없이 포함된다.
3. Admin operation에서 fallback 컨트롤러 태그, summary 누락, 메서드명과 동일한 summary, 빈 정상 응답 스키마가 각각 0건이다.
4. Admin의 정상 응답 문서가 실제 `GlobalResponse` envelope와 내부 data 타입을 표현하며, 런타임 응답을 이중으로 감싼 endpoint가 0건이다.
5. Admin 운영 컨트롤러의 `ResponseEntity<*>` 선언이 0건이며 이를 회귀 검출하는 규칙이 존재한다.
6. Admin Swagger UI 경로는 HTTP 200을 반환하지 않는다.
7. Product/Admin OpenAPI JSON을 임의의 외부 `Origin`으로 호출하거나 preflight 요청했을 때 permissive CORS 응답 헤더가 반환되지 않는다. 일반 API의 기존 CORS 동작은 유지된다.
8. Product OpenAPI JSON의 무인증 HTTP 200, OpenAPI 3.1, 기존 operation 문서 품질이 유지된다.
9. 저장소의 활성 코드·테스트·빌드·CI에서 RestDocs, AsciiDoc, Antora 실행 및 참조가 0건이다. 보존 목적의 완료 plan 기록은 검사 대상에서 제외한다.
10. 로컬에서는 컴파일, 단위 테스트, 아키텍처 규칙 및 정적 검증이 통과하고, 통합 테스트는 PR CI의 `integration_test`와 `admin_integration_test` 결과로 통과를 확인한다.

### Impact Scope

**영향 모듈**

- `bottlenote-admin-api`: springdoc 문서 계약, 공개 보안 정책, 문서 전용 CORS 정책, 명시적 응답 타입, 문서 품질 검증
- `bottlenote-product-api`: 기존 OpenAPI 유지, 문서 전용 CORS 정책 분리, 레거시 문서 자산 제거
- `bottlenote-mono`: 공통 OpenAPI/응답 타입과 아키텍처 규칙의 재사용 가능성 검토
- 루트 Gradle·version catalog·CI workflow·프로젝트 지침: 레거시 문서 파이프라인 참조 정리
- `docs/`: Antora 기반 정적 문서 사이트 제거

**변경 없음**

- Flyway 마이그레이션 및 데이터베이스 스키마
- 도메인 서비스, Facade, Repository 및 이벤트 계약
- OpenAPI JSON 외 일반 API의 인증·인가 정책
- 배포 인프라와 DNS/HTTPRoute

## Execution Mode

- mode: **delegated**
- scope: **plan, implement, test, verify, commit, push, pr**
- verification-exception:
  - 로컬 통합 테스트는 실행하지 않는다.
  - 통합 테스트는 push 후 PR의 CI에서 실행되는 결과만 확인한다.
  - 로컬에서는 컴파일, 단위 테스트, 아키텍처 규칙 및 통합 테스트를 제외한 정적 검증을 수행한다.
- stop-conditions:
  1. 가정 붕괴 — Assumptions를 깨는 발견 시 즉시 정지하고 재개봉 프로토콜을 따른다.
  2. 검증 반복 실패 — 허용된 로컬 검증 또는 CI 실패를 3회 시도 안에 해결하지 못하면 정지한다.
  3. scope 밖 행동 — 선언된 범위 밖의 되돌리기 어려운 행동이 필요해지면 직전에 정지한다.

## Tasks

### Task 1: Admin springdoc 기반과 공개 스펙 경로
- Acceptance:
  - context-path와 스펙 경로의 조합이 외부 계약 `/admin/api/v1/openapi.admin.json`과 일치한다.
  - 스펙 경로가 PUBLIC이고 Swagger UI는 비활성화된다.
  - Admin OpenAPI 상단 정보와 bearer 인증 스키마가 정의된다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin`
- Files (advisory): admin build, main/test application 설정, `OpenApiConfig`, `SecurityPolicyConfig`
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: Admin GlobalResponse와 공통 오류 스키마 문서화
- Acceptance:
  - 정상 응답 스키마가 `success/code/data/errors/meta` envelope를 사용한다.
  - 이미 envelope인 응답이 이중으로 감싸지지 않는다.
  - 오류 응답 문서가 product-api의 기존 전역 정책과 일치한다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin`
- Files (advisory): admin 전용 응답 schema customizer 3개
- Depends: 1
- Size: S
- Status: [x] done

### Task 3: Product 문서 전용 CORS 정책
- Acceptance:
  - Product 스펙 경로는 일반 API와 분리된 빈 문서 CORS set에 매칭된다.
  - `Origin`이 있는 문서 요청은 403이고 permissive CORS 헤더가 없다.
  - 문서 외 API의 기존 whitelist CORS 동작은 유지된다.
- Verification: product compile/test compile; 실행 검증은 PR CI `integration_test`
- Files (advisory): product CORS properties/config, main/test application 설정, CORS 통합 테스트
- Depends: 없음
- Size: M
- Status: [x] done

### Task 4: Admin 문서 전용 CORS 정책
- Acceptance:
  - Admin 스펙 경로는 context-path를 제외한 실제 매칭 경로로 빈 문서 CORS set에 등록된다.
  - `Origin`이 있는 문서 요청은 403이고 permissive CORS 헤더가 없다.
  - 문서 외 API의 기존 whitelist CORS 동작은 유지된다.
- Verification: admin compile/test compile; 실행 검증은 PR CI `admin_integration_test`
- Files (advisory): admin CORS properties/config, main/test application 설정, CORS 통합 테스트
- Depends: 1
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-4
- [x] Admin/Product 컴파일과 테스트 컴파일 통과
- [x] 로컬 통합 테스트 실행 0건

### Task 5: 위스키 참조 operation 문서화
- Acceptance:
  - Distillery·Region·TastingTag 22개 operation이 한국어 태그·summary·실제 data 스키마를 갖는다.
  - 세 컨트롤러의 반환 타입이 명시적 `GlobalResponse`이고 런타임 반환식은 유지된다.
  - OpenAPI components 식별자에 허용되지 않는 이름이 없다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin`
- Files (advisory): 컨트롤러 3개, 문서 어노테이션 3개
- Depends: 2
- Size: M
- Status: [ ] not done

### Task 6: 위스키 본체 operation 문서화
- Acceptance:
  - Alcohols·AdminCuration 17개 operation이 한국어 태그·summary·실제 data 스키마를 갖는다.
  - 두 컨트롤러의 반환 타입이 명시적 `GlobalResponse`이고 런타임 반환식은 유지된다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin`
- Files (advisory): 컨트롤러 2개, 문서 어노테이션 2개
- Depends: 2
- Size: M
- Status: [ ] not done

### Task 7: 인증·사용자·리뷰 operation 문서화
- Acceptance:
  - Auth·Users·Review 7개 operation이 태그·summary·data 스키마를 갖는다.
  - 로그인·갱신·Agent 로그인 3건은 무인증, 나머지는 bearer 인증으로 문서화된다.
  - 와일드카드 반환 타입만 명시적으로 바뀌고 인증·응답 동작은 유지된다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin`
- Files (advisory): 컨트롤러 3개, 문서 어노테이션 3개
- Depends: 2
- Size: M
- Status: [ ] not done

### Task 8: 배너·이미지·문의 operation 문서화
- Acceptance:
  - Banner·ImageUpload·Help 12개 operation이 태그·summary·data 스키마를 갖는다.
  - 와일드카드 반환 타입만 명시적으로 바뀌고 런타임 동작은 유지된다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin`
- Files (advisory): 컨트롤러 3개, 문서 어노테이션 3개
- Depends: 2
- Size: M
- Status: [ ] not done

### Task 9: 큐레이션 스펙 operation 문서화
- Acceptance:
  - CurationSpec·SpecBasedCuration 7개 operation이 태그·summary·data 스키마를 갖는다.
  - `/v1` 접두사가 없는 현재 두 컨트롤러의 실제 경로를 그대로 문서화한다.
  - Admin 65개 operation 문서 작성이 완성된다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin`
- Files (advisory): 컨트롤러 2개, 문서 어노테이션 2개
- Depends: 2
- Size: M
- Status: [ ] not done

### Task 10: Admin 응답 타입 회귀 규칙
- Acceptance:
  - Admin 운영 컨트롤러의 `ResponseEntity<*>`가 0건이다.
  - 와일드카드를 다시 넣으면 실패하는 `rule` 테스트가 존재한다.
  - 기존 아키텍처 규칙 전체가 통과한다.
- Verification: `./gradlew check_rule_test`
- Files (advisory): 기존 product rule 테스트 1개
- Depends: 5, 6, 7, 8, 9
- Size: S
- Status: [ ] not done

### Task 11: Admin OpenAPI 품질 통합 테스트
- Acceptance:
  - 65 operation, 태그·summary·스키마 품질과 envelope 비중첩을 검사한다.
  - 무인증 스펙 200, OpenAPI 3.1, Swagger UI 비활성, 보안 요구사항을 검사한다.
  - 모든 테스트가 CI 대상 `admin_integration` 태그를 사용한다.
- Verification: 로컬 `compileTestKotlin`; 실행은 PR CI `admin_integration_test`
- Files (advisory): Admin OpenAPI 테스트 지원·품질·노출·보안 테스트 4개
- Depends: 3, 4, 5, 6, 7, 8, 9
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 5-11
- [ ] Admin `ResponseEntity<*>` 0건 및 65 operation 문서 코드 완성
- [ ] compile, compileTest, unit, rule 검증 통과
- [ ] Draft PR push 후 Product/Admin 통합 테스트 CI 통과

### Task 12: Product RestDocs 테스트 자산 제거
- Acceptance:
  - Product의 RestDocs 테스트·지원 클래스·스니펫 템플릿과 `restdocs` 태그가 0건이다.
  - 일반 통합 테스트에 남은 RestDocs request builder import는 표준 MockMvc builder로 교체된다.
  - Product 테스트 컴파일이 통과한다.
- Verification: `./gradlew :bottlenote-product-api:compileTestJava unit_test`
- Files (advisory): product docs 테스트 디렉터리, external docs, 템플릿, `UserMyPageControllerTest`
- Depends: 없음
- Size: M
- Status: [ ] not done

### Task 13: Product AsciiDoc와 Gradle 문서 설정 제거
- Acceptance:
  - Product AsciiDoc 원본과 build의 RestDocs/AsciiDoctor 설정이 0건이다.
  - Product springdoc 의존성과 OpenAPI 구현은 유지된다.
  - Product 모듈 빌드가 허용된 검증 범위에서 통과한다.
- Verification: `./gradlew :bottlenote-product-api:build -x test -x asciidoctor`
- Files (advisory): product `src/docs/asciidoc`, product build
- Depends: 12
- Size: M
- Status: [ ] not done

### Task 14: Admin RestDocs 테스트 자산 제거
- Acceptance:
  - Admin RestDocs 테스트와 `restdocs` 태그가 0건이다.
  - Admin OpenAPI 대체 문서가 직전 Draft PR CI에서 통과한 상태다.
  - Admin 테스트 컴파일이 통과한다.
- Verification: `./gradlew :bottlenote-admin-api:compileTestKotlin`
- Files (advisory): admin docs 테스트 디렉터리
- Depends: 11 및 중간 CI checkpoint
- Size: M
- Status: [ ] not done

### Task 15: Admin AsciiDoc와 CI 문서 파이프라인 절단
- Acceptance:
  - Admin AsciiDoc/build 문서 설정과 GitHub Pages workflow가 제거된다.
  - 4개 CI·배포 workflow의 `-x asciidoctor`가 0건이다.
  - AsciiDoctor 태스크가 모두 사라진 상태에서 `build -x test`가 성립한다.
- Verification: `./gradlew build -x test`; workflow 정적 검색
- Files (advisory): admin docs/build, GitHub Pages, CI·배포 workflow 4개
- Depends: 13, 14
- Size: M
- Status: [ ] not done

### Task 16: 루트 Gradle·version catalog 문서 설정 제거
- Acceptance:
  - `asciidoctor`, `restDocsTest`, `docs_test`, `verifyRestDocsIncludes` 태스크가 0건이다.
  - version catalog의 RestDocs/AsciiDoctor 항목은 0건이고 springdoc 항목은 유지된다.
  - 통합 테스트를 제외한 전체 build가 통과한다.
- Verification: `./gradlew tasks --all`; `./gradlew build -x test --build-cache --parallel`
- Files (advisory): root build, version catalog
- Depends: 15
- Size: S
- Status: [ ] not done

### Task 17: Antora 사이트와 RestDocs 테스트 태그 정책 제거
- Acceptance:
  - `docs/`와 활성 `.adoc` 파일이 0건이다.
  - 허용 테스트 태그에서 `restdocs`가 제거된다.
  - 전체 rule 테스트가 통과한다.
- Verification: 정적 검색; `./gradlew check_rule_test`
- Files (advisory): `docs/`, `TestTagRules`
- Depends: 12, 14, 15, 16
- Size: S
- Status: [ ] not done

### Task 18: 프로젝트 지침과 문서화 가이드 갱신
- Acceptance:
  - 활성 지침·스킬·Admin 가이드의 RestDocs/AsciiDoc/Antora 절차 참조가 0건이다.
  - `AGENTS.md`/`CLAUDE.md` 및 `.agents`/`.claude` 스킬 미러가 일치한다.
  - 기존 Product OpenAPI 완료 plan은 후속 제거 사실과 모순되지 않게 보존된다.
- Verification: 관련 문자열 검색 0건; `diff -rq .claude/skills .agents/skills`
- Files (advisory): 지침 2개, 스킬 미러 6개, Admin 가이드, 완료 plan
- Depends: 16, 17
- Size: M
- Status: [ ] not done

### Final Verification
- [ ] 로컬 통합 테스트 실행 0건
- [ ] compile, compileTest, `unit_test`, `check_rule_test`, `build -x test` 통과
- [ ] 활성 RestDocs·AsciiDoc·Antora 자산과 참조 0건, springdoc 양성 확인
- [ ] 최종 push 후 PR CI의 `integration_test`, `admin_integration_test` 포함 전체 잡 통과
- [ ] 외부 개발 URL HTTP 200은 merge·개발 배포 후 확인 항목으로 명시

## Progress Log

- 2026-08-02: Execution Mode를 delegated로 확정했다. 로컬 통합 테스트는 실행하지 않고 PR CI 결과만 확인한다.
- 2026-08-02: Admin 13개 컨트롤러, 65 operation, `ResponseEntity<*>` 58건과 명시적 `GlobalResponse` 7건을 재확인했다.
- 2026-08-02: 공통 OpenAPI customizer를 mono로 이동하면 batch까지 영향이 확장되므로 admin-api 내부 구현으로 범위를 유지하기로 계획했다.
- 2026-08-02: 빈 문서 CORS set의 실제 동작을 cross-origin 403, Origin 없는 무인증 GET 200으로 정밀화했다.
- 2026-08-02: Task 1 완료. Admin springdoc 의존성·OpenAPI 설정·PUBLIC 스펙 경로를 추가했고 `compileKotlin`, `compileTestKotlin`이 통과했다. 로컬 통합 테스트는 실행하지 않았다.
- 2026-08-02: Task 3 완료. Product 문서 경로를 빈 `docs-allowed-origins` CORS set으로 분리하고 CI 실행 대상 통합 테스트 3개를 추가했다. `compileJava`, `compileTestJava`, `unit_test`가 통과했으며 통합 테스트는 실행하지 않았다.
- 2026-08-02: Task 2 완료. Admin 전용 성공 envelope·공통 오류·SecurityPolicy 기반 인증 customizer를 추가했고 `compileKotlin`이 통과했다.
- 2026-08-02: Task 4 완료. Admin 문서 경로를 빈 `docs-allowed-origins` CORS set으로 분리하고 CI 실행 대상 통합 테스트 3개를 추가했다. `compileTestKotlin`과 `unit_test`가 통과했다.

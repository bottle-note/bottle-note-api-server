# bottlenote-patterns

> bottle-note 한정 implement 함정 / 실수 방지 부록.
> 일반 컨벤션은 `plan/conventions.md`, 일반 Java/Spring 패턴은 `java-spring.md` 참조.
> 본 파일은 GSL sync 와 무관한 프로젝트 특화 reference.

## When to load

`/implement` Phase 0 에서 `plan/conventions.md` 와 함께 자동 참조. Repository / RestDocs / 이벤트 발행 관련 작업 시 우선 확인.

---

## P4. InMemory 구현체 갱신 누락 (Repository interface 변경 시)

### 함정

Repository interface 에 메서드 추가·시그니처 변경 시, JPA 구현체만 갱신하고 **InMemory 테스트 구현체 갱신을 잊는 경우** — PR #578 rebase 등에서 반복 발생.

### 점검 경로 (필수)

```text
bottlenote-test-support/src/main/java/app/bottlenote/{domain}/fixture/InMemory{Domain}Repository.java
bottlenote-product-api/src/test/java/app/bottlenote/{domain}/fixture/InMemory{Domain}Repository.java
```

Admin / 외부 모듈에서도 동일 패턴이 추가될 수 있음:
```text
bottlenote-admin-api/src/test/kotlin/.../fixture/InMemory{Domain}Repository.kt
```

### 절차 (`/implement` 또는 `/self-review` 단계)

1. Repository interface diff 의 변경 메서드 시그니처 확인
2. 위 두 경로에서 `InMemory{Domain}Repository` 검색
3. 동일 시그니처 구현 추가 (도메인 객체 상태 변경의 단순 모방으로 충분)
4. 해당 fake 를 사용하는 단위 테스트가 컴파일·통과하는지 확인

---

## P3 흡수. RestDocs / asciidoctor 워크플로우

원래 `/docs` 신규 스킬 후보였던 흐름. GSL 9 스킬로 분배:

### 책임 분리

| 단계 | GSL 스킬 | bottle-note 구체 |
|------|---------|------------------|
| RestDocs 테스트 작성 | `/test` | `Rest{Domain}ControllerDocsTest` (product), `Admin{Domain}ControllerDocsTest` (admin) |
| `.adoc` 인덱스·조각 작성 | `/implement` Slice | `bottlenote-{product\|admin}-api/src/docs/asciidoc/**.adoc` |
| asciidoctor 빌드 검증 | `/verify` L3 | `./gradlew asciidoctor` |
| admin default test 검증 | `/verify` L3 | `./gradlew :bottlenote-admin-api:test` |

### admin default test 케이스 (PR #578 사례)

- product 의 `Rest*DocsTest` 는 `@Tag("integration")` 로 분리돼 root `test` 에서 제외됨
- 그러나 **admin 의 `Admin*DocsTest` 는 default `:bottlenote-admin-api:test` 에서 함께 도는 경우 있음**
- 따라서 admin RestDocs 수정 시 `./gradlew :bottlenote-admin-api:test` 도 반드시 확인 (누락 시 PR drift 발생)

### `/verify full` 권장 명령 시퀀스

```bash
./gradlew check_rule_test                  # baseline rule check
./gradlew unit_test integration_test       # 표준 단위·통합
./gradlew admin_integration_test           # admin 통합
./gradlew :bottlenote-admin-api:test       # admin default test (DocsTest 누락 방지)
./gradlew asciidoctor                      # RestDocs HTML 빌드 검증
```

---

## P5. (GSL 참조) `Projections.constructor` 와 local record

→ 이미 GSL `java-spring.md` Tier 3 trap 에 명시됨. 본 plan 의 SC3 는 GSL 표준으로 충족.
요약: `Projections.constructor()` 인자에 쓰는 record 는 **메서드 본문 안 local record 금지**, 반드시 클래스/인터페이스 레벨 정의.

---

## 추가 함정

### publishEvent drift

- `ApplicationEventPublisher.publishEvent(...)` 호출이 도메인 로직 수정·리팩토링 중 **소리 없이 누락** 되는 경우
- 검출: `/self-review` Correctness 축에서 "이 변경이 기존 publishEvent 호출을 제거·우회하지 않았는가?"
- 보조: `git diff` 시 `publishEvent` 키워드 grep 으로 누락 여부 확인

### Facade ↔ Service 경계 drift

- Cross-domain 접근은 `{Domain}Facade` 경유 원칙
- 일부 service 가 facade 도 구현하는 기존 drift 존재 (`conventions.md` Comparison 표 참조)
- 새 작업: facade 분리 권장. 기존 drift 는 plan 에 명시되지 않은 한 정리 대상 아님

### `@ThirdPartyService` 사용 시점

→ GSL `java-spring.md` External Integration Layer 섹션 참조. bottle-note 에서는 `app.external` 또는 도메인 외부 패키지에 위치.
- 테스트 격리: 외부 client 의 fake (예: `FakeProfanityClient`, `FakeWebhookRestTemplate`) 사용
- 트랜잭션 불필요, `@Service` 포함하지만 도메인 service 와 의도 명확히 분리

### Batch 모듈 특이사항 (`/define` / `/plan` 시 주의)

`conventions.md` 의 "Batch-Specific Current Conventions" 참조. 핵심만:
- `bottlenote-batch` 는 `testFixtures` 대신 `mono` test output 직접 사용 (drift)
- `git.environment-variables` 가 main+test 양쪽 resources 에 포함됨
- main resource 에 하드코딩된 JWT secret / nonce salt default 존재
- batch source test 가 0 (테스트 공백 인지 필요)

위 항목들은 **알려진 drift** — 새 plan 이 명시적으로 정리하지 않는 한 보존.

---

## Maintenance

- 항목이 GSL 표준 `java-spring.md` 에 흡수되면 본 파일에서 제거 → 중복 방지
- 새 함정 발견 시 추가 (PR 사례 인용 권장)
- conventions.md 가 자체 갱신될 때 본 파일과 충돌하는 부분은 conventions.md 우선

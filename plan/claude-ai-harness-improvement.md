# Claude AI 하드 하네스 구조 개선안

> CLAUDE.md 중심 구조에서 Skills + Hooks 기반 분산 구조로 전환
---

## 1. 현재 구조 분석

### 현재 파일 구성

```
.claude/
├── settings.json              # 훅: SessionStart (Docker 세팅)만 존재
├── hooks/
│   └── session-start.sh       # 원격 환경 Docker 설치 전용
├── docs/
│   └── ADMIN-API-GUIDE.md     # 210줄, 스킬/훅 미연결 (사실상 사문서)
└── skills/
    └── deploy-batch/          # 배포 스킬 1개만 존재
        ├── SKILL.md
        └── scripts/*.sh

CLAUDE.md                      # ~210줄, 모든 규칙이 여기에 집중
```

### 문제점

| 문제 | 영향 |
|------|------|
| CLAUDE.md에 개요 + 구현 규칙 + 테스트 규칙 + 코드 스타일 전부 포함 | 컨텍스트 윈도우 낭비, 모든 대화에 전부 로딩됨 |
| ADMIN-API-GUIDE.md가 스킬로 연결되지 않음 | 수동으로 읽어야만 참조 가능 |
| 포매팅(spotless) 자동화 없음 | Claude가 수정한 코드가 포맷 위반 상태로 남음 |
| 커밋 메시지 검증 없음 | 규칙(타입: 제목) 위반 가능 |
| 구현 워크플로우가 코드화되지 않음 | 매번 대화로 가이드를 재설명해야 함 |

### 참고: 프론트엔드 프로젝트 (이미 스킬 구조 적용)

```
bottle-note-frontend/.claude/skills/Code/
├── SKILL.md                  # 메인 스킬
└── Workflows/
    ├── api.md                # /api - API 함수 + Query 훅 생성
    ├── component.md          # /component
    ├── fix.md                # /fix
    ├── hook.md               # /hook
    ├── page.md               # /page
    ├── refactor.md           # /refactor
    ├── store.md              # /store
    └── test.md               # /test
```

---

## 2. 목표 구조

```
CLAUDE.md                          # 경량화: 개요 + 모듈 구조 + 빌드 명령 + 핵심 원칙만

.claude/
├── settings.json                  # 훅 설정 (포매팅, 커밋 검증 등)
├── hooks/
│   ├── session-start.sh           # [기존] 원격 환경 Docker
│   ├── post-edit-format.sh        # [신규] Edit/Write 후 spotless 실행
│   └── pre-commit-validate.sh     # [신규] 커밋 메시지 규칙 검증
│
├── skills/
│   ├── deploy-batch/              # [기존] 배포 스킬
│   │
│   ├── admin-api/                 # [신규] /admin-api 스킬
│   │   ├── SKILL.md               # 진입점, Phase 분기
│   │   └── references/
│   │       ├── checklist.md       # 구현 체크리스트
│   │       ├── controller.md      # 컨트롤러 규칙
│   │       ├── service.md         # 서비스 규칙
│   │       └── test.md            # 테스트 규칙
│   │
│   ├── product-api/               # [신규] /product-api 스킬
│   │   ├── SKILL.md
│   │   └── references/
│   │       ├── checklist.md
│   │       └── controller.md
│   │
│   ├── test/                      # [신규] /test 스킬
│   │   ├── SKILL.md
│   │   └── references/
│   │       ├── unit-test.md       # 단위 테스트 (Fake/Stub 패턴)
│   │       ├── integration-test.md # 통합 테스트 (TestContainers)
│   │       └── restdocs-test.md   # RestDocs API 문서화 테스트
│   │
│   └── domain/                    # [신규] /domain 스킬
│       ├── SKILL.md
│       └── references/
│           ├── entity.md          # 엔티티 작성 규칙
│           ├── repository.md      # 3계층 레포지토리 패턴
│           └── event.md           # 이벤트 기반 아키텍처
│
└── docs/
    └── ADMIN-API-GUIDE.md         # admin-api 스킬 references로 흡수 후 제거
```

---

## 3. CLAUDE.md 경량화 방안

### 유지할 내용 (CLAUDE.md에 남길 것)

| 섹션 | 이유 |
|------|------|
| 프로젝트 개요 (기술 스택, 아키텍처) | 모든 대화에서 필요한 기본 컨텍스트 |
| 모듈 구조 다이어그램 | 모듈 간 의존성 파악에 필수 |
| 빌드 및 실행 명령어 | 빈번히 참조됨 |
| 핵심 원칙 요약 (5줄 이내) | DDD, 계층 구조, 네이밍 컨벤션 키워드만 |

### 스킬로 이동할 내용

| 현재 CLAUDE.md 섹션 | 이동 대상 스킬 |
|---------------------|---------------|
| Admin API 구현 규칙 (~50줄) | `/admin-api` 스킬 |
| 코드 작성 규칙 - 아키텍처 패턴, 네이밍, 예외 처리 | `/domain` 스킬 |
| 테스트 작성 규칙 (~30줄) | `/test` 스킬 |
| 데이터베이스 설계 - 레포지토리 계층 구조 (~60줄) | `/domain` 스킬 references/repository.md |
| 보안 및 인증, 외부 서비스 연동 | `/product-api`, `/admin-api` 스킬에서 필요 시 참조 |

### 예상 결과

- **현재**: CLAUDE.md ~210줄 (매 대화마다 전체 로딩)
- **목표**: CLAUDE.md ~60줄 (개요만) + 스킬별 on-demand 로딩

---

## 4. 스킬 설계

### 4.1 `/admin-api` 스킬

```yaml
# .claude/skills/admin-api/SKILL.md 프론트매터
---
name: admin-api
description: |
  Admin API 구현 가이드. "어드민 API", "admin api", "관리자 API" 요청 시 사용.
  mono 모듈 서비스 작성 -> admin-api 컨트롤러 -> 테스트 순서로 안내.
argument-hint: "[도메인명] [작업유형: crud|search|action]"
allowed-tools: Read, Edit, Write, Bash, Glob, Grep, Agent
---
```

**트리거 예시**:
- `/admin-api banner crud` - 배너 CRUD 구현
- `/admin-api curation search` - 큐레이션 검색 API 구현

**동작**:
1. `$ARGUMENTS[0]`(도메인)으로 기존 코드 탐색 (mono 모듈 내 해당 도메인)
2. `$ARGUMENTS[1]`(작업유형)에 따라 체크리스트 분기
3. 단계별 구현 가이드 제공 (Phase 1: mono, Phase 2: admin-api, Phase 3: test)

**핵심**: 기존 `.claude/docs/ADMIN-API-GUIDE.md`의 내용을 references/로 분산 흡수

### 4.2 `/product-api` 스킬

```yaml
---
name: product-api
description: |
  Product API 구현 가이드. "API 추가", "엔드포인트 구현" 요청 시 사용.
  mono 모듈 Facade/Service -> product-api 컨트롤러 -> 테스트 순서로 안내.
argument-hint: "[도메인명] [작업유형]"
allowed-tools: Read, Edit, Write, Bash, Glob, Grep, Agent
---
```

**동작**:
1. 도메인 탐색 (mono 모듈)
2. Facade -> Service -> Controller 순서로 구현 가이드
3. 커서 페이징 패턴 적용 (Admin과 다름)

### 4.3 `/test` 스킬

```yaml
---
name: test
description: |
  테스트 코드 작성 가이드. "테스트 작성", "test" 요청 시 사용.
  단위/통합/RestDocs 테스트를 구분하여 안내.
argument-hint: "[대상클래스 또는 도메인] [unit|integration|restdocs]"
allowed-tools: Read, Edit, Write, Bash, Glob, Grep, Agent
---
```

**트리거 예시**:
- `/test ReviewService unit` - 리뷰 서비스 단위 테스트
- `/test AdminBannerController restdocs` - 배너 API 문서화 테스트
- `/test AlcoholService integration` - 주류 서비스 통합 테스트

**동작 분기**:
- `unit`: Fake/Stub 패턴 안내, InMemory 레포지토리 사용
- `integration`: IntegrationTestSupport 상속, TestContainers, Awaitility
- `restdocs`: @WebMvcTest, MockitoBean, document() 스니펫 생성

### 4.4 `/domain` 스킬

```yaml
---
name: domain
description: |
  도메인 모델 구현 가이드. "엔티티 추가", "도메인 설계", "레포지토리" 요청 시 사용.
  엔티티/레포지토리 3계층/이벤트 패턴을 안내.
argument-hint: "[도메인명] [entity|repository|event]"
allowed-tools: Read, Edit, Write, Bash, Glob, Grep, Agent
---
```

**동작 분기**:
- `entity`: BaseEntity 상속, @Embeddable 복합키, Hibernate @Filter
- `repository`: 3계층 (DomainRepository -> JpaRepository -> QueryDSL Custom)
- `event`: ApplicationEventPublisher, @TransactionalEventListener, @Async

---

## 5. 훅 설계

### 5.1 PostToolUse: 자동 포매팅

**목적**: Claude가 Java 파일을 수정하면 자동으로 spotless 적용

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/post-edit-format.sh",
            "timeout": 30,
            "statusMessage": "spotless 포매팅 적용 중..."
          }
        ]
      }
    ]
  }
}
```

**post-edit-format.sh 동작**:
1. stdin으로 전달된 JSON에서 `tool_input.file_path` 추출
2. `.java` 파일인 경우에만 spotless 실행
3. mono 또는 product-api 모듈의 파일인지 판별 후 해당 모듈에 spotlessApply
4. `.kt` 파일은 별도 포매터 적용 (ktlint 등, 추후 검토)

**고려사항**:
- spotlessApply는 프로젝트 전체를 포매팅하므로 성능 이슈 가능
- 대안: google-java-format CLI를 직접 호출하여 단일 파일만 포매팅
- 타임아웃 30초 설정 (빌드 캐시가 있으면 빠름)

### 5.2 PreToolUse: 커밋 메시지 검증

**목적**: 커밋 메시지가 `타입: 제목` 형식을 따르는지 검증

```json
{
  "PreToolUse": [
    {
      "matcher": "Bash",
      "hooks": [
        {
          "type": "command",
          "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/pre-commit-validate.sh",
          "timeout": 5
        }
      ]
    }
  ]
}
```

**pre-commit-validate.sh 동작**:
1. stdin JSON에서 `tool_input.command` 추출
2. `git commit` 명령인지 확인 (아니면 즉시 통과)
3. 커밋 메시지 추출 후 정규식 검증: `^(feat|fix|refactor|test|docs|chore): .{1,50}$`
4. 실패 시 exit 2 (BLOCK) + stderr로 피드백 메시지 출력

### 5.3 훅 설정 종합 (settings.json 최종 형태)

```json
{
  "hooks": {
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/session-start.sh"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/post-edit-format.sh",
            "timeout": 30,
            "statusMessage": "spotless 포매팅 적용 중..."
          }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/pre-commit-validate.sh",
            "timeout": 5
          }
        ]
      }
    ]
  }
}
```

---

## 6. 구현 로드맵

### Phase 1: CLAUDE.md 경량화

- [ ] CLAUDE.md에서 스킬로 이동할 섹션 식별 및 분리
- [ ] 경량화된 CLAUDE.md 작성 (~60줄)
- [ ] 기존 내용 백업

### Phase 2: 스킬 구현

- [ ] `/admin-api` 스킬 (기존 ADMIN-API-GUIDE.md 흡수)
- [ ] `/test` 스킬
- [ ] `/product-api` 스킬
- [ ] `/domain` 스킬
- [ ] 각 스킬 테스트 (실제 트리거 및 동작 확인)

### Phase 3: 훅 구현

- [ ] `post-edit-format.sh` 작성 및 테스트
- [ ] `pre-commit-validate.sh` 작성 및 테스트
- [ ] settings.json 업데이트
- [ ] 훅 성능 측정 (spotless 소요 시간)

### Phase 4: 검증 및 조정

- [ ] 실제 작업 시나리오 테스트 (Admin API 1개 구현해보기)
- [ ] 컨텍스트 윈도우 사용량 비교 (전/후)
- [ ] 스킬 트리거 정확도 확인
- [ ] 훅 오탐/미탐 확인

---

## 7. 의사결정 필요 사항

| # | 질문 | 선택지 | 메모 |
|---|------|--------|------|
| 1 | spotless를 PostToolUse에서 실행할지, 커밋 전에만 실행할지 | A: 매 편집마다 / B: 커밋 전만 | A는 정확하지만 느림, B는 빠르지만 중간 상태가 어색 |
| 2 | 스킬 간 공통 규칙(네이밍, 예외 처리)을 어디에 둘지 | A: CLAUDE.md / B: 별도 공통 스킬 / C: 각 스킬에 중복 | A가 가장 단순 |
| 3 | admin-api 스킬에 context: fork를 적용할지 | A: fork (독립 컨텍스트) / B: 기본 (대화 공유) | fork면 대화 이력 참조 불가 |
| 4 | Kotlin 파일 포매팅은 어떻게 할지 | A: ktlint / B: ktfmt / C: 당분간 제외 | admin-api는 Kotlin |

---

## 8. 기대 효과

| 지표 | 현재 | 목표 |
|------|------|------|
| CLAUDE.md 크기 | ~210줄 (항상 로딩) | ~60줄 (개요만) |
| 구현 가이드 접근 | 수동 (대화로 설명) | `/admin-api banner crud` 한 줄 |
| 코드 포매팅 | 수동 spotlessApply | 자동 (훅) |
| 커밋 메시지 검증 | 없음 | 자동 (훅) |
| 새 기능 구현 시간 | 매번 규칙 재설명 필요 | 스킬이 컨텍스트 제공 |

---

## 참고 자료

- Claude Code Hooks 공식 문서: https://code.claude.com/docs/en/hooks
- Claude Code Skills 공식 문서: https://code.claude.com/docs/en/skills
- 프론트엔드 스킬 구조: `bottle-note-frontend/.claude/skills/Code/`
- 기존 배포 스킬: `.claude/skills/deploy-batch/`

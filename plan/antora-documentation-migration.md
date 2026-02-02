# Antora 기반 API 문서 시스템 마이그레이션 계획

```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **PLANNING**
Start Date: 2026-02-02
Last Updated: 2026-02-02

** Completed Work **
- 현행 문서 시스템 분석 완료
- Antora 아키텍처 조사 완료
- 기존 GitHub Actions 워크플로우 분석 완료

** Remaining Work **
- Antora 디렉토리 구조 설계
- antora-playbook.yml 작성
- GitHub Actions 워크플로우 수정
- 기존 ADOC 문서 include 경로 수정
================================================================================
```

---

## 1. Antora란?

### 1.1 개요

Antora는 **멀티 리포지토리, 멀티 버전 문서 사이트 생성기**입니다.

| 특징 | 설명 |
|------|------|
| **AsciiDoc 네이티브** | AsciiDoc 마크업 언어를 기본 지원 |
| **멀티 버전** | 동일 문서의 여러 버전을 동시에 관리 |
| **멀티 컴포넌트** | 여러 프로젝트/모듈 문서를 하나의 사이트로 통합 |
| **Git 기반** | Git 저장소에서 직접 콘텐츠 수집 |
| **정적 사이트** | HTML 정적 파일 생성 → 어디서든 호스팅 가능 |

### 1.2 현재 시스템 vs Antora

| 항목 | 현재 (Asciidoctor + Jekyll) | Antora |
|------|------------------------------|--------|
| **문서 형식** | AsciiDoc (.adoc) | AsciiDoc (.adoc) |
| **빌드 도구** | Asciidoctor → HTML | Antora (Asciidoctor 내장) |
| **배포** | Jekyll → GitHub Pages | Antora → GitHub Pages |
| **버전 관리** | 단일 버전 | 멀티 버전 지원 |
| **검색** | 커스텀 JavaScript | 내장 검색 또는 Algolia |
| **네비게이션** | 커스텀 탭 UI | 자동 생성 사이드바 |
| **테마** | 직접 CSS 작성 | UI Bundle 시스템 |

---

## 2. Antora 디렉토리 구조

### 2.1 표준 구조

```
docs/                              # 문서 루트
├── antora-playbook.yml            # Antora 설정 파일 (필수)
├── modules/
│   ├── ROOT/                      # 기본 모듈 (홈페이지)
│   │   ├── pages/
│   │   │   └── index.adoc
│   │   └── nav.adoc               # 네비게이션 정의
│   │
│   ├── product-api/               # Product API 모듈
│   │   ├── pages/                 # 페이지 파일
│   │   │   ├── index.adoc
│   │   │   └── api/
│   │   │       ├── overview/
│   │   │       ├── alcohols/
│   │   │       ├── review/
│   │   │       └── ...
│   │   ├── partials/              # 재사용 콘텐츠
│   │   ├── examples/              # 코드 예제 (스니펫)
│   │   │   └── generated-snippets/  ← REST Docs 스니펫
│   │   └── nav.adoc
│   │
│   └── admin-api/                 # Admin API 모듈
│       ├── pages/
│       │   ├── index.adoc
│       │   └── api/
│       │       ├── overview/
│       │       ├── admin-auth/
│       │       ├── admin-alcohols/
│       │       └── ...
│       ├── examples/
│       │   └── generated-snippets/  ← REST Docs 스니펫
│       └── nav.adoc
│
└── antora.yml                     # 컴포넌트 버전 설명자
```

### 2.2 핵심 파일 설명

| 파일 | 역할 |
|------|------|
| `antora-playbook.yml` | 사이트 전체 설정 (소스 위치, 출력 경로, UI 번들) |
| `antora.yml` | 컴포넌트/버전 정보 (name, version, title) |
| `nav.adoc` | 사이드바 네비게이션 구조 정의 |
| `pages/` | 게시될 페이지 파일 |
| `partials/` | include용 재사용 콘텐츠 조각 |
| `examples/` | 코드 예제 파일 (REST Docs 스니펫 포함) |

---

## 3. 현재 시스템 분석

### 3.1 현재 GitHub Actions 워크플로우

**파일**: `.github/workflows/github-pages.yml`

```yaml
# 현재 동작 흐름
1. REST Docs 테스트 실행
   ./gradlew restDocsTest

2. Asciidoctor로 HTML 생성
   ./gradlew :bottlenote-product-api:asciidoctor
   ./gradlew :bottlenote-admin-api:asciidoctor

3. HTML 파일을 docs/ 폴더로 복사
   cp bottlenote-product-api/build/docs/asciidoc/product-api.html docs/
   cp bottlenote-admin-api/build/docs/asciidoc/admin-api.html docs/

4. Jekyll로 GitHub Pages 빌드/배포
```

### 3.2 현재 문서 구조

```
bottlenote-product-api/
└── src/docs/asciidoc/
    ├── product-api.adoc              # 메인 문서
    └── api/
        ├── overview/
        ├── alcohols/
        ├── review/
        └── ...

bottlenote-admin-api/
└── src/docs/asciidoc/
    ├── admin-api.adoc                # 메인 문서
    └── api/
        ├── overview/
        ├── admin-auth/
        ├── admin-alcohols/
        └── ...

docs/
├── index.html                        # 탭 전환 UI
├── product-api.html                  # 빌드된 Product API 문서
└── admin-api.html                    # 빌드된 Admin API 문서
```

### 3.3 현재 Include 방식

```asciidoc
ifndef::snippets[]
:snippets: ../../build/generated-snippets
endif::[]

include::{snippets}/admin/help/list/query-parameters.adoc[]
```

- `{snippets}` 변수로 빌드 시 생성된 스니펫 경로 참조
- Gradle 빌드 시 `generated-snippets/` 폴더에 REST Docs 스니펫 생성

---

## 4. 마이그레이션 방안

### 4.1 선택지 비교

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **A. 완전 Antora 전환** | Antora가 AsciiDoc을 직접 빌드 | 풀 기능 활용 | include 경로 전체 수정 필요 |
| **B. 하이브리드** | Asciidoctor로 빌드 후 Antora가 HTML 수집 | 기존 구조 유지 | Antora 기능 제한적 |
| **C. 현재 구조 유지** | 기존 방식 계속 사용 | 변경 없음 | Antora 도입 불가 |

### 4.2 권장 방안: A. 완전 Antora 전환

**이유**:
1. Antora의 멀티 버전, 검색, 네비게이션 기능 활용
2. 장기적으로 유지보수 용이
3. Spring 공식 문서도 Antora 사용 중

---

## 5. 마이그레이션 작업 항목

### 5.1 Phase 1: Antora 구조 생성

#### 5.1.1 antora.yml 생성

**파일**: `docs/antora.yml`

```yaml
name: bottle-note
title: Bottle Note API
version: '1.0'
start_page: ROOT:index.adoc

nav:
  - modules/ROOT/nav.adoc
  - modules/product-api/nav.adoc
  - modules/admin-api/nav.adoc
```

#### 5.1.2 antora-playbook.yml 생성

**파일**: `docs/antora-playbook.yml`

```yaml
site:
  title: Bottle Note API Documentation
  url: https://bottle-note.github.io/bottle-note-api-server
  start_page: bottle-note::index.adoc

content:
  sources:
    - url: .
      start_path: docs
      branches: HEAD

ui:
  bundle:
    url: https://gitlab.com/antora/antora-ui-default/-/jobs/artifacts/HEAD/raw/build/ui-bundle.zip?job=bundle-stable
    snapshot: true

output:
  dir: ./_site

asciidoc:
  attributes:
    page-pagination: true
```

#### 5.1.3 네비게이션 파일 생성

**파일**: `docs/modules/ROOT/nav.adoc`

```asciidoc
* xref:index.adoc[홈]
* xref:product-api:index.adoc[Product API]
* xref:admin-api:index.adoc[Admin API]
```

**파일**: `docs/modules/product-api/nav.adoc`

```asciidoc
* 개요
** xref:api/overview/overview.adoc[API 서버 경로]
** xref:api/overview/global-response.adoc[공통 응답]
** xref:api/overview/global-exception.adoc[예외 처리]

* 술(Alcohol) API
** xref:api/alcohols/search.adoc[검색]
** xref:api/alcohols/detail.adoc[상세 조회]
// ... 추가 항목
```

### 5.2 Phase 2: 기존 ADOC 파일 이동

#### 5.2.1 파일 복사 스크립트

```bash
#!/bin/bash

# Product API 문서 복사
mkdir -p docs/modules/product-api/pages/api
cp -r bottlenote-product-api/src/docs/asciidoc/api/* docs/modules/product-api/pages/api/

# Admin API 문서 복사
mkdir -p docs/modules/admin-api/pages/api
cp -r bottlenote-admin-api/src/docs/asciidoc/api/* docs/modules/admin-api/pages/api/
```

#### 5.2.2 Include 경로 수정

**변경 전**:
```asciidoc
include::{snippets}/admin/help/list/query-parameters.adoc[]
```

**변경 후**:
```asciidoc
include::example$generated-snippets/admin/help/list/query-parameters.adoc[]
```

### 5.3 Phase 3: GitHub Actions 수정

#### 5.3.1 새 워크플로우

**파일**: `.github/workflows/github-pages.yml` (수정)

```yaml
name: Deploy Antora Documentation

on:
  push:
    branches: [ "main" ]
    paths:
      - 'bottlenote-*/src/docs/**'
      - 'bottlenote-*/src/test/java/**/docs/**'
      - 'bottlenote-*/src/test/kotlin/**/docs/**'
      - 'docs/**'
      - '.github/workflows/github-pages.yml'
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Generate REST Docs snippets
        run: ./gradlew restDocsTest

      - name: Copy snippets to Antora structure
        run: |
          # Product API 스니펫 복사
          mkdir -p docs/modules/product-api/examples/generated-snippets
          cp -r bottlenote-product-api/build/generated-snippets/* \
                docs/modules/product-api/examples/generated-snippets/

          # Admin API 스니펫 복사
          mkdir -p docs/modules/admin-api/examples/generated-snippets
          cp -r bottlenote-admin-api/build/generated-snippets/* \
                docs/modules/admin-api/examples/generated-snippets/

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Install Antora
        run: npm install -g @antora/cli @antora/site-generator

      - name: Build Antora site
        run: |
          cd docs
          antora antora-playbook.yml

      - name: Setup Pages
        uses: actions/configure-pages@v5

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: docs/_site

  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    needs: build
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

---

## 6. 체크리스트

### Phase 1: 구조 생성
- [ ] `docs/antora.yml` 생성
- [ ] `docs/antora-playbook.yml` 생성
- [ ] `docs/modules/ROOT/` 디렉토리 생성
- [ ] `docs/modules/product-api/` 디렉토리 생성
- [ ] `docs/modules/admin-api/` 디렉토리 생성
- [ ] 각 모듈 `nav.adoc` 작성

### Phase 2: 문서 이동
- [ ] Product API ADOC 파일 복사
- [ ] Admin API ADOC 파일 복사
- [ ] Include 경로 수정 (`{snippets}` → `example$generated-snippets`)
- [ ] `tasting-tags.adoc:23` 오타 수정 ("룰" 제거)

### Phase 3: 빌드 설정
- [ ] `.github/workflows/github-pages.yml` 수정
- [ ] 로컬 Antora 빌드 테스트
- [ ] GitHub Actions 테스트

### Phase 4: 검증
- [ ] 모든 페이지 정상 렌더링 확인
- [ ] 모든 include 스니펫 정상 로드 확인
- [ ] 네비게이션 동작 확인
- [ ] 검색 기능 확인 (추가 설정 필요 시)

---

## 7. 롤백 계획

### 문제 발생 시

```bash
# 1. 현재 index.html 기반 구조로 즉시 복귀

# GitHub Actions 워크플로우를 이전 버전으로 복원
git checkout HEAD~1 -- .github/workflows/github-pages.yml

# 커밋 및 푸시
git add .github/workflows/github-pages.yml
git commit -m "revert: rollback to Jekyll-based documentation"
git push origin main
```

### 백업 항목

| 파일 | 백업 경로 |
|------|----------|
| `github-pages.yml` | `github-pages.yml.bak` |
| `docs/index.html` | 그대로 유지 (삭제하지 않음) |

---

## 8. 추가 고려사항

### 8.1 Spring Antora Extensions (선택)

Spring 공식 문서에서 사용하는 확장 기능:

```bash
npm install @springio/antora-extensions
```

**기능**:
- Partial Build (단일 버전만 빌드)
- Latest Version 매핑
- Tabs 마이그레이션

### 8.2 커스텀 UI Bundle (선택)

기본 Antora UI 대신 Spring 스타일 UI 사용 가능:

```yaml
ui:
  bundle:
    url: https://github.com/spring-io/antora-ui-spring/releases/download/latest/ui-bundle.zip
```

### 8.3 Algolia 검색 통합 (선택)

Antora에 Algolia DocSearch 통합 가능:

```yaml
site:
  keys:
    algolia-api-key: 'YOUR_API_KEY'
    algolia-index-name: 'bottle-note-docs'
```

---

## 9. 참고 자료

| 자료 | URL |
|------|-----|
| Antora 공식 문서 | https://docs.antora.org/ |
| Antora Collector Extension | https://gitlab.com/antora/antora-collector-extension |
| Spring Antora Extensions | https://github.com/spring-io/antora-extensions |
| Spring Boot Antora Wiki | https://github.com/spring-projects/spring-boot/wiki/Antora |
| AsciiDoc 언어 문서 | https://docs.asciidoctor.org/asciidoc/latest/ |

---

**작성일**: 2026-02-02
**버전**: 1.0
**담당자**: Development Team

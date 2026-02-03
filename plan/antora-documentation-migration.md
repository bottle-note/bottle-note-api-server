# Antora 기반 API 문서 시스템 마이그레이션 계획

---

## 1. Antora란?

### 1.1 개요

Antora는 **멀티 리포지토리, 멀티 버전 문서 사이트 생성기**입니다.

| 특징 | 설명 |
|------|------|
| **AsciiDoc 네이티브** | AsciiDoc 마크업 언어를 기본 지원 |
| **멀티 버전** | 동일 문서의 여러 버전을 동시에 관리 |
| **멀티 컴포넌트** | 여러 프로젝트/모듈 문서를 하나의 사이트로 통합 |
| **Git 기반** | ``Git 저장소에서 직접 콘텐츠 수집 |
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

### Phase 5: UI 커스터마이징 ✅ 완료
- [x] Antora 기본 UI 번들 설정
- [x] supplemental-ui 폴더 구조 생성
- [x] header-content.hbs 작성 (헤더 간소화)
- [x] footer-content.hbs 작성 (푸터 간소화)
- [x] toolbar.hbs 작성 (Edit this page 제거)
- [x] 다크모드 토글 스위치 구현
- [x] Spring 다크모드 색상 적용
- [x] 로컬 빌드 테스트 통과

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

## 8. 파일 관리 전략: Git에 포함할 파일 vs CI에서 생성할 파일

### 8.1 권장 전략: CI에서 조립

원본 ADOC 파일은 각 모듈에서 계속 관리하고, **빌드 시에만 Antora 구조로 조립**합니다.

| 항목 | Git에 포함 | CI에서 생성 |
|------|:----------:|:-----------:|
| `antora.yml` | ✅ | |
| `antora-playbook.yml` | ✅ | |
| `nav.adoc` | ✅ | |
| `modules/ROOT/pages/index.adoc` | ✅ | |
| `modules/{api}/pages/*.adoc` | | ✅ (복사) |
| `modules/{api}/examples/snippets/` | | ✅ (복사) |
| `_site/` (빌드 결과) | | ✅ (생성) |

### 8.2 이유

1. **중복 방지**: `src/docs/asciidoc/`에 있는 원본과 `docs/modules/`에 복사본이 생기면 동기화 문제 발생
2. **단일 진실 소스(Single Source of Truth)**: 원본은 각 모듈의 `src/docs/`에만 유지
3. **저장소 용량**: 스니펫은 빌드마다 생성되므로 Git에 불필요

### 8.3 Git에 커밋할 파일 (설정만)

```
docs/
├── antora.yml
├── antora-playbook.yml
└── modules/
    ├── ROOT/
    │   ├── nav.adoc
    │   └── pages/
    │       └── index.adoc    # 홈페이지만
    ├── product-api/
    │   └── nav.adoc          # 네비게이션만
    └── admin-api/
        └── nav.adoc          # 네비게이션만
```

### 8.4 CI에서 복사/생성할 파일

```bash
# GitHub Actions에서 실행
# 1. ADOC 원본 복사
cp -r bottlenote-product-api/src/docs/asciidoc/* docs/modules/product-api/pages/
cp -r bottlenote-admin-api/src/docs/asciidoc/* docs/modules/admin-api/pages/

# 2. REST Docs 스니펫 복사
cp -r bottlenote-product-api/build/generated-snippets/* docs/modules/product-api/examples/
cp -r bottlenote-admin-api/build/generated-snippets/* docs/modules/admin-api/examples/

# 3. Antora 빌드 → _site/ 생성
antora antora-playbook.yml
```

---

## 9. 배포 방식: GitHub Pages 유지

### 9.1 현재 vs Antora 배포 비교

배포 대상(GitHub Pages)은 동일하고, **빌드 도구만 변경**됩니다.

| 단계 | 현재 방식 | Antora 방식 |
|------|-----------|-------------|
| 1. 스니펫 생성 | `./gradlew restDocsTest` | `./gradlew restDocsTest` |
| 2. HTML 빌드 | `./gradlew asciidoctor` | `antora antora-playbook.yml` |
| 3. 결과물 위치 | `docs/*.html` | `docs/_site/` |
| 4. 배포 | Jekyll → GitHub Pages | **그대로** GitHub Pages |

### 9.2 GitHub Actions 변경점 비교

**현재 방식**:
```yaml
- name: Generate API documentation
  run: |
    ./gradlew :bottlenote-product-api:asciidoctor :bottlenote-admin-api:asciidoctor
    cp bottlenote-product-api/build/docs/asciidoc/product-api.html docs/
    cp bottlenote-admin-api/build/docs/asciidoc/admin-api.html docs/

- name: Build with Jekyll
  uses: actions/jekyll-build-pages@v1
  with:
    source: ./docs
```

**Antora 전환 후**:
```yaml
- name: Build Antora site
  run: |
    # 파일 복사 (CI에서만)
    cp -r bottlenote-product-api/src/docs/asciidoc/* docs/modules/product-api/pages/
    cp -r bottlenote-admin-api/src/docs/asciidoc/* docs/modules/admin-api/pages/
    cp -r bottlenote-product-api/build/generated-snippets/* docs/modules/product-api/examples/
    cp -r bottlenote-admin-api/build/generated-snippets/* docs/modules/admin-api/examples/

    # Antora 빌드
    npx antora docs/antora-playbook.yml

- name: Upload artifact
  uses: actions/upload-pages-artifact@v3
  with:
    path: docs/_site    # Antora 출력 폴더
```

---

## 10. 추가 고려사항

### 10.1 Spring Antora Extensions (선택)

Spring 공식 문서에서 사용하는 확장 기능:

```bash
npm install @springio/antora-extensions
```

**기능**:
- Partial Build (단일 버전만 빌드)
- Latest Version 매핑
- Tabs 마이그레이션

### 10.2 커스텀 UI Bundle (선택)

기본 Antora UI 대신 Spring 스타일 UI 사용 가능:

```yaml
ui:
  bundle:
    url: https://github.com/spring-io/antora-ui-spring/releases/download/latest/ui-bundle.zip
```

### 10.3 Algolia 검색 통합 (선택)

Antora에 Algolia DocSearch 통합 가능:

```yaml
site:
  keys:
    algolia-api-key: 'YOUR_API_KEY'
    algolia-index-name: 'bottle-note-docs'
```

---

## 11. UI 커스터마이징 (완료)

### 11.1 적용된 방식

**Antora 기본 UI + Supplemental Files**로 헤더/푸터만 오버라이드하는 방식을 채택했습니다.

| 시도 | 결과 | 문제점 |
|------|------|--------|
| Spring UI 번들 | ❌ 실패 | Spring 브랜딩이 너무 강함 |
| Spring UI + supplemental files | ❌ 실패 | CSS 스타일 없이 HTML만 넣어서 토글 깨짐 |
| Minimized Header UI (v1.1) | ❌ 실패 | 호환성 문제로 사이트 완전히 깨짐 |
| **Antora 기본 UI + supplemental files** | ✅ 성공 | 안정적이고 커스터마이징 용이 |

### 11.2 현재 파일 구조

```
docs/
├── antora-playbook.yml
└── supplemental-ui/
    └── partials/
        ├── header-content.hbs   # 커스텀 헤더 + 다크모드 CSS/JS
        ├── footer-content.hbs   # 커스텀 푸터
        └── toolbar.hbs          # Edit this page 제거
```

### 11.3 antora-playbook.yml 설정

```yaml
ui:
  bundle:
    url: https://gitlab.com/antora/antora-ui-default/-/jobs/artifacts/HEAD/raw/build/ui-bundle.zip?job=bundle-stable
    snapshot: true
  supplemental_files: ./supplemental-ui

content:
  sources:
    - url: ..
      start_path: docs
      branches: HEAD
      edit_url: false  # Edit this page 비활성화
```

### 11.4 커스터마이징 항목

#### 헤더 (header-content.hbs)
- Products/Services/Download 메뉴 제거
- Home 링크만 유지
- 다크모드 토글 스위치 추가 (☀️/🌙 아이콘)

#### 푸터 (footer-content.hbs)
- Antora 라이선스 문구 제거
- 사이트 제목만 표시

#### 툴바 (toolbar.hbs)
- "Edit this Page" 링크 완전 제거

### 11.5 다크모드 구현

#### 토글 스위치 UI
- 슬라이더 형태의 토글 (50px × 26px)
- 왼쪽: ☀️ (라이트), 오른쪽: 🌙 (다크)
- 부드러운 전환 애니메이션 (0.2s)

#### 색상 테마 (Spring 다크모드 색상 적용)

| 요소 | 라이트 모드 | 다크 모드 |
|------|-------------|-----------|
| 배경 | 기본 (흰색) | `#1b1f23` |
| 패널/코드 | 기본 | `#262a2d` |
| 텍스트 | 기본 | `#bbbcbe` |
| 제목 | 기본 | `#cecfd1` |
| 링크 | 기본 | `#086dc3` |
| 링크 호버 | 기본 | `#107ddd` |

#### 기능
- localStorage에 테마 설정 저장 (`antora-theme` 키)
- 시스템 다크모드 설정 자동 감지 (`prefers-color-scheme: dark`)
- 페이지 로드 시 저장된 테마 즉시 적용 (깜빡임 방지)

### 11.6 빌드 및 확인

```bash
# 빌드
cd docs
npx antora --fetch antora-playbook.yml

# 결과 확인
open _site/bottle-note/index.html
```

### 11.7 검증 체크리스트

- [x] 기본 사이트 CSS 정상 로드
- [x] 헤더: Products/Services/Download 메뉴 제거됨
- [x] 헤더: Home 링크만 표시
- [x] 다크모드 토글 스위치 표시
- [x] 다크모드 전환 정상 작동
- [x] 다크모드 색상 Spring 테마 적용 (중립 그레이)
- [x] 테마 설정 localStorage 저장/복원
- [x] Edit this page 링크 제거됨
- [x] 푸터 Antora 라이선스 문구 제거됨
- [x] 좌측 사이드바 네비게이션 정상 작동

---

## 12. 참고 자료

| 자료 | URL |
|------|-----|
| Antora 공식 문서 | https://docs.antora.org/ |
| Antora Collector Extension | https://gitlab.com/antora/antora-collector-extension |
| Spring Antora Extensions | https://github.com/spring-io/antora-extensions |
| Spring Boot Antora Wiki | https://github.com/spring-projects/spring-boot/wiki/Antora |
| AsciiDoc 언어 문서 | https://docs.asciidoctor.org/asciidoc/latest/ |

---

**작성일**: 2026-02-03
**버전**: 1.1
**담당자**: Development Team

### 변경 이력

| 버전 | 날짜 | 내용 |
|------|------|------|
| 1.0 | 2026-02-02 | 초안 작성 |
| 1.1 | 2026-02-03 | UI 커스터마이징 완료 (섹션 11 추가) |

```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-04-08

** Core Achievements **
- GET /admin/api/v1/users 엔드포인트 구현 (키워드 검색, 상태 필터, 5종 정렬, 페이징)
- 활동 지표(reviewCount, ratingCount, picksCount) 서브쿼리 기반 집계
- admin 통합 테스트 8개 작성, L3 전체 통과

** Key Components **
- AdminUsersController.kt: admin-api 컨트롤러 (Kotlin)
- CustomUserRepositoryImpl.searchAdminUsers(): QueryDSL 쿼리 (socialType 배치 로딩)
- AdminUserService.java: 서비스 계층 (GlobalResponse.fromPage 위임)

** Deferred Items **
- RestDocs API 문서화: /docs 스킬 부재로 미작성
================================================================================
```

# Plan: 어드민 - 유저 목록 조회 API

## Overview

어드민 페이지에서 앱 사용자(User) 목록을 조회하는 API를 구현한다.
유저 기본 정보와 함께 리뷰 수, 별점 수, 찜 수 등 활동 지표를 포함하여 반환한다.

### Assumptions

- 조회 대상은 일반 `User` (앱 사용자)이며 `AdminUser`가 아님
- 어드민 JWT 인증 필요
- 오프셋 기반 페이징 (기존 admin-api 패턴 동일)
- 활동 지표: 리뷰 수, 별점 수, 찜(Picks) 수 (팔로워/팔로잉 제외)
- 목록 조회만 구현 (상세 조회 제외)

### Success Criteria

- `GET /admin/api/v1/users` 엔드포인트가 유저 목록을 페이징하여 반환한다
- 응답에 유저 기본 정보(id, email, nickName, imageUrl, role, status, socialType, 가입일, 최종 로그인일)가 포함된다
- 응답에 활동 지표(reviewCount, ratingCount, picksCount)가 포함된다
- 키워드 검색(닉네임/이메일)이 동작한다
- 상태 필터(ACTIVE/DELETED)가 동작한다
- 정렬 옵션: 가입일, 이름순, 이메일순, 별점 많은 순, 리뷰 많은 순
- `GlobalResponse`로 래핑된 표준 응답 형식을 따른다

### Impact Scope

- **mono 모듈**: UserRepository에 목록 조회 메서드 추가, DTO/QueryDSL 쿼리 추가
- **admin-api 모듈**: Controller, Service 신규 생성 (Kotlin)
- **도메인**: user (주), review/rating/picks (활동 지표 집계 - 읽기 전용)
- **테스트**: unit, integration, RestDocs

## Tasks

### Task 1: mono - DTO, Constant 정의
- 수용 기준:
  - `AdminUserSortType` enum 생성 (CREATED_AT, NICK_NAME, EMAIL, RATING_COUNT, REVIEW_COUNT)
  - `AdminUserSearchRequest` record 생성 (keyword, status, sortType, sortOrder, page, size)
  - `AdminUserListResponse` record 생성 (userId, email, nickName, imageUrl, role, status, socialType, reviewCount, ratingCount, picksCount, createAt, lastLoginAt)
- 검증: `./gradlew :bottlenote-mono:compileJava`
- 파일:
  - `mono/.../user/constant/AdminUserSortType.java`
  - `mono/.../user/dto/request/AdminUserSearchRequest.java`
  - `mono/.../user/dto/response/AdminUserListResponse.java`
- 크기: S
- 상태: [x] 완료

### Task 2: mono - QueryDSL 쿼리 구현
- 수용 기준:
  - `CustomUserRepository`에 `searchAdminUsers(AdminUserSearchRequest)` 메서드 추가
  - `CustomUserRepositoryImpl`에 QueryDSL 구현 (키워드 검색, 상태 필터, 정렬, 페이징)
  - 기존 `reviewCountSubQuery`, `ratingCountSubQuery`, `picksCountSubQuery` 서브쿼리 재활용
  - `Page<AdminUserListResponse>` 반환 (오프셋 기반)
- 검증: `./gradlew :bottlenote-mono:compileJava`
- 파일:
  - `mono/.../user/repository/CustomUserRepository.java` (메서드 추가)
  - `mono/.../user/repository/CustomUserRepositoryImpl.java` (구현 추가)
- 크기: M
- 상태: [x] 완료

### Checkpoint: Task 1-2 완료 후
- [x] mono 모듈 컴파일 통과
- [x] 아키텍처 규칙 통과 (`./gradlew check_rule_test`)

### Task 3: mono - Service 구현
- 수용 기준:
  - `AdminUserService` 클래스 생성
  - `searchUsers(AdminUserSearchRequest)` 메서드 구현
  - `GlobalResponse` 래핑하여 반환
- 검증: `./gradlew :bottlenote-mono:compileJava`
- 파일:
  - `mono/.../user/service/AdminUserService.java`
- 크기: S
- 상태: [x] 완료

### Task 4: admin-api - Controller 구현
- 수용 기준:
  - `AdminUsersController.kt` 생성
  - `GET /users` 엔드포인트 (context-path `/admin/api/v1` 하위)
  - `@ModelAttribute`로 검색 조건 바인딩
  - `AdminUserService` 위임
- 검증: `./gradlew :bottlenote-admin-api:compileKotlin`
- 파일:
  - `admin-api/.../user/presentation/AdminUsersController.kt`
- 크기: S
- 상태: [x] 완료

### Checkpoint: Task 3-4 완료 후
- [x] 전체 빌드 통과 (`./gradlew build -x test`)
- [x] admin-api 모듈 컴파일 통과

## Progress Log

- Task 1 완료: DTO, Constant 3개 파일 생성, 컴파일 통과
- Task 2 완료: QueryDSL 구현, RatingQuerySupporter NumberPath 오버로드 추가
- Task 3 완료: AdminUserService 생성, UserRepository 인터페이스 메서드 추가
- Task 4 완료: AdminUsersController.kt 생성
- Self-review: InMemory 구현체 3개 누락 발견 및 수정
- Test: admin 통합 테스트 8개 작성 (목록 조회 7개 + 인증 1개)
- Verify L3: local record QueryDSL Projection 버그 수정 (인터페이스 레벨 record로 이동), 전체 통과

## Skill Cycle Summary (2026-04-08)

| Skill | Result |
|-------|--------|
| `/define` | Plan 문서 생성, 가정 7개 확인, 성공 기준 7개 정의 |
| `/plan` | 4개 Task 분해 (S x 3, M x 1) |
| `/implement` | 9개 파일 생성/수정, Task 4개 완료 |
| `/self-review` | InMemory 구현체 누락 3건 발견 및 수정 (Critical) |
| `/test` | 통합 테스트 8개 작성 (목록 조회 7 + 인증 1), 전체 통과 |
| `/verify full` | L3 전체 통과 (local record Projection 버그 1건 수정) |

### 변경 파일 목록 (12개)

| File | Change | Module |
|------|--------|--------|
| `user/constant/AdminUserSortType.java` | 신규 | mono |
| `user/dto/request/AdminUserSearchRequest.java` | 신규 | mono |
| `user/dto/response/AdminUserListResponse.java` | 신규 | mono |
| `user/domain/UserRepository.java` | 메서드 추가 | mono |
| `user/repository/CustomUserRepository.java` | 메서드 + AdminUserRow record 추가 | mono |
| `user/repository/CustomUserRepositoryImpl.java` | QueryDSL 구현 추가 | mono |
| `rating/repository/RatingQuerySupporter.java` | NumberPath 오버로드 추가 | mono |
| `user/service/AdminUserService.java` | 신규 | mono |
| `user/presentation/AdminUsersController.kt` | 신규 | admin-api |
| `user/fixture/InMemoryUserQueryRepository.java` (mono) | stub 추가 | mono test |
| `user/fixture/InMemoryUserRepository.java` (product) | stub 추가 | product test |
| `user/fixture/InMemoryUserQueryRepository.java` (product) | stub 추가 | product test |
| `integration/user/AdminUsersIntegrationTest.kt` | 신규 | admin-api test |

### Self-review/Verify에서 발견한 이슈

1. **InMemory 구현체 누락** (self-review) - `UserRepository` 인터페이스 변경 시 3개 InMemory 구현체 미갱신 -> 컴파일 에러
2. **Local record QueryDSL Projection 실패** (verify L3) - 메서드 내 local record는 리플렉션에서 외부 클래스 참조 파라미터가 추가되어 `Projections.constructor()` 매칭 실패 -> 인터페이스 레벨 record로 이동하여 해결

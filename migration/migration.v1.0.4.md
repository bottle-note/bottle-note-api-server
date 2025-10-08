# Shared 모듈 마이그레이션 현황

## 🎯 Shared 모듈 원칙

### 실무적 정의

```
"bottlenote-shared는 여러 모듈에서 공통으로 사용되는 컴포넌트"
- 2개 이상 모듈에서 사용되는 공유 컴포넌트
- 필수 라이브러리 의존성은 허용 (Jackson, JWT 등)
- 도메인 독립적인 공통 기능
- 안정적이고 자주 변경되지 않는 코드
```

---

## 📊 현재 Shared 모듈 상태 - 모두 적절함 ✅

### 현재 Shared에 있는 컴포넌트들 (유지)

- `JwtTokenProvider` - 모든 인증 모듈에서 사용 ✅
- `CustomSerializers/Deserializers` - 모든 API에서 사용 (Jackson 의존 OK) ✅
- `GlobalResponse` - 모든 API 응답 표준화 ✅
- `CursorPageable` - 모든 페이징 처리 공통 ✅
- `AbstractCustomException` - 모든 예외의 기반 ✅
- `MetaService`, `MetaInfos` - 메타 정보 공통 처리 ✅
- `ExceptionCode`, `CustomExceptionCode`, `ValidExceptionCode` - 예외 코드 표준 ✅
- `SortOrder`, `UserType` - 공통 enum ✅
- `Const` - 전역 상수 ✅
- `BigDecimalUtil` - 공통 유틸리티 ✅
- `@ExcludeRule` - 아키텍처 규칙 제외 어노테이션 ✅
- `CollectionResponse` - 컬렉션 응답 래퍼 ✅

---

## 🔄 Legacy → Shared 이관 가능 항목

### ✅ 이관 추천 (공유성 높음)

1. **공통 어노테이션** (Spring 의존 있어도 공유성 높으면 OK)
	- `@FacadeService` - 모든 Facade에서 사용
	- `@DomainRepository` - 도메인 레포지토리 마커
	- `@JpaRepositoryImpl` - JPA 구현체 마커
	- `@DomainEventListener` - 이벤트 리스너 마커
	- `@ThirdPartyService` - 외부 서비스 마커

2. **공통 유틸리티**
	- `ExceptionUtil` - 예외 메시지 유틸
	- `ImageUtil`, `ImageInfo` - 이미지 처리 공통
	- `SortOrderUtils` - 정렬 유틸 (QueryDSL 의존 있지만 공유)

3. **공통 예외**
	- `CommonException` - 공통 예외 클래스
	- `CommonExceptionCode` - 공통 예외 코드

4. **차단 시스템** (여러 도메인에서 사용)
	- `@BlockWord` - 차단 어노테이션
	- `BlockWordSerializer` - Jackson 직렬화 (이미 Jackson 의존 있음)
	- `BlockWordConfig` - 차단 설정

5. **설정 Properties**
	- `JwtProperties` - JWT 설정 (이미 JWT 의존 있음)
	- `RedisConfigProperties` - Redis 공통 설정

6. **캐시 관련**
	- `LocalCacheType` - 로컬 캐시 타입 정의

### ❌ Core 모듈로 이관 (v3 문서 기준)

- `BaseEntity` - JPA 의존 → Core 모듈
- `BaseTimeEntity` - JPA 의존 → Core 모듈

---

## 📋 Legacy → Shared 이관 작업 계획

### Phase 1: 공통 어노테이션 (우선순위 높음)

**5개 어노테이션 이관:**

- `@FacadeService`
- `@DomainRepository`
- `@JpaRepositoryImpl`
- `@DomainEventListener`
- `@ThirdPartyService`

### Phase 2: 유틸리티 & 예외

**공통 클래스:**

- `CommonException`, `CommonExceptionCode`
- `ExceptionUtil`
- `ImageUtil`, `ImageInfo`
- `SortOrderUtils`

### Phase 3: 공통 기능 시스템

**차단 시스템 & 설정:**

- 차단 시스템 (`@BlockWord`, `BlockWordSerializer`, `BlockWordConfig`)
- Properties (`JwtProperties`, `RedisConfigProperties`)
- `LocalCacheType`

---

## ✅ Shared 모듈 진입 기준

### 포함 기준

- [ ] 2개 이상 모듈에서 사용
- [ ] 도메인 독립적
- [ ] 공통 인프라/아키텍처 컴포넌트
- [ ] 안정적이고 변경 빈도 낮음

### 의존성 허용 범위

- [ ] 필수 공통 라이브러리 OK (Spring, JPA, Jackson, JWT 등)
- [ ] 단, 해당 의존성이 대부분의 모듈에서 사용되는 경우

### 제외 기준

- [ ] 특정 도메인에만 종속
- [ ] 자주 변경되는 비즈니스 로직
- [ ] 특수 목적 라이브러리 (1개 모듈만 사용)

---

## 📊 마이그레이션 요약

### 현재 Shared 모듈

**모두 유지 (적절하게 배치됨):**

- 인증: JwtTokenProvider
- 직렬화: CustomSerializers/Deserializers
- 응답: GlobalResponse, CollectionResponse
- 페이징: CursorPageable
- 예외: AbstractCustomException, ExceptionCode 계열
- 기타: MetaService, SortOrder, UserType, Const, BigDecimalUtil

### Legacy → Shared 이관 대상

**총 17개 컴포넌트:**

- 어노테이션 5개
- 유틸리티 & 예외 6개
- 차단 시스템 3개
- Properties 2개
- 캐시 타입 1개

### Legacy → Core 이관 대상 (v3 문서)

**2개 컴포넌트:**

- `BaseEntity`
- `BaseTimeEntity`

---

*최종 수정: 2025-09-12*

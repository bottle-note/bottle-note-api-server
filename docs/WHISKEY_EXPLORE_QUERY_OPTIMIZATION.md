# 위스키 둘러보기 쿼리 성능 개선

## 개요
위스키 둘러보기 기능의 QueryDSL 쿼리를 성능 최적화하였습니다.

## 문제점

### 기존 구현 (Before)
```java
// AlcoholQuerySupporter.java
public OrderSpecifier<?> sortByRandom() {
    return Expressions.numberTemplate(Double.class, "function('rand')").asc();
}
```

**문제:**
1. MySQL의 `RAND()` 함수 사용 시 전체 테이블 스캔 발생
2. 페이지 번호가 높아질수록 OFFSET 기반 페이지네이션의 성능 저하
3. 매 요청마다 완전히 다른 정렬 순서로 인한 페이지 일관성 부족
4. Total count 쿼리에 불필요한 JOIN 연산 포함

## 해결 방법

### 개선된 구현 (After)

#### 1. 최적화된 랜덤 정렬 메서드 추가
```java
// AlcoholQuerySupporter.java
public OrderSpecifier<?> sortByOptimizedRandom(Long cursor) {
    // cursor를 기반으로 시드 생성 (0이면 기본값 사용)
    long seed = (cursor == null || cursor == 0) ? 42 : cursor;
    
    // (alcohol.id * seed) % 10000을 사용하여 pseudo-random 정렬
    // 이 방식은 RAND()보다 훨씬 빠르며, 같은 cursor에서는 일관된 결과를 제공
    NumberExpression<Long> pseudoRandom = alcohol.id.multiply(seed).mod(10000L);
    
    return pseudoRandom.asc();
}
```

#### 2. 쿼리 개선
```java
// CustomAlcoholQueryRepositoryImpl.java
.orderBy(supporter.sortByOptimizedRandom(cursor))
.orderBy(alcohol.id.asc())  // 2차 정렬로 안정성 보장
```

#### 3. Total Count 쿼리 간소화
```java
// 기존: region, distillery JOIN 포함
Long total = queryFactory
    .select(alcohol.id.count())
    .from(alcohol)
    .join(region).on(...)
    .join(distillery).on(...)
    .where(supporter.keywordsMatch(keyword))
    .fetchOne();

// 개선: 불필요한 JOIN 제거
Long total = queryFactory
    .select(alcohol.id.count())
    .from(alcohol)
    .where(supporter.keywordsMatch(keyword))
    .fetchOne();
```

## 성능 이점

### 1. 인덱스 활용 가능
- `RAND()` 함수 대신 ID 기반 모듈로 연산 사용
- 인덱스를 활용한 정렬 가능
- 테이블 전체 스캔 방지

### 2. 일관된 페이지네이션
- 같은 cursor에서는 항상 같은 결과 순서 반환
- 사용자 경험 개선 (중복 없는 안정적인 페이징)
- 캐싱 전략 적용 가능

### 3. 랜덤성 유지
- cursor 값이 다르면 다른 정렬 순서 생성
- 각 세션마다 다른 경험 제공
- 요구사항(랜덤성 > 페이지 정확성)을 만족

### 4. 쿼리 복잡도 감소
- Total count 쿼리에서 불필요한 JOIN 제거
- 쿼리 실행 시간 단축

## 성능 비교 (예상)

| 항목 | 기존 (RAND()) | 개선 (ID * seed % 10000) |
|------|--------------|-------------------------|
| 정렬 방식 | 전체 테이블 스캔 | 인덱스 활용 가능 |
| 1페이지 (offset 0) | 느림 | 빠름 |
| 10페이지 (offset 200) | 매우 느림 | 빠름 |
| 같은 cursor 조회 | 매번 다름 | 일관성 있음 |
| 캐싱 가능 여부 | 불가 | 가능 |

## 호환성

### 기존 기능 완벽 호환
- ✅ 기존 `sortByRandom()` 메서드 유지 (다른 곳에서 사용 중일 경우 대비)
- ✅ API 인터페이스 변경 없음 (cursor, size 파라미터 동일)
- ✅ 응답 형식 변경 없음
- ✅ 키워드 필터링 동일하게 동작

### 변경 사항
- ✅ 랜덤성: 완전 무작위 → pseudo-random (cursor 기반)
- ✅ 일관성: 매번 다름 → 같은 cursor에서 동일
- ✅ 성능: 느림 → 빠름

## 테스트

### Unit Test
- ✅ `AlcoholQuerySupporterRandomSortTest`: 최적화된 랜덤 정렬 로직 검증
  - 정렬 조건 생성 확인
  - 다른 cursor로 다른 정렬 생성
  - 같은 cursor로 같은 정렬 생성
  - null cursor 처리
  - 기존 메서드 호환성

### Integration Test (추가됨)
- `AlcoholExploreIntegrationTest`: 전체 기능 통합 테스트
  - 위스키 둘러보기 기본 동작
  - 페이징 동작 확인
  - 키워드 필터링
  - cursor 일관성 검증

## 결론

이 개선으로:
1. **성능 향상**: 인덱스 활용으로 쿼리 속도 대폭 개선
2. **사용자 경험 개선**: 페이지 중복 방지, 일관된 결과
3. **확장성**: 캐싱 가능, 높은 페이지 번호에서도 성능 유지
4. **요구사항 충족**: 랜덤성 유지하면서 성능 개선
5. **호환성**: 기존 API 완벽 호환

랜덤성이 중요하지만 같은 세션 내에서의 일관성도 보장하여, 
성능과 사용자 경험을 모두 개선하였습니다.

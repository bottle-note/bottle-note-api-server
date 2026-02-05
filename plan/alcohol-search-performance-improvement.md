# 위스키 검색 API 성능 개선 계획

## 1. 개요

### 대상 API
- **Endpoint**: `GET /api/v1/alcohols/search`
- **문제**: 키워드 검색 시 느린 응답 속도

### 현재 병목 지점 분석

| 병목 | 심각도 | 원인 |
|------|--------|------|
| `LIKE '%keyword%'` | ⭐⭐⭐⭐⭐ | Full Table Scan, 인덱스 무용 |
| 유연한 패턴 `'%맥%캘%란%'` | ⭐⭐⭐⭐⭐ | 최악의 LIKE 패턴 |
| 테이스팅 태그 EXISTS 서브쿼리 | ⭐⭐⭐⭐ | 매 row마다 서브쿼리 실행 |
| JOIN 3개 (rating, picks, review) | ⭐⭐ | 집계 함수 실시간 계산 |

---

## 2. 성능 측정 계획 (K6)

### 2.1 테스트 환경

```
k6-tests/
├── scripts/
│   └── alcohol-search-test.js
├── data/
│   └── search-keywords.json
└── reports/
    ├── baseline/      # 개선 전
    └── improved/      # 개선 후
```

### 2.2 테스트 시나리오

| 시나리오 | 설명 | 예시 |
|----------|------|------|
| 단일 키워드 | 한 단어 검색 | `?keyword=맥캘란` |
| 다중 키워드 | 여러 단어 검색 | `?keyword=스모키 위스키` |
| 복합 필터 | 키워드 + 필터 조합 | `?keyword=맥캘란&category=SINGLE_MALT&sortType=POPULAR` |
| 페이징 | 커서 기반 페이징 | `?keyword=위스키&cursor=20&pageSize=10` |
| 유연한 패턴 | 띄어쓰기 포함 | `?keyword=맥 캘 란` |

### 2.3 부하 단계

| 단계 | VUs | Duration | 목적 |
|------|-----|----------|------|
| Smoke | 1 | 30s | 기본 동작 확인 |
| Load | 10 | 2m | 일반 부하 |
| Stress | 50 | 3m | 스트레스 |
| Spike | 100 | 1m | 급격한 부하 |

### 2.4 측정 지표

| 지표 | 목표 |
|------|------|
| p95 응답 시간 | < 500ms |
| p99 응답 시간 | < 1000ms |
| 실패율 | < 1% |
| RPS | > 100 |

### 2.5 테스트 키워드 목록

```json
{
  "single_keywords": ["맥캘란", "글렌피딕", "발베니", "라프로익", "Macallan"],
  "multi_keywords": ["스모키 위스키", "셰리 캐스크", "싱글몰트 스코틀랜드"],
  "flexible_patterns": ["맥 캘 란", "글렌 피딕"]
}
```

### 2.6 실행 명령어

```bash
# Baseline 측정
k6 run --env BASE_URL=http://localhost:8080 scripts/alcohol-search-test.js

# 결과 저장
k6 run --out json=reports/baseline/result.json scripts/alcohol-search-test.js
```

---

## 3. 개선 방안: 집계 테이블 + 검색 키워드 비정규화

### 3.1 핵심 아이디어

1. **집계 데이터 비정규화**: rating, review, pick count를 미리 계산하여 저장
2. **검색 키워드 통합**: 모든 검색 대상 필드를 하나의 컬럼에 통합

### 3.2 테이블 설계

```sql
CREATE TABLE alcohol_statistics (
    alcohol_id BIGINT PRIMARY KEY,

    -- 집계 데이터
    avg_rating DECIMAL(3,2) DEFAULT 0.00,
    rating_count BIGINT DEFAULT 0,
    review_count BIGINT DEFAULT 0,
    pick_count BIGINT DEFAULT 0,
    popular_score DECIMAL(10,2) DEFAULT 0.00,

    -- 검색용 비정규화 컬럼
    search_keywords TEXT NOT NULL,

    -- 메타
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 인덱스
    INDEX idx_popular_score (popular_score DESC),
    INDEX idx_avg_rating (avg_rating DESC),
    FULLTEXT INDEX ft_search_keywords (search_keywords) WITH PARSER ngram
);
```

### 3.3 search_keywords 컬럼 구성

```
{한글이름}|{영문이름}|{한글카테고리}|{영문카테고리}|{지역}|{증류소}|{테이스팅태그들}
```

**예시**:
```
맥캘란 12년|Macallan 12 Years|싱글몰트|Single Malt|스페이사이드|Speyside|셰리|바닐라|꿀
```

### 3.4 쿼리 변화

**Before**:
```sql
SELECT a.*, AVG(r.rating), COUNT(...)
FROM alcohols a
LEFT JOIN ratings r ON ...
LEFT JOIN picks p ON ...
LEFT JOIN reviews rv ON ...
WHERE a.kor_name LIKE '%맥캘란%'
   OR a.eng_name LIKE '%...'
   OR EXISTS (SELECT 1 FROM tasting_tags ...)
GROUP BY a.id;
```

**After**:
```sql
SELECT a.*, s.avg_rating, s.rating_count, s.pick_count, s.review_count
FROM alcohols a
JOIN alcohol_statistics s ON a.id = s.alcohol_id
WHERE MATCH(s.search_keywords) AGAINST('맥캘란' IN BOOLEAN MODE)
ORDER BY s.popular_score DESC;
```

### 3.5 예상 개선 효과

| 항목 | Before | After |
|------|--------|-------|
| JOIN 수 | 3개 | 1개 |
| WHERE 조건 | LIKE 다중 + EXISTS | FULLTEXT 1개 |
| GROUP BY | 필요 | 불필요 |
| 인덱스 활용 | 불가 | FULLTEXT |

---

## 4. 배치 Job 설계

### 4.1 위치

```
bottlenote-batch/
└── job/statistics/
    └── AlcoholStatisticsJobConfig.java
```

### 4.2 실행 주기

- **권장**: 1시간마다 (`0 0 * * * ?`)

### 4.3 처리 흐름

```
[Reader] 모든 alcohol 조회
    ↓
[Processor] 각 alcohol별 통계 계산 + 검색 키워드 생성
    ↓
[Writer] alcohol_statistics 테이블 UPSERT
```

---

## 5. 구현 단계

### Phase 1: 성능 측정 (Baseline)
- [ ] k6 테스트 스크립트 작성
- [ ] 현재 성능 측정 및 기록

### Phase 2: 테이블 및 엔티티
- [ ] DDL 작성 및 테이블 생성
- [ ] `AlcoholStatistics` 엔티티 (mono 모듈)
- [ ] Repository 생성

### Phase 3: 배치 Job
- [ ] `AlcoholStatisticsJobConfig` (batch 모듈)
- [ ] Quartz 스케줄 등록
- [ ] 배치 테스트

### Phase 4: 검색 쿼리 수정
- [ ] `CustomAlcoholQueryRepositoryImpl` 수정
- [ ] 기존 테스트 통과 확인

### Phase 5: 성능 측정 (After)
- [ ] k6 테스트 재실행
- [ ] Before/After 비교

---

## 6. 롤백 계획

Feature Flag로 기존 로직과 신규 로직 전환 가능:

```yaml
feature:
  alcohol-search:
    use-statistics-table: false  # true로 변경 시 새 로직 적용
```

---

## 7. 참고 자료

- [MySQL FULLTEXT ngram parser](https://dev.mysql.com/doc/refman/8.0/en/fulltext-search-ngram.html)
- [K6 Documentation](https://k6.io/docs/)

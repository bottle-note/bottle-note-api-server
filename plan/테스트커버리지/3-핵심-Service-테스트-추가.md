# 핵심 Service 테스트 추가

## 0. 왜 핵심 Service 테스트인가

### Service의 본질

Service 계층은 프로젝트의 심장입니다:
- 비즈니스 규칙을 코드로 구현
- 도메인 엔티티를 조작하여 가치 창출
- 데이터 정합성과 일관성 보장

### Service = 비즈니스 로직 = 돈이 오가는 곳

**ReviewService**: 리뷰 작성, 수정, 삭제
- 사용자가 리뷰를 작성함 → 컨텐츠 생성 → 플랫폼 가치 증가
- 잘못된 리뷰 처리 → 사용자 이탈 → 매출 감소

**RatingService**: 평점 계산 및 관리
- 평점이 정확해야 → 추천 시스템 정확도 향상 → 사용자 만족
- 평점 버그 → 잘못된 추천 → 사용자 불만

**AlcoholViewHistoryService**: 조회 이력 관리
- Redis + DB 동기화로 성능과 정확성 동시 확보
- 동기화 버그 → 데이터 불일치 → 추천 알고리즘 오작동

### 테스트 = 비즈니스 요구사항의 명세

PM: "리뷰는 본인만 삭제할 수 있어야 합니다"

이를 코드로 작성하면:
```java
if (review.getUserId() != currentUserId) {
    throw new ReviewException("본인만 삭제 가능");
}
```

이를 테스트로 명세화하면:
```java
@Test
@DisplayName("본인이 작성하지 않은 리뷰를 삭제할 때 예외가 발생해야 한다")
void 본인이_작성하지_않은_리뷰를_삭제할때_예외가_발생해야한다() {
    // given
    Review review = createReview(작성자 = 1L);

    // when & then
    assertThatThrownBy(() -> reviewService.delete(review.getId(), 다른사람 = 2L))
        .isInstanceOf(ReviewException.class);
}
```

**테스트 = 요구사항이 영원히 보장됨**

### 복잡도가 높은 곳 = 버그가 많은 곳

**AlcoholViewHistoryService**의 복잡성:
1. Redis에 조회 이력 저장 (빠른 조회)
2. 일정 주기로 DB 동기화 (영구 보관)
3. Redis 장애 시 대체 로직
4. 동시성 제어

이런 복잡한 로직에 테스트가 없으면:
- Redis-DB 동기화 타이밍 버그
- 동시 접근 시 데이터 손실
- Redis 장애 시 서비스 다운

**복잡한 로직 = 테스트 필수**

### Service 없으면 전체 기능 마비

계층 간 의존성:
```
Controller → Facade → Service → Repository
```

- Controller 버그: 특정 API만 영향
- Service 버그: 해당 기능 전체 마비
- Repository 버그: 데이터 접근만 영향

**Service는 기능의 핵심, 테스트는 핵심의 보증서**

## 1. 현황 분석

### 테스트 누락된 핵심 Service

| Service | 위치 | 책임 | 복잡도 | 테스트 상태 |
|---------|------|------|--------|------------|
| ReviewService | bottlenote-mono/src/main/java/app/bottlenote/review/service/ | 리뷰 CRUD | 높음 | 0% |
| AlcoholViewHistoryService | bottlenote-mono/src/main/java/app/bottlenote/history/service/ | 조회 이력 관리 (Redis+DB) | 매우 높음 | 0% |
| RatingQueryService | bottlenote-mono/src/main/java/app/bottlenote/rating/service/ | 평점 조회 | 중간 | 0% |
| ReviewExploreService | bottlenote-mono/src/main/java/app/bottlenote/review/service/ | 리뷰 탐색 | 중간 | 0% |
| AlcoholPopularService | bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/ | 인기 주류 조회 | 중간 | 0% |
| UserHistoryQueryService | bottlenote-mono/src/main/java/app/bottlenote/history/service/ | 사용자 이력 조회 | 낮음 | 0% |
| ReviewReportService | bottlenote-mono/src/main/java/app/bottlenote/support/report/service/ | 리뷰 신고 | 중간 | 0% |
| DailyDataReportService | bottlenote-mono/src/main/java/app/bottlenote/support/report/service/ | 일일 데이터 리포트 | 낮음 | 0% |

### 테스트 존재하는 Service

| Service | 위치 | 테스트 상태 |
|---------|------|------------|
| UserBasicService | bottlenote-mono/src/main/java/app/bottlenote/user/service/ | 양호 |
| ReviewReplyService | bottlenote-mono/src/main/java/app/bottlenote/review/service/ | 우수 |
| RatingCommandService | bottlenote-mono/src/main/java/app/bottlenote/rating/service/ | 양호 |
| AlcoholQueryService | bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/ | 양호 |

### 커버리지 현황

- 전체 핵심 Service: 약 20개
- 테스트 존재: 약 10개
- **현재 커버리지: 약 50%**
- **목표 커버리지: 90%**

### 왜 50%인가?

초기 개발 단계에서 일부 Service (User, Rating 등)에 집중적으로 테스트를 작성했습니다.
이후 기능 추가 시 테스트 작성이 누락되었고, 특히 복잡한 로직(Redis 동기화 등)은 "나중에"로 미뤄졌습니다.
하지만 이런 복잡한 로직일수록 테스트가 더 필요합니다.

## 2. 우선순위

### P0: 최우선 작성 대상

**ReviewService**
- 이유: 리뷰는 플랫폼의 핵심 컨텐츠, CRUD 로직
- 영향도: 매우 높음 (전체 비즈니스에 직접 영향)
- 복잡도: 높음 (비속어 필터, 권한 검증, 이미지 처리 등)
- 위험도: 높음 (현재 테스트 0%)

**AlcoholViewHistoryService**
- 이유: Redis + DB 동기화의 복잡한 로직
- 영향도: 높음 (추천 알고리즘 의존)
- 복잡도: 매우 높음 (캐시, 동기화, 장애 처리)
- 위험도: 매우 높음 (동기화 버그 시 데이터 불일치)

### P1: 중요 작성 대상

**RatingQueryService**
- 이유: 평점 조회 로직, 추천 시스템 기반
- 영향도: 높음
- 복잡도: 중간
- 위험도: 중간

**ReviewExploreService**
- 이유: 리뷰 탐색 및 필터링
- 영향도: 중간
- 복잡도: 중간
- 위험도: 중간

**AlcoholPopularService**
- 이유: 인기 주류 계산 로직
- 영향도: 중간
- 복잡도: 중간
- 위험도: 중간

### P2: 보통 작성 대상

**UserHistoryQueryService**
- 이유: 사용자 이력 조회
- 영향도: 낮음
- 복잡도: 낮음
- 위험도: 낮음

**ReviewReportService**
- 이유: 리뷰 신고 처리
- 영향도: 중간
- 복잡도: 중간
- 위험도: 낮음

**DailyDataReportService**
- 이유: 일일 데이터 리포트
- 영향도: 낮음 (내부 관리용)
- 복잡도: 낮음
- 위험도: 낮음

## 3. 구체적 테스트 시나리오

### ReviewService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/review/service/ReviewService.java`

#### 리뷰 작성

**시나리오 1: 정상적인 리뷰 작성**
- 시나리오: 유효한 입력으로 리뷰를 작성할 때 정상적으로 저장되어야 한다
- 왜: 핵심 기능의 정상 케이스 검증
- 테스트 방법: 리뷰 정보 전달 후 작성, 저장 확인

**시나리오 2: 비속어가 포함된 리뷰 작성 시 마스킹 처리**
- 시나리오: 비속어가 포함된 내용으로 리뷰를 작성할 때 마스킹 처리되어야 한다
- 왜: 컨텐츠 품질 관리, 비속어 필터링 정책 준수
- 테스트 방법: 비속어 포함 리뷰 작성, 마스킹된 내용 확인

**시나리오 3: 필수 항목 누락 시 예외 발생**
- 시나리오: 필수 항목이 누락된 리뷰를 작성할 때 예외가 발생해야 한다
- 왜: 데이터 무결성 보장
- 테스트 방법: 내용 없는 리뷰 작성 시도, 예외 확인

**시나리오 4: 동일한 술에 중복 리뷰 작성 방지**
- 시나리오: 이미 리뷰를 작성한 술에 다시 리뷰를 작성할 때 예외가 발생해야 한다
- 왜: 중복 리뷰 방지 정책
- 테스트 방법: 동일 사용자, 동일 술로 재작성 시도, 예외 확인

**시나리오 5: 존재하지 않는 술에 리뷰 작성 시 예외 발생**
- 시나리오: 존재하지 않는 술 ID로 리뷰를 작성할 때 예외가 발생해야 한다
- 왜: 참조 무결성 보장
- 테스트 방법: 잘못된 술 ID로 작성 시도, 예외 확인

#### 리뷰 수정

**시나리오 1: 본인 리뷰 수정 성공**
- 시나리오: 본인이 작성한 리뷰를 수정할 때 정상적으로 수정되어야 한다
- 왜: 리뷰 수정 기능 검증
- 테스트 방법: 리뷰 작성 후 수정, 변경 사항 확인

**시나리오 2: 타인 리뷰 수정 시 예외 발생**
- 시나리오: 타인이 작성한 리뷰를 수정할 때 예외가 발생해야 한다
- 왜: 권한 검증, 보안
- 테스트 방법: 다른 사용자 ID로 수정 시도, 예외 확인

**시나리오 3: 수정 시 비속어 필터링**
- 시나리오: 리뷰 수정 시에도 비속어가 마스킹되어야 한다
- 왜: 일관된 비속어 필터링 정책
- 테스트 방법: 비속어 포함 내용으로 수정, 마스킹 확인

**시나리오 4: 존재하지 않는 리뷰 수정 시 예외 발생**
- 시나리오: 존재하지 않는 리뷰 ID로 수정 시도 시 예외가 발생해야 한다
- 왜: 잘못된 요청 방어
- 테스트 방법: 잘못된 리뷰 ID로 수정, 예외 확인

#### 리뷰 삭제

**시나리오 1: 본인 리뷰 삭제 성공**
- 시나리오: 본인이 작성한 리뷰를 삭제할 때 정상적으로 삭제되어야 한다
- 왜: 리뷰 삭제 기능 검증
- 테스트 방법: 리뷰 작성 후 삭제, 조회 시 없음 확인

**시나리오 2: 타인 리뷰 삭제 시 예외 발생**
- 시나리오: 타인이 작성한 리뷰를 삭제할 때 예외가 발생해야 한다
- 왜: 권한 검증, 보안 (핵심 요구사항)
- 테스트 방법: 다른 사용자 ID로 삭제 시도, 예외 확인

**시나리오 3: 존재하지 않는 리뷰 삭제 시 예외 발생**
- 시나리오: 존재하지 않는 리뷰 ID로 삭제 시도 시 예외가 발생해야 한다
- 왜: 잘못된 요청 방어
- 테스트 방법: 잘못된 리뷰 ID로 삭제, 예외 확인

**시나리오 4: 삭제된 리뷰는 조회되지 않음**
- 시나리오: 삭제된 리뷰를 조회할 때 조회되지 않아야 한다
- 왜: soft delete 또는 hard delete 정책 확인
- 테스트 방법: 삭제 후 조회, 결과 없음 확인

#### 리뷰 조회

**시나리오 1: 리뷰 상세 조회 성공**
- 시나리오: 리뷰 ID로 상세 정보를 조회할 때 정확한 정보가 반환되어야 한다
- 왜: 조회 기능 검증
- 테스트 방법: 리뷰 작성 후 조회, 모든 필드 일치 확인

**시나리오 2: 차단된 리뷰는 조회 시 특별 처리**
- 시나리오: 차단된 리뷰를 조회할 때 적절히 처리되어야 한다
- 왜: 차단된 컨텐츠 노출 방지
- 테스트 방법: 리뷰 차단 후 조회, 차단 상태 확인

**시나리오 3: 좋아요 수 정확히 반영**
- 시나리오: 리뷰의 좋아요 수가 정확히 계산되어야 한다
- 왜: 통계 정확성
- 테스트 방법: 좋아요 추가 후 조회, 수 일치 확인

### AlcoholViewHistoryService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/history/service/AlcoholViewHistoryService.java`

#### 조회 이력 저장 (Redis)

**시나리오 1: Redis에 조회 이력 저장**
- 시나리오: 술을 조회할 때 Redis에 이력이 저장되어야 한다
- 왜: 빠른 조회 이력 접근
- 테스트 방법: 술 조회 후 Redis 확인, 이력 존재 확인

**시나리오 2: 동일한 술 재조회 시 중복 저장 방지**
- 시나리오: 같은 사용자가 같은 술을 다시 조회할 때 적절히 처리되어야 한다
- 왜: 중복 데이터 방지 또는 조회 시간 갱신
- 테스트 방법: 재조회 후 Redis 확인, 의도된 동작 검증

**시나리오 3: Redis 장애 시 대체 로직**
- 시나리오: Redis 연결 실패 시 서비스가 중단되지 않아야 한다
- 왜: 장애 격리, 서비스 가용성
- 테스트 방법: Redis Mock 장애 설정 후 조회, 예외 없이 처리 확인

**시나리오 4: 저장 용량 제한**
- 시나리오: 사용자당 조회 이력 개수가 제한되어야 한다
- 왜: 메모리 관리
- 테스트 방법: 제한 초과 저장 시도, 오래된 이력 제거 확인

#### Redis-DB 동기화

**시나리오 1: 주기적인 DB 동기화**
- 시나리오: Redis의 조회 이력이 주기적으로 DB에 동기화되어야 한다
- 왜: 영구 보관, 데이터 손실 방지
- 테스트 방법: 동기화 트리거 후 DB 확인, 데이터 일치 확인

**시나리오 2: 동기화 실패 시 재시도**
- 시나리오: DB 동기화 실패 시 재시도해야 한다
- 왜: 데이터 정합성 보장
- 테스트 방법: DB Mock 장애 후 복구, 재시도로 동기화 확인

**시나리오 3: 동기화 중 중복 방지**
- 시나리오: 동기화 과정에서 중복 데이터가 생성되지 않아야 한다
- 왜: 데이터 무결성
- 테스트 방법: 동일 데이터 재동기화, 중복 없음 확인

**시나리오 4: 대량 데이터 동기화 성능**
- 시나리오: 대량의 조회 이력을 동기화할 때 타임아웃 없이 처리되어야 한다
- 왜: 성능 보장
- 테스트 방법: 다수 이력 생성 후 동기화, 시간 측정

#### 조회 이력 조회

**시나리오 1: 최근 조회한 술 목록 반환**
- 시나리오: 사용자의 최근 조회 이력을 정확한 순서로 반환해야 한다
- 왜: 사용자 경험, 추천 알고리즘 입력
- 테스트 방법: 여러 술 조회 후 이력 조회, 순서 확인

**시나리오 2: Redis 우선 조회 후 DB 폴백**
- 시나리오: Redis에서 먼저 조회하고 없으면 DB에서 조회해야 한다
- 왜: 성능 최적화
- 테스트 방법: Redis 비우고 조회, DB에서 조회 확인

**시나리오 3: 조회 이력이 없을 때 빈 목록 반환**
- 시나리오: 조회 이력이 없는 사용자의 경우 빈 목록을 반환해야 한다
- 왜: null 방지, 안전한 처리
- 테스트 방법: 신규 사용자로 조회, 빈 목록 확인

### RatingQueryService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/rating/service/RatingQueryService.java`

#### 평점 조회

**시나리오 1: 술의 평균 평점 조회**
- 시나리오: 술 ID로 평균 평점을 조회할 때 정확한 값이 반환되어야 한다
- 왜: 평점 표시 기본 기능
- 테스트 방법: 여러 평점 등록 후 평균 계산, 일치 확인

**시나리오 2: 평점이 없는 술의 기본값**
- 시나리오: 평점이 없는 술을 조회할 때 기본값(0 또는 null)을 반환해야 한다
- 왜: 엣지 케이스 처리
- 테스트 방법: 평점 없는 술 조회, 기본값 확인

**시나리오 3: 사용자별 평점 조회**
- 시나리오: 특정 사용자가 매긴 평점을 조회할 수 있어야 한다
- 왜: 마이페이지 기능
- 테스트 방법: 사용자 평점 등록 후 조회, 일치 확인

**시나리오 4: 평점 분포 조회**
- 시나리오: 술의 평점 분포(1점 몇 개, 2점 몇 개 등)를 조회할 수 있어야 한다
- 왜: 상세 통계 제공
- 테스트 방법: 다양한 평점 등록 후 분포 조회, 정확성 확인

#### 평점 통계

**시나리오 1: 전체 평점 개수 조회**
- 시나리오: 술의 전체 평점 개수를 조회할 수 있어야 한다
- 왜: 신뢰도 표시
- 테스트 방법: 평점 등록 후 개수 조회, 일치 확인

**시나리오 2: 높은 평점 술 목록 조회**
- 시나리오: 평점이 높은 순으로 술 목록을 조회할 수 있어야 한다
- 왜: 인기 술 추천
- 테스트 방법: 다양한 평점 술 생성 후 조회, 순서 확인

### ReviewExploreService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/review/service/ReviewExploreService.java`

#### 리뷰 탐색

**시나리오 1: 키워드로 리뷰 검색**
- 시나리오: 키워드로 리뷰를 검색할 때 관련 리뷰가 반환되어야 한다
- 왜: 리뷰 검색 기능
- 테스트 방법: 키워드 포함 리뷰 작성 후 검색, 결과 확인

**시나리오 2: 필터링 조건 적용**
- 시나리오: 평점, 날짜 등 필터링 조건을 적용할 수 있어야 한다
- 왜: 고급 검색 기능
- 테스트 방법: 필터 조건 전달 후 검색, 조건 일치 확인

**시나리오 3: 페이징 처리**
- 시나리오: 검색 결과가 페이징 처리되어야 한다
- 왜: 성능 및 사용자 경험
- 테스트 방법: 다수 리뷰 검색 후 페이지 크기 확인

**시나리오 4: 차단된 리뷰 제외**
- 시나리오: 탐색 결과에 차단된 리뷰가 포함되지 않아야 한다
- 왜: 차단 정책 준수
- 테스트 방법: 차단 리뷰 포함 검색, 결과에 없음 확인

### AlcoholPopularService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/AlcoholPopularService.java`

#### 인기 술 계산

**시나리오 1: 조회수 기반 인기 술 계산**
- 시나리오: 조회수가 많은 술이 인기 술로 선정되어야 한다
- 왜: 인기도 측정 기준
- 테스트 방법: 다양한 조회수 술 생성 후 계산, 순서 확인

**시나리오 2: 리뷰 수 반영**
- 시나리오: 리뷰 수가 많은 술의 인기도가 높아야 한다
- 왜: 활동성 반영
- 테스트 방법: 리뷰 수 다른 술 생성 후 계산, 순서 확인

**시나리오 3: 평점 반영**
- 시나리오: 평점이 높은 술의 인기도가 높아야 한다
- 왜: 품질 반영
- 테스트 방법: 평점 다른 술 생성 후 계산, 순서 확인

**시나리오 4: 기간별 인기 술**
- 시나리오: 특정 기간의 인기 술을 계산할 수 있어야 한다
- 왜: 트렌드 파악
- 테스트 방법: 기간 지정 후 계산, 기간 내 데이터만 반영 확인

## 4. 체크리스트

### ReviewService

#### 리뷰 작성
- [ ] 정상적인 리뷰 작성
- [ ] 비속어 포함 시 마스킹 처리
- [ ] 필수 항목 누락 시 예외
- [ ] 동일 술 중복 리뷰 방지
- [ ] 존재하지 않는 술 리뷰 작성 시 예외

#### 리뷰 수정
- [ ] 본인 리뷰 수정 성공
- [ ] 타인 리뷰 수정 시 예외
- [ ] 수정 시 비속어 필터링
- [ ] 존재하지 않는 리뷰 수정 시 예외

#### 리뷰 삭제
- [ ] 본인 리뷰 삭제 성공
- [ ] 타인 리뷰 삭제 시 예외
- [ ] 존재하지 않는 리뷰 삭제 시 예외
- [ ] 삭제된 리뷰 조회 불가

#### 리뷰 조회
- [ ] 리뷰 상세 조회 성공
- [ ] 차단된 리뷰 특별 처리
- [ ] 좋아요 수 정확히 반영

### AlcoholViewHistoryService

#### 조회 이력 저장
- [ ] Redis에 조회 이력 저장
- [ ] 동일 술 재조회 처리
- [ ] Redis 장애 시 대체 로직
- [ ] 저장 용량 제한

#### Redis-DB 동기화
- [ ] 주기적인 DB 동기화
- [ ] 동기화 실패 시 재시도
- [ ] 동기화 중 중복 방지
- [ ] 대량 데이터 동기화 성능

#### 조회 이력 조회
- [ ] 최근 조회 술 목록 반환
- [ ] Redis 우선 조회 후 DB 폴백
- [ ] 조회 이력 없을 때 빈 목록

### RatingQueryService

#### 평점 조회
- [ ] 술의 평균 평점 조회
- [ ] 평점 없는 술 기본값
- [ ] 사용자별 평점 조회
- [ ] 평점 분포 조회

#### 평점 통계
- [ ] 전체 평점 개수 조회
- [ ] 높은 평점 술 목록 조회

### ReviewExploreService

- [ ] 키워드로 리뷰 검색
- [ ] 필터링 조건 적용
- [ ] 페이징 처리
- [ ] 차단된 리뷰 제외

### AlcoholPopularService

- [ ] 조회수 기반 인기 술 계산
- [ ] 리뷰 수 반영
- [ ] 평점 반영
- [ ] 기간별 인기 술

### 테스트 파일 생성

- [ ] ReviewServiceTest.java 생성
- [ ] AlcoholViewHistoryServiceTest.java 생성
- [ ] RatingQueryServiceTest.java 생성
- [ ] ReviewExploreServiceTest.java 생성
- [ ] AlcoholPopularServiceTest.java 생성
- [ ] UserHistoryQueryServiceTest.java 생성
- [ ] ReviewReportServiceTest.java 생성
- [ ] DailyDataReportServiceTest.java 생성

### 통합 테스트

- [ ] ReviewService 통합 테스트
- [ ] AlcoholViewHistoryService 통합 테스트 (Redis+DB)
- [ ] RatingQueryService 통합 테스트
- [ ] ReviewExploreService 통합 테스트

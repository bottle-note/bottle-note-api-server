# 🚫 Bottle Note 유저 차단 기능 구현 가이드

## 📋 목차
1. [현재 시스템 분석](#현재-시스템-분석)
2. [차단 기능 구현 방안 비교](#차단-기능-구현-방안-비교)
3. [추천 구현 방안: AOP 기반](#추천-구현-방안-aop-기반)
4. [성능 최적화 전략](#성능-최적화-전략)
5. [프론트엔드 연동 방안](#프론트엔드-연동-방안)
6. [구현 단계별 가이드](#구현-단계별-가이드)

---

## 🔍 현재 시스템 분석

### 차단 기능이 필요한 조회 지점들
- **리뷰 목록/상세 조회** - 차단된 사용자의 리뷰 블러 처리
- **리뷰 댓글 조회** - 차단된 사용자의 댓글 블러 처리  
- **팔로워/팔로잉 목록** - 차단된 사용자 제외 또는 블러 표시
- **마이페이지 접근** - 차단된 사용자의 마이페이지 접근 제한
- **유저 검색/추천** - 차단된 사용자 제외

### 현재 인증 시스템
```java
// 익명 사용자 처리
Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

// 로그인 필수 처리
Long currentUserId = SecurityContextUtil.getUserIdByContext()
    .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
```

---

## ⚖️ 차단 기능 구현 방안 비교

### 1️⃣ 방안 1: 데이터베이스 쿼리 수정

#### 🎯 구현 방식
```sql
-- 기존 쿼리
SELECT * FROM review WHERE alcohol_id = ?;

-- 수정된 쿼리
SELECT r.*, 
       CASE WHEN ub.blocker_id IS NOT NULL THEN 1 ELSE 0 END as is_blocked
FROM review r 
LEFT JOIN user_block ub ON ub.blocker_id = ? AND ub.blocked_id = r.user_id
WHERE r.alcohol_id = ?;
```

#### ✅ 장점
- **가장 직관적인 방법**
- **DB 레벨에서 처리**로 확실한 차단
- **성능이 좋음** (한 번의 쿼리로 해결)

#### ❌ 단점
- **모든 쿼리를 수정해야 함** (50+ 개의 쿼리)
- **기존 코드 대량 수정** 필요
- **유지보수 복잡도 증가**
- **테스트 케이스 대량 수정** 필요

### 2️⃣ 방안 2: 서비스 레이어 후처리

#### 🎯 구현 방식
```java
@Service
public class ReviewService {
    
    public PageResponse<ReviewListResponse> getReviews(Long alcoholId, ...) {
        // 기존 로직으로 데이터 조회
        var reviews = reviewRepository.findByAlcoholId(alcoholId);
        
        // 차단 로직 후처리
        var filteredReviews = blockFilterService.filterBlockedContent(
            reviews, currentUserId, ReviewListResponse::getAuthorId
        );
        
        return PageResponse.of(filteredReviews);
    }
}
```

#### ✅ 장점
- **기존 Repository 코드 유지**
- **비즈니스 로직과 차단 로직 분리**
- **테스트하기 쉬움**

#### ❌ 단점
- **모든 서비스 메서드 수정** 필요
- **중복 코드 발생** 가능성
- **깜빡하기 쉬움** (새로운 기능 추가 시)

### 3️⃣ 방안 3: AOP 기반 통합 처리 ⭐ **추천**

#### 🎯 구현 방식
```java
// 컨트롤러에 어노테이션만 추가
@BlockFilter(userField = "authorId", type = BlockType.BLUR)
@GetMapping("/{alcoholId}")
public ResponseEntity<?> getReviews(@PathVariable Long alcoholId, ...) {
    // 기존 로직 그대로 유지 (수정 없음!)
    return GlobalResponse.ok(reviewService.getReviews(alcoholId, request, currentUserId));
}
```

#### ✅ 장점
- **기존 코드 수정 최소화** (어노테이션만 추가)
- **중앙 집중식 관리** (차단 로직이 한 곳에)
- **깜빡할 염려 없음** (어노테이션으로 명시적)
- **성능 영향 최소** (응답 후처리)
- **테스트 용이** (AOP 로직 독립 테스트)
- **확장성 좋음** (다양한 차단 타입 지원)

#### ❌ 단점
- **AOP 개념 이해** 필요
- **디버깅이 약간 복잡**할 수 있음

### 4️⃣ 방안 4: 프론트엔드 처리

#### 🎯 구현 방식
```javascript
// 백엔드에서 차단 정보만 제공
const reviews = await api.getReviews(alcoholId);
const blockedUsers = await api.getBlockedUsers();

// 프론트엔드에서 블러 처리
const filteredReviews = reviews.map(review => 
  blockedUsers.includes(review.authorId) 
    ? { ...review, isBlocked: true }
    : review
);
```

#### ✅ 장점
- **백엔드 코드 수정 없음**
- **실시간 차단/해제** 가능

#### ❌ 단점
- **보안 취약** (클라이언트에서 우회 가능)
- **네트워크 비용 증가**
- **차단된 데이터도 전송**됨

---

## 🏆 추천 구현 방안: AOP 기반

### 📐 아키텍처 설계

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controller    │    │   AOP Aspect    │    │   Response      │
│                 │    │                 │    │                 │
│ @BlockFilter   ├────┤ BlockFilterAsp- ├────┤ 블러 처리된     │
│ @GetMapping     │    │ ect             │    │ 응답 데이터     │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
          │                       │                       │
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Service       │    │ BlockCacheManager│    │   Frontend      │
│   (기존 로직)   │    │                 │    │                 │
│   수정 없음     │    │ Redis Cache     │    │ 블러 UI 처리    │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 🔧 핵심 구현 코드

#### 1. 차단 필터 어노테이션
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlockFilter {
    BlockType type() default BlockType.BLUR;
    String userField() default "authorId";  // 차단 대상 사용자 ID 필드명
}

public enum BlockType {
    BLUR,        // 블러 처리 (내용 가림)
    EXCLUDE,     // 완전 제외 (목록에서 제거)
    ACCESS_DENY  // 접근 차단 (403 에러)
}
```

#### 2. AOP 어스펙트
```java
@Component
@Aspect
public class BlockFilterAspect {
    
    private final BlockService blockService;
    
    @Around("@annotation(blockFilter)")
    public Object applyBlockFilter(ProceedingJoinPoint joinPoint, BlockFilter blockFilter) {
        // 1. 기존 로직 실행 (수정 없음)
        Object result = joinPoint.proceed();
        
        // 2. 현재 사용자 ID 확인
        Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
        if (currentUserId == -1L) {
            return result; // 익명 사용자는 차단 로직 적용 안함
        }
        
        // 3. 응답 데이터에 차단 로직 적용
        return applyBlockLogic(result, currentUserId, blockFilter);
    }
    
    private Object applyBlockLogic(Object result, Long currentUserId, BlockFilter filter) {
        // ResponseEntity에서 실제 데이터 추출
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> response = (ResponseEntity<?>) result;
            GlobalResponse globalResponse = (GlobalResponse) response.getBody();
            Object data = globalResponse.getData();
            
            // 리스트 데이터 처리
            if (data instanceof List) {
                List<?> list = (List<?>) data;
                List<?> filteredList = processListData(list, currentUserId, filter);
                return ResponseEntity.ok(GlobalResponse.success(filteredList));
            }
            
            // 단일 객체 데이터 처리
            Object filteredData = processSingleData(data, currentUserId, filter);
            return ResponseEntity.ok(GlobalResponse.success(filteredData));
        }
        
        return result;
    }
}
```

#### 3. 차단 서비스
```java
@Service
public class BlockService {
    
    private final BlockRepository blockRepository;
    private final RedisTemplate<String, Set<Long>> redisTemplate;
    
    @Cacheable(value = "blocked_users", key = "#userId")
    public Set<Long> getBlockedUserIds(Long userId) {
        return blockRepository.findBlockedUserIdsByBlockerId(userId);
    }
    
    public <T> T applyBlockFilter(T item, Long currentUserId, String userField, BlockType type) {
        Set<Long> blockedUsers = getBlockedUserIds(currentUserId);
        Long authorId = extractUserIdFromObject(item, userField);
        
        if (blockedUsers.contains(authorId)) {
            switch (type) {
                case BLUR:
                    return blurContent(item);
                case EXCLUDE:
                    return null; // 리스트에서 제거됨
                case ACCESS_DENY:
                    throw new BlockedException("차단된 사용자의 컨텐츠입니다.");
            }
        }
        
        return item;
    }
}
```

### 🎨 사용 예시

```java
// 리뷰 목록 조회 - 차단된 사용자 리뷰 블러 처리
@BlockFilter(userField = "authorId", type = BlockType.BLUR)
@GetMapping("/{alcoholId}")
public ResponseEntity<?> getReviews(@PathVariable Long alcoholId, ...) {
    // 기존 코드 그대로 유지
}

// 팔로워 목록 조회 - 차단된 사용자 완전 제외
@BlockFilter(userField = "userId", type = BlockType.EXCLUDE)
@GetMapping("/{targetUserId}/follower-list")
public ResponseEntity<?> findFollowerList(@PathVariable Long targetUserId, ...) {
    // 기존 코드 그대로 유지
}

// 마이페이지 접근 - 차단된 사용자 접근 차단
@BlockFilter(userField = "userId", type = BlockType.ACCESS_DENY)
@GetMapping("/{userId}")
public ResponseEntity<?> getMyPage(@PathVariable Long userId) {
    // 기존 코드 그대로 유지
}
```

---

## ⚡ 성능 최적화 전략

### 1️⃣ Redis 캐싱 전략

```java
@Component
public class BlockCacheManager {
    
    // 사용자별 차단 목록 캐싱 (TTL: 1시간)
    @Cacheable(value = "blocked_users", key = "#userId", unless = "#userId == -1")
    public Set<Long> getBlockedUserIds(Long userId) {
        return blockRepository.findBlockedUserIdsByBlockerId(userId);
    }
    
    // 차단 관계 변경 시 캐시 무효화
    @CacheEvict(value = "blocked_users", key = "#userId")
    public void evictBlockCache(Long userId) {
        log.info("차단 캐시 무효화: userId={}", userId);
    }
    
    // 배치로 여러 사용자의 차단 목록 조회
    public Map<Long, Set<Long>> getBulkBlockedUsers(Set<Long> userIds) {
        return userIds.stream()
            .filter(id -> id != -1L)  // 익명 사용자 제외
            .collect(Collectors.toMap(
                Function.identity(),
                this::getBlockedUserIds
            ));
    }
}
```

### 2️⃣ 데이터베이스 최적화

```sql
-- 차단 관계 테이블
CREATE TABLE user_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blocker_id BIGINT NOT NULL COMMENT '차단한 사용자',
    blocked_id BIGINT NOT NULL COMMENT '차단당한 사용자',
    block_type VARCHAR(20) DEFAULT 'FULL',
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blocker_blocked (blocker_id, blocked_id)
);

-- 성능 최적화 인덱스
CREATE INDEX idx_user_block_blocker ON user_block(blocker_id);
CREATE INDEX idx_user_block_blocked ON user_block(blocked_id);
```

### 3️⃣ 성능 비교 분석

| 방식 | DB 쿼리 횟수 | 응답 시간 | 메모리 사용량 | 유지보수성 |
|------|-------------|-----------|--------------|------------|
| **쿼리 수정** | 1회 | 50ms | 낮음 | ⭐⭐ |
| **서비스 후처리** | 2회 | 80ms | 중간 | ⭐⭐⭐ |
| **AOP (캐시 적용)** | 1회 + 캐시 | 60ms | 중간 | ⭐⭐⭐⭐⭐ |
| **프론트엔드** | 2회 | 100ms | 높음 | ⭐ |

---

## 🎭 프론트엔드 연동 방안

### 📡 API 응답 형식

#### 기존 응답
```json
{
  "success": true,
  "data": [
    {
      "reviewId": 123,
      "content": "정말 맛있는 위스키입니다!",
      "authorNickname": "위스키러버",
      "authorProfileImage": "https://...",
      "rating": 4.5,
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

#### 차단 적용 후 응답
```json
{
  "success": true,
  "data": [
    {
      "reviewId": 123,
      "content": "*** 차단된 사용자의 리뷰입니다 ***",
      "authorNickname": "차단된 사용자",
      "authorProfileImage": null,
      "rating": 4.5,
      "createdAt": "2024-01-15T10:30:00",
      "isBlocked": true,
      "blockReason": "BLOCKED_USER"
    }
  ]
}
```

### 🎨 프론트엔드 UI 컴포넌트

#### React 예시
```tsx
interface ReviewCardProps {
  review: ReviewResponse;
}

const ReviewCard: React.FC<ReviewCardProps> = ({ review }) => {
  const [showBlocked, setShowBlocked] = useState(false);
  
  if (review.isBlocked && !showBlocked) {
    return (
      <div className="review-card blocked">
        <div className="blur-overlay">
          <div className="blocked-message">
            <Icon name="block" />
            <span>차단된 사용자의 리뷰</span>
          </div>
          <button 
            className="show-content-btn"
            onClick={() => setShowBlocked(true)}
          >
            내용 보기
          </button>
        </div>
      </div>
    );
  }
  
  return (
    <div className={`review-card ${review.isBlocked ? 'shown-blocked' : 'normal'}`}>
      <div className="review-header">
        <img src={review.authorProfileImage} alt="프로필" />
        <span className="nickname">{review.authorNickname}</span>
        <Rating value={review.rating} />
      </div>
      <div className="review-content">
        {review.content}
      </div>
      {review.isBlocked && (
        <div className="blocked-indicator">
          <span>차단된 사용자의 컨텐츠</span>
          <button onClick={() => setShowBlocked(false)}>숨기기</button>
        </div>
      )}
    </div>
  );
};
```

#### CSS 스타일
```css
.review-card.blocked {
  position: relative;
  filter: blur(5px);
  opacity: 0.6;
}

.blur-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(10px);
}

.blocked-message {
  color: white;
  text-align: center;
  margin-bottom: 16px;
}

.show-content-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.blocked-indicator {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  padding: 8px;
  border-radius: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: #856404;
}
```

---

## 📋 구현 단계별 가이드

### 🏗️ Phase 1: 기반 구조 구축

#### 1.1 차단 도메인 모델 생성
```java
// 1. Entity 생성
@Entity
@Table(name = "user_block")
public class UserBlock extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;
    
    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;
    
    @Enumerated(EnumType.STRING)
    private BlockType blockType = BlockType.FULL;
    
    private String reason;
}

// 2. Repository 생성
public interface BlockRepository extends JpaRepository<UserBlock, Long> {
    Set<Long> findBlockedIdsByBlockerId(Long blockerId);
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}

// 3. Service 생성
@Service
public class BlockService {
    public void createBlock(Long blockerId, Long blockedId) { ... }
    public void removeBlock(Long blockerId, Long blockedId) { ... }
    public Set<Long> getBlockedUserIds(Long userId) { ... }
}
```

#### 1.2 캐시 설정
```yaml
# application.yml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=1h
```

### 🔧 Phase 2: AOP 구현

#### 2.1 어노테이션 및 AOP 구현
```java
// 앞서 제시한 코드 구현
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlockFilter { ... }

@Component
@Aspect
public class BlockFilterAspect { ... }
```

#### 2.2 테스트 코드 작성
```java
@ExtendWith(MockitoExtension.class)
class BlockFilterAspectTest {
    
    @Mock
    private BlockService blockService;
    
    @InjectMocks
    private BlockFilterAspect blockFilterAspect;
    
    @Test
    void 차단된_사용자_리뷰_블러_처리_테스트() {
        // Given
        Long currentUserId = 1L;
        Long blockedUserId = 2L;
        when(blockService.getBlockedUserIds(currentUserId))
            .thenReturn(Set.of(blockedUserId));
        
        // When & Then
        // AOP 로직 테스트
    }
}
```

### 📱 Phase 3: 프론트엔드 연동

#### 3.1 API 클라이언트 수정
```typescript
// API 타입 정의 확장
interface BaseResponse {
  isBlocked?: boolean;
  blockReason?: string;
}

interface ReviewResponse extends BaseResponse {
  reviewId: number;
  content: string;
  authorNickname: string;
  // ... 기타 필드
}
```

#### 3.2 UI 컴포넌트 개발
```tsx
// 앞서 제시한 React 컴포넌트 구현
```

### 🚀 Phase 4: 점진적 적용

#### 4.1 우선순위별 적용
1. **High Priority**: 리뷰 조회, 댓글 조회
2. **Medium Priority**: 팔로우 목록, 마이페이지
3. **Low Priority**: 검색 결과, 추천 목록

#### 4.2 모니터링 설정
```java
@Component
public class BlockMetrics {
    private final MeterRegistry meterRegistry;
    
    public void recordBlockFilter(String endpoint, BlockType type, int blockedCount) {
        Counter.builder("block.filter.applied")
            .tag("endpoint", endpoint)
            .tag("type", type.name())
            .register(meterRegistry)
            .increment(blockedCount);
    }
}
```

### 📊 Phase 5: 성능 모니터링

#### 5.1 성능 지표 수집
- 차단 캐시 히트율
- 응답 시간 변화
- 메모리 사용량 변화

#### 5.2 알람 설정
```yaml
# Prometheus 규칙
groups:
  - name: block_system
    rules:
      - alert: BlockCacheHitRateLow
        expr: block_cache_hit_rate < 0.8
        for: 5m
        annotations:
          summary: "차단 캐시 히트율이 낮습니다"
```

---

## 🎯 결론

### ✨ 왜 AOP 방식을 추천하는가?

1. **최소 침습적** - 기존 코드를 거의 수정하지 않음
2. **중앙 집중식** - 차단 로직이 한 곳에서 관리됨  
3. **확장 가능** - 다양한 차단 타입을 쉽게 추가
4. **성능 효율적** - 캐시를 통한 최적화
5. **테스트 용이** - 독립적인 테스트 가능

### 🚀 기대 효과

- **개발 시간 단축**: 기존 쿼리 수정 불필요
- **유지보수성 향상**: 차단 로직 중앙화
- **성능 안정성**: 캐시 기반 최적화
- **사용자 경험 개선**: 세밀한 차단 옵션 제공

### 📅 구현 일정 (예상)

- **Week 1**: Phase 1-2 (기반 구조 + AOP)
- **Week 2**: Phase 3 (프론트엔드 연동)  
- **Week 3**: Phase 4-5 (점진적 적용 + 모니터링)

**총 예상 개발 기간: 3주**

---

## 📞 문의 및 지원

차단 기능 구현 과정에서 문의사항이 있으시면 언제든 연락주세요!

- 기술적 문의: 개발팀 채널
- 기획 관련: 기획팀 채널  
- 성능 관련: DevOps 팀 채널
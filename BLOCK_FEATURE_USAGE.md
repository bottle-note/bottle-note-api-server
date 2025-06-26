# 🚫 차단 기능 사용 가이드

## 🎯 구현 완료된 기능들

### ✅ 1. BlockFilter 어노테이션
- **위치**: `app.bottlenote.global.annotation.BlockFilter`
- **용도**: 컨트롤러 메서드에 차단 필터링 적용

### ✅ 2. BlockFilterAspect AOP
- **위치**: `app.bottlenote.global.aspect.BlockFilterAspect`
- **용도**: 어노테이션 기반 자동 차단 처리

### ✅ 3. BlockService
- **위치**: `app.bottlenote.global.service.BlockService`
- **용도**: 차단 관계 관리 (현재 메모리 기반)

### ✅ 4. ReviewController 적용 샘플
- **적용된 메서드**: `getReviews()`, `getDetailReview()`

### ✅ 5. BlockTestController
- **위치**: `app.bottlenote.global.controller.BlockTestController`
- **용도**: 차단 기능 테스트

---

## 🚀 사용 방법

### 1️⃣ 기본 사용법
```java
// 차단된 사용자의 리뷰를 블러 처리
@BlockFilter(userField = "authorId", type = BlockType.BLUR)
@GetMapping("/reviews")
public ResponseEntity<?> getReviews() {
    // 기존 로직 그대로 유지
}
```

### 2️⃣ 차단 타입 선택
```java
// 블러 처리
@BlockFilter(type = BlockType.BLUR)

// 완전 제외
@BlockFilter(type = BlockType.EXCLUDE)

// 접근 차단 (403 에러)
@BlockFilter(type = BlockType.ACCESS_DENY)
```

### 3️⃣ 사용자 ID 필드 지정
```java
// 기본값: authorId
@BlockFilter(userField = "authorId")

// 다른 필드명 사용
@BlockFilter(userField = "userId")
@BlockFilter(userField = "writerId")
```

---

## 🧪 테스트 방법

### 1. 서버 실행
```bash
./gradlew bootRun
```

### 2. 차단 관계 생성
```bash
# 사용자 1이 사용자 2를 차단
curl -X POST "http://localhost:8080/api/v1/test/block/add?blockerId=1&blockedId=2"
```

### 3. 차단 목록 확인
```bash
# 사용자 1의 차단 목록 조회
curl "http://localhost:8080/api/v1/test/block/list/1"
```

### 4. 리뷰 조회 테스트
```bash
# JWT 토큰으로 로그인한 상태에서 리뷰 조회
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     "http://localhost:8080/api/v1/reviews/1"
```

### 5. 차단 관계 해제
```bash
curl -X DELETE "http://localhost:8080/api/v1/test/block/remove?blockerId=1&blockedId=2"
```

---

## 📋 동작 확인 포인트

### ✅ 로그 확인
```
2024-01-15 10:30:00 INFO  - 차단 관계 생성: 1 -> 2
2024-01-15 10:31:00 DEBUG - 사용자 1의 차단 목록 조회: 1 명
2024-01-15 10:32:00 INFO  - 사용자 1의 차단 필터 적용 완료 - 차단 목록: 1 개
```

### ✅ 응답 데이터 변화
**차단 전:**
```json
{
  "success": true,
  "data": [
    {
      "reviewId": 123,
      "content": "정말 맛있는 위스키입니다!",
      "authorId": 2,
      "authorNickname": "위스키러버"
    }
  ]
}
```

**차단 후:**
```json
{
  "success": true,
  "data": [
    {
      "reviewId": 123,
      "content": "*** 차단된 사용자의 글입니다 ***",
      "authorId": 2,
      "authorNickname": "차단된 사용자",
      "isBlocked": true,
      "blockReason": "BLOCKED_USER"
    }
  ]
}
```

---

## 🔧 다른 컨트롤러에 적용하기

### 1. Import 추가
```java
import app.bottlenote.global.annotation.BlockFilter;
import app.bottlenote.global.annotation.BlockFilter.BlockType;
```

### 2. 어노테이션 적용
```java
// 댓글 목록 조회
@BlockFilter(userField = "authorId", type = BlockType.BLUR)
@GetMapping("/comments")
public ResponseEntity<?> getComments() { ... }

// 팔로워 목록 조회 (차단된 사용자 제외)
@BlockFilter(userField = "userId", type = BlockType.EXCLUDE)
@GetMapping("/followers")
public ResponseEntity<?> getFollowers() { ... }

// 마이페이지 접근 (차단된 사용자 접근 금지)
@BlockFilter(userField = "userId", type = BlockType.ACCESS_DENY)
@GetMapping("/mypage/{userId}")
public ResponseEntity<?> getMyPage(@PathVariable Long userId) { ... }
```

---

## ⚠️ 주의사항

### 1. 현재 제한사항
- **메모리 기반 저장**: 서버 재시작 시 차단 관계 초기화
- **단순 객체 복사**: 복잡한 객체 구조는 블러 처리 제한
- **필드명 의존**: 응답 DTO의 필드명이 정확해야 함

### 2. 성능 고려사항
- **익명 사용자**: 차단 로직 적용 안함 (성능 최적화)
- **캐시 적용**: `@Cacheable`로 반복 조회 최적화
- **로그 레벨**: 운영 시 DEBUG 로그 비활성화 권장

### 3. 보안 고려사항
- **클라이언트 검증**: 프론트엔드에서 추가 검증 필요
- **API 접근 제어**: 차단된 사용자의 직접 API 호출 차단 고려

---

## 🔄 다음 단계 (실제 운영 환경)

### 1. 데이터베이스 연동
```sql
CREATE TABLE user_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blocker_blocked (blocker_id, blocked_id)
);
```

### 2. Redis 캐시 적용
```yaml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
```

### 3. 차단 관리 API 개발
- 차단하기/해제하기 API
- 차단 목록 조회 API
- 차단 사유 관리

### 4. 프론트엔드 연동
- 블러 UI 컴포넌트
- 차단 버튼 추가
- 차단 목록 관리 페이지

---

## 🆘 문제 해결

### Q: AOP가 적용되지 않음
**A:** `@EnableAspectJAutoProxy` 확인 또는 Spring Boot의 자동 설정 확인

### Q: 필드를 찾을 수 없다는 오류
**A:** `userField` 값이 응답 DTO의 실제 필드명과 일치하는지 확인

### Q: 블러 처리가 동작하지 않음
**A:** 객체의 setter가 있는지, 또는 필드가 final이 아닌지 확인

### Q: 성능이 느림
**A:** 차단 목록이 많은 경우 배치 조회나 캐시 TTL 조정 고려

---

## 📞 연락처

구현 과정에서 문의사항이나 개선 제안이 있으시면 언제든 연락주세요!

- 기술 문의: 개발팀 채널
- 기능 제안: 기획팀 채널
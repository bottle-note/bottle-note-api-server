# Block 기능 - 사용자 차단 시스템

## 📋 개요

사용자 간 차단/해제 및 차단된 사용자 컨텐츠 자동 필터링을 제공하는 독립적인 도메인 모듈입니다.

## 🏗️ 구조

```
support/block/                          # Block 도메인
├── controller/BlockController.java     # REST API
├── domain/UserBlock.java               # 차단 관계 엔티티
├── exception/                          # Block 전용 예외
├── repository/                         # Repository 인터페이스 & 구현체
└── service/BlockService.java           # 비즈니스 로직

common/block/                           # 공통 컴포넌트
├── annotation/BlockWord.java           # @BlockWord 어노테이션
├── config/BlockWordConfig.java         # Spring 설정
└── serializer/BlockWordSerializer.java # Jackson 시리얼라이저
```

## 🎯 핵심 기능

### 1. 차단 관리 API

```http
POST /api/v1/blocks/create              # 사용자 차단
DELETE /api/v1/blocks/{blockedUserId}   # 차단 해제
GET /api/v1/blocks                      # 차단 목록 조회
```

### 2. 자동 컨텐츠 필터링

`@BlockWord` 어노테이션을 통해 JSON 직렬화 시점에서 차단된 사용자의 컨텐츠를 자동으로 대체합니다.

```java
public record ReviewInfo(
    @BlockWord(userIdPath = "userInfo.userId")
    String reviewContent,               // "차단된 사용자의 글입니다"
    UserInfo userInfo
) {}

public record UserInfo(
    Long userId,
    @BlockWord(value = "차단된 사용자", userIdPath = "userId")
    String nickName,                    // "차단된 사용자"
    String userProfileImage
) {}
```

**결과:**
```json
// 차단 전
{
    "reviewContent": "정말 맛있는 위스키입니다!",
    "userInfo": {
        "nickName": "위스키러버"
    }
}

// 차단 후
{
    "reviewContent": "차단된 사용자의 글입니다",
    "userInfo": {
        "nickName": "차단된 사용자"
    }
}
```

## 🗄️ 데이터베이스

### UserBlock 엔티티
- **차단 관계**: blocker_id ↔ blocked_id
- **유니크 제약**: 중복 차단 방지
- **인덱스**: 조회 성능 최적화
- **BaseTimeEntity**: 생성/수정 시간 자동 관리

## ⚡ 성능 최적화

### 캐싱
- **차단 목록**: `@Cacheable(value = "blocked_users")` - 2시간 TTL
- **캐시 무효화**: `@CacheEvict` - 차단/해제 시 자동 무효화

### Repository 주요 메서드
```java
Set<Long> findBlockedUserIdsByBlockerId(Long blockerId);     // 차단 목록
boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId); // 차단 여부
boolean existsMutualBlock(Long userId1, Long userId2);       // 상호 차단
long countByBlockerId(Long blockerId);                       // 차단한 수
long countByBlockedId(Long blockedId);                       // 차단당한 수
```

## 🧪 테스트

### HTTP 테스트 파일
```
http/block/차단_필터링_테스트.http
```

### 기본 테스트 시나리오
1. 토큰 발급 → 사용자 차단 → 차단 목록 조회 → 컨텐츠 필터링 확인 → 차단 해제

## 🔧 동작 원리

### 차단 생성
1. API 호출 → 유효성 검증 → 중복 확인 → DB 저장 → 캐시 무효화

### 컨텐츠 필터링
1. JSON 직렬화 → @BlockWord 감지 → 현재 사용자 확인 → 차단 여부 확인 → 대체 메시지/원본 반환

---

**핵심**: 간단한 어노테이션 하나로 전체 시스템에서 차단된 사용자 컨텐츠가 자동으로 필터링됩니다.
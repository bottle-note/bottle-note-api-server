```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **IN PROGRESS**
Start Date: 2026-01-05
Last Updated: 2026-01-22

** Completed Work **
- Phase 1: 이벤트 클래스 추가 (ImageResourceInvalidatedEvent, ImageResourceDeletedEvent)
- Phase 2: ResourceCommandService에 invalidate/delete 메서드 추가
- Phase 3: ResourceEventListener에 핸들러 추가
- Phase 4-5: Review 도메인 수정/삭제 이벤트 발행 및 테스트 완료

** Key Components **
- ImageResourceInvalidatedEvent.java: 이미지 교체 시 무효화 이벤트
- ImageResourceDeletedEvent.java: 이미지 삭제 시 삭제 이벤트
- ResourceEventListener.java: INVALIDATED/DELETED 핸들러

** Remaining Work **
- User 도메인: 프로필 이미지 변경 시 INVALIDATED 이벤트 발행
- Help 도메인: 수정/삭제 시 이벤트 발행
- Business 도메인: 수정/삭제 시 이벤트 발행
================================================================================
```

# 이미지 리소스 관리 분석

## 1. 현재 아키텍처 개요

```
[클라이언트] → [ImageUploadController] → [ImageUploadService] → [AWS S3]
                                              ↓
                                    [ResourceCommandService]
                                              ↓
                                      [ResourceLog 저장]

[도메인 서비스] → [이벤트 발행] → [ResourceEventListener] → [ResourceLog 상태 변경]
```

## 2. 핵심 흐름

### 2.1 PreSigned URL 발급 단계

1. 클라이언트가 `/api/v1/s3/presign-url`로 요청 (rootPath, uploadSize)
2. `ImageUploadService`가 S3 PreSigned URL 생성 (만료 5분)
3. CloudFront 기반 viewUrl과 S3 uploadUrl 쌍 반환
4. 로그인 사용자인 경우 `ResourceLog` **CREATED** 상태 저장

### 2.2 이미지 사용 활성화 단계

1. 도메인에서 이미지가 실제 사용될 때 `ImageResourceActivatedEvent` 발행
2. `ResourceEventListener`가 이벤트 수신
3. `ResourceLog` **ACTIVATED** 상태 로그 저장

## 3. 이벤트 타입 (ResourceEventType)

| 상태 | 설명 | 구현 상태 |
|------|------|----------|
| CREATED | PreSigned URL 발급 시 | 구현 완료 |
| ACTIVATED | 엔티티에 연결되어 사용됨 | 구현 완료 |
| INVALIDATED | 무효화됨 | 미구현 |
| DELETED | 삭제됨 | 미구현 |

## 4. 이미지 사용 도메인 (4곳)

| 도메인 | referenceType | 엔티티 | 특징 |
|--------|--------------|--------|------|
| Review | REVIEW | `ReviewImage` | `ImageInfo` 임베드, 최대 5장 |
| User | PROFILE | User 필드 | 단일 이미지, `imageUrl` 필드 |
| Help | HELP | `HelpImage` | 문의글 첨부 이미지 |
| Business | BUSINESS | `BusinessImage` | 사업자 지원 문서 |

## 5. 공통 구조

### 5.1 ImageInfo (Embeddable)

```java
@Embeddable
public class ImageInfo {
    private Long order;      // 이미지 순서
    private String imageUrl; // 전체 URL (CloudFront)
    private String imageKey; // S3 객체 키
    private String imagePath;// 저장 경로
    private String imageName;// 파일명
}
```

### 5.2 ResourceLog 엔티티

```java
@Entity
public class ResourceLog {
    private Long id;
    private Long userId;           // 요청 사용자
    private String resourceKey;    // S3 객체 키
    private String resourceType;   // IMAGE
    private ResourceEventType eventType; // CREATED/ACTIVATED/INVALIDATED/DELETED
    private Long referenceId;      // 연결된 엔티티 ID
    private String referenceType;  // REVIEW/PROFILE/HELP/BUSINESS
    private String viewUrl;        // CloudFront URL
    private String rootPath;       // 저장 경로
    private String bucketName;     // S3 버킷명
    private LocalDateTime createAt;
}
```

## 6. 주요 파일 위치

### 6.1 Core

| 파일 | 경로 |
|------|------|
| ImageUploadService | `bottlenote-mono/.../common/file/service/ImageUploadService.java` |
| ResourceCommandService | `bottlenote-mono/.../common/file/service/ResourceCommandService.java` |
| ResourceLog | `bottlenote-mono/.../common/file/domain/ResourceLog.java` |
| ResourceEventType | `bottlenote-mono/.../common/file/constant/ResourceEventType.java` |
| ImageResourceActivatedEvent | `bottlenote-mono/.../common/file/event/payload/ImageResourceActivatedEvent.java` |
| ResourceEventListener | `bottlenote-mono/.../common/file/event/listener/ResourceEventListener.java` |

### 6.2 도메인별 이미지 엔티티

| 도메인 | 파일 |
|--------|------|
| Review | `review/domain/ReviewImage.java`, `ReviewImages.java` |
| Help | `support/help/domain/HelpImage.java`, `HelpImageList.java` |
| Business | `support/business/domain/BusinessImage.java`, `BusinessImageList.java` |
| User | `user/domain/User.java` (imageUrl 필드) |

### 6.3 이벤트 발행 서비스

| 서비스 | 이벤트 발행 위치 |
|--------|-----------------|
| ReviewService | `publishImageActivatedEvent()` |
| UserBasicService | `profileImageChange()` |
| HelpService | 이미지 저장 시 |
| BusinessSupportService | 이미지 저장 시 |

## 7. Git 히스토리 (진화 과정)

| 날짜 | 커밋 | 내용 |
|------|------|------|
| 2024-05 | `98c17191` | PreSigned URL 기본 기능 구현 |
| - | `b96a558b` | ImageUtil 클래스 추가 |
| - | `3a5abd50` | ImageInfo로 중복 제거 리팩토링 |
| 2026-01-05 | `e00eecc5` | ImageUploadLog 도입 (상태 기반) |
| 2026-01-06 | `5dbd511c` | ResourceLog로 이벤트 기반 리팩토링 |
| 2026-01-09 | `c3e173dc` | ImageResourceActivatedEvent 도입 |

## 8. 2차 작업: 삭제 시 상태 업데이트

### 8.1 현재 미구현 상태

- `INVALIDATED`: 이미지가 교체되어 기존 이미지가 무효화될 때
- `DELETED`: 이미지가 완전히 삭제될 때

### 8.2 삭제/무효화 발생 시나리오

| 도메인 | 시나리오 | 예상 이벤트 |
|--------|----------|------------|
| Review | 리뷰 수정 시 이미지 교체 | 기존 이미지 INVALIDATED |
| Review | 리뷰 삭제 | 연관 이미지 DELETED |
| User | 프로필 이미지 변경 | 기존 이미지 INVALIDATED |
| Help | 문의글 수정/삭제 | 이미지 INVALIDATED/DELETED |
| Business | 지원글 수정/삭제 | 이미지 INVALIDATED/DELETED |

### 8.3 구현 필요 항목

1. `ImageResourceInvalidatedEvent` 이벤트 클래스
2. `ImageResourceDeletedEvent` 이벤트 클래스
3. `ResourceEventListener`에 핸들러 추가
4. 각 도메인 서비스에서 삭제/수정 시 이벤트 발행

---

## 9. 2차 작업 상세 분석: 삭제/무효화 이벤트 구현

### 9.1 현재 이미지 변경 시 동작 분석

#### Review 도메인

**수정 시 (`ReviewService.modifyReview`)**
```
1. Review.imageInitialization() 호출
2. ReviewImages.update() → clear() 후 새 이미지 추가 (orphanRemoval=true)
3. publishImageActivatedEvent() → 새 이미지만 ACTIVATED
```
- 문제: 기존 이미지에 대한 INVALIDATED 이벤트 누락

**삭제 시 (`ReviewService.deleteReview`)**
```
1. review.updateReviewActiveStatus(DELETED) → 소프트 삭제
2. 이미지는 그대로 유지 (물리적 삭제 안함)
```
- 문제: 이미지에 대한 DELETED 이벤트 누락

#### User 도메인

**프로필 이미지 변경 시 (`UserBasicService.profileImageChange`)**
```
1. user.changeProfileImage(newUrl) → 단순 덮어쓰기
2. ImageResourceActivatedEvent 발행 (새 이미지)
```
- 문제: 기존 이미지에 대한 INVALIDATED 이벤트 누락

#### Help 도메인

**수정 시 (`HelpService.modifyHelp`)**
```
1. help.updateHelp() → helpImageList.clear() 후 새 이미지 추가
2. publishImageActivatedEvent() → 새 이미지만 ACTIVATED
```
- 문제: 기존 이미지에 대한 INVALIDATED 이벤트 누락

**삭제 시 (`HelpService.deleteHelp`)**
```
1. help.deleteHelp() → status = DELETED (소프트 삭제)
```
- 문제: 이미지에 대한 DELETED 이벤트 누락

#### Business 도메인

**수정 시 (`BusinessSupportService.modify`)**
```
1. bs.update() → 이미지 clear 후 새로 추가
2. publishImageActivatedEvent() → 새 이미지만 ACTIVATED
```
- 문제: 기존 이미지에 대한 INVALIDATED 이벤트 누락

**삭제 시 (`BusinessSupportService.delete`)**
```
1. bs.delete() → 소프트 삭제
```
- 문제: 이미지에 대한 DELETED 이벤트 누락

### 9.2 구현 방안

#### 9.2.1 새 이벤트 클래스

```java
// 이미지 무효화 이벤트 (교체 시)
public record ImageResourceInvalidatedEvent(
    List<String> resourceKeys,
    Long referenceId,
    String referenceType
) {}

// 이미지 삭제 이벤트 (엔티티 삭제 시)
public record ImageResourceDeletedEvent(
    List<String> resourceKeys,
    Long referenceId,
    String referenceType
) {}
```

#### 9.2.2 도메인별 수정 필요 사항

| 도메인 | 서비스 메서드 | 수정 내용 |
|--------|-------------|----------|
| Review | `modifyReview()` | 수정 전 기존 이미지 조회 → INVALIDATED 이벤트 발행 |
| Review | `deleteReview()` | 삭제 시 연관 이미지 → DELETED 이벤트 발행 |
| User | `profileImageChange()` | 변경 전 기존 이미지 조회 → INVALIDATED 이벤트 발행 |
| Help | `modifyHelp()` | 수정 전 기존 이미지 조회 → INVALIDATED 이벤트 발행 |
| Help | `deleteHelp()` | 삭제 시 연관 이미지 → DELETED 이벤트 발행 |
| Business | `modify()` | 수정 전 기존 이미지 조회 → INVALIDATED 이벤트 발행 |
| Business | `delete()` | 삭제 시 연관 이미지 → DELETED 이벤트 발행 |

#### 9.2.3 기존 이미지 추출 방법

| 도메인 | 기존 이미지 접근 방법 |
|--------|---------------------|
| Review | `review.getReviewImages().getImages()` → `ImageInfo.getImageUrl()` |
| User | `user.getImageUrl()` (단일) |
| Help | `help.getHelpImageList().getHelpImages()` → `ImageInfo.getImageUrl()` |
| Business | `bs.getBusinessImageList().getBusinessImages()` → `ImageInfo.getImageUrl()` |

#### 9.2.4 ResourceCommandService 확장

```java
// 기존
activateImageResource(resourceKey, referenceId, referenceType)

// 추가 필요
invalidateImageResource(resourceKey, referenceId, referenceType)
deleteImageResource(resourceKey, referenceId, referenceType)
```

### 9.3 구현 우선순위

1. **Phase 1**: 이벤트 클래스 추가 (`ImageResourceInvalidatedEvent`, `ImageResourceDeletedEvent`) - **완료**
2. **Phase 2**: `ResourceCommandService`에 invalidate/delete 메서드 추가 - **완료**
3. **Phase 3**: `ResourceEventListener`에 핸들러 추가 - **완료**
4. **Phase 4**: 각 도메인 서비스 수정 (Review 완료 / User, Help, Business 미구현)
5. **Phase 5**: 단위 테스트 및 통합 테스트 추가 - **Review 도메인 완료**

### 9.4 주의사항

- **트랜잭션 순서**: 이벤트 발행은 엔티티 변경 후 `@TransactionalEventListener`로 처리
- **비동기 처리**: `@Async`로 메인 트랜잭션 블로킹 방지
- **이미지 비교**: 수정 시 "변경된 이미지"만 INVALIDATED 처리 (동일 이미지 제외)
- **Soft Delete**: 엔티티가 소프트 삭제되어도 이미지 상태는 DELETED로 기록

---

## 10. 구현 완료 내역

### 10.1 추가된 파일

| 파일 | 위치 |
|------|------|
| `ImageResourceInvalidatedEvent.java` | `bottlenote-mono/.../file/event/payload/` |
| `ImageResourceDeletedEvent.java` | `bottlenote-mono/.../file/event/payload/` |

### 10.2 수정된 파일

| 파일 | 변경 내용 |
|------|----------|
| `ResourceCommandService.java` | `invalidateImageResource`, `deleteImageResource` 메서드 추가 |
| `ResourceEventListener.java` | `handleImageResourceInvalidated`, `handleImageResourceDeleted` 핸들러 추가 |
| `ReviewService.java` | 수정/삭제 시 INVALIDATED/DELETED 이벤트 발행 로직 추가 |
| `ResourceCommandServiceTest.java` | 메서드 시그니처 변경 및 삭제 테스트 추가 |
| `ImageUploadIntegrationTest.java` | INVALIDATED, DELETED 이벤트 통합 테스트 추가 |

### 10.3 남은 작업

- User 도메인: 프로필 이미지 변경 시 INVALIDATED 이벤트 발행
- Help 도메인: 수정/삭제 시 이벤트 발행
- Business 도메인: 수정/삭제 시 이벤트 발행

---

*작성일: 2026-01-15*
*업데이트: 2026-01-15 (2차 작업 분석 추가)*
*업데이트: 2026-01-15 (Review 도메인 구현 완료)*

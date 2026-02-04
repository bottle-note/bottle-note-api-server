# UserHistory 테이블 리팩토링 가이드

## 1. 현재 아키텍처

### 3단계 흐름: 발행 → 수집 → 저장

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           1. 발행 (Publish)                                  │
│                         도메인 서비스에서 직접 호출                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ReviewService          → reviewEventPublisher.publishReviewHistoryEvent()  │
│  ReviewReplyService     → reviewReplyEventPublisher.publishReplyHistoryEvent()│
│  LikesCommandService    → likesEventPublisher.publishLikesHistoryEvent()    │
│  PicksCommandService    → picksEventPublisher.publishPicksHistoryEvent()    │
│  RatingCommandService   → ratingEventPublisher.publishRatingHistoryEvent()  │
│                                                                             │
│  * 각 도메인별 이벤트 페이로드 사용                                            │
│    - ReviewRegistryEvent, LikesRegistryEvent, PicksRegistryEvent 등         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           2. 수집 (Collect)                                  │
│                        HistoryEventPublisher                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  역할: 도메인 이벤트 → HistoryEvent 변환                                      │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  도메인 이벤트          →        HistoryEvent                        │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  ReviewRegistryEvent   →  eventType: REVIEW_CREATE                  │   │
│  │                            eventCategory: REVIEW                     │   │
│  │                            redirectUrl: /review/{reviewId}          │   │
│  │                            alcoholId: event.alcoholId()              │   │
│  │                            content: event.content()                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  RatingRegistryEvent   →  eventType: START_RATING / MODIFY / DELETE │   │
│  │                            dynamicMessage: {currentValue, prevValue} │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  * 추가 정보 조회                                                            │
│    - ReviewFacade.getAlcoholIdByReviewId() (좋아요 시)                       │
│                                                                             │
│  * ApplicationEventPublisher.publishEvent(HistoryEvent) 호출                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           3. 저장 (Store)                                    │
│                          HistoryListener                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  @Async                          ← 비동기 처리                               │
│  @Transactional(REQUIRES_NEW)    ← 별도 트랜잭션 (원본과 분리)                │
│  @TransactionalEventListener     ← 원본 트랜잭션 커밋 후 실행                 │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  1. 이미지 URL 조회                                                  │   │
│  │     alcoholFacade.findAlcoholImageUrlById(alcoholId)                │   │
│  │                                                                     │   │
│  │  2. UserHistory 엔티티 빌드                                          │   │
│  │     - userId, alcoholId, eventCategory, eventType                   │   │
│  │     - redirectUrl, imageUrl, content, dynamicMessage                │   │
│  │     - eventYear, eventMonth (현재 시점)                              │   │
│  │                                                                     │   │
│  │  3. DB 저장                                                          │   │
│  │     userHistoryRepository.save()                                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  * 실패해도 원본 트랜잭션에 영향 없음                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 단계별 요약

| 단계        | 역할         | 주체                    | 특징                        |
|-----------|------------|-----------------------|---------------------------|
| **1. 발행** | 도메인 이벤트 발생 | 각 도메인 서비스             | 동기 호출, 도메인별 이벤트 페이로드      |
| **2. 수집** | 이벤트 변환/정규화 | HistoryEventPublisher | 도메인 이벤트 → HistoryEvent 변환 |
| **3. 저장** | DB 저장      | HistoryListener       | 비동기, 별도 트랜잭션, 실패 격리       |

---

## 2. 설계 철학

### 추상 이벤트 로그 테이블

UserHistory는 **특정 도메인에 종속되지 않는 추상 이벤트 로그 테이블**이다.

- 사용자의 모든 활동 히스토리를 관장
- 특정 엔티티(리뷰, 주류 등)에 FK로 종속되지 않음
- **조합식**으로 의미를 결정하는 유연한 구조

### 조합식 활용

```
event_type + event_resource_id = 이벤트 의미
─────────────────────────────────────────────
REVIEW_CREATE + 64        → 64번 리뷰 작성
REVIEW_LIKES + 64         → 64번 리뷰 좋아요
REVIEW_REPLY_CREATE + 123 → 123번 댓글 작성
IS_PICK + 6332            → 6332번 주류 찜
START_RATING + 6332       → 6332번 주류 별점

# 미래 확장 예시
FOLLOW_USER + 42          → 42번 유저 팔로우
BADGE_EARNED + 7          → 7번 뱃지 획득
```

### 락인 속성은 dynamic_message에서 제어

고정 컬럼은 최소화하고, **이벤트별 특수 속성은 `dynamic_message` (JSON)** 에서 관리한다.

```json
// 별점 이벤트 - 변경 이력 저장
{
	"currentValue": "5.0",
	"prevValue": "4.5",
	"ratingDiff": "0.5"
}

// 미래 확장 - 뱃지 획득 이벤트
{
	"badgeName": "리뷰왕",
	"badgeLevel": "gold"
}

// 미래 확장 - 팔로우 이벤트
{
	"targetUserNickname": "위스키러버"
}
```

### 컬럼 역할 정리

| 컬럼                  | 역할                    | 락인 여부  |
|---------------------|-----------------------|--------|
| `event_type`        | 이벤트 종류 (enum)         | ✅ 고정   |
| `event_category`    | 이벤트 카테고리 (enum)       | ✅ 고정   |
| `event_resource_id` | 이벤트 대상 리소스 ID         | ✅ 고정   |
| `alcohol_id`        | 주류 ID (nullable, 필터용) | ⚠️ 선택적 |
| `dynamic_message`   | 이벤트별 특수 속성 (JSON)     | ❌ 유연   |
| `content`           | 텍스트 내용 (리뷰 본문 등)      | ❌ 유연   |
| `redirect_url`      | 클릭 시 이동 경로            | ❌ 유연   |

### 확장성

새로운 이벤트 추가 시:

1. `EventType` enum에 추가
2. 이벤트 발행 코드 작성
3. **스키마 변경 없음**

```java
// 1. EventType에 추가
FOLLOW_USER(EventCategory.SOCIAL, "팔로우")

// 2. 저장
HistoryEvent.

builder()
    .

eventType(FOLLOW_USER)
    .

eventResourceId(targetUserId)  // 팔로우 대상 유저 ID
    .

alcoholId(null)                // 주류 무관
    .

dynamicMessage(Map.of("targetUserNickname", "위스키러버"))
		.

build();
```

---

## 3. 변경 목적

- `alcohol_id` → `event_resource_id`로 이름 변경
- 이벤트 대상 리소스 ID를 명확히 저장 (reviewId, replyId, alcoholId 등)
- 기존 주류 필터/검색용 `alcohol_id` 컬럼 별도 유지 (nullable)

---

## 4. 스키마 변경

### 변경 후 컬럼 구조

| 컬럼                  | 타입                | 설명             |
|---------------------|-------------------|----------------|
| `event_resource_id` | BIGINT            | 이벤트 발생 리소스 ID  |
| `alcohol_id`        | BIGINT (nullable) | 주류 ID (검색/필터용) |

### DDL

```sql
-- 1) 기존 alcohol_id 백업
ALTER TABLE user_histories
	ADD COLUMN temp_alcohol_id BIGINT;
UPDATE user_histories
SET temp_alcohol_id = alcohol_id;

-- 2) alcohol_id → event_resource_id 이름 변경
ALTER TABLE user_histories
	CHANGE COLUMN alcohol_id event_resource_id BIGINT
COMMENT
'이벤트 발생 리소스 ID';

-- 3) alcohol_id 컬럼 추가 (nullable)
ALTER TABLE user_histories
	ADD COLUMN alcohol_id BIGINT NULL COMMENT '주류 ID' AFTER event_resource_id;

-- 4) alcohol_id 복원
UPDATE user_histories
SET alcohol_id = temp_alcohol_id;

-- 5) REVIEW_CREATE, REVIEW_LIKES, BEST_REVIEW_SELECTED만 redirect_url에서 reviewId 추출
UPDATE user_histories
SET event_resource_id = CAST(SUBSTRING_INDEX(redirect_url, '/', -1) AS UNSIGNED)
WHERE event_type IN ('REVIEW_CREATE', 'REVIEW_LIKES', 'BEST_REVIEW_SELECTED');

-- 6) 임시 컬럼 삭제
ALTER TABLE user_histories
	DROP COLUMN temp_alcohol_id;

-- 7) 인덱스 추가 (선택)
CREATE INDEX idx_user_histories_event_resource ON user_histories (event_type, event_resource_id);
```

---

## 5. 마이그레이션 현황

| 이벤트                    | 현재 저장값    | redirect_url              | 마이그레이션 | 비고                  |
|------------------------|-----------|---------------------------|--------|---------------------|
| `REVIEW_CREATE`        | alcoholId | `/review/{reviewId}`      | ✅ 가능   | reviewId 추출         |
| `REVIEW_LIKES`         | alcoholId | `/review/{reviewId}`      | ✅ 가능   | reviewId 추출         |
| `BEST_REVIEW_SELECTED` | alcoholId | `/review/{reviewId}`      | ✅ 가능   | reviewId 추출         |
| `REVIEW_REPLY_CREATE`  | alcoholId | `/review/{reviewId}`      | ❌ 불가   | replyId 없음, 신규부터 적용 |
| `IS_PICK` / `UNPICK`   | alcoholId | `/search/all/{alcoholId}` | 변경 없음  |                     |
| `START_RATING` 등       | alcoholId | `/search/all/{alcoholId}` | 변경 없음  |                     |

---

## 6. 이벤트별 저장 값 (신규)

| 이벤트                    | event_resource_id | alcohol_id | dynamic_message                         |
|------------------------|-------------------|------------|-----------------------------------------|
| `REVIEW_CREATE`        | reviewId          | alcoholId  | -                                       |
| `REVIEW_LIKES`         | reviewId          | alcoholId  | -                                       |
| `REVIEW_REPLY_CREATE`  | replyId           | alcoholId  | -                                       |
| `BEST_REVIEW_SELECTED` | reviewId          | alcoholId  | -                                       |
| `IS_PICK` / `UNPICK`   | alcoholId         | alcoholId  | -                                       |
| `START_RATING`         | alcoholId         | alcoholId  | `{currentValue}`                        |
| `RATING_MODIFY`        | alcoholId         | alcoholId  | `{currentValue, prevValue, ratingDiff}` |
| `RATING_DELETE`        | alcoholId         | alcoholId  | `{currentValue}`                        |

---

## 7. 코드 변경

### 7.1 엔티티 (UserHistory.java)

```java
// 변경 전
@Comment("알콜 ID")
@Column(name = "alcohol_id")
private Long alcoholId;

// 변경 후
@Comment("이벤트 발생 리소스 ID")
@Column(name = "event_resource_id")
private Long eventResourceId;

@Comment("주류 ID")
@Column(name = "alcohol_id")
private Long alcoholId;
```

### 7.2 이벤트 페이로드 (HistoryEvent.java)

```java

@Builder
public record HistoryEvent(
		Long userId,
		EventCategory eventCategory,
		EventType eventType,
		String redirectUrl,
		Long eventResourceId,  // 추가
		Long alcoholId,        // nullable
		String content,
		Map<String, String> dynamicMessage) {

	public HistoryEvent {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(eventResourceId, "eventResourceId must not be null");
		// alcoholId는 nullable (주류 무관 이벤트 허용)
	}
}
```

### 7.3 이벤트 발행자 (HistoryEventPublisher.java)

```java
// 리뷰 생성
public void publishReviewHistoryEvent(ReviewRegistryEvent event) {
	final Long reviewId = event.reviewId();

	HistoryEvent historyEvent =
			HistoryEvent.builder()
					.userId(event.userId())
					.eventCategory(REVIEW)
					.eventType(REVIEW_CREATE)
					.redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
					.eventResourceId(reviewId)  // reviewId
					.alcoholId(event.alcoholId())
					.content(event.content())
					.build();
	eventPublisher.publishEvent(historyEvent);
}

// 댓글 생성 (이벤트 타입 변경 필요)
public void publishReplyHistoryEvent(ReviewReplyRegistryEvent event) {
	final Long replyId = event.replyId();
	final Long reviewId = event.reviewId();

	HistoryEvent historyEvent =
			HistoryEvent.builder()
					.userId(event.userId())
					.eventCategory(REVIEW)
					.eventType(REVIEW_REPLY_CREATE)
					.redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
					.eventResourceId(replyId)  // replyId
					.alcoholId(event.alcoholId())
					.content(event.content())
					.build();
	eventPublisher.publishEvent(historyEvent);
}

// 좋아요
public void publishLikesHistoryEvent(LikesRegistryEvent event) {
	final Long alcoholId = reviewFacade.getAlcoholIdByReviewId(event.reviewId());
	final Long reviewId = event.reviewId();

	HistoryEvent historyEvent =
			HistoryEvent.builder()
					.userId(event.userId())
					.eventCategory(REVIEW)
					.eventType(REVIEW_LIKES)
					.redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
					.eventResourceId(reviewId)  // reviewId
					.alcoholId(alcoholId)
					.content(event.content())
					.build();
	eventPublisher.publishEvent(historyEvent);
}

// 찜하기
public void publishPicksHistoryEvent(PicksRegistryEvent event) {
	final Long alcoholId = event.alcoholId();

	HistoryEvent historyEvent =
			HistoryEvent.builder()
					.userId(event.userId())
					.eventCategory(EventCategory.PICK)
					.eventType(event.picksStatus() == PICK ? IS_PICK : UNPICK)
					.redirectUrl(RedirectUrlType.ALCOHOL.getUrl() + "/" + alcoholId)
					.eventResourceId(alcoholId)  // alcoholId
					.alcoholId(alcoholId)
					.build();
	eventPublisher.publishEvent(historyEvent);
}

// 별점
public void publishRatingHistoryEvent(RatingRegistryEvent event) {
	final Long alcoholId = event.alcoholId();
	final boolean isUpdate = !Objects.isNull(event.prevRating());
	double prevRatingPoint = 0.0;

	if (isUpdate) {
		prevRatingPoint = event.prevRating().getRating();
	}
	Double currentRatingPoint = event.currentRating().getRating();

	HistoryEvent historyEvent =
			HistoryEvent.builder()
					.userId(event.userId())
					.eventCategory(RATING)
					.eventType(makeEventType(isUpdate, currentRatingPoint))
					.redirectUrl(RedirectUrlType.ALCOHOL.getUrl() + "/" + alcoholId)
					.eventResourceId(alcoholId)  // alcoholId
					.alcoholId(alcoholId)
					.dynamicMessage(
							isUpdate
									? makeDynamicMessage(currentRatingPoint, prevRatingPoint)
									: Map.of("currentValue", currentRatingPoint.toString()))
					.build();
	eventPublisher.publishEvent(historyEvent);
}
```

### 7.4 댓글 이벤트 추가 (ReviewReplyRegistryEvent.java)

```java
package app.bottlenote.review.event.payload;

public record ReviewReplyRegistryEvent(
		Long replyId,
		Long reviewId,
		Long alcoholId,
		Long userId,
		String content) {

	public static ReviewReplyRegistryEvent of(
			Long replyId, Long reviewId, Long alcoholId, Long userId, String content) {
		return new ReviewReplyRegistryEvent(replyId, reviewId, alcoholId, userId, content);
	}
}
```

### 7.5 ReviewReplyService.java 수정

```java
// 변경 전
ReviewRegistryEvent event =
		ReviewRegistryEvent.of(reply.getReviewId(), alcoholId, reply.getUserId(), reply.getContent());
reviewReplyEventPublisher.

publishReplyHistoryEvent(event);

// 변경 후
ReviewReplyRegistryEvent event =
		ReviewReplyRegistryEvent.of(
				reply.getId(),           // replyId 추가
				reply.getReviewId(),
				alcoholId,
				reply.getUserId(),
				reply.getContent());
reviewReplyEventPublisher.

publishReplyHistoryEvent(event);
```

### 7.6 리스너 (HistoryListener.java)

```java

@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
@TransactionalEventListener
public void handleUserHistoryRegistry(HistoryEvent event) {
	String alcoholImageUrl = event.alcoholId() != null
			? alcoholFacade.findAlcoholImageUrlById(event.alcoholId()).orElse(null)
			: null;

	UserHistory save =
			userHistoryRepository.save(
					UserHistory.builder()
							.userId(event.userId())
							.eventResourceId(event.eventResourceId())  // 변경
							.alcoholId(event.alcoholId())              // nullable
							.eventCategory(event.eventCategory())
							.eventType(event.eventType())
							.redirectUrl(event.redirectUrl())
							.imageUrl(alcoholImageUrl)
							.content(event.content())
							.dynamicMessage(event.dynamicMessage())
							.eventYear(String.valueOf(LocalDateTime.now().getYear()))
							.eventMonth(String.valueOf(LocalDateTime.now().getMonth()))
							.build());

	log.debug("History saved: {}", save);
}
```

---

## 8. 롤백 DDL

```sql
ALTER TABLE user_histories
	DROP INDEX idx_user_histories_event_resource,
DROP
COLUMN alcohol_id,
  CHANGE COLUMN event_resource_id alcohol_id BIGINT COMMENT
'알콜 ID';
```

---

## 9. 주의사항

1. **REVIEW_REPLY_CREATE 마이그레이션 불가**
	- 기존 데이터는 `event_resource_id`에 alcoholId 유지
	- 신규 데이터부터 replyId 저장

2. **마이그레이션 순서 중요**
	- 반드시 `temp_alcohol_id`로 백업 후 DDL 실행
	- 순서 틀리면 alcohol_id 데이터 유실

3. **alcohol_id nullable**
	- 주류 무관 이벤트(팔로우, 뱃지 등) 확장 대비
	- 기존 조회 쿼리에서 NULL 체크 필요할 수 있음

---

## 10. 페이로드 네이밍 변경

### 배경

현재 도메인별 페이로드가 `*RegistryEvent`로 명명되어 있어 "진짜 이벤트"인 `HistoryEvent`와 혼동됨.
실제로는 **Publisher에게 전달하는 데이터 묶음(Payload)** 이므로 이름 변경.

### 변경 내용

| 현재                        | 변경 후                      | 위치                              |
|---------------------------|---------------------------|----------------------------------|
| `ReviewRegistryEvent`     | `ReviewHistoryPayload`    | `review.event.payload`           |
| `ReviewReplyRegistryEvent`| `ReviewReplyHistoryPayload`| `review.event.payload`          |
| `LikesRegistryEvent`      | `LikesHistoryPayload`     | `like.event.payload`             |
| `PicksRegistryEvent`      | `PicksHistoryPayload`     | `picks.event.payload`            |
| `RatingRegistryEvent`     | `RatingHistoryPayload`    | `rating.event.payload`           |

### 구조 명확화

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  [도메인 서비스]                                                             │
│       ↓ *HistoryPayload (데이터 묶음)                                        │
│  [HistoryEventPublisher] - 정제/변환                                        │
│       ↓ HistoryEvent (진짜 이벤트)                                          │
│  [HistoryListener / MQ]                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 아키텍처 룰 체크 수정

`*RegistryEvent` → `*HistoryPayload` 변경에 따라 아키텍처 테스트 규칙 수정 필요.

```java
// 기존 룰 (수정 필요)
.should().haveSimpleNameEndingWith("Event")

// 변경 후
.should().haveSimpleNameEndingWith("Payload")  // History 페이로드
.should().haveSimpleNameEndingWith("Event")    // 진짜 이벤트 (HistoryEvent)
```

---

## 11. EventCategory/EventType 확장 계획

### 신규 EventCategory

| 카테고리       | 용도              | alcohol_id |
|--------------|------------------|------------|
| `REVIEW`     | 리뷰, 댓글, 좋아요   | 필수         |
| `PICK`       | 찜하기             | 필수         |
| `RATING`     | 별점               | 필수         |
| `SOCIAL`     | 팔로우, 차단 (신규)  | null        |
| `REPORT`     | 신고 (신규)         | nullable    |
| `PROFILE`    | 프로필 변경 (신규)   | null        |
| `ACHIEVEMENT`| 뱃지, 티어 (신규)   | null        |

### 신규 EventType (우선순위: 팔로우)

| 이벤트            | 카테고리   | event_resource_id | alcohol_id | dynamic_message              |
|-----------------|----------|-------------------|------------|------------------------------|
| `FOLLOW_USER`   | SOCIAL   | targetUserId      | null       | `{targetNickname, followerCount}` |
| `UNFOLLOW_USER` | SOCIAL   | targetUserId      | null       | -                            |

### 팔로우 이벤트 예시

```java
// FollowService.java
private FollowHistoryPayload buildFollowPayload(Long userId, Long targetUserId, String targetNickname) {
    return new FollowHistoryPayload(userId, targetUserId, targetNickname);
}

// HistoryEventPublisher.java
public void publishFollowHistoryEvent(FollowHistoryPayload payload) {
    HistoryEvent historyEvent = HistoryEvent.builder()
        .userId(payload.userId())
        .eventCategory(EventCategory.SOCIAL)
        .eventType(EventType.FOLLOW_USER)
        .eventResourceId(payload.targetUserId())
        .alcoholId(null)
        .redirectUrl("/user/" + payload.targetUserId() + "/profile")
        .dynamicMessage(Map.of("targetNickname", payload.targetNickname()))
        .build();
    eventPublisher.publishEvent(historyEvent);
}
```

---

## 12. MQ 전환 대비

### 현재 → 미래 전환

```
현재: ApplicationEventPublisher → HistoryListener (로컬)
미래: ApplicationEventPublisher → RabbitMQ/NATS → 외부 서비스
```

### Publisher 확장 포인트

```java
@Component
public class HistoryEventPublisher {

    private final ApplicationEventPublisher springPublisher;
    // private final RabbitTemplate rabbitTemplate;  // 미래 추가
    // private final NatsConnection natsConnection;  // 미래 추가

    public void publish(HistoryEvent event) {
        // 로컬 리스너
        springPublisher.publishEvent(event);

        // MQ 발행 (미래)
        // rabbitTemplate.convertAndSend("history.exchange", "history.event", event);
    }
}
```

### 직렬화 대상

- `HistoryEvent` 하나만 직렬화 스키마 관리
- `*HistoryPayload`는 내부용이므로 직렬화 불필요

package app.bottlenote.global.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.BlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 차단 기능 테스트용 컨트롤러
 *
 * 개발/테스트 환경에서만 활성화됩니다.
 * 운영 환경(prod)에서는 자동으로 비활성화됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/block")
@RequiredArgsConstructor
@Profile({"local", "dev", "test", "!prod"})
public class BlockTestController {

    private final BlockService blockService;

    /**
     * 차단 관계 생성 (테스트용)
     *
     * @param blockerId 차단한 사용자 ID
     * @param blockedId 차단될 사용자 ID
     * @param reason 차단 사유 (선택사항)
     */
    @PostMapping("/add")
    public ResponseEntity<?> createBlock(
            @RequestParam Long blockerId,
            @RequestParam Long blockedId,
            @RequestParam(required = false, defaultValue = "테스트 차단") String reason) {

        try {
            var userBlock = blockService.createBlock(blockerId, blockedId, reason);
            return GlobalResponse.ok("차단 관계 생성 완료: " + blockerId + " -> " + blockedId +
                                   " (ID: " + userBlock.getId() + ")");
        } catch (Exception e) {
            log.error("테스트 차단 생성 실패", e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.fail("차단 관계 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 차단 관계 해제 (테스트용)
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> deleteBlock(
            @RequestParam Long blockerId,
            @RequestParam Long blockedId) {

        blockService.removeBlock(blockerId, blockedId);

        return GlobalResponse.ok("차단 관계 해제 완료: " + blockerId + " -> " + blockedId);
    }

    /**
     * 차단 목록 조회 (테스트용)
     */
    @GetMapping("/list/{userId}")
    public ResponseEntity<?> getBlockList(@PathVariable Long userId) {

        var blockedUsers = blockService.getBlockedUserIds(userId);

        return GlobalResponse.ok(blockedUsers);
    }

    /**
     * 차단 여부 확인 (테스트용)
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkBlock(
            @RequestParam Long blockerId,
            @RequestParam Long blockedId) {

        boolean isBlocked = blockService.isBlocked(blockerId, blockedId);

        return GlobalResponse.ok("차단 여부: " + isBlocked);
    }

    /**
     * 모든 차단 관계 초기화 (테스트용) - 현재 DB 기반으로 구현되어 있어 이 기능은 비활성화
     */
    @DeleteMapping("/clear")
    public ResponseEntity<?> deleteAllBlocks() {

        // DB 기반으로 변경되어 전체 삭제는 위험하므로 비활성화
        log.warn("전체 차단 관계 초기화는 DB 기반에서 지원하지 않습니다.");

        return GlobalResponse.ok("DB 기반에서는 전체 초기화를 지원하지 않습니다. 개별 해제를 사용해주세요.");
    }

    /**
     * 사용자 차단 통계 조회 (테스트용)
     */
    @GetMapping("/statistics/{userId}")
    public ResponseEntity<?> getBlockStatistics(@PathVariable Long userId) {

        try {
            var statistics = blockService.getBlockStatistics(userId);
            return GlobalResponse.ok(statistics);
        } catch (Exception e) {
            log.error("차단 통계 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.fail("차단 통계 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 차단 기능 테스트 (테스트용) - 실제 리뷰 구조와 유사한 응답
     *
     * 이 API는 실제 리뷰 목록 응답과 유사한 구조로 테스트 데이터를 제공합니다.
     * 직접 차단 필터링을 적용하여 차단된 사용자의 컨텐츠가 마스킹됩니다.
     *
     * 테스트 시나리오:
     * 1. 사용자 1이 사용자 2,3을 차단한 상태에서 X-User-Id=1로 요청
     * 2. 사용자 2,3의 리뷰 내용이 "차단된 사용자의 글입니다"로 마스킹됨
     * 3. 닉네임이 "차단된 사용자"로 변경됨
     */
    @GetMapping("/test-data/{userId}")
    public ResponseEntity<?> getTestData(@PathVariable Long userId,
                                        @RequestHeader(value = "X-User-Id", required = false) Long requestUserId) {

        log.info("차단 필터 테스트 데이터 요청 - pathUserId: {}, requestUserId: {}", userId, requestUserId);

        // 요청한 사용자 ID 결정 (X-User-Id 헤더 우선)
        Long currentUserId = requestUserId != null ? requestUserId : userId;

        // 차단된 사용자 목록 조회
        java.util.Set<Long> blockedUserIds = java.util.Collections.emptySet();
        if (currentUserId != null && currentUserId != -1L) {
            blockedUserIds = blockService.getBlockedUserIds(currentUserId);
            log.info("사용자 {}의 차단 목록: {}", currentUserId, blockedUserIds);
        }

        // 실제 리뷰 구조와 유사한 테스트 데이터 생성
        var testReviews = java.util.List.of(
            createTestReview(1L, "정말 맛있는 위스키였습니다. 스모키한 향이 일품이네요!", "위스키_러버", "profile1.jpg", 4.5),
            createTestReview(2L, "첫 시음인데 생각보다 부드럽고 달콤합니다. 입문자에게 추천!", "초보_위스키", "profile2.jpg", 4.0),
            createTestReview(3L, "가격 대비 훌륭한 위스키입니다. 재구매 의향 100%", "위스키_마니아", "profile3.jpg", 4.8),
            createTestReview(4L, "오크향이 강해서 호불호가 갈릴 것 같아요.", "리뷰_고수", "profile4.jpg", 3.5),
            createTestReview(5L, "친구들과 함께 마시기 좋은 위스키였습니다.", "소셜_드링커", "profile5.jpg", 4.2)
        );

        // 차단된 사용자의 리뷰에 필터링 적용
        var filteredReviews = testReviews.stream()
            .map(review -> {
                Long authorId = (Long) review.get("authorId");
                if (blockedUserIds.contains(authorId)) {
                    return applyBlockMask(review);
                }
                return review;
            })
            .collect(java.util.stream.Collectors.toList());

        // 리뷰 목록 응답 구조로 래핑
        var response = java.util.Map.of(
            "totalCount", filteredReviews.size(),
            "reviewList", filteredReviews,
            "message", "차단 필터 테스트용 리뷰 데이터 (직접 필터링 적용)",
            "blockedUserIds", blockedUserIds,
            "currentUserId", currentUserId
        );

        return GlobalResponse.ok(response);
    }

    /**
     * 테스트용 리뷰 데이터 생성 헬퍼 메서드
     */
    private java.util.Map<String, Object> createTestReview(
        Long authorId, String content, String nickname, String profileImage, Double rating) {

        java.util.Map<String, Object> review = new java.util.HashMap<>();

        // 리뷰 기본 정보
        review.put("reviewId", 1000L + authorId);
        review.put("reviewContent", content);
        review.put("reviewImageUrl", "review_image_" + authorId + ".jpg");
        review.put("createAt", java.time.LocalDateTime.now().minusDays(authorId));
        review.put("totalImageCount", 2L);

        // 작성자 정보 (차단 필터링 대상)
        review.put("authorId", authorId);
        review.put("authorNickname", nickname);
        review.put("authorProfileImage", profileImage);

        // 사용자 정보 (UserInfo 구조 모방)
        review.put("userInfo", java.util.Map.of(
            "userId", authorId,
            "nickName", nickname,
            "userProfileImage", profileImage
        ));

        // 리뷰 상태 및 평점
        review.put("isMyReview", false);
        review.put("status", "PUBLIC");
        review.put("isBestReview", rating >= 4.5);
        review.put("rating", rating);

        // 상호작용 정보
        review.put("likeCount", (long) (Math.random() * 20));
        review.put("replyCount", (long) (Math.random() * 10));
        review.put("isLikedByMe", false);
        review.put("hasReplyByMe", false);
        review.put("viewCount", (long) (Math.random() * 100));

        // 기타 정보
        review.put("price", java.math.BigDecimal.valueOf(50000 + (authorId * 10000)));
        review.put("sizeType", "BOTTLE_750");
        review.put("locationInfo", "서울시 강남구");
        review.put("tastingTagList", "스모키, 오크, 바닐라");

        return review;
    }

    /**
     * 테스트용 SecurityContext 모킹 메서드
     * 실제 운영에서는 JWT 토큰으로 SecurityContext가 설정되지만,
     * 테스트에서는 직접 설정합니다.
     */
    private void mockSecurityContext(Long userId) {
        try {
            // SecurityContext에 직접 사용자 ID 설정
            // 주의: 이는 테스트용이므로 운영 환경에서는 사용하지 말 것!
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(
                    userId.toString(), null, "ROLE_USER"));

            log.debug("테스트용 SecurityContext 설정 완료: {}", userId);
        } catch (Exception e) {
            log.warn("테스트용 SecurityContext 설정 실패: {}", e.getMessage());
        }
    }
}

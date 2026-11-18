package app.bottlenote.user.dto.response;

import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.constant.UserType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민 유저 목록 응답 항목
 *
 * @param userId 유저 ID
 * @param email 이메일
 * @param nickName 닉네임
 * @param imageUrl 프로필 이미지
 * @param role 유저 권한
 * @param status 유저 상태
 * @param socialType 소셜 로그인 타입 목록
 * @param reviewCount 리뷰 수
 * @param ratingCount 별점 수
 * @param picksCount 찜 수
 * @param createAt 가입일
 * @param lastLoginAt 최종 로그인일
 */
public record AdminUserListResponse(
    Long userId,
    String email,
    String nickName,
    String imageUrl,
    UserType role,
    UserStatus status,
    List<SocialType> socialType,
    Long reviewCount,
    Long ratingCount,
    Long picksCount,
    LocalDateTime createAt,
    LocalDateTime lastLoginAt) {}

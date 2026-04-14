package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.request.AdminUserSearchRequest;
import app.bottlenote.user.dto.response.AdminUserListResponse;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;

public interface CustomUserRepository {

  MyPageResponse getMyPage(Long userId, Long currentUserId);

  PageResponse<MyBottleResponse> getReviewMyBottle(MyBottlePageableCriteria criteria);

  PageResponse<MyBottleResponse> getRatingMyBottle(MyBottlePageableCriteria criteria);

  PageResponse<MyBottleResponse> getPicksMyBottle(MyBottlePageableCriteria criteria);

  Page<AdminUserListResponse> searchAdminUsers(AdminUserSearchRequest request);

  /** QueryDSL 프로젝션용 중간 레코드 (socialType은 JSON 컬럼이라 별도 처리) */
  record AdminUserRow(
      Long userId,
      String email,
      String nickName,
      String imageUrl,
      UserType role,
      UserStatus status,
      Long reviewCount,
      Long ratingCount,
      Long picksCount,
      LocalDateTime createAt,
      LocalDateTime lastLoginAt) {}
}

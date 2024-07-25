package app.bottlenote.user.repository;

import app.bottlenote.user.dto.response.MyPageResponse;

public interface CustomUserRepository {

	MyPageResponse getMyPage(Long userId, Long currentUserId);

}

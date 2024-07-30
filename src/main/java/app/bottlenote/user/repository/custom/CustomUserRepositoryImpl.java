package app.bottlenote.user.repository.custom;

import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.repository.UserQuerySupporter;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static app.bottlenote.user.domain.QUser.user;

@Slf4j
@RequiredArgsConstructor
public class CustomUserRepositoryImpl implements CustomUserRepository {

	private final JPAQueryFactory queryFactory;
	private final UserQuerySupporter supporter;

	@Override
	public MyPageResponse getMyPage(Long userId, Long currentUserId) {

		return queryFactory
			.select(Projections.constructor(
				MyPageResponse.class,
				user.id.as("userId"),
				user.nickName.as("nickName"),
				user.imageUrl.as("userProfileImage"),
				supporter.reviewCountSubQuery(user.id),     // 마이 페이지 사용자의 리뷰 개수
				supporter.ratingCountSubQuery(user.id),     // 마이 페이지 사용자의 평점 개수
				supporter.picksCountSubQuery(user.id),      // 마이 페이지 사용자의 찜하기 개수
				supporter.followCountSubQuery(user.id),     // 마이 페이지 사용자가 팔로우 하는 유저 수
				supporter.followerCountSubQuery(user.id),   //  마이 페이지 사용자를 팔로우 하는 유저 수
				supporter.isFollowSubQuery(user.id, currentUserId), // 로그인 사용자가 마이 페이지 사용자를 팔로우 하고 있는지 여부
				supporter.isMyPageSubQuery(user.id, currentUserId) // 로그인 사용자가 마이 페이지 사용자인지 여부(나의 마이페이지인지 여부)
			))
			.from(user)
			.where(user.id.eq(userId))
			.fetchOne();

	}

}

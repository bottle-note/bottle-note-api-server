package app.bottlenote.user.fixture;

import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
@Component
public class UserTestFactory {

	private final Random random = new SecureRandom();

	@Autowired
	private EntityManager em;

	// ========== User 생성 메서드들 ==========

	/**
	 * 기본 User 생성
	 */
	@Transactional
	public User persistUser() {
		User user = User.builder()
				.email("user" + generateRandomSuffix() + "@example.com")
				.nickName("사용자-" + generateRandomSuffix())
				.age(25)
				.gender(GenderType.MALE)
				.socialType(List.of(SocialType.KAKAO))
				.role(UserType.ROLE_USER)
				.build();
		em.persist(user);
		return user;
	}

	/**
	 * 이메일과 닉네임으로 User 생성
	 */
	@Transactional
	public User persistUser(String email, String nickName) {
		User user = User.builder()
				.email(email + "-" + generateRandomSuffix() + "@example.com")
				.nickName(nickName + "-" + generateRandomSuffix())
				.age(25)
				.gender(GenderType.MALE)
				.socialType(List.of(SocialType.KAKAO))
				.role(UserType.ROLE_USER)
				.build();
		em.persist(user);
		em.flush();
		return user;
	}

	/**
	 * 빌더를 통한 User 생성 - 누락 필드 자동 채우기
	 */
	@Transactional
	public User persistUser(User.UserBuilder builder) {
		// 누락 필드 채우기
		User.UserBuilder filledBuilder = fillMissingUserFields(builder);
		User user = filledBuilder.build();
		em.persist(user);
		return user;
	}

	/**
	 * 빌더를 통한 User 생성 후 flush (즉시 ID 필요한 경우)
	 */
	@Transactional
	public User persistAndFlushUser(User.UserBuilder builder) {
		// 누락 필드 채우기
		User.UserBuilder filledBuilder = fillMissingUserFields(builder);
		User user = filledBuilder.build();
		em.persist(user);
		em.flush(); // 즉시 ID 필요한 경우에만 사용
		return user;
	}

	// ========== Follow 생성 메서드들 ==========

	/**
	 * 사용자 객체로 팔로우 관계 생성
	 */
	@Transactional
	public Follow persistFollow(User follower, User following) {
		Follow follow = Follow.builder()
				.status(FollowStatus.FOLLOWING)
				.userId(follower.getId())
				.targetUserId(following.getId())
				.build();
		em.persist(follow);
		em.flush();
		return follow;
	}

	/**
	 * 사용자 ID로 팔로우 관계 생성
	 */
	@Transactional
	public Follow persistFollow(Long followerId, Long followingId) {
		Follow follow = Follow.builder()
				.status(FollowStatus.FOLLOWING)
				.userId(followerId)
				.targetUserId(followingId)
				.build();
		em.persist(follow);
		return follow;
	}

	/**
	 * 빌더를 통한 Follow 생성 - 누락 필드 자동 채우기
	 */
	@Transactional
	public Follow persistFollow(Follow.FollowBuilder builder) {
		// 누락 필드 채우기
		Follow.FollowBuilder filledBuilder = fillMissingFollowFields(builder);
		Follow follow = filledBuilder.build();
		em.persist(follow);
		return follow;
	}

	// ========== 편의 메서드들 ==========

	/**
	 * 팔로워와 팔로잉 사용자를 모두 생성하고 팔로우 관계 설정
	 */
	@Transactional
	public Follow persistFollowWithUsers() {
		User follower = persistUserInternal("follower");
		User following = persistUserInternal("following");
		return persistFollow(follower, following);
	}

	/**
	 * 특정 사용자의 팔로워 N명 생성
	 */
	@Transactional
	public List<Follow> persistFollowers(User targetUser, int count) {
		return java.util.stream.IntStream.range(0, count)
				.mapToObj(i -> {
					User follower = persistUserInternal("follower" + i);
					return persistFollow(follower, targetUser);
				})
				.toList();
	}

	/**
	 * 특정 사용자가 팔로우하는 사용자 N명 생성
	 */
	@Transactional
	public List<Follow> persistFollowings(User follower, int count) {
		return java.util.stream.IntStream.range(0, count)
				.mapToObj(i -> {
					User following = persistUserInternal("following" + i);
					return persistFollow(follower, following);
				})
				.toList();
	}

	// ========== 헬퍼 메서드들 ==========

	/**
	 * 랜덤 접미사 생성 헬퍼 메서드
	 */
	private String generateRandomSuffix() {
		return String.valueOf(random.nextInt(10000));
	}

	/**
	 * 내부용 User 생성 (트랜잭션 전파 없음)
	 */
	private User persistUserInternal(String namePrefix) {
		User user = User.builder()
				.email(namePrefix + generateRandomSuffix() + "@example.com")
				.nickName(namePrefix + "-" + generateRandomSuffix())
				.age(25)
				.gender(GenderType.MALE)
				.socialType(List.of(SocialType.KAKAO))
				.role(UserType.ROLE_USER)
				.build();
		em.persist(user);
		return user;
	}

	/**
	 * User 빌더의 누락 필드 채우기
	 */
	private User.UserBuilder fillMissingUserFields(User.UserBuilder builder) {
		// 빌더를 임시로 빌드해서 필드 체크
		User tempUser;
		try {
			tempUser = builder.build();
		} catch (Exception e) {
			// 필수 필드 누락 시 기본값으로 채우기
			return builder
					.email("user" + generateRandomSuffix() + "@example.com")
					.nickName("사용자-" + generateRandomSuffix())
					.age(25)
					.gender(GenderType.MALE)
					.socialType(List.of(SocialType.KAKAO))
					.role(UserType.ROLE_USER);
		}

		// 개별 필드 체크 및 채우기
		if (tempUser.getEmail() == null || tempUser.getEmail().isEmpty()) {
			builder.email("user" + generateRandomSuffix() + "@example.com");
		}
		if (tempUser.getNickName() == null || tempUser.getNickName().isEmpty()) {
			builder.nickName("사용자-" + generateRandomSuffix());
		}
		if (tempUser.getAge() == null) {
			builder.age(25);
		}
		if (tempUser.getGender() == null) {
			builder.gender(GenderType.MALE);
		}
		if (tempUser.getSocialType() == null || tempUser.getSocialType().isEmpty()) {
			builder.socialType(List.of(SocialType.KAKAO));
		}
		if (tempUser.getRole() == null) {
			builder.role(UserType.ROLE_USER);
		}

		return builder;
	}

	/**
	 * Follow 빌더의 누락 필드 채우기
	 */
	private Follow.FollowBuilder fillMissingFollowFields(Follow.FollowBuilder builder) {
		// 빌더를 임시로 빌드해서 필드 체크
		Follow tempFollow;
		try {
			tempFollow = builder.build();
		} catch (Exception e) {
			// 필수 필드 누락 시 기본 사용자들 생성
			User follower = persistUserInternal("follower");
			User following = persistUserInternal("following");
			return builder
					.status(FollowStatus.FOLLOWING)
					.userId(follower.getId())
					.targetUserId(following.getId());
		}

		// 개별 필드 체크 및 채우기
		if (tempFollow.getStatus() == null) {
			builder.status(FollowStatus.FOLLOWING);
		}
		if (tempFollow.getUserId() == null) {
			User follower = persistUserInternal("follower");
			builder.userId(follower.getId());
		}
		if (tempFollow.getTargetUserId() == null) {
			User following = persistUserInternal("following");
			builder.targetUserId(following.getId());
		}

		return builder;
	}

}

package app.bottlenote.user.fixture;

import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserFacade;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FakeUserFacade implements UserFacade {

	private static final Logger log = LogManager.getLogger(FakeUserFacade.class);
	
	private final Map<Long, UserProfileInfo> userDatabase = new ConcurrentHashMap<>();

	public FakeUserFacade() {
		// Initialize with some default data
		userDatabase.put(1L, new UserProfileInfo(
			1L,
			"Kim",
			"https://bottlenote.app/user/1/image"
		));
		userDatabase.put(2L, new UserProfileInfo(
			2L,
			"Cha",
			"https://bottlenote.app/user/2/image"
		));
		userDatabase.put(3L, new UserProfileInfo(
			3L,
			"Park",
			"https://bottlenote.app/user/3/image"
		));
	}

	/**
	 * 유틸리티 메서드: 테스트 목적의 유저 데이터를 추가합니다.
	 *
	 * @param userProfileInfo 추가할 유저 정보
	 */
	public void addUser(UserProfileInfo userProfileInfo) {
		Objects.requireNonNull(userProfileInfo, "UserProfileInfo cannot be null");
		userDatabase.put(userProfileInfo.id(), userProfileInfo);
		log.debug("Added user: {}", userProfileInfo);
	}

	/**
	 * 유틸리티 메서드: ID로 유저 데이터를 제거합니다.
	 *
	 * @param userId 제거할 유저 ID
	 */
	public void removeUserById(Long userId) {
		userDatabase.remove(userId);
		log.debug("Removed user with ID: {}", userId);
	}

	/**
	 * 유틸리티 메서드: 모든 유저 데이터를 초기화합니다.
	 */
	public void clearUserDatabase() {
		userDatabase.clear();
		log.debug("Cleared all user data");
	}

	@Override
	public Boolean existsByUserId(Long userId) {
		boolean exists = userDatabase.containsKey(userId);
		log.debug("Exists check for User ID {}: {}", userId, exists);
		return exists;
	}

	@Override
	public void isValidUserId(Long userId) {
		if (!existsByUserId(userId)) {
			log.error("User ID {} not found", userId);
			throw new UserException(UserExceptionCode.USER_NOT_FOUND);
		}
	}

	@Override
	public UserProfileInfo getUserProfileInfo(Long userId) {
		UserProfileInfo userProfileInfo = userDatabase.get(userId);
		if (userProfileInfo != null) {
			log.debug("Found UserProfileInfo for ID {}: {}", userId, userProfileInfo);
			return userProfileInfo;
		}
		throw new UserException(UserExceptionCode.USER_NOT_FOUND);
	}
}
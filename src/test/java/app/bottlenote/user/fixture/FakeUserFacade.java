package app.bottlenote.user.fixture;

import app.bottlenote.user.dto.response.UserProfileItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserFacade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class FakeUserFacade implements UserFacade {

	private static final Logger log = LogManager.getLogger(FakeUserFacade.class);

	private final Map<Long, UserProfileItem> userDatabase = new ConcurrentHashMap<>();

	public FakeUserFacade(UserProfileItem... userProfileItems) {
		if (userProfileItems != null && userProfileItems.length > 0) {
			for (UserProfileItem user : userProfileItems) {
				if (user != null) {
					Long userId = user.id();
					if (userId == null) {
						userId = (long) (userDatabase.size() + 1);
						ReflectionTestUtils.setField(user, "id", userId);
						log.info("Assigned new ID {} to UserProfileItem: {}", userId, user);
					}
					userDatabase.put(userId, user);
					log.info("Added UserProfileItem: {}", user);
				} else {
					log.warn("Null UserProfileItem encountered and skipped.");
				}
			}
		} else {
			log.warn("No UserProfileItem provided to initialize dataSource.");
		}
	}

	/**
	 * 유틸리티 메서드: 테스트 목적의 유저 데이터를 추가합니다.
	 *
	 * @param userProfileItem 추가할 유저 정보
	 */
	public void addUser(UserProfileItem userProfileItem) {
		Objects.requireNonNull(userProfileItem, "UserProfileItem cannot be null");
		userDatabase.put(userProfileItem.id(), userProfileItem);
		log.debug("Added user: {}", userProfileItem);
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
	public UserProfileItem getUserProfileInfo(Long userId) {
		UserProfileItem userProfileItem = userDatabase.get(userId);
		if (userProfileItem != null) {
			log.debug("Found UserProfileItem for ID {}: {}", userId, userProfileItem);
			return userProfileItem;
		}
		throw new UserException(UserExceptionCode.USER_NOT_FOUND);
	}
}

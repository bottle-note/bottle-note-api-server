package app.bottlenote.user.service.domain;

/**
 * 유저 도메인 서포트를 위한 인터페이스입니다.
 * 이 인터페이스는 유저 식별자를 기반으로 유저 데이터를 검증하는 메소드를 제공합니다.
 */
public interface UserDomainSupport {

	/**
	 * 주어진 유저 이름를 가진 유저의 수를 반환합니다.
	 *
	 * @param userName 유저 식별자
	 * @return 유저의 수
	 */
	Long countByUsername(String userName);

	/**
	 * 주어진 유저 식별자를 가진 유저가 존재하는지 확인합니다.
	 *
	 * @param userId 유저 식별자
	 * @return 유저가 존재하면 true, 그렇지 않으면 false
	 */
	Boolean existsByUserId(Long userId);

	/**
	 * 주어진 유저 식별자가 사용 가능한 유효한 유저인지 검증합니다.
	 * <p>
	 * 유효하지 않는 경우 Exception을 발생시킵니다.
	 *
	 * @param userId 유저 식별자
	 * @return 유효한 유저 식별자
	 */
	void isValidUserId(Long userId);
}

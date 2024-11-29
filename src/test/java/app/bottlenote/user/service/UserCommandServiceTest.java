package app.bottlenote.user.service;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.domain.constant.UserStatus;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.fixture.UserObjectFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NICKNAME_NOT_VALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("[unit] [service] UserCommandService")
@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

	@InjectMocks
	UserBasicService userCommandService;

	@Mock
	UserRepository userCommandRepository;

	@Test
	@DisplayName("닉네임을 변경할수 있다.")
	void testChangeNickname() {

		Long userId = 1L;
		User user = UserObjectFixture.getUserFixtureObject();

		String newNickname = "newNickname";
		String beforeNickname = user.getNickName();


		NicknameChangeRequest request = new NicknameChangeRequest(newNickname);

		// when
		when(userCommandRepository.existsByNickName(newNickname)).thenReturn(false);
		when(userCommandRepository.findById(any())).thenReturn(Optional.of(user));


		NicknameChangeResponse response = userCommandService.nicknameChange(userId, request);

		// then
		assertEquals(response.getMessage(), NicknameChangeResponse.Message.SUCCESS.getResponseName());
		assertEquals(response.getUserId(), userId);
		assertEquals(response.getBeforeNickname(), beforeNickname);
		assertEquals(response.getChangedNickname(), newNickname);

	}

	@DisplayName("중복된 닉네임은 변경할 수 없다.")
	@Test
	void testChangeNicknameWithDuplicateNickname() {

		Long userId = 1L;

		// given
		String newNickname = "newNickname";
		NicknameChangeRequest request = new NicknameChangeRequest(newNickname);

		// when
		when(userCommandRepository.existsByNickName(newNickname)).thenReturn(true);
		UserException aThrows = assertThrows(UserException.class, () -> userCommandService.nicknameChange(userId, request));

		// then
		assertEquals(USER_NICKNAME_NOT_VALID, aThrows.getExceptionCode());
	}

	@DisplayName("회원 탈퇴를 할 수 있다.")
	@Test
	void testWithdrawUserSuccess() {
		// given
		Long userId = 1L;
		User user = UserObjectFixture.getUserFixtureObject();

		// when
		when(userCommandRepository.findById(anyLong()))
			.thenReturn(Optional.of(user));

		userCommandService.withdrawUser(userId);

		// then
		assertEquals(UserStatus.DELETED, user.getStatus());
	}

	@DisplayName("존재하지 않는 회원은 탈퇴할 수 없다.")
	@Test
	void testWithdrawUserFailedWhenUserNotExist() {
		// given
		Long userId = 1L;

		// when
		when(userCommandRepository.findById(anyLong()))
			.thenReturn(Optional.empty());

		// then
		assertThrows(UserException.class, () -> userCommandService.withdrawUser(userId));
	}

}

package app.bottlenote.user.service;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

	@InjectMocks
	UserCommandService userCommandService;

	@Mock
	UserCommandRepository userCommandRepository;

	@Test
	@DisplayName("닉네임을 변경할수 있다.")
	void testChangeNickname() {

		String newNickname = "newNickname";
		String beforeNickname = "beforeNickname";

		// given
		User user = User.builder()
			.id(1L)
			.nickName(beforeNickname)
			.build();
		NicknameChangeRequest request = new NicknameChangeRequest(1L, newNickname);

		// when
		when(userCommandRepository.existsByNickName(newNickname)).thenReturn(false);
		when(userCommandRepository.findById(any())).thenReturn(Optional.of(user));


		NicknameChangeResponse response = userCommandService.nicknameChange(request);

		// then
		assertEquals(response.getMessage(), NicknameChangeResponse.Message.SUCCESS.getMessage());
		assertEquals(response.getUserId(), 1L);
		assertEquals(response.getBeforeNickname(), beforeNickname);
		assertEquals(response.getChangedNickname(), newNickname);

	}

	@DisplayName("중복된 닉네임은 변경할 수 없다.")
	@Test
	void testChangeNicknameWithDuplicateNickname() {

		// given
		String newNickname = "newNickname";
		NicknameChangeRequest request = new NicknameChangeRequest(1L, newNickname);

		// when
		when(userCommandRepository.existsByNickName(newNickname)).thenReturn(true);
		UserException aThrows = assertThrows(UserException.class, () -> userCommandService.nicknameChange(request));

		// then
		assertEquals(aThrows.getExceptionCode(), USER_NICKNAME_NOT_VALID);

	}

}

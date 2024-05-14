package app.bottlenote.user.service;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserCommandServiceTest {

	@InjectMocks
	UserCommandService userCommandService;

	@Mock
	UserCommandRepository userCommandRepository;

	@Test
	@DisplayName("닉네임을 변경할수 있다.")
	public void testChangeNickname() {
		// given
		User user = User.builder()
			.id(1L)
			.nickName("beforeNickname")
			.build();

		// when
		when(userCommandRepository.findById(any(Long.class))).thenReturn(Optional.of(user));

		// then
		NicknameChangeRequest request = new NicknameChangeRequest(1L, "newNickname");

	}

	@DisplayName("중복된 닉네임은 변경할 수 없다.")
	@Test
	public void testChangeNicknameWithDuplicateNickname() {
		// given
		User user = User.builder()
			.id(1L)
			.email("newNickname")
			.nickName("newNickname")
			.build();

		NicknameChangeRequest request = new NicknameChangeRequest(2L, "newNickname");

		// when
		when(userCommandRepository.existsByNickName("newNickname")).thenReturn(true);

		// then
		assertThrows(UserException.class, () -> userCommandService.nicknameChange(request), "이미 존재하는 사용자입니다.");
	}

}

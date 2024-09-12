package app.bottlenote.support.help.service;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpUpsertResponse;
import app.bottlenote.support.help.fixture.HelpObjectFixture;
import app.bottlenote.support.help.repository.HelpRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.domain.UserDomainSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("[unit] [service] HelpService")
@ExtendWith(MockitoExtension.class)
class HelpServiceTest {

	@InjectMocks
	private HelpService helpService;

	@Mock
	private HelpRepository helpRepository;

	@Mock
	private UserDomainSupport userDomainSupport;

	private final HelpUpsertRequest helpUpsertRequest = HelpObjectFixture.getHelpUpsertRequest();
	private final Help help = HelpObjectFixture.getHelpDefaultFixture();

	@DisplayName("회원은 문의글을 작성할 수 있다.")
	@Test
	void testUserRegisterHelp_Success() {

		//when
		doNothing().when(userDomainSupport).isValidUserId(anyLong());
		when(helpRepository.save(any(Help.class))).thenReturn(help);
		HelpUpsertResponse helpUpsertResponse = helpService.registerHelp(helpUpsertRequest, 1L);

		// then
		assertEquals(REGISTER_SUCCESS, helpUpsertResponse.codeMessage());
	}

	@DisplayName("로그인 하지 않은 유저는 문의글을 작성할 수 없다.")
	@Test
	void testHelpRegister_fail_when_unauthorized_user() {

		// when
		doThrow(UserException.class).when(userDomainSupport).isValidUserId(anyLong());

		// then
		assertThrows(UserException.class, () -> helpService.registerHelp(helpUpsertRequest, 1L));
	}

	@DisplayName("문의글을 수정할 수 있다.")
	@Test
	void testHelpUpdate_success() {
	    // given
		HelpUpsertRequest updateRequest = new HelpUpsertRequest("수정 후 제목","수정 후 내용", HelpType.USER);

	    // when
		when(helpRepository.findByIdAndUserId(anyLong(), anyLong()))
			.thenReturn(Optional.of(help));

		HelpUpsertResponse response = helpService.modifyHelp(updateRequest, 1L, 1L);

		// then
		assertEquals(MODIFY_SUCCESS, response.codeMessage());
	}

	@DisplayName("유저는 본인이 작성한 문의글만 수정할 수 있다.")
	@Test
	void testHelpUpdate_fail_when_user_is_not_owner() {
	    // given

	    // when
		when(helpRepository.findByIdAndUserId(anyLong(), anyLong()))
			.thenThrow(UserException.class);

	    // then
		assertThrows(UserException.class,
			() -> helpService.modifyHelp(helpUpsertRequest, 1L, 1L));
	}

	@DisplayName("수정 요청에 null인 필드는 허용하지 않는다.")
	@Test
	void testHelpUpdate_fail_when_request_contains_null() {

		assertThrows(NullPointerException.class,
			() -> help.updateHelp("title",null,HelpType.USER ));
	}
}

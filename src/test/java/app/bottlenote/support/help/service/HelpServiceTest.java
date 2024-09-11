package app.bottlenote.support.help.service;

import app.bottlenote.support.help.domain.Help;
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

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

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

	@DisplayName("회원은 문의글을 작성할 수 있다.")
	@Test
	void testUserRegisterHelp_Success() {
		// given

		HelpUpsertRequest helpUpsertRequest = HelpObjectFixture.getHelpRegisterRequest();

		Help help = Help.create(1L,
			helpUpsertRequest.title(),
			helpUpsertRequest.content(),
			helpUpsertRequest.type()
		);

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

		// given
		HelpUpsertRequest helpUpsertRequest = HelpObjectFixture.getHelpRegisterRequest();

		// when
		doThrow(UserException.class).when(userDomainSupport).isValidUserId(anyLong());

		// then
		assertThrows(UserException.class, () -> helpService.registerHelp(helpUpsertRequest, 1L));
	}


}

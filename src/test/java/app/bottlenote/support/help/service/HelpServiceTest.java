package app.bottlenote.support.help.service;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_AUTHORIZED;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpImageInfo;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpDetailInfo;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.exception.HelpException;
import app.bottlenote.support.help.fixture.HelpObjectFixture;
import app.bottlenote.support.help.repository.HelpRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.domain.UserDomainSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
	private final PageResponse<HelpListResponse> helpPageResponse = HelpObjectFixture.getHelpListPageResponse();
	private final HelpPageableRequest emptyPageableRequest = HelpObjectFixture.getEmptyHelpPageableRequest();
	private final Help help = HelpObjectFixture.getHelpDefaultFixture();

	@DisplayName("회원은 문의글을 작성할 수 있다.")
	@Test
	void testUserRegisterHelp_Success() {

		//when
		doNothing().when(userDomainSupport).isValidUserId(anyLong());
		when(helpRepository.save(any(Help.class))).thenReturn(help);
		HelpResultResponse helpResultResponse = helpService.registerHelp(helpUpsertRequest, 1L);

		// then
		assertEquals(REGISTER_SUCCESS, helpResultResponse.codeMessage());
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
		HelpUpsertRequest updateRequest = new HelpUpsertRequest("수정 후 제목", HelpType.USER, List.of(new HelpImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")));

	    // when
		when(helpRepository.findById(anyLong()))
			.thenReturn(Optional.of(help));

		HelpResultResponse response = helpService.modifyHelp(updateRequest, 1L, 1L);

		// then
		assertEquals(MODIFY_SUCCESS, response.codeMessage());
	}

	@DisplayName("유저는 본인이 작성한 문의글만 수정할 수 있다.")
	@Test
	void testHelpUpdate_fail_when_user_is_not_owner() {

	    // when
		when(helpRepository.findById(anyLong()))
			.thenThrow(HelpException.class);

	    // then
		assertThrows(HelpException.class,
			() -> helpService.modifyHelp(helpUpsertRequest, 1L, 1L));
	}

	@DisplayName("수정 요청에 null인 필드는 허용하지 않는다.")
	@Test
	void testHelpUpdate_fail_when_request_contains_null() {

		assertThrows(NullPointerException.class,
			() -> help.updateHelp("title", List.of(), null));
	}

	@DisplayName("문의글을 삭제할 수 있다.")
	@Test
	void testHelpDelete_success() {
	    //when
		when(helpRepository.findById(anyLong()))
			.thenReturn(Optional.of(help));

		HelpResultResponse helpResultResponse = helpService.deleteHelp(1L, 1L);

		// then
		assertEquals(DELETE_SUCCESS, helpResultResponse.codeMessage());
	}

	@DisplayName("유저는 존재하는 문의글만 삭제할 수 있다.")
	@Test
	void testHelpDelete_fail_when_help_is_not_exist() {
		//when
		when(helpRepository.findById(anyLong()))
			.thenThrow(new HelpException(HELP_NOT_FOUND));

		// then
		assertThrows(HelpException.class,
			() -> helpService.deleteHelp(1L, 1L));
	}

	@DisplayName("유저는 본인이 작성한 문의글만 삭제할 수 있다.")
	@Test
	void testHelpDelete_fail_when_user_is_not_owner() {
		//when
		when(helpRepository.findById(anyLong()))
			.thenThrow(new HelpException(HELP_NOT_AUTHORIZED));

		// then
		assertThrows(HelpException.class,
			() -> helpService.deleteHelp(1L, 1L));
	}

	@DisplayName("문의글 작성 목록을 조회할 수 있다.")
	@Test
	void testGetHelpList_success() {
	    // given
		when(helpRepository.getHelpList(any(HelpPageableRequest.class), anyLong()))
			.thenReturn(helpPageResponse);

	    // when
		PageResponse<HelpListResponse> helpList = helpService.getHelpList(emptyPageableRequest, 1L);

		// then
		assertEquals(helpPageResponse.content(), helpList.content());
	}


	@DisplayName("문의글을 상세 조회할 수 있다.")
	@Test
	void testGetDetailHelp_success() {
	    // given
	    when(helpRepository.findByIdAndUserId(anyLong(), anyLong()))
			.thenReturn(Optional.of(help));

	    // when
		HelpDetailInfo detailHelp = helpService.getDetailHelp(1L, 1L);

		// then
		assertEquals(detailHelp.content(), help.getContent());
	}

	@DisplayName("유저는 자신이 작성한 문의글만 조회할 수 있다.")
	@Test
	void testGetHelp_fail_when_user_is_not_owner() {
		// when
		when(helpRepository.findByIdAndUserId(anyLong(), anyLong()))
			.thenThrow(new HelpException(HELP_NOT_FOUND));

	    // then
		assertThrows(HelpException.class,
			() -> helpService.getDetailHelp(1L, 1L));
	}

}

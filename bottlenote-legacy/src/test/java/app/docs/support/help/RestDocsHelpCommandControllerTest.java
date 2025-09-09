package app.docs.support.help;

import static app.bottlenote.support.help.constant.HelpResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.help.constant.HelpResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.help.constant.HelpResultMessage.REGISTER_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.controller.HelpCommandController;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpDetailItem;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.fixture.HelpObjectFixture;
import app.bottlenote.support.help.service.HelpService;
import app.docs.AbstractRestDocs;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

@DisplayName("문의글 커맨드 컨트롤러 RestDocs용 테스트")
class RestDocsHelpCommandControllerTest extends AbstractRestDocs {

  private final HelpService helpService = mock(HelpService.class);

  private final MockedStatic<SecurityContextUtil> mockedSecurityUtil =
      mockStatic(SecurityContextUtil.class);

  private final PageResponse<HelpListResponse> helpPageResponse =
      HelpObjectFixture.getHelpListPageResponse();
  private final HelpDetailItem helpDetailItem =
      HelpObjectFixture.getDetailHelpInfo("content", HelpType.USER);
  private final HelpUpsertRequest helpUpsertRequest = HelpObjectFixture.getHelpUpsertRequest();
  private final HelpResultResponse successRegisterResponse =
      HelpObjectFixture.getSuccessHelpResponse(REGISTER_SUCCESS);
  private final HelpResultResponse successModifyResponse =
      HelpObjectFixture.getSuccessHelpResponse(MODIFY_SUCCESS);
  private final HelpResultResponse successDeleteResponse =
      HelpObjectFixture.getSuccessHelpResponse(DELETE_SUCCESS);

  @Override
  protected Object initController() {
    return new HelpCommandController(helpService);
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtil.close();
  }

  @Test
  @DisplayName("문의글을 작성할 수 있다.")
  void help_register_test() throws Exception {

    Long userId = 1L;

    // when
    when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

    when(helpService.registerHelp(any(HelpUpsertRequest.class), anyLong()))
        .thenReturn(successRegisterResponse);

    // then
    mockMvc
        .perform(
            post("/api/v1/help")
                .content(objectMapper.writeValueAsString(helpUpsertRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "support/help/help-register",
                requestFields(
                    fieldWithPath("title").type(JsonFieldType.STRING).description("문의글 제목"),
                    fieldWithPath("content").type(JsonFieldType.STRING).description("문의글 내용"),
                    fieldWithPath("type")
                        .type(JsonFieldType.STRING)
                        .description("문의글 타입  (WHISKEY, REVIEW, USER, ETC)"),
                    fieldWithPath("imageUrlList")
                        .type(JsonFieldType.ARRAY)
                        .description("이미지 URL 목록"),
                    fieldWithPath("imageUrlList[].order")
                        .type(JsonFieldType.NUMBER)
                        .description("이미지 순서"),
                    fieldWithPath("imageUrlList[].viewUrl")
                        .type(JsonFieldType.STRING)
                        .description("이미지 URL")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
                    fieldWithPath("data.message").description("성공 메시지"),
                    fieldWithPath("data.helpId").description("문의글 아이디"),
                    fieldWithPath("data.responseAt").description("서버 응답 일시"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  @Test
  @DisplayName("문의글을 목록을 조회할 수 있다.")
  void help_read_list_test() throws Exception {

    Long userId = 1L;

    // when
    when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

    when(helpService.getHelpList(any(HelpPageableRequest.class), anyLong()))
        .thenReturn(helpPageResponse);

    // then
    mockMvc
        .perform(
            get("/api/v1/help")
                .contentType(MediaType.APPLICATION_JSON)
                .param("cursor", "0")
                .param("pageSize", "2"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "support/help/help-read-list",
                queryParameters(
                    parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
                    parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.totalCount").description("성공 메시지 코드"),
                    fieldWithPath("data.helpList[].helpId").description("문의글 ID"),
                    fieldWithPath("data.helpList[].title").description("문의글 제목"),
                    fieldWithPath("data.helpList[].content").description("문의글 내용"),
                    fieldWithPath("data.helpList[].createAt").description("문의글 등록일시"),
                    fieldWithPath("data.helpList[].helpStatus").description("문의글 처리상태"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored(),
                    fieldWithPath("meta.pageable").description("페이징 정보"),
                    fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
                    fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
                    fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
                    fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부"))));
  }

  @Test
  @DisplayName("문의글을 상세 조회할 수 있다.")
  void help_read_detail_test() throws Exception {

    Long userId = 1L;

    // when
    when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

    when(helpService.getDetailHelp(anyLong(), anyLong())).thenReturn(helpDetailItem);

    // then
    mockMvc
        .perform(get("/api/v1/help/{helpId}", 1L).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "support/help/help-read-detail",
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.helpId").description("문의글 ID"),
                    fieldWithPath("data.title").description("문의글 제목"),
                    fieldWithPath("data.content").description("문의글 내용"),
                    fieldWithPath("data.helpType").description("문의글 타입"),
                    fieldWithPath("data.imageUrlList[].order")
                        .type(JsonFieldType.NUMBER)
                        .description("이미지 순서"),
                    fieldWithPath("data.imageUrlList[].viewUrl")
                        .type(JsonFieldType.STRING)
                        .description("이미지 URL"),
                    fieldWithPath("data.createAt").description("문의글 등록일시"),
                    fieldWithPath("data.adminId").description("시스템 관리자 ID").optional(),
                    fieldWithPath("data.responseContent").description("시스템 관리자 답변").optional(),
                    fieldWithPath("data.lastModifyAt").description("최종 수정 일자"),
                    fieldWithPath("data.statusType").description("문의글 상태"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  @Test
  @DisplayName("문의글을 수정할 수 있다.")
  void help_modify_test() throws Exception {

    Long userId = 1L;
    Long helpId = 1L;

    // when
    when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

    when(helpService.modifyHelp(any(HelpUpsertRequest.class), anyLong(), anyLong()))
        .thenReturn(successModifyResponse);

    // then
    mockMvc
        .perform(
            patch("/api/v1/help/{helpId}", helpId)
                .content(objectMapper.writeValueAsString(helpUpsertRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "support/help/help-update",
                requestFields(
                    fieldWithPath("title").type(JsonFieldType.STRING).description("문의글 제목"),
                    fieldWithPath("content").type(JsonFieldType.STRING).description("문의글 내용"),
                    fieldWithPath("type")
                        .type(JsonFieldType.STRING)
                        .description("문의글 타입  (WHISKEY, REVIEW, USER, ETC)"),
                    fieldWithPath("imageUrlList")
                        .type(JsonFieldType.ARRAY)
                        .description("이미지 URL 목록"),
                    fieldWithPath("imageUrlList[].order")
                        .type(JsonFieldType.NUMBER)
                        .description("이미지 순서"),
                    fieldWithPath("imageUrlList[].viewUrl")
                        .type(JsonFieldType.STRING)
                        .description("이미지 URL")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
                    fieldWithPath("data.message").description("성공 메시지"),
                    fieldWithPath("data.helpId").description("문의글 아이디"),
                    fieldWithPath("data.responseAt").description("서버 응답 일시"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  @Test
  @DisplayName("문의글을 삭제할 수 있다.")
  void help_delete_test() throws Exception {

    Long userId = 1L;
    Long helpId = 1L;

    // when
    when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

    when(helpService.deleteHelp(anyLong(), anyLong())).thenReturn(successDeleteResponse);

    // then
    mockMvc
        .perform(
            delete("/api/v1/help/{helpId}", helpId)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "support/help/help-delete",
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
                    fieldWithPath("data.message").description("성공 메시지"),
                    fieldWithPath("data.helpId").description("문의글 아이디"),
                    fieldWithPath("data.responseAt").description("서버 응답 일시"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }
}

package app.docs.support.block;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.block.controller.BlockController;
import app.bottlenote.support.block.dto.request.BlockCreateRequest;
import app.bottlenote.support.block.dto.response.UserBlockItem;
import app.bottlenote.support.block.service.BlockService;
import app.docs.AbstractRestDocs;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

@DisplayName("BlockController RestDocs 테스트")
class RestDocsBlockControllerTest extends AbstractRestDocs {

  private final BlockService blockService = mock(BlockService.class);
  private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

  @Override
  protected Object initController() {
    return new BlockController(blockService);
  }

  @BeforeEach
  void setup() {
    mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
    mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtil.close();
  }

  @DisplayName("[restdocs] 사용자를 차단할 수 있다")
  @Test
  void createBlockTest() throws Exception {
    // given
    Long currentUserId = 1L;
    BlockCreateRequest request = new BlockCreateRequest(2L);
    CollectionResponse<UserBlockItem> response =
        CollectionResponse.of(1L, List.of(new UserBlockItem(2L, "차단된사용자", LocalDateTime.now())));

    // when
    doNothing().when(blockService).blockUser(currentUserId, 2L);
    when(blockService.getBlockedUserItems(currentUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(
            post("/api/v1/blocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "block-create",
                requestFields(
                    fieldWithPath("blockedUserId")
                        .type(JsonFieldType.NUMBER)
                        .description("차단할 사용자 ID")),
                responseFields(
                    fieldWithPath("success")
                        .type(JsonFieldType.BOOLEAN)
                        .description("성공 여부")
                        .ignored(),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드").ignored(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록")
                        .ignored(),
                    fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
                    fieldWithPath("meta.serverVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 버전")
                        .ignored(),
                    fieldWithPath("meta.serverEncoding")
                        .type(JsonFieldType.STRING)
                        .description("서버 인코딩")
                        .ignored(),
                    fieldWithPath("meta.serverResponseTime")
                        .type(JsonFieldType.ARRAY)
                        .description("서버 응답 시간")
                        .ignored(),
                    fieldWithPath("meta.serverPathVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 경로 버전")
                        .ignored(),
                    fieldWithPath("data.totalCount")
                        .type(JsonFieldType.NUMBER)
                        .description("차단된 사용자 총 수"),
                    fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("차단된 사용자 목록"),
                    fieldWithPath("data.items[].userId")
                        .type(JsonFieldType.NUMBER)
                        .description("차단된 사용자 ID"),
                    fieldWithPath("data.items[].userName")
                        .type(JsonFieldType.STRING)
                        .description("차단된 사용자 이름"),
                    fieldWithPath("data.items[].blockedAt")
                        .type(JsonFieldType.ARRAY)
                        .description("차단된 시각"))));
  }

  @DisplayName("[restdocs] 사용자 차단을 해제할 수 있다")
  @Test
  void deleteBlockTest() throws Exception {
    // given
    Long currentUserId = 1L;
    Long blockedUserId = 2L;
    CollectionResponse<UserBlockItem> response = CollectionResponse.of(0L, List.of());

    // when
    doNothing().when(blockService).unblockUser(currentUserId, blockedUserId);
    when(blockService.getBlockedUserItems(currentUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(delete("/api/v1/blocks/{blockedUserId}", blockedUserId))
        .andExpect(status().isOk())
        .andDo(
            document(
                "block-delete",
                pathParameters(parameterWithName("blockedUserId").description("차단 해제할 사용자 ID")),
                responseFields(
                    fieldWithPath("success")
                        .type(JsonFieldType.BOOLEAN)
                        .description("성공 여부")
                        .ignored(),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드").ignored(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록")
                        .ignored(),
                    fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
                    fieldWithPath("meta.serverVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 버전")
                        .ignored(),
                    fieldWithPath("meta.serverEncoding")
                        .type(JsonFieldType.STRING)
                        .description("서버 인코딩")
                        .ignored(),
                    fieldWithPath("meta.serverResponseTime")
                        .type(JsonFieldType.ARRAY)
                        .description("서버 응답 시간")
                        .ignored(),
                    fieldWithPath("meta.serverPathVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 경로 버전")
                        .ignored(),
                    fieldWithPath("data.totalCount")
                        .type(JsonFieldType.NUMBER)
                        .description("차단된 사용자 총 수"),
                    fieldWithPath("data.items")
                        .type(JsonFieldType.ARRAY)
                        .description("차단된 사용자 목록"))));
  }

  @DisplayName("[restdocs] 차단된 사용자 목록을 조회할 수 있다")
  @Test
  void getBlockedUsersTest() throws Exception {
    // given
    Long currentUserId = 1L;
    CollectionResponse<UserBlockItem> response =
        CollectionResponse.of(
            2L,
            List.of(
                new UserBlockItem(2L, "차단된사용자1", LocalDateTime.now()),
                new UserBlockItem(3L, "차단된사용자2", LocalDateTime.now())));

    // when
    when(blockService.getBlockedUserItems(currentUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(get("/api/v1/blocks"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "block-list",
                responseFields(
                    fieldWithPath("success")
                        .type(JsonFieldType.BOOLEAN)
                        .description("성공 여부")
                        .ignored(),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드").ignored(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록")
                        .ignored(),
                    fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
                    fieldWithPath("meta.serverVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 버전")
                        .ignored(),
                    fieldWithPath("meta.serverEncoding")
                        .type(JsonFieldType.STRING)
                        .description("서버 인코딩")
                        .ignored(),
                    fieldWithPath("meta.serverResponseTime")
                        .type(JsonFieldType.ARRAY)
                        .description("서버 응답 시간")
                        .ignored(),
                    fieldWithPath("meta.serverPathVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 경로 버전")
                        .ignored(),
                    fieldWithPath("data.totalCount")
                        .type(JsonFieldType.NUMBER)
                        .description("차단된 사용자 총 수"),
                    fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("차단된 사용자 목록"),
                    fieldWithPath("data.items[].userId")
                        .type(JsonFieldType.NUMBER)
                        .description("차단된 사용자 ID"),
                    fieldWithPath("data.items[].userName")
                        .type(JsonFieldType.STRING)
                        .description("차단된 사용자 이름"),
                    fieldWithPath("data.items[].blockedAt")
                        .type(JsonFieldType.ARRAY)
                        .description("차단된 시각"))));
  }

  @DisplayName("[restdocs] 차단된 사용자 ID 목록을 조회할 수 있다")
  @Test
  void getBlockedUserIdsTest() throws Exception {
    // given
    Long currentUserId = 1L;
    Set<Long> response = Set.of(2L, 3L);

    // when
    when(blockService.getBlockedUserIds(currentUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(get("/api/v1/blocks/ids"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "block-ids",
                responseFields(
                    fieldWithPath("success")
                        .type(JsonFieldType.BOOLEAN)
                        .description("성공 여부")
                        .ignored(),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드").ignored(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록")
                        .ignored(),
                    fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
                    fieldWithPath("meta.serverVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 버전")
                        .ignored(),
                    fieldWithPath("meta.serverEncoding")
                        .type(JsonFieldType.STRING)
                        .description("서버 인코딩")
                        .ignored(),
                    fieldWithPath("meta.serverResponseTime")
                        .type(JsonFieldType.ARRAY)
                        .description("서버 응답 시간")
                        .ignored(),
                    fieldWithPath("meta.serverPathVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 경로 버전")
                        .ignored(),
                    fieldWithPath("data").type(JsonFieldType.ARRAY).description("차단된 사용자 ID 목록"))));
  }

  @DisplayName("[restdocs] 특정 사용자 차단 여부를 확인할 수 있다")
  @Test
  void checkBlockedTest() throws Exception {
    // given
    Long currentUserId = 1L;
    Long targetUserId = 2L;
    Boolean response = true;

    // when
    when(blockService.isBlocked(currentUserId, targetUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(get("/api/v1/blocks/check/{targetUserId}", targetUserId))
        .andExpect(status().isOk())
        .andDo(
            document(
                "block-check",
                pathParameters(parameterWithName("targetUserId").description("차단 여부를 확인할 사용자 ID")),
                responseFields(
                    fieldWithPath("success")
                        .type(JsonFieldType.BOOLEAN)
                        .description("성공 여부")
                        .ignored(),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드").ignored(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록")
                        .ignored(),
                    fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
                    fieldWithPath("meta.serverVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 버전")
                        .ignored(),
                    fieldWithPath("meta.serverEncoding")
                        .type(JsonFieldType.STRING)
                        .description("서버 인코딩")
                        .ignored(),
                    fieldWithPath("meta.serverResponseTime")
                        .type(JsonFieldType.ARRAY)
                        .description("서버 응답 시간")
                        .ignored(),
                    fieldWithPath("meta.serverPathVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 경로 버전")
                        .ignored(),
                    fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("차단 여부"))));
  }

  @DisplayName("[restdocs] 상호 차단 여부를 확인할 수 있다")
  @Test
  void checkMutualBlockedTest() throws Exception {
    // given
    Long currentUserId = 1L;
    Long targetUserId = 2L;
    Boolean response = false;

    // when
    when(blockService.isMutualBlocked(currentUserId, targetUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(get("/api/v1/blocks/mutual-check/{targetUserId}", targetUserId))
        .andExpect(status().isOk())
        .andDo(
            document(
                "block-mutual-check",
                pathParameters(
                    parameterWithName("targetUserId").description("상호 차단 여부를 확인할 사용자 ID")),
                responseFields(
                    fieldWithPath("success")
                        .type(JsonFieldType.BOOLEAN)
                        .description("성공 여부")
                        .ignored(),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드").ignored(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록")
                        .ignored(),
                    fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
                    fieldWithPath("meta.serverVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 버전")
                        .ignored(),
                    fieldWithPath("meta.serverEncoding")
                        .type(JsonFieldType.STRING)
                        .description("서버 인코딩")
                        .ignored(),
                    fieldWithPath("meta.serverResponseTime")
                        .type(JsonFieldType.ARRAY)
                        .description("서버 응답 시간")
                        .ignored(),
                    fieldWithPath("meta.serverPathVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 경로 버전")
                        .ignored(),
                    fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("상호 차단 여부"))));
  }

  @DisplayName("[restdocs] 나를 차단한 사용자 수를 조회할 수 있다")
  @Test
  void getBlockedByCountTest() throws Exception {
    // given
    Long currentUserId = 1L;
    Long response = 5L;

    // when
    when(blockService.getBlockedByCount(currentUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(get("/api/v1/blocks/stats/blocked-by-count"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "block-blocked-by-count",
                responseFields(
                    fieldWithPath("success")
                        .type(JsonFieldType.BOOLEAN)
                        .description("성공 여부")
                        .ignored(),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드").ignored(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록")
                        .ignored(),
                    fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
                    fieldWithPath("meta.serverVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 버전")
                        .ignored(),
                    fieldWithPath("meta.serverEncoding")
                        .type(JsonFieldType.STRING)
                        .description("서버 인코딩")
                        .ignored(),
                    fieldWithPath("meta.serverResponseTime")
                        .type(JsonFieldType.ARRAY)
                        .description("서버 응답 시간")
                        .ignored(),
                    fieldWithPath("meta.serverPathVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 경로 버전")
                        .ignored(),
                    fieldWithPath("data").type(JsonFieldType.NUMBER).description("나를 차단한 사용자 수"))));
  }

  @DisplayName("[restdocs] 내가 차단한 사용자 수를 조회할 수 있다")
  @Test
  void getBlockingCountTest() throws Exception {
    // given
    Long currentUserId = 1L;
    Long response = 3L;

    // when
    when(blockService.getBlockingCount(currentUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(get("/api/v1/blocks/stats/blocking-count"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "block-blocking-count",
                responseFields(
                    fieldWithPath("success")
                        .type(JsonFieldType.BOOLEAN)
                        .description("성공 여부")
                        .ignored(),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드").ignored(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록")
                        .ignored(),
                    fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
                    fieldWithPath("meta.serverVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 버전")
                        .ignored(),
                    fieldWithPath("meta.serverEncoding")
                        .type(JsonFieldType.STRING)
                        .description("서버 인코딩")
                        .ignored(),
                    fieldWithPath("meta.serverResponseTime")
                        .type(JsonFieldType.ARRAY)
                        .description("서버 응답 시간")
                        .ignored(),
                    fieldWithPath("meta.serverPathVersion")
                        .type(JsonFieldType.STRING)
                        .description("서버 경로 버전")
                        .ignored(),
                    fieldWithPath("data").type(JsonFieldType.NUMBER).description("내가 차단한 사용자 수"))));
  }
}

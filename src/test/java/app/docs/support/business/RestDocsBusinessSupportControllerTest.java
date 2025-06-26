package app.docs.support.business;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.business.constant.ContactType;
import app.bottlenote.support.business.controller.BusinessSupportController;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessSupportDetailItem;
import app.bottlenote.support.business.dto.response.BusinessSupportListResponse;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.service.BusinessSupportService;
import app.bottlenote.support.constant.StatusType;
import app.docs.AbstractRestDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static app.bottlenote.support.business.constant.BusinessResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.REGISTER_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Tag("restdocs")
@DisplayName("[restdocs] BusinessSupportController")
class RestDocsBusinessSupportControllerTest extends AbstractRestDocs {

	private BusinessSupportService businessSupportService = mock(BusinessSupportService.class);

	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Override
	protected Object initController() {
		return new BusinessSupportController(businessSupportService);
	}

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}


	@Test
	@DisplayName("비즈니스 문의를 등록할 수 있다.")
	void register_success() throws Exception {
		// given
		Long userId = 1L;
		BusinessSupportUpsertRequest request = new BusinessSupportUpsertRequest("새로운 비즈니스 문의입니다.", ContactType.EMAIL);
		BusinessSupportResultResponse response = BusinessSupportResultResponse.response(REGISTER_SUCCESS, 1L);

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
		when(businessSupportService.register(any(), anyLong())).thenReturn(response);

		// then
		mockMvc.perform(post("/api/v1/business-support")
						.with(csrf())
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("support/business/register",
						requestFields(
								fieldWithPath("content").type(JsonFieldType.STRING).description("문의 내용"),
								fieldWithPath("contactWay").type(JsonFieldType.STRING).description("연락 방식 (이메일, 전화번호 등)").optional()
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
								fieldWithPath("data.message").description("처리 결과 메시지"),
								fieldWithPath("data.id").description("등록된 문의 ID"),
								fieldWithPath("data.responseAt").description("서버 응답 일시"),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored()
						)
				));
	}

	@Test
	@DisplayName("비즈니스 문의 목록을 조회할 수 있다.")
	void get_list_success() throws Exception {
		// given
		Long userId = 1L;
		List<BusinessSupportListResponse.BusinessInfo> infos = List.of(
				new BusinessSupportListResponse.BusinessInfo(2L, "두번째 문의", LocalDateTime.now(), StatusType.WAITING),
				new BusinessSupportListResponse.BusinessInfo(1L, "첫번째 문의", LocalDateTime.now().minusDays(1), StatusType.SUCCESS)
		);
		BusinessSupportListResponse listResponse = BusinessSupportListResponse.of((long) infos.size(), infos);
		PageResponse<BusinessSupportListResponse> pageResponse = PageResponse.of(listResponse, CursorPageable.of(infos, 10L, 10L));

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
		when(businessSupportService.getList(any(), anyLong())).thenReturn(pageResponse);

		// then
		mockMvc.perform(get("/api/v1/business-support")
						.param("pageSize", "10")
						.param("cursor", "0")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("support/business/read-list",
						queryParameters(
								parameterWithName("pageSize").description("페이지 당 항목 수").optional(),
								parameterWithName("cursor").description("다음 페이지를 위한 커서 값").optional()
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data.totalCount").description("총 문의 개수"),
								fieldWithPath("data.list[].id").description("문의 ID"),
								fieldWithPath("data.list[].content").description("문의 내용"),
								fieldWithPath("data.list[].createAt").description("문의 생성일"),
								fieldWithPath("data.list[].status").description("문의 상태 (WAITING, ANSWERED 등)"),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored(),
								fieldWithPath("meta.pageable.currentCursor").description("현재 커서 위치"),
								fieldWithPath("meta.pageable.cursor").description("다음 페이지 조회를 위한 커서 정보"),
								fieldWithPath("meta.pageable.pageSize").description("페이지 크기"),
								fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부")
						)
				));
	}

	@Test
	@DisplayName("비즈니스 문의를 상세 조회할 수 있다.")
	void get_detail_success() throws Exception {
		// given
		Long userId = 1L;
		Long supportId = 1L;
		BusinessSupportDetailItem response = BusinessSupportDetailItem.builder()
				.id(supportId)
				.content("문의 상세 내용입니다.")
				.contactWay("test@example.com")
				.createAt(LocalDateTime.now().minusDays(1))
				.status(StatusType.SUCCESS)
				.adminId(100L)
				.responseContent("답변 드립니다.")
				.lastModifyAt(LocalDateTime.now())
				.build();

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
		when(businessSupportService.getDetail(anyLong(), anyLong())).thenReturn(response);

		// then
		mockMvc.perform(get("/api/v1/business-support/{id}", supportId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("support/business/read-detail",
						pathParameters(
								parameterWithName("id").description("문의 아이디")
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data.id").description("문의 ID"),
								fieldWithPath("data.content").description("문의 내용"),
								fieldWithPath("data.contactWay").description("연락 방식"),
								fieldWithPath("data.createAt").description("문의 생성일"),
								fieldWithPath("data.status").description("문의 상태"),
								fieldWithPath("data.adminId").description("답변 관리자 ID").optional(),
								fieldWithPath("data.responseContent").description("관리자 답변 내용").optional(),
								fieldWithPath("data.lastModifyAt").description("최종 수정일"),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored()
						)
				));
	}


	@Test
	@DisplayName("비즈니스 문의를 수정할 수 있다.")
	void modify_success() throws Exception {
		// given
		Long userId = 1L;
		Long supportId = 1L;
		BusinessSupportUpsertRequest request = new BusinessSupportUpsertRequest("수정된 내용입니다.", ContactType.EMAIL);
		BusinessSupportResultResponse response = BusinessSupportResultResponse.response(MODIFY_SUCCESS, supportId);

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
		when(businessSupportService.modify(anyLong(), any(), anyLong())).thenReturn(response);

		// then
		mockMvc.perform(patch("/api/v1/business-support/{id}", supportId)
						.with(csrf())
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("support/business/modify",
						pathParameters(
								parameterWithName("id").description("수정할 문의 ID")
						),
						requestFields(
								fieldWithPath("content").description("수정할 내용"),
								fieldWithPath("contactWay").description("수정할 연락 방식").optional()
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
								fieldWithPath("data.message").description("처리 결과 메시지"),
								fieldWithPath("data.id").description("수정된 문의 ID"),
								fieldWithPath("data.responseAt").description("서버 응답 일시"),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored()
						)
				));
	}


	@Test
	@DisplayName("비즈니스 문의를 삭제할 수 있다.")
	void delete_success() throws Exception {
		// given
		Long userId = 1L;
		Long supportId = 1L;
		BusinessSupportResultResponse response = BusinessSupportResultResponse.response(DELETE_SUCCESS, supportId);

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
		when(businessSupportService.delete(anyLong(), anyLong())).thenReturn(response);

		// then
		mockMvc.perform(delete("/api/v1/business-support/{id}", supportId)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("support/business/delete",
						pathParameters(
								parameterWithName("id").description("삭제할 문의 ID")
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
								fieldWithPath("data.message").description("처리 결과 메시지"),
								fieldWithPath("data.id").description("삭제된 문의 ID"),
								fieldWithPath("data.responseAt").description("서버 응답 일시"),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored()
						)
				));
	}
}

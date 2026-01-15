package app.docs.help

import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.global.service.cursor.CursorPageable
import app.bottlenote.global.service.cursor.PageResponse
import app.bottlenote.support.constant.StatusType
import app.bottlenote.support.help.constant.HelpType
import app.bottlenote.support.help.dto.request.HelpImageItem
import app.bottlenote.support.help.dto.response.AdminHelpAnswerResponse
import app.bottlenote.support.help.dto.response.AdminHelpDetailResponse
import app.bottlenote.support.help.dto.response.AdminHelpListResponse
import app.bottlenote.support.help.presentation.AdminHelpController
import app.bottlenote.support.help.service.AdminHelpService
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(
	controllers = [AdminHelpController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Help 컨트롤러 RestDocs 테스트")
class AdminHelpControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@MockitoBean
	private lateinit var adminHelpService: AdminHelpService

	@Test
	@DisplayName("문의 목록 조회")
	fun getHelpList() {
		// given
		val helpList = listOf(
			AdminHelpListResponse.AdminHelpInfo.builder()
				.helpId(1L)
				.userId(100L)
				.userNickname("테스트유저")
				.title("위스키 관련 문의")
				.type(HelpType.WHISKEY)
				.status(StatusType.WAITING)
				.createAt(LocalDateTime.now())
				.build()
		)
		val response = AdminHelpListResponse.of(1L, helpList)
		val cursorPageable = CursorPageable.builder()
			.cursor(20L)
			.pageSize(20L)
			.hasNext(false)
			.currentCursor(0L)
			.build()
		val pageResponse = PageResponse.of(response, cursorPageable)

		given(adminHelpService.getHelpList(any())).willReturn(pageResponse)

		// when & then
		assertThat(
			mvc.get().uri("/helps")
				.header("Authorization", "Bearer test_access_token")
				.param("status", StatusType.WAITING.name)
				.param("type", HelpType.WHISKEY.name)
				.param("cursor", "0")
				.param("pageSize", "20")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/help/list",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					requestHeaders(
						headerWithName("Authorization").description("Bearer 액세스 토큰")
					),
					queryParameters(
						parameterWithName("status").optional().description("상태 필터 (WAITING, SUCCESS, REJECT, DELETED)"),
						parameterWithName("type").optional().description("문의 유형 필터 (WHISKEY, REVIEW, USER, ETC)"),
						parameterWithName("cursor").optional().description("페이징 커서 (기본값: 0)"),
						parameterWithName("pageSize").optional().description("페이지 크기 (기본값: 20)")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data.content.totalCount").type(JsonFieldType.NUMBER).description("전체 문의 수"),
						fieldWithPath("data.content.helpList[].helpId").type(JsonFieldType.NUMBER).description("문의 ID"),
						fieldWithPath("data.content.helpList[].userId").type(JsonFieldType.NUMBER).description("문의자 ID"),
						fieldWithPath("data.content.helpList[].userNickname").type(JsonFieldType.STRING).description("문의자 닉네임"),
						fieldWithPath("data.content.helpList[].title").type(JsonFieldType.STRING).description("문의 제목"),
						fieldWithPath("data.content.helpList[].type").type(JsonFieldType.STRING).description("문의 유형"),
						fieldWithPath("data.content.helpList[].status").type(JsonFieldType.STRING).description("처리 상태"),
						fieldWithPath("data.content.helpList[].createAt").type(JsonFieldType.STRING).description("생성일시"),
						fieldWithPath("data.cursorPageable.cursor").type(JsonFieldType.NUMBER).description("다음 커서"),
						fieldWithPath("data.cursorPageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
						fieldWithPath("data.cursorPageable.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부"),
						fieldWithPath("data.cursorPageable.currentCursor").type(JsonFieldType.NUMBER).description("현재 커서"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
					)
				)
			)
	}

	@Test
	@DisplayName("문의 상세 조회")
	fun getHelpDetail() {
		// given
		val response = AdminHelpDetailResponse.builder()
			.helpId(1L)
			.userId(100L)
			.userNickname("테스트유저")
			.title("위스키 관련 문의")
			.content("위스키에 대해 문의드립니다.")
			.type(HelpType.WHISKEY)
			.imageUrlList(listOf(HelpImageItem.create(1, "https://example.com/image.jpg")))
			.status(StatusType.WAITING)
			.adminId(null)
			.responseContent(null)
			.createAt(LocalDateTime.now())
			.lastModifyAt(LocalDateTime.now())
			.build()

		given(adminHelpService.getHelpDetail(anyLong())).willReturn(response)

		// when & then
		assertThat(
			mvc.get().uri("/helps/{helpId}", 1L)
				.header("Authorization", "Bearer test_access_token")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/help/detail",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					requestHeaders(
						headerWithName("Authorization").description("Bearer 액세스 토큰")
					),
					pathParameters(
						parameterWithName("helpId").description("문의 ID")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data.helpId").type(JsonFieldType.NUMBER).description("문의 ID"),
						fieldWithPath("data.userId").type(JsonFieldType.NUMBER).description("문의자 ID"),
						fieldWithPath("data.userNickname").type(JsonFieldType.STRING).description("문의자 닉네임"),
						fieldWithPath("data.title").type(JsonFieldType.STRING).description("문의 제목"),
						fieldWithPath("data.content").type(JsonFieldType.STRING).description("문의 내용"),
						fieldWithPath("data.type").type(JsonFieldType.STRING).description("문의 유형"),
						fieldWithPath("data.imageUrlList[].order").type(JsonFieldType.NUMBER).description("이미지 순서"),
						fieldWithPath("data.imageUrlList[].viewUrl").type(JsonFieldType.STRING).description("이미지 URL"),
						fieldWithPath("data.status").type(JsonFieldType.STRING).description("처리 상태"),
						fieldWithPath("data.adminId").type(JsonFieldType.NULL).description("담당 관리자 ID"),
						fieldWithPath("data.responseContent").type(JsonFieldType.NULL).description("답변 내용"),
						fieldWithPath("data.createAt").type(JsonFieldType.STRING).description("생성일시"),
						fieldWithPath("data.lastModifyAt").type(JsonFieldType.STRING).description("수정일시"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
					)
				)
			)
	}

	@Test
	@DisplayName("문의 답변 등록")
	fun answerHelp() {
		// given
		val response = AdminHelpAnswerResponse.of(1L, StatusType.SUCCESS)

		given(adminHelpService.answerHelp(anyLong(), anyLong(), any())).willReturn(response)

		Mockito.mockStatic(SecurityContextUtil::class.java).use { mockedStatic: MockedStatic<SecurityContextUtil> ->
			mockedStatic.`when`<Optional<Long>> { SecurityContextUtil.getAdminUserIdByContext() }
				.thenReturn(Optional.of(1L))

			val request = mapOf(
				"responseContent" to "답변 내용입니다.",
				"status" to StatusType.SUCCESS.name
			)

			// when & then
			assertThat(
				mvc.post().uri("/helps/{helpId}/answer", 1L)
					.header("Authorization", "Bearer test_access_token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/help/answer",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						requestHeaders(
							headerWithName("Authorization").description("Bearer 액세스 토큰")
						),
						pathParameters(
							parameterWithName("helpId").description("문의 ID")
						),
						requestFields(
							fieldWithPath("responseContent").type(JsonFieldType.STRING).description("답변 내용"),
							fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태 (SUCCESS, REJECT)")
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data.helpId").type(JsonFieldType.NUMBER).description("문의 ID"),
							fieldWithPath("data.status").type(JsonFieldType.STRING).description("처리 상태"),
							fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}
	}
}

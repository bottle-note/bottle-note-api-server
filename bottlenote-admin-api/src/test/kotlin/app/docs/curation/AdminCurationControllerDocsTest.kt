package app.docs.curation

import app.bottlenote.alcohols.dto.request.AdminCurationAlcoholRequest
import app.bottlenote.alcohols.dto.request.AdminCurationCreateRequest
import app.bottlenote.alcohols.dto.request.AdminCurationDisplayOrderRequest
import app.bottlenote.alcohols.dto.request.AdminCurationSearchRequest
import app.bottlenote.alcohols.dto.request.AdminCurationStatusRequest
import app.bottlenote.alcohols.dto.request.AdminCurationUpdateRequest
import app.bottlenote.alcohols.presentation.AdminCurationController
import app.bottlenote.alcohols.service.AdminCurationService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import app.helper.curation.CurationHelper
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester

@WebMvcTest(
    controllers = [AdminCurationController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Curation 컨트롤러 RestDocs 테스트")
class AdminCurationControllerDocsTest {

    @Autowired
    private lateinit var mvc: MockMvcTester

    @Autowired
    private lateinit var mapper: ObjectMapper

    @MockitoBean
    private lateinit var adminCurationService: AdminCurationService

    @Nested
    @DisplayName("큐레이션 목록 조회")
    inner class ListCurations {

        @Test
        @DisplayName("큐레이션 목록을 조회할 수 있다")
        fun listCurations() {
            // given
            val items = CurationHelper.createAdminCurationListResponses(3)
            val page = PageImpl(items)
            val response = GlobalResponse.fromPage(page)

            given(adminCurationService.search(any(AdminCurationSearchRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.get().uri("/curations?keyword=&isActive=true&page=0&size=20")
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                            parameterWithName("keyword").description("검색어 (큐레이션명)").optional(),
                            parameterWithName("isActive").description("활성화 상태 필터 (true/false/null)").optional(),
                            parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                            parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("페이징 데이터"),
                            fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("큐레이션 목록"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("큐레이션 ID"),
                            fieldWithPath("data.content[].name").type(JsonFieldType.STRING).description("큐레이션명"),
                            fieldWithPath("data.content[].alcoholCount").type(JsonFieldType.NUMBER).description("포함된 위스키 수"),
                            fieldWithPath("data.content[].displayOrder").type(JsonFieldType.NUMBER).description("노출 순서"),
                            fieldWithPath("data.content[].isActive").type(JsonFieldType.BOOLEAN).description("활성화 상태"),
                            fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
                            fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이징 정보").ignored(),
                            fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER).description("페이지 번호").ignored(),
                            fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기").ignored(),
                            fieldWithPath("data.pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보").ignored(),
                            fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN).ignored(),
                            fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN).ignored(),
                            fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).ignored(),
                            fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).ignored(),
                            fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).ignored(),
                            fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).ignored(),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                            fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.sort").type(JsonFieldType.OBJECT).description("정렬 정보").ignored(),
                            fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN).ignored(),
                            fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN).ignored(),
                            fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN).ignored(),
                            fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                            fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                            fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("빈 페이지 여부"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
                        )
                    )
                )
        }
    }

    @Nested
    @DisplayName("큐레이션 상세 조회")
    inner class GetCurationDetail {

        @Test
        @DisplayName("큐레이션 상세 정보를 조회할 수 있다")
        fun getCurationDetail() {
            // given
            val response = CurationHelper.createAdminCurationDetailResponse()

            given(adminCurationService.getDetail(anyLong())).willReturn(response)

            // when & then
            assertThat(mvc.get().uri("/curations/{curationId}", 1L))
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("curationId").description("큐레이션 ID")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("큐레이션 상세 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("큐레이션 ID"),
                            fieldWithPath("data.name").type(JsonFieldType.STRING).description("큐레이션명"),
                            fieldWithPath("data.description").type(JsonFieldType.STRING).description("설명").optional(),
                            fieldWithPath("data.coverImageUrl").type(JsonFieldType.STRING).description("커버 이미지 URL").optional(),
                            fieldWithPath("data.displayOrder").type(JsonFieldType.NUMBER).description("노출 순서"),
                            fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("활성화 상태"),
                            fieldWithPath("data.alcoholIds").type(JsonFieldType.ARRAY).description("포함된 위스키 ID 목록"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일시"),
                            fieldWithPath("data.modifiedAt").type(JsonFieldType.STRING).description("수정일시"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
                        )
                    )
                )
        }
    }

    @Nested
    @DisplayName("큐레이션 생성")
    inner class CreateCuration {

        @Test
        @DisplayName("큐레이션을 생성할 수 있다")
        fun createCuration() {
            // given
            val request = CurationHelper.createCurationCreateRequest()
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_CREATED, 1L)

            given(adminCurationService.create(any(AdminCurationCreateRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.post().uri("/curations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("name").type(JsonFieldType.STRING).description("큐레이션명 (필수)"),
                            fieldWithPath("description").type(JsonFieldType.STRING).description("설명").optional(),
                            fieldWithPath("coverImageUrl").type(JsonFieldType.STRING).description("커버 이미지 URL").optional(),
                            fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).description("노출 순서 (기본값: 0)").optional(),
                            fieldWithPath("alcoholIds").type(JsonFieldType.ARRAY).description("포함할 위스키 ID 목록").optional()
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("생성된 큐레이션 ID"),
                            fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
                        )
                    )
                )
        }
    }

    @Nested
    @DisplayName("큐레이션 수정")
    inner class UpdateCuration {

        @Test
        @DisplayName("큐레이션을 수정할 수 있다")
        fun updateCuration() {
            // given
            val request = CurationHelper.createCurationUpdateRequest()
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_UPDATED, 1L)

            given(adminCurationService.update(anyLong(), any(AdminCurationUpdateRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.put().uri("/curations/{curationId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("curationId").description("큐레이션 ID")
                        ),
                        requestFields(
                            fieldWithPath("name").type(JsonFieldType.STRING).description("큐레이션명 (필수)"),
                            fieldWithPath("description").type(JsonFieldType.STRING).description("설명").optional(),
                            fieldWithPath("coverImageUrl").type(JsonFieldType.STRING).description("커버 이미지 URL").optional(),
                            fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).description("노출 순서 (필수)"),
                            fieldWithPath("isActive").type(JsonFieldType.BOOLEAN).description("활성화 상태 (필수)"),
                            fieldWithPath("alcoholIds").type(JsonFieldType.ARRAY).description("포함할 위스키 ID 목록")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("수정된 큐레이션 ID"),
                            fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
                        )
                    )
                )
        }
    }

    @Nested
    @DisplayName("큐레이션 삭제")
    inner class DeleteCuration {

        @Test
        @DisplayName("큐레이션을 삭제할 수 있다")
        fun deleteCuration() {
            // given
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_DELETED, 1L)

            given(adminCurationService.delete(anyLong())).willReturn(response)

            // when & then
            assertThat(mvc.delete().uri("/curations/{curationId}", 1L))
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("curationId").description("큐레이션 ID")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("삭제된 큐레이션 ID"),
                            fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
                        )
                    )
                )
        }
    }

    @Nested
    @DisplayName("큐레이션 활성화 상태 변경")
    inner class UpdateCurationStatus {

        @Test
        @DisplayName("큐레이션 활성화 상태를 변경할 수 있다")
        fun updateStatus() {
            // given
            val request = CurationHelper.createCurationStatusRequest(isActive = false)
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_STATUS_UPDATED, 1L)

            given(adminCurationService.updateStatus(anyLong(), any(AdminCurationStatusRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.patch().uri("/curations/{curationId}/status", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/update-status",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("curationId").description("큐레이션 ID")
                        ),
                        requestFields(
                            fieldWithPath("isActive").type(JsonFieldType.BOOLEAN).description("활성화 상태 (필수)")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("큐레이션 ID"),
                            fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
                        )
                    )
                )
        }
    }

    @Nested
    @DisplayName("큐레이션 노출 순서 변경")
    inner class UpdateCurationDisplayOrder {

        @Test
        @DisplayName("큐레이션 노출 순서를 변경할 수 있다")
        fun updateDisplayOrder() {
            // given
            val request = CurationHelper.createCurationDisplayOrderRequest(displayOrder = 5)
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_DISPLAY_ORDER_UPDATED, 1L)

            given(adminCurationService.updateDisplayOrder(anyLong(), any(AdminCurationDisplayOrderRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.patch().uri("/curations/{curationId}/display-order", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/update-display-order",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("curationId").description("큐레이션 ID")
                        ),
                        requestFields(
                            fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).description("노출 순서 (0 이상, 필수)")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("큐레이션 ID"),
                            fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
                        )
                    )
                )
        }
    }

    @Nested
    @DisplayName("큐레이션 위스키 관리")
    inner class ManageCurationAlcohols {

        @Test
        @DisplayName("큐레이션에 위스키를 추가할 수 있다")
        fun addAlcohols() {
            // given
            val request = CurationHelper.createCurationAlcoholRequest(alcoholIds = setOf(1L, 2L, 3L))
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_ALCOHOL_ADDED, 1L)

            given(adminCurationService.addAlcohols(anyLong(), any(AdminCurationAlcoholRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.post().uri("/curations/{curationId}/alcohols", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/add-alcohols",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("curationId").description("큐레이션 ID")
                        ),
                        requestFields(
                            fieldWithPath("alcoholIds").type(JsonFieldType.ARRAY).description("추가할 위스키 ID 목록 (필수)")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("큐레이션 ID"),
                            fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
                            fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
                        )
                    )
                )
        }

        @Test
        @DisplayName("큐레이션에서 위스키를 제거할 수 있다")
        fun removeAlcohol() {
            // given
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_ALCOHOL_REMOVED, 1L)

            given(adminCurationService.removeAlcohol(anyLong(), anyLong())).willReturn(response)

            // when & then
            assertThat(
                mvc.delete().uri("/curations/{curationId}/alcohols/{alcoholId}", 1L, 5L)
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/curations/remove-alcohol",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("curationId").description("큐레이션 ID"),
                            parameterWithName("alcoholId").description("제거할 위스키 ID")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("큐레이션 ID"),
                            fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
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

package app.docs.banner

import app.bottlenote.banner.dto.request.AdminBannerCreateRequest
import app.bottlenote.banner.dto.request.AdminBannerSearchRequest
import app.bottlenote.banner.dto.request.AdminBannerSortOrderRequest
import app.bottlenote.banner.dto.request.AdminBannerStatusRequest
import app.bottlenote.banner.dto.request.AdminBannerUpdateRequest
import app.bottlenote.banner.presentation.AdminBannerController
import app.bottlenote.banner.service.AdminBannerService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import app.helper.banner.BannerHelper
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
    controllers = [AdminBannerController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Banner 컨트롤러 RestDocs 테스트")
class AdminBannerControllerDocsTest {

    @Autowired
    private lateinit var mvc: MockMvcTester

    @Autowired
    private lateinit var mapper: ObjectMapper

    @MockitoBean
    private lateinit var adminBannerService: AdminBannerService

    @Nested
    @DisplayName("배너 목록 조회")
    inner class ListBanners {

        @Test
        @DisplayName("배너 목록을 조회할 수 있다")
        fun listBanners() {
            // given
            val items = BannerHelper.createAdminBannerListResponses(3)
            val page = PageImpl(items)
            val response = GlobalResponse.fromPage(page)

            given(adminBannerService.search(any(AdminBannerSearchRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.get().uri("/banners?keyword=&isActive=true&bannerType=CURATION&page=0&size=20")
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/banners/list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                            parameterWithName("keyword").description("검색어 (배너명)").optional(),
                            parameterWithName("isActive").description("활성화 상태 필터 (true/false/null)").optional(),
                            parameterWithName("bannerType").description("배너 유형 필터 (CURATION/AD/SURVEY/PARTNERSHIP/ETC)").optional(),
                            parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                            parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("배너 목록"),
                            fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("배너 ID"),
                            fieldWithPath("data[].name").type(JsonFieldType.STRING).description("배너명"),
                            fieldWithPath("data[].bannerType").type(JsonFieldType.STRING).description("배너 유형"),
                            fieldWithPath("data[].sortOrder").type(JsonFieldType.NUMBER).description("정렬 순서"),
                            fieldWithPath("data[].isActive").type(JsonFieldType.BOOLEAN).description("활성화 상태"),
                            fieldWithPath("data[].startDate").type(JsonFieldType.VARIES).description("시작일시").optional(),
                            fieldWithPath("data[].endDate").type(JsonFieldType.VARIES).description("종료일시").optional(),
                            fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
                            fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                            fieldWithPath("meta.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("meta.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("meta.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                            fieldWithPath("meta.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("meta.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
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
    @DisplayName("배너 상세 조회")
    inner class GetBannerDetail {

        @Test
        @DisplayName("배너 상세 정보를 조회할 수 있다")
        fun getBannerDetail() {
            // given
            val response = BannerHelper.createAdminBannerDetailResponse()

            given(adminBannerService.getDetail(anyLong())).willReturn(response)

            // when & then
            assertThat(mvc.get().uri("/banners/{bannerId}", 1L))
                .hasStatusOk()
                .apply(
                    document(
                        "admin/banners/detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("bannerId").description("배너 ID")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("배너 상세 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("배너 ID"),
                            fieldWithPath("data.name").type(JsonFieldType.STRING).description("배너명"),
                            fieldWithPath("data.nameFontColor").type(JsonFieldType.STRING).description("배너명 폰트 색상 (HEX)"),
                            fieldWithPath("data.descriptionA").type(JsonFieldType.STRING).description("배너 설명A").optional(),
                            fieldWithPath("data.descriptionB").type(JsonFieldType.STRING).description("배너 설명B").optional(),
                            fieldWithPath("data.descriptionFontColor").type(JsonFieldType.STRING).description("설명 폰트 색상 (HEX)"),
                            fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("이미지 URL. [주의] URL 형식 검증을 수행하지 않으므로 클라이언트에서 유효한 URL을 전달해야 합니다"),
                            fieldWithPath("data.textPosition").type(JsonFieldType.STRING).description("텍스트 위치 (RT/CENTER/LB 등)"),
                            fieldWithPath("data.isExternalUrl").type(JsonFieldType.BOOLEAN).description("외부 URL 여부"),
                            fieldWithPath("data.targetUrl").type(JsonFieldType.VARIES).description("이동 URL. [주의] URL 형식 검증을 수행하지 않으므로 클라이언트에서 유효한 URL을 전달해야 합니다").optional(),
                            fieldWithPath("data.bannerType").type(JsonFieldType.STRING).description("배너 유형"),
                            fieldWithPath("data.sortOrder").type(JsonFieldType.NUMBER).description("정렬 순서"),
                            fieldWithPath("data.startDate").type(JsonFieldType.VARIES).description("시작일시").optional(),
                            fieldWithPath("data.endDate").type(JsonFieldType.VARIES).description("종료일시").optional(),
                            fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("활성화 상태"),
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
    @DisplayName("배너 생성")
    inner class CreateBanner {

        @Test
        @DisplayName("배너를 생성할 수 있다")
        fun createBanner() {
            // given
            val request = BannerHelper.createBannerCreateRequest()
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.BANNER_CREATED, 1L)

            given(adminBannerService.create(any(AdminBannerCreateRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.post().uri("/banners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/banners/create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("name").type(JsonFieldType.STRING).description("배너명 (필수)"),
                            fieldWithPath("nameFontColor").type(JsonFieldType.STRING).description("배너명 폰트 색상 (HEX, 기본값: #ffffff)").optional(),
                            fieldWithPath("descriptionA").type(JsonFieldType.STRING).description("배너 설명A (최대 50자)").optional(),
                            fieldWithPath("descriptionB").type(JsonFieldType.STRING).description("배너 설명B (최대 50자)").optional(),
                            fieldWithPath("descriptionFontColor").type(JsonFieldType.STRING).description("설명 폰트 색상 (HEX, 기본값: #ffffff)").optional(),
                            fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("이미지 URL (필수). [주의] URL 형식 검증을 수행하지 않으므로 클라이언트에서 유효한 URL을 전달해야 합니다"),
                            fieldWithPath("textPosition").type(JsonFieldType.STRING).description("텍스트 위치 (RT/CENTER/LB 등, 기본값: RT)").optional(),
                            fieldWithPath("isExternalUrl").type(JsonFieldType.BOOLEAN).description("외부 URL 여부 (기본값: false)").optional(),
                            fieldWithPath("targetUrl").type(JsonFieldType.VARIES).description("이동 URL (isExternalUrl=true 시 필수). [주의] URL 형식 검증을 수행하지 않으므로 클라이언트에서 유효한 URL을 전달해야 합니다").optional(),
                            fieldWithPath("bannerType").type(JsonFieldType.STRING).description("배너 유형 (필수: CURATION/AD/SURVEY/PARTNERSHIP/ETC)"),
                            fieldWithPath("sortOrder").type(JsonFieldType.NUMBER).description("정렬 순서 (0 이상, 기본값: 0)").optional(),
                            fieldWithPath("startDate").type(JsonFieldType.VARIES).description("시작일시").optional(),
                            fieldWithPath("endDate").type(JsonFieldType.VARIES).description("종료일시").optional()
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("생성된 배너 ID"),
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
    @DisplayName("배너 수정")
    inner class UpdateBanner {

        @Test
        @DisplayName("배너를 수정할 수 있다")
        fun updateBanner() {
            // given
            val request = BannerHelper.createBannerUpdateRequest()
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.BANNER_UPDATED, 1L)

            given(adminBannerService.update(anyLong(), any(AdminBannerUpdateRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.put().uri("/banners/{bannerId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/banners/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("bannerId").description("배너 ID")
                        ),
                        requestFields(
                            fieldWithPath("name").type(JsonFieldType.STRING).description("배너명 (필수)"),
                            fieldWithPath("nameFontColor").type(JsonFieldType.STRING).description("배너명 폰트 색상 (HEX)"),
                            fieldWithPath("descriptionA").type(JsonFieldType.STRING).description("배너 설명A (최대 50자)").optional(),
                            fieldWithPath("descriptionB").type(JsonFieldType.STRING).description("배너 설명B (최대 50자)").optional(),
                            fieldWithPath("descriptionFontColor").type(JsonFieldType.STRING).description("설명 폰트 색상 (HEX)"),
                            fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("이미지 URL (필수). [주의] URL 형식 검증을 수행하지 않으므로 클라이언트에서 유효한 URL을 전달해야 합니다"),
                            fieldWithPath("textPosition").type(JsonFieldType.STRING).description("텍스트 위치 (RT/CENTER/LB 등)"),
                            fieldWithPath("isExternalUrl").type(JsonFieldType.BOOLEAN).description("외부 URL 여부"),
                            fieldWithPath("targetUrl").type(JsonFieldType.VARIES).description("이동 URL (isExternalUrl=true 시 필수). [주의] URL 형식 검증을 수행하지 않으므로 클라이언트에서 유효한 URL을 전달해야 합니다").optional(),
                            fieldWithPath("bannerType").type(JsonFieldType.STRING).description("배너 유형 (필수: CURATION/AD/SURVEY/PARTNERSHIP/ETC)"),
                            fieldWithPath("sortOrder").type(JsonFieldType.NUMBER).description("정렬 순서 (0 이상, 필수)"),
                            fieldWithPath("startDate").type(JsonFieldType.VARIES).description("시작일시").optional(),
                            fieldWithPath("endDate").type(JsonFieldType.VARIES).description("종료일시").optional(),
                            fieldWithPath("isActive").type(JsonFieldType.BOOLEAN).description("활성화 상태 (필수)")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("수정된 배너 ID"),
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
    @DisplayName("배너 삭제")
    inner class DeleteBanner {

        @Test
        @DisplayName("배너를 삭제할 수 있다")
        fun deleteBanner() {
            // given
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.BANNER_DELETED, 1L)

            given(adminBannerService.delete(anyLong())).willReturn(response)

            // when & then
            assertThat(mvc.delete().uri("/banners/{bannerId}", 1L))
                .hasStatusOk()
                .apply(
                    document(
                        "admin/banners/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("bannerId").description("배너 ID")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("삭제된 배너 ID"),
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
    @DisplayName("배너 활성화 상태 변경")
    inner class UpdateBannerStatus {

        @Test
        @DisplayName("배너 활성화 상태를 변경할 수 있다")
        fun updateStatus() {
            // given
            val request = BannerHelper.createBannerStatusRequest(isActive = false)
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.BANNER_STATUS_UPDATED, 1L)

            given(adminBannerService.updateStatus(anyLong(), any(AdminBannerStatusRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.patch().uri("/banners/{bannerId}/status", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/banners/update-status",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("bannerId").description("배너 ID")
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
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("배너 ID"),
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
    @DisplayName("배너 정렬 순서 변경")
    inner class UpdateBannerSortOrder {

        @Test
        @DisplayName("배너 정렬 순서를 변경할 수 있다")
        fun updateSortOrder() {
            // given
            val request = BannerHelper.createBannerSortOrderRequest(sortOrder = 5)
            val response = AdminResultResponse.of(AdminResultResponse.ResultCode.BANNER_SORT_ORDER_UPDATED, 1L)

            given(adminBannerService.updateSortOrder(anyLong(), any(AdminBannerSortOrderRequest::class.java)))
                .willReturn(response)

            // when & then
            assertThat(
                mvc.patch().uri("/banners/{bannerId}/sort-order", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .apply(
                    document(
                        "admin/banners/update-sort-order",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("bannerId").description("배너 ID")
                        ),
                        requestFields(
                            fieldWithPath("sortOrder").type(JsonFieldType.NUMBER).description("정렬 순서 (0 이상, 필수)")
                        ),
                        responseFields(
                            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                            fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
                            fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
                            fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("배너 ID"),
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

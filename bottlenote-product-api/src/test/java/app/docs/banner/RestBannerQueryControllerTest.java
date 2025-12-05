package app.docs.banner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.constant.TextPosition;
import app.bottlenote.banner.controller.BannerQueryController;
import app.bottlenote.banner.dto.response.BannerResponse;
import app.bottlenote.banner.service.BannerQueryService;
import app.docs.AbstractRestDocs;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

@DisplayName("배너 조회 컨트롤러 Rest API 문서화 테스트")
class RestBannerQueryControllerTest extends AbstractRestDocs {

  private final BannerQueryService bannerQueryService = mock(BannerQueryService.class);

  @Override
  protected Object initController() {
    return new BannerQueryController(bannerQueryService);
  }

  @DisplayName("활성화된 배너 목록을 조회할 수 있다.")
  @Test
  void getActiveBanners() throws Exception {
    // given
    List<BannerResponse> banners =
        List.of(
            BannerResponse.builder()
                .id(1L)
                .name("신규 위스키 출시 이벤트")
                .nameFontColor("ffffff")
                .descriptionA("신규 위스키 출시 기념")
                .descriptionB("특별 이벤트를 진행합니다.")
                .descriptionFontColor("ffffff")
                .imageUrl("https://cdn.bottle-note.com/banners/event1.jpg")
                .textPosition(TextPosition.LB)
                .targetUrl("/events/new-whiskey")
                .isExternalUrl(false)
                .bannerType(BannerType.CURATION)
                .sortOrder(0)
                .startDate(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endDate(LocalDateTime.of(2025, 12, 31, 23, 59))
                .build(),
            BannerResponse.builder()
                .id(2L)
                .name("사용자 설문조사")
                .nameFontColor("ffffff")
                .descriptionA("서비스 개선을 위한")
                .descriptionB("설문조사에 참여해주세요.")
                .descriptionFontColor("ffffff")
                .imageUrl("https://cdn.bottle-note.com/banners/survey.jpg")
                .textPosition(TextPosition.CENTER)
                .targetUrl("https://forms.google.com/survey123")
                .isExternalUrl(true)
                .bannerType(BannerType.SURVEY)
                .sortOrder(1)
                .startDate(LocalDateTime.of(2025, 6, 1, 0, 0))
                .endDate(LocalDateTime.of(2025, 6, 30, 23, 59))
                .build(),
            BannerResponse.builder()
                .id(3L)
                .name("제휴 브랜드 소개")
                .nameFontColor("ffffff")
                .descriptionA("새로운 제휴 브랜드를")
                .descriptionB("소개합니다.")
                .descriptionFontColor("ffffff")
                .imageUrl("https://cdn.bottle-note.com/banners/partner.jpg")
                .textPosition(TextPosition.RT)
                .targetUrl("/partners/brand-abc")
                .isExternalUrl(false)
                .bannerType(BannerType.PARTNERSHIP)
                .sortOrder(2)
                .startDate(null)
                .endDate(null)
                .build());

    when(bannerQueryService.getActiveBanners(10)).thenReturn(banners);

    // when & then
    mockMvc
        .perform(get("/api/v1/banners").param("limit", "10"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "banner/list",
                queryParameters(
                    parameterWithName("limit")
                        .optional()
                        .description("조회할 배너 개수 (기본값: 10, 최대 권장: 10)")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드 (HTTP 상태 코드)"),
                    fieldWithPath("data").type(JsonFieldType.ARRAY).description("배너 목록"),
                    fieldWithPath("data[].id").description("배너 고유 식별자"),
                    fieldWithPath("data[].name").description("배너명"),
                    fieldWithPath("data[].nameFontColor").description("배너명 텍스트 색상 (HEX)"),
                    fieldWithPath("data[].descriptionA").description("배너 설명 파트 A").optional(),
                    fieldWithPath("data[].descriptionB").description("배너 설명 파트 B").optional(),
                    fieldWithPath("data[].descriptionFontColor").description("배너 설명 텍스트 색상 (HEX)"),
                    fieldWithPath("data[].imageUrl").description("배너 이미지 URL"),
                    fieldWithPath("data[].textPosition").description("텍스트 위치 (하단 TextPosition 참조)"),
                    fieldWithPath("data[].targetUrl").description("클릭 시 이동할 URL").optional(),
                    fieldWithPath("data[].isExternalUrl")
                        .description("외부 URL 여부 (true: 외부, false: 내부)"),
                    fieldWithPath("data[].bannerType").description("배너 유형 (하단 BannerType 참조)"),
                    fieldWithPath("data[].sortOrder").description("정렬 순서 (오름차순)"),
                    fieldWithPath("data[].startDate").description("노출 시작일시 (nullable)").optional(),
                    fieldWithPath("data[].endDate").description("노출 종료일시 (nullable)").optional(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("에러 목록 (성공 시 빈 배열)"),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }
}

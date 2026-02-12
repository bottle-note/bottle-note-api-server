package app.integration.banner

import app.IntegrationTestSupport
import app.bottlenote.banner.fixture.BannerTestFactory
import app.helper.banner.BannerHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin Banner API 통합 테스트")
class AdminBannerIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var bannerTestFactory: BannerTestFactory

    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        val admin = adminUserTestFactory.persistRootAdmin()
        accessToken = getAccessToken(admin)
    }

    @Nested
    @DisplayName("배너 목록 조회 API")
    inner class ListBanners {

        @Test
        @DisplayName("배너 목록을 조회할 수 있다")
        fun listSuccess() {
            // given
            bannerTestFactory.persistMultipleBanners(3)

            // when & then
            assertThat(
                mockMvcTester.get().uri("/banners")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.success").isEqualTo(true)
        }

        @Test
        @DisplayName("키워드로 필터링하여 조회할 수 있다")
        fun listWithKeywordFilter() {
            // given
            bannerTestFactory.persistBanner("특별 배너", "https://example.com/special.jpg")

            // when & then
            assertThat(
                mockMvcTester.get().uri("/banners")
                    .param("keyword", "특별")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.success").isEqualTo(true)
        }

        @Test
        @DisplayName("활성화 상태로 필터링하여 조회할 수 있다")
        fun listWithIsActiveFilter() {
            // given
            bannerTestFactory.persistMixedActiveBanners(2, 1)

            // when & then
            assertThat(
                mockMvcTester.get().uri("/banners")
                    .param("isActive", "true")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.success").isEqualTo(true)
        }

        @Test
        @DisplayName("배너 유형으로 필터링하여 조회할 수 있다")
        fun listWithBannerTypeFilter() {
            // given
            bannerTestFactory.persistMixedActiveBanners(2, 1)

            // when & then
            assertThat(
                mockMvcTester.get().uri("/banners")
                    .param("bannerType", "CURATION")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.success").isEqualTo(true)
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        fun listUnauthorized() {
            assertThat(
                mockMvcTester.get().uri("/banners")
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("배너 상세 조회 API")
    inner class GetBannerDetail {

        @Test
        @DisplayName("배너 상세 정보를 조회할 수 있다")
        fun getDetailSuccess() {
            // given
            val banner = bannerTestFactory.persistBanner("상세 배너", "https://example.com/detail.jpg")

            // when & then
            assertThat(
                mockMvcTester.get().uri("/banners/${banner.id}")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.success").isEqualTo(true)
        }

        @Test
        @DisplayName("존재하지 않는 배너 조회 시 404를 반환한다")
        fun getDetailNotFound() {
            // when & then
            assertThat(
                mockMvcTester.get().uri("/banners/999999")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("배너 생성 API")
    inner class CreateBanner {

        @Test
        @DisplayName("배너를 생성할 수 있다")
        fun createSuccess() {
            // given
            val request = BannerHelper.createBannerCreateRequest(
                name = "새로운 배너"
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/banners")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("BANNER_CREATED")
        }

        @Test
        @DisplayName("이름이 중복되면 409를 반환한다")
        fun createDuplicateName() {
            // given
            bannerTestFactory.persistBanner("중복 배너", "https://example.com/dup.jpg")
            val request = BannerHelper.createBannerCreateRequest(
                name = "중복 배너"
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/banners")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }

        @Test
        @DisplayName("필수 필드 누락 시 400을 반환한다")
        fun createValidationFail() {
            // given
            val request = mapOf(
                "name" to "",
                "imageUrl" to ""
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/banners")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }

        @Test
        @DisplayName("startDate가 endDate보다 이후이면 400을 반환한다")
        fun createInvalidDateRange() {
            // given
            val request = BannerHelper.createBannerCreateRequest(
                name = "날짜 오류 배너",
                startDate = "2025-12-31T00:00:00",
                endDate = "2025-01-01T00:00:00"
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/banners")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }

        @Test
        @DisplayName("isExternalUrl이 true이고 targetUrl이 없으면 400을 반환한다")
        fun createExternalUrlWithoutTarget() {
            // given
            val request = BannerHelper.createBannerCreateRequest(
                name = "외부 URL 오류 배너",
                isExternalUrl = true,
                targetUrl = null
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/banners")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }

        @Test
        @DisplayName("잘못된 HEX 색상 형식이면 400을 반환한다")
        fun createInvalidHexColor() {
            // given
            val request = BannerHelper.createBannerCreateRequest(
                name = "색상 오류 배너",
                nameFontColor = "invalid"
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/banners")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("배너 수정 API")
    inner class UpdateBanner {

        @Test
        @DisplayName("배너를 수정할 수 있다")
        fun updateSuccess() {
            // given
            val banner = bannerTestFactory.persistBanner("수정 대상 배너", "https://example.com/edit.jpg")
            val request = BannerHelper.createBannerUpdateRequest(
                name = "수정된 배너"
            )

            // when & then
            assertThat(
                mockMvcTester.put().uri("/banners/${banner.id}")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("BANNER_UPDATED")
        }

        @Test
        @DisplayName("존재하지 않는 배너 수정 시 404를 반환한다")
        fun updateNotFound() {
            // given
            val request = BannerHelper.createBannerUpdateRequest()

            // when & then
            assertThat(
                mockMvcTester.put().uri("/banners/999999")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("배너 삭제 API")
    inner class DeleteBanner {

        @Test
        @DisplayName("배너를 삭제할 수 있다")
        fun deleteSuccess() {
            // given
            val banner = bannerTestFactory.persistBanner("삭제 대상 배너", "https://example.com/del.jpg")

            // when & then
            assertThat(
                mockMvcTester.delete().uri("/banners/${banner.id}")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("BANNER_DELETED")
        }

        @Test
        @DisplayName("존재하지 않는 배너 삭제 시 404를 반환한다")
        fun deleteNotFound() {
            // when & then
            assertThat(
                mockMvcTester.delete().uri("/banners/999999")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("배너 활성화 상태 변경 API")
    inner class UpdateBannerStatus {

        @Test
        @DisplayName("배너 활성화 상태를 변경할 수 있다")
        fun updateStatusSuccess() {
            // given
            val banner = bannerTestFactory.persistBanner("상태 변경 배너", "https://example.com/status.jpg")
            val request = BannerHelper.createBannerStatusRequest(isActive = false)

            // when & then
            assertThat(
                mockMvcTester.patch().uri("/banners/${banner.id}/status")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("BANNER_STATUS_UPDATED")
        }

        @Test
        @DisplayName("존재하지 않는 배너의 상태 변경 시 404를 반환한다")
        fun updateStatusNotFound() {
            // given
            val request = BannerHelper.createBannerStatusRequest(isActive = false)

            // when & then
            assertThat(
                mockMvcTester.patch().uri("/banners/999999/status")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("배너 정렬 순서 변경 API")
    inner class UpdateBannerSortOrder {

        @Test
        @DisplayName("배너 정렬 순서를 변경할 수 있다")
        fun updateSortOrderSuccess() {
            // given
            val banner = bannerTestFactory.persistBanner("정렬 변경 배너", "https://example.com/sort.jpg")
            val request = BannerHelper.createBannerSortOrderRequest(sortOrder = 5)

            // when & then
            assertThat(
                mockMvcTester.patch().uri("/banners/${banner.id}/sort-order")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("BANNER_SORT_ORDER_UPDATED")
        }
    }

    @Nested
    @DisplayName("인증 테스트")
    inner class AuthenticationTest {

        @Test
        @DisplayName("인증 없이 요청 시 실패한다")
        fun requestWithoutAuth() {
            // when & then
            assertThat(mockMvcTester.get().uri("/banners"))
                .hasStatus4xxClientError()

            assertThat(mockMvcTester.get().uri("/banners/1"))
                .hasStatus4xxClientError()

            assertThat(
                mockMvcTester.post().uri("/banners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(BannerHelper.createBannerCreateRequest()))
            )
                .hasStatus4xxClientError()
        }
    }
}

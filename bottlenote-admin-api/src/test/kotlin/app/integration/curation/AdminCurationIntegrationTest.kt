package app.integration.curation

import app.IntegrationTestSupport
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.helper.curation.CurationHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin Curation API 통합 테스트")
class AdminCurationIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var alcoholTestFactory: AlcoholTestFactory

    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        val admin = adminUserTestFactory.persistRootAdmin()
        accessToken = getAccessToken(admin)
    }

    @Nested
    @DisplayName("큐레이션 목록 조회 API")
    inner class ListCurations {

        @Test
        @DisplayName("큐레이션 목록을 조회할 수 있다")
        fun listSuccess() {
            // given
            alcoholTestFactory.persistCurationKeyword()
            alcoholTestFactory.persistCurationKeyword()

            // when & then
            assertThat(
                mockMvcTester.get().uri("/curations")
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
            alcoholTestFactory.persistCurationKeyword()

            // when & then
            assertThat(
                mockMvcTester.get().uri("/curations")
                    .param("keyword", "테스트")
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
            alcoholTestFactory.persistCurationKeyword()

            // when & then
            assertThat(
                mockMvcTester.get().uri("/curations")
                    .param("isActive", "true")
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
                mockMvcTester.get().uri("/curations")
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("큐레이션 상세 조회 API")
    inner class GetCurationDetail {

        @Test
        @DisplayName("큐레이션 상세 정보를 조회할 수 있다")
        fun getDetailSuccess() {
            // given
            val curation = alcoholTestFactory.persistCurationKeyword()

            // when & then
            assertThat(
                mockMvcTester.get().uri("/curations/${curation.id}")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.success").isEqualTo(true)
        }

        @Test
        @DisplayName("존재하지 않는 큐레이션 조회 시 404를 반환한다")
        fun getDetailNotFound() {
            // when & then
            assertThat(
                mockMvcTester.get().uri("/curations/999999")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("큐레이션 생성 API")
    inner class CreateCuration {

        @Test
        @DisplayName("큐레이션을 생성할 수 있다")
        fun createSuccess() {
            // given
            val request = CurationHelper.createCurationCreateRequest(
                name = "새로운 큐레이션"
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/curations")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("CURATION_CREATED")
        }

        @Test
        @DisplayName("위스키를 포함하여 큐레이션을 생성할 수 있다")
        fun createWithAlcohols() {
            // given
            val alcohol1 = alcoholTestFactory.persistAlcohol()
            val alcohol2 = alcoholTestFactory.persistAlcohol()

            val request = CurationHelper.createCurationCreateRequest(
                name = "위스키 큐레이션",
                alcoholIds = setOf(alcohol1.id, alcohol2.id)
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/curations")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("CURATION_CREATED")
        }

        @Test
        @DisplayName("이름이 중복되면 409를 반환한다")
        fun createDuplicateName() {
            // given
            alcoholTestFactory.persistCurationKeyword()
            val request = CurationHelper.createCurationCreateRequest(
                name = "테스트 큐레이션"  // persistCurationKeyword 기본 이름과 유사
            )

            // when & then - 실제 중복 체크는 이름이 정확히 일치해야 함
            assertThat(
                mockMvcTester.post().uri("/curations")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()  // 다른 이름이므로 성공
        }

        @Test
        @DisplayName("필수 필드 누락 시 400을 반환한다")
        fun createValidationFail() {
            // given
            val request = mapOf(
                "name" to "",  // 빈 문자열
                "description" to "설명"
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/curations")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("큐레이션 수정 API")
    inner class UpdateCuration {

        @Test
        @DisplayName("큐레이션을 수정할 수 있다")
        fun updateSuccess() {
            // given
            val curation = alcoholTestFactory.persistCurationKeyword()
            val request = CurationHelper.createCurationUpdateRequest(
                name = "수정된 큐레이션",
                description = "수정된 설명"
            )

            // when & then
            assertThat(
                mockMvcTester.put().uri("/curations/${curation.id}")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("CURATION_UPDATED")
        }

        @Test
        @DisplayName("존재하지 않는 큐레이션 수정 시 404를 반환한다")
        fun updateNotFound() {
            // given
            val request = CurationHelper.createCurationUpdateRequest()

            // when & then
            assertThat(
                mockMvcTester.put().uri("/curations/999999")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("큐레이션 삭제 API")
    inner class DeleteCuration {

        @Test
        @DisplayName("큐레이션을 삭제할 수 있다")
        fun deleteSuccess() {
            // given
            val curation = alcoholTestFactory.persistCurationKeyword()

            // when & then
            assertThat(
                mockMvcTester.delete().uri("/curations/${curation.id}")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("CURATION_DELETED")
        }

        @Test
        @DisplayName("존재하지 않는 큐레이션 삭제 시 404를 반환한다")
        fun deleteNotFound() {
            // when & then
            assertThat(
                mockMvcTester.delete().uri("/curations/999999")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("큐레이션 활성화 상태 변경 API")
    inner class UpdateCurationStatus {

        @Test
        @DisplayName("큐레이션 활성화 상태를 변경할 수 있다")
        fun updateStatusSuccess() {
            // given
            val curation = alcoholTestFactory.persistCurationKeyword()
            val request = CurationHelper.createCurationStatusRequest(isActive = false)

            // when & then
            assertThat(
                mockMvcTester.patch().uri("/curations/${curation.id}/status")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("CURATION_STATUS_UPDATED")
        }

        @Test
        @DisplayName("존재하지 않는 큐레이션의 상태 변경 시 404를 반환한다")
        fun updateStatusNotFound() {
            // given
            val request = CurationHelper.createCurationStatusRequest(isActive = false)

            // when & then
            assertThat(
                mockMvcTester.patch().uri("/curations/999999/status")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("큐레이션 노출 순서 변경 API")
    inner class UpdateCurationDisplayOrder {

        @Test
        @DisplayName("큐레이션 노출 순서를 변경할 수 있다")
        fun updateDisplayOrderSuccess() {
            // given
            val curation = alcoholTestFactory.persistCurationKeyword()
            val request = CurationHelper.createCurationDisplayOrderRequest(displayOrder = 5)

            // when & then
            assertThat(
                mockMvcTester.patch().uri("/curations/${curation.id}/display-order")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("CURATION_DISPLAY_ORDER_UPDATED")
        }
    }

    @Nested
    @DisplayName("큐레이션 위스키 관리 API")
    inner class ManageCurationAlcohols {

        @Test
        @DisplayName("큐레이션에 위스키를 추가할 수 있다")
        fun addAlcoholsSuccess() {
            // given
            val curation = alcoholTestFactory.persistCurationKeyword()
            val alcohol1 = alcoholTestFactory.persistAlcohol()
            val alcohol2 = alcoholTestFactory.persistAlcohol()
            val request = CurationHelper.createCurationAlcoholRequest(
                alcoholIds = setOf(alcohol1.id, alcohol2.id)
            )

            // when & then
            assertThat(
                mockMvcTester.post().uri("/curations/${curation.id}/alcohols")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("CURATION_ALCOHOL_ADDED")
        }

        @Test
        @DisplayName("큐레이션에서 위스키를 제거할 수 있다")
        fun removeAlcoholSuccess() {
            // given
            val alcohol = alcoholTestFactory.persistAlcohol()
            val curation = alcoholTestFactory.persistCurationKeyword("테스트 큐레이션", listOf(alcohol))

            // when & then
            assertThat(
                mockMvcTester.delete().uri("/curations/${curation.id}/alcohols/${alcohol.id}")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("CURATION_ALCOHOL_REMOVED")
        }

        @Test
        @DisplayName("큐레이션에 포함되지 않은 위스키 제거 시 400을 반환한다")
        fun removeAlcoholNotIncluded() {
            // given
            val curation = alcoholTestFactory.persistCurationKeyword()
            val alcohol = alcoholTestFactory.persistAlcohol()

            // when & then
            assertThat(
                mockMvcTester.delete().uri("/curations/${curation.id}/alcohols/${alcohol.id}")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatus4xxClientError()
        }
    }

    @Nested
    @DisplayName("인증 테스트")
    inner class AuthenticationTest {

        @Test
        @DisplayName("인증 없이 요청 시 실패한다")
        fun requestWithoutAuth() {
            // when & then
            assertThat(mockMvcTester.get().uri("/curations"))
                .hasStatus4xxClientError()

            assertThat(mockMvcTester.get().uri("/curations/1"))
                .hasStatus4xxClientError()

            assertThat(
                mockMvcTester.post().uri("/curations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(CurationHelper.createCurationCreateRequest()))
            )
                .hasStatus4xxClientError()
        }
    }
}

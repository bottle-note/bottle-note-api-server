package app.docs.curation

import app.bottlenote.curation.dto.request.CurationCreateRequest
import app.bottlenote.curation.dto.request.CurationSearchRequest
import app.bottlenote.curation.dto.request.CurationUpdateRequest
import app.bottlenote.curation.dto.response.AdminSpecBasedCurationDetailResponse
import app.bottlenote.curation.dto.response.AdminSpecBasedCurationListResponse
import app.bottlenote.curation.dto.response.CurationSpecResponse
import app.bottlenote.curation.presentation.AdminCurationSpecController
import app.bottlenote.curation.presentation.AdminSpecBasedCurationController
import app.bottlenote.curation.service.AdminSpecBasedCurationService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
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
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(
	controllers = [
		AdminSpecBasedCurationController::class,
		AdminCurationSpecController::class
	],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Spec Based Curation 컨트롤러 RestDocs 테스트")
class AdminSpecBasedCurationControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@Autowired
	private lateinit var mapper: ObjectMapper

	@MockitoBean
	private lateinit var adminSpecBasedCurationService: AdminSpecBasedCurationService

	@Test
	@DisplayName("큐레이션 스펙 목록을 조회할 수 있다")
	fun listSpecs() {
		given(adminSpecBasedCurationService.listSpecs()).willReturn(listOf(specResponse()))

		assertThat(mvc.get().uri("/v2/curation-specs"))
			.hasStatusOk()
			.apply(
				document(
					"admin/v2/curation-specs/list",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint())
				)
			)
	}

	@Test
	@DisplayName("큐레이션 스펙 상세를 조회할 수 있다")
	fun getSpecDetail() {
		given(adminSpecBasedCurationService.getSpecDetail(anyLong())).willReturn(specResponse())

		assertThat(mvc.get().uri("/v2/curation-specs/{specId}", 1L))
			.hasStatusOk()
			.apply(
				document(
					"admin/v2/curation-specs/detail",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint())
				)
			)
	}

	@Test
	@DisplayName("spec 기반 큐레이션 목록을 조회할 수 있다")
	fun listCurations() {
		given(adminSpecBasedCurationService.search(any(CurationSearchRequest::class.java)))
			.willReturn(GlobalResponse.fromPage(PageImpl(listOf(listResponse()))))

		assertThat(mvc.get().uri("/v2/curations?keyword=&isActive=true&page=0&size=20"))
			.hasStatusOk()
			.apply(
				document(
					"admin/v2/curations/list",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint())
				)
			)
	}

	@Test
	@DisplayName("spec 기반 큐레이션 상세를 조회할 수 있다")
	fun getDetail() {
		given(adminSpecBasedCurationService.getDetail(anyLong())).willReturn(detailResponse())

		assertThat(mvc.get().uri("/v2/curations/{curationId}", 1L))
			.hasStatusOk()
			.apply(
				document(
					"admin/v2/curations/detail",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint())
				)
			)
	}

	@Test
	@DisplayName("spec 기반 큐레이션을 등록할 수 있다")
	fun create() {
		given(adminSpecBasedCurationService.create(any(CurationCreateRequest::class.java)))
			.willReturn(AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_CREATED, 1L))

		assertThat(
			mvc.post().uri("/v2/curations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(createRequest()))
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/v2/curations/create",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint())
				)
			)
	}

	@Test
	@DisplayName("spec 기반 큐레이션을 수정할 수 있다")
	fun update() {
		given(adminSpecBasedCurationService.update(anyLong(), any(CurationUpdateRequest::class.java)))
			.willReturn(AdminResultResponse.of(AdminResultResponse.ResultCode.CURATION_UPDATED, 1L))

		assertThat(
			mvc.put().uri("/v2/curations/{curationId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(updateRequest()))
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/v2/curations/update",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint())
				)
			)
	}

	private fun specResponse(): CurationSpecResponse = CurationSpecResponse(
		1L,
		"RECOMMENDED_WHISKY",
		"추천 위스키",
		"추천 위스키 카드 목록",
		"alcohol",
		1,
		true,
		mapOf("type" to "object", "required" to listOf("source", "alcohol")),
		mapOf("type" to "object")
	)

	private fun listResponse(): AdminSpecBasedCurationListResponse = AdminSpecBasedCurationListResponse(
		1L,
		1L,
		"RECOMMENDED_WHISKY",
		"비 오는 날 위스키",
		1,
		true,
		LocalDateTime.of(2026, 5, 15, 0, 0)
	)

	private fun detailResponse(): AdminSpecBasedCurationDetailResponse = AdminSpecBasedCurationDetailResponse(
		1L,
		"비 오는 날 위스키",
		"스모키 위스키 추천",
		"https://cdn.example.com/cover.jpg",
		listOf("https://cdn.example.com/cover.jpg"),
		LocalDate.of(2026, 6, 1),
		LocalDate.of(2026, 6, 30),
		1,
		true,
		LocalDateTime.of(2026, 5, 15, 0, 0),
		LocalDateTime.of(2026, 5, 15, 0, 0),
		specResponse(),
		mapOf("source" to "BOTTLE_NOTE", "alcohol" to mapOf("korName" to "테스트 위스키"))
	)

	private fun createRequest(): Map<String, Any?> = mapOf(
		"specId" to 1L,
		"name" to "비 오는 날 위스키",
		"description" to "스모키 위스키 추천",
		"imageUrls" to listOf("https://cdn.example.com/cover.jpg"),
		"exposureStartDate" to "2026-06-01",
		"exposureEndDate" to "2026-06-30",
		"displayOrder" to 1,
		"isActive" to true,
		"payload" to mapOf("source" to "BOTTLE_NOTE", "alcohol" to mapOf("korName" to "테스트 위스키"))
	)

	private fun updateRequest(): Map<String, Any?> = createRequest() + mapOf("name" to "수정된 큐레이션")
}

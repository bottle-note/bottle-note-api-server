package app.integration.mfds

import app.IntegrationTestSupport
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus
import app.bottlenote.mfds.constant.MfdsNormalizationStatus
import app.bottlenote.mfds.domain.MfdsDeclarationRepository
import app.bottlenote.mfds.domain.MfdsImporterRcnoLinkRepository
import app.bottlenote.mfds.domain.MfdsImporterRepository
import app.bottlenote.mfds.dto.request.MfdsDeclarationImporterLinkRequest
import app.bottlenote.mfds.dto.request.MfdsDeclarationStatusRequest
import app.bottlenote.mfds.dto.request.MfdsImporterCreateRequest
import app.bottlenote.mfds.dto.request.MfdsImporterUpdateRequest
import app.bottlenote.mfds.dto.request.MfdsRcnoLinkCreateRequest
import app.bottlenote.mfds.fixture.MfdsTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin MFDS API 통합 테스트")
class AdminMfdsIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var mfdsTestFactory: MfdsTestFactory

	@Autowired
	private lateinit var importerRepository: MfdsImporterRepository

	@Autowired
	private lateinit var declarationRepository: MfdsDeclarationRepository

	@Autowired
	private lateinit var rcnoLinkRepository: MfdsImporterRcnoLinkRepository

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("수입사 API")
	inner class Importers {
		@Test
		@DisplayName("관리 상태 필터로 수입사 목록을 조회할 수 있다")
		fun searchByAdminStatus() {
			mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)
			mfdsTestFactory.persistImporter("BIZ-002", "노트무역", MfdsImporterAdminStatus.INACTIVE)

			assertThat(
				mockMvcTester
					.get()
					.uri("/v1/mfds/importers?adminStatus=ACTIVE")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.length()").isEqualTo(1)
		}

		@Test
		@DisplayName("수입사 상세를 조회할 수 있다")
		fun detail() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)

			assertThat(
				mockMvcTester
					.get()
					.uri("/v1/mfds/importers/${importer.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.businessName").isEqualTo("보틀상사")
		}

		@Test
		@DisplayName("수입사를 등록할 수 있다")
		fun create() {
			val request = MfdsImporterCreateRequest(
				"BIZ-100",
				"제0100호",
				"새수입사",
				"김대표",
				"https://impfood.mfds.go.kr/list",
				"설명",
				null,
				null
			)

			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/mfds/importers")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("MFDS_IMPORTER_CREATED")
		}

		@Test
		@DisplayName("공식 업소 코드가 중복되면 409를 반환한다")
		fun createDuplicate() {
			mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)
			val request = MfdsImporterCreateRequest(
				"BIZ-001",
				"제0001호",
				"다른수입사",
				null,
				"https://impfood.mfds.go.kr/list",
				null,
				null,
				null
			)

			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/mfds/importers")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus(409)
		}

		@Test
		@DisplayName("수입사 관리 항목을 수정할 수 있다")
		fun update() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)
			val request = MfdsImporterUpdateRequest("보틀상사코리아", "새 설명", "메모", MfdsImporterAdminStatus.INACTIVE)

			assertThat(
				mockMvcTester
					.put()
					.uri("/v1/mfds/importers/${importer.id}")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()

			val updated = importerRepository.findById(importer.id).orElseThrow()
			assertThat(updated.businessName).isEqualTo("보틀상사코리아")
			assertThat(updated.adminStatus).isEqualTo(MfdsImporterAdminStatus.INACTIVE)
		}

		@Test
		@DisplayName("연결된 신고가 있으면 수입사 삭제 시 409를 반환한다")
		fun deleteRejectedWhenDeclarationLinked() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)
			mfdsTestFactory.persistDeclaration("RCNO-001", MfdsNormalizationStatus.NORMALIZED, importer.id, null, null)

			assertThat(
				mockMvcTester
					.delete()
					.uri("/v1/mfds/importers/${importer.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatus(409)
		}

		@Test
		@DisplayName("연결이 없는 수입사를 삭제할 수 있다")
		fun deleteSuccess() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)

			assertThat(
				mockMvcTester
					.delete()
					.uri("/v1/mfds/importers/${importer.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()

			assertThat(importerRepository.findById(importer.id)).isEmpty
		}
	}

	@Nested
	@DisplayName("수입 신고 API")
	inner class Declarations {
		@Test
		@DisplayName("정규화 상태 필터로 신고 목록을 조회할 수 있다")
		fun searchByStatus() {
			mfdsTestFactory.persistDeclaration("RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null)
			mfdsTestFactory.persistDeclaration("RCNO-002", MfdsNormalizationStatus.REVIEW_REQUIRED, null, null, null)

			assertThat(
				mockMvcTester
					.get()
					.uri("/v1/mfds/declarations?normalizationStatus=REVIEW_REQUIRED")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.length()").isEqualTo(1)
		}

		@Test
		@DisplayName("신고 상세에 연결 수입사 정보를 포함한다")
		fun detailWithImporter() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)
			val declaration =
				mfdsTestFactory.persistDeclaration("RCNO-001", MfdsNormalizationStatus.NORMALIZED, importer.id, null, null)

			assertThat(
				mockMvcTester
					.get()
					.uri("/v1/mfds/declarations/${declaration.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.importer.businessName").isEqualTo("보틀상사")
		}

		@Test
		@DisplayName("정규화 상태를 변경할 수 있다")
		fun changeStatus() {
			val declaration =
				mfdsTestFactory.persistDeclaration("RCNO-001", MfdsNormalizationStatus.REVIEW_REQUIRED, null, null, null)
			val request = MfdsDeclarationStatusRequest(MfdsNormalizationStatus.NORMALIZED, "admin", "검토 완료")

			assertThat(
				mockMvcTester
					.patch()
					.uri("/v1/mfds/declarations/${declaration.id}/normalization-status")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()

			val updated = declarationRepository.findById(declaration.id).orElseThrow()
			assertThat(updated.normalizationStatus).isEqualTo(MfdsNormalizationStatus.NORMALIZED)
			assertThat(updated.reviewedBy).isEqualTo("admin")
		}

		@Test
		@DisplayName("수입사를 수동 연결하면 MANUAL 근거와 RCNO 연결 근거가 남는다")
		fun linkImporter() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)
			val declaration =
				mfdsTestFactory.persistDeclaration("RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null)

			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/mfds/declarations/${declaration.id}/importer")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(MfdsDeclarationImporterLinkRequest(importer.id)))
			).hasStatusOk()

			val linked = declarationRepository.findById(declaration.id).orElseThrow()
			assertThat(linked.importerId).isEqualTo(importer.id)
			assertThat(rcnoLinkRepository.findByRcno("RCNO-001")).isPresent
		}

		@Test
		@DisplayName("수입사 연결을 해제하면 RCNO 연결 근거도 제거된다")
		fun unlinkImporter() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)
			val declaration =
				mfdsTestFactory.persistDeclaration("RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null)
			mockMvcTester
				.post()
				.uri("/v1/mfds/declarations/${declaration.id}/importer")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(MfdsDeclarationImporterLinkRequest(importer.id)))
				.exchange()

			assertThat(
				mockMvcTester
					.delete()
					.uri("/v1/mfds/declarations/${declaration.id}/importer")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()

			val unlinked = declarationRepository.findById(declaration.id).orElseThrow()
			assertThat(unlinked.isImporterLinked).isFalse()
			assertThat(rcnoLinkRepository.findByRcno("RCNO-001")).isEmpty
		}
	}

	@Nested
	@DisplayName("RCNO 연결 근거 API")
	inner class RcnoLinks {
		@Test
		@DisplayName("수입사 기준으로 연결 근거 목록을 조회할 수 있다")
		fun searchByImporter() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)
			mfdsTestFactory.persistRcnoLink("RCNO-001", importer.id, "보틀상사")
			mfdsTestFactory.persistRcnoLink("RCNO-002", importer.id, "보틀상사")

			assertThat(
				mockMvcTester
					.get()
					.uri("/v1/mfds/rcno-links?importerId=${importer.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.length()").isEqualTo(2)
		}

		@Test
		@DisplayName("연결 근거를 등록하고 삭제할 수 있다")
		fun createAndDelete() {
			val importer = mfdsTestFactory.persistImporter("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE)

			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/mfds/rcno-links")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(MfdsRcnoLinkCreateRequest("RCNO-001", importer.id)))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("MFDS_RCNO_LINK_CREATED")

			assertThat(
				mockMvcTester
					.delete()
					.uri("/v1/mfds/rcno-links/RCNO-001")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()

			assertThat(rcnoLinkRepository.findByRcno("RCNO-001")).isEmpty
		}

		@Test
		@DisplayName("없는 근거를 삭제하면 404를 반환한다")
		fun deleteNotFound() {
			assertThat(
				mockMvcTester
					.delete()
					.uri("/v1/mfds/rcno-links/RCNO-404")
					.header("Authorization", "Bearer $accessToken")
			).hasStatus(404)
		}
	}
}

package app.bottlenote.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.TastingTagGroupRole
import app.bottlenote.alcohols.domain.AlcoholsTastingTags
import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupCreateRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagRoleRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagsRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupUpdateRequest
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupItem
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupListResponse
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.alcohols.fixture.TastingTagTestFactory
import app.bottlenote.global.dto.response.AdminResultResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MvcTestResult

@Tag("admin_integration")
@DisplayName("[integration] 맛 그룹 어드민 API")
class AdminTastingTagGroupIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var tastingTagTestFactory: TastingTagTestFactory

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var alcoholsTastingTagsRepository: AlcoholsTastingTagsRepository

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		accessToken = getAccessToken(adminUserTestFactory.persistRootAdmin())
	}

	@Test
	@DisplayName("그룹을 등록하고 태그의 역할과 위스키 수까지 조회할 수 있다")
	fun createAndReadGroup() {
		val peat = tastingTagTestFactory.persistTastingTag("피트", "peat")
		val vanilla = tastingTagTestFactory.persistTastingTag("바닐라", "vanilla")
		val alcohol = alcoholTestFactory.persistAlcohol()
		val another = alcoholTestFactory.persistAlcohol()
		alcoholsTastingTagsRepository.saveAll(
			listOf(AlcoholsTastingTags.of(alcohol, peat), AlcoholsTastingTags.of(another, peat))
		)

		val created = extractData(
			post("/v1/tasting/groups", AdminTastingTagGroupCreateRequest("PEAT", "피트", "스카치", "연기", 2)),
			AdminResultResponse::class.java
		)
		assertThat(created.code).isEqualTo("TASTING_TAG_GROUP_CREATED")
		post(
			"/v1/tasting/groups/PEAT/tags",
			AdminTastingTagGroupTagsRequest(listOf(peat.id, vanilla.id), TastingTagGroupRole.PERIPHERAL)
		)
		put(
			"/v1/tasting/groups/PEAT/tags/${peat.id}",
			AdminTastingTagGroupTagRoleRequest(TastingTagGroupRole.CORE)
		)

		val list = extractData(get("/v1/tasting/groups"), AdminTastingTagGroupListResponse::class.java)
		assertThat(list.families).containsExactly("스카치")
		assertThat(list.groups).hasSize(1)
		val group = list.groups[0]
		assertThat(group.code).isEqualTo("PEAT")
		assertThat(group.id).isEqualTo(created.targetId)
		assertThat(group.tags.map { it.korName }).containsExactly("피트", "바닐라")
		assertThat(group.tags[0].role.code).isEqualTo("CORE")
		assertThat(group.tags[0].role.korName).isEqualTo("대표 태그")
		assertThat(group.tags[0].provider.code).isEqualTo("ADMIN")
		assertThat(group.tags[0].alcoholCount).isEqualTo(2L)
		assertThat(group.tags[1].role.code).isEqualTo("PERIPHERAL")
		assertThat(group.tags[1].alcoholCount).isZero()

		val detail = extractData(get("/v1/tasting/groups/PEAT"), AdminTastingTagGroupItem::class.java)
		assertThat(detail).isEqualTo(group)
	}

	@Test
	@DisplayName("코드나 이름이 겹치면 409를 반환한다")
	fun rejectDuplicateCodeAndName() {
		post("/v1/tasting/groups", AdminTastingTagGroupCreateRequest("PEAT", "피트", null, null, null))

		assertThat(
			post("/v1/tasting/groups", AdminTastingTagGroupCreateRequest("PEAT", "다른 이름", null, null, null))
				.response.status
		).isEqualTo(409)
		assertThat(
			post("/v1/tasting/groups", AdminTastingTagGroupCreateRequest("OTHER", "피트", null, null, null))
				.response.contentAsString
		).contains("동일한 이름의 맛 그룹이 이미 존재합니다.")
	}

	@Test
	@DisplayName("코드 형식이 아니면 400을 반환한다")
	fun rejectInvalidCode() {
		assertThat(
			post("/v1/tasting/groups", AdminTastingTagGroupCreateRequest("peat", "피트", null, null, null))
				.response.status
		).isEqualTo(400)
	}

	@Test
	@DisplayName("그룹을 수정하고 태그를 뺀 뒤에만 삭제할 수 있다")
	fun updateThenDeleteAfterRemovingTags() {
		val peat = tastingTagTestFactory.persistTastingTag("피트", "peat")
		post("/v1/tasting/groups", AdminTastingTagGroupCreateRequest("PEAT", "피트", "스카치", "설명", 1))
		post(
			"/v1/tasting/groups/PEAT/tags",
			AdminTastingTagGroupTagsRequest(listOf(peat.id), TastingTagGroupRole.CORE)
		)

		val updated = extractData(
			put("/v1/tasting/groups/PEAT", AdminTastingTagGroupUpdateRequest("피트 연기", "아일라", null, null)),
			AdminResultResponse::class.java
		)
		assertThat(updated.code).isEqualTo("TASTING_TAG_GROUP_UPDATED")
		assertThat(get("/v1/tasting/groups/PEAT").response.status).isEqualTo(200)

		assertThat(delete("/v1/tasting/groups/PEAT").response.status).isEqualTo(409)
		assertThat(delete("/v1/tasting/groups/PEAT/tags/999").response.status).isEqualTo(404)

		val removed = extractData(
			delete("/v1/tasting/groups/PEAT/tags/${peat.id}"),
			AdminResultResponse::class.java
		)
		assertThat(removed.code).isEqualTo("TASTING_TAG_GROUP_TAG_REMOVED")
		val deleted = extractData(delete("/v1/tasting/groups/PEAT"), AdminResultResponse::class.java)
		assertThat(deleted.code).isEqualTo("TASTING_TAG_GROUP_DELETED")
		assertThat(get("/v1/tasting/groups/PEAT").response.status).isEqualTo(404)
	}

	@Test
	@DisplayName("없는 태그 ID는 연결하지 않는다")
	fun rejectUnknownTag() {
		post("/v1/tasting/groups", AdminTastingTagGroupCreateRequest("PEAT", "피트", null, null, 0))

		assertThat(
			post(
				"/v1/tasting/groups/PEAT/tags",
				AdminTastingTagGroupTagsRequest(listOf(999L), TastingTagGroupRole.CORE)
			).response.status
		).isEqualTo(404)
	}

	@Test
	@DisplayName("역할이 없으면 400을 반환한다")
	fun rejectMissingRole() {
		val peat = tastingTagTestFactory.persistTastingTag("피트", "peat")
		post("/v1/tasting/groups", AdminTastingTagGroupCreateRequest("PEAT", "피트", null, null, 0))

		assertThat(
			mockMvcTester.post().uri("/v1/tasting/groups/PEAT/tags")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"tastingTagIds":[${peat.id}]}""")
		).hasStatus(400)
	}

	private fun get(uri: String): MvcTestResult = mockMvcTester.get().uri(uri)
		.header("Authorization", "Bearer $accessToken")
		.exchange()

	private fun post(uri: String, body: Any): MvcTestResult = mockMvcTester.post().uri(uri)
		.header("Authorization", "Bearer $accessToken")
		.contentType(MediaType.APPLICATION_JSON)
		.content(mapper.writeValueAsString(body))
		.exchange()

	private fun put(uri: String, body: Any): MvcTestResult = mockMvcTester.put().uri(uri)
		.header("Authorization", "Bearer $accessToken")
		.contentType(MediaType.APPLICATION_JSON)
		.content(mapper.writeValueAsString(body))
		.exchange()

	private fun delete(uri: String): MvcTestResult = mockMvcTester.delete().uri(uri)
		.header("Authorization", "Bearer $accessToken")
		.exchange()
}

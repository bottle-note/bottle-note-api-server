package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.TastingTagGroupRole
import app.bottlenote.alcohols.domain.TastingTagGroup
import app.bottlenote.alcohols.domain.TastingTagGroupRepository
import app.bottlenote.alcohols.domain.TastingTagRepository
import app.bottlenote.alcohols.dto.response.AdminTastingTagCatalogItem
import app.bottlenote.alcohols.dto.response.AdminTastingTagIconResponse
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.alcohols.fixture.TastingTagTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.assertj.MvcTestResult

@Tag("admin_integration")
@DisplayName("[integration] 테이스팅 태그 경량 조회 어드민 API")
class AdminTastingTagCatalogIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var tastingTagTestFactory: TastingTagTestFactory

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var tastingTagRepository: TastingTagRepository

	@Autowired
	private lateinit var tastingTagGroupRepository: TastingTagGroupRepository

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Test
	@DisplayName("전체 태그를 조회할 때 아이콘 없이 한글 이름 순으로 위스키 수와 그룹을 반환한다")
	fun findsAllTagsWithoutIconInKoreanNameOrder() {
		val parent = tastingTagTestFactory.persistTastingTag("가나", "Gana")
		tastingTagTestFactory.persistTastingTagWithParent("가나다", "Ganada", parent)
		val grouped = tastingTagTestFactory.persistTastingTag("나나", "Nana")
		grouped.update("나나", "Nana", "QUJD", grouped.description, null)
		tastingTagRepository.save(grouped)
		val popular = tastingTagTestFactory.persistTastingTag("다다", "Dada")
		tastingTagTestFactory.linkAlcoholToTag(alcoholTestFactory.persistAlcohol(), grouped)
		tastingTagTestFactory.linkAlcoholToTag(alcoholTestFactory.persistAlcohol(), grouped)
		tastingTagTestFactory.linkAlcoholToTag(alcoholTestFactory.persistAlcohol(), popular)
		val group = TastingTagGroup.create("PEAT_SMOKY", "피트 스모키", "스카치", "이탄", 1)
		group.putTag(grouped.id, TastingTagGroupRole.CORE)
		tastingTagGroupRepository.save(group)

		val result = getAs(accessToken, "/v1/tasting/tags/all")
		assertThat(result).hasStatusOk()
		val items = extractData(result, Array<AdminTastingTagCatalogItem>::class.java).toList()

		assertThat(result.response.contentAsString).doesNotContain("QUJD")
		assertThat(items.map { it.korName }).containsExactly("가나", "가나다", "나나", "다다")
		assertThat(items.map { it.alcoholCount }).containsExactly(0L, 0L, 2L, 1L)
		assertThat(items[1].parentId).isEqualTo(parent.id)
		assertThat(items[0].groups).isEmpty()
		assertThat(items[2].groups).singleElement().satisfies({ ref ->
			assertThat(ref.groupCode).isEqualTo("PEAT_SMOKY")
			assertThat(ref.groupName).isEqualTo("피트 스모키")
			assertThat(ref.role.code).isEqualTo("CORE")
			assertThat(ref.role.korName).isEqualTo(TastingTagGroupRole.CORE.korName)
			assertThat(ref.role.engName).isEqualTo(TastingTagGroupRole.CORE.engName)
			assertThat(ref.role.description).isEqualTo(TastingTagGroupRole.CORE.description)
		})
	}

	@Test
	@DisplayName("미소속 태그를 조회할 때 한글 이름 부분 일치와 위스키 수 내림차순을 적용한다")
	fun findsUngroupedTagsByKeywordAndAlcoholCount() {
		tastingTagTestFactory.persistTastingTag("가나", "Gana")
		val grouped = tastingTagTestFactory.persistTastingTag("나나", "Nana")
		val popular = tastingTagTestFactory.persistTastingTag("다다", "Dada")
		val child = tastingTagTestFactory.persistTastingTag("가나다", "Ganada")
		tastingTagTestFactory.linkAlcoholToTag(alcoholTestFactory.persistAlcohol(), popular)
		val group = TastingTagGroup.create("PEAT_SMOKY", "피트 스모키", "스카치", null, 1)
		group.putTag(grouped.id, TastingTagGroupRole.PERIPHERAL)
		tastingTagGroupRepository.save(group)

		val allUngroupedResult = getAs(accessToken, "/v1/tasting/tags/ungrouped")
		assertThat(allUngroupedResult).hasStatusOk()
		val allUngrouped = extractData(allUngroupedResult, Array<AdminTastingTagCatalogItem>::class.java)
		assertThat(allUngrouped.map { it.korName }).containsExactly("다다", "가나", "가나다")
		assertThat(allUngrouped.map { it.alcoholCount }).containsExactly(1L, 0L, 0L)
		assertThat(allUngrouped).allSatisfy { item -> assertThat(item.groups).isEmpty() }

		val matchedResult = getAs(accessToken, "/v1/tasting/tags/ungrouped?keyword=다")
		assertThat(matchedResult).hasStatusOk()
		val matched = extractData(matchedResult, Array<AdminTastingTagCatalogItem>::class.java)
		assertThat(matched.map { it.korName }).containsExactly("다다", "가나다")
		assertThat(child.id).isNotNull()

		val groupedNameResult = getAs(accessToken, "/v1/tasting/tags/ungrouped?keyword=나나")
		assertThat(groupedNameResult).hasStatusOk()
		val groupedName = extractData(groupedNameResult, Array<AdminTastingTagCatalogItem>::class.java)
		assertThat(groupedName).isEmpty()
	}

	@Test
	@DisplayName("아이콘만 조회할 때 있으면 문자열을 반환하고 없으면 null을 반환한다")
	fun findsIconOrNull() {
		val withIcon = tastingTagTestFactory.persistTastingTag("허니", "Honey")
		withIcon.update("허니", "Honey", "QUJD", withIcon.description, null)
		tastingTagRepository.save(withIcon)
		val withoutIcon = tastingTagTestFactory.persistTastingTag("오크", "Oak")

		val iconResult = getAs(accessToken, "/v1/tasting/tags/${withIcon.id}/icon")
		val emptyResult = getAs(accessToken, "/v1/tasting/tags/${withoutIcon.id}/icon")
		assertThat(iconResult).hasStatusOk()
		assertThat(emptyResult).hasStatusOk()
		val icon = extractData(iconResult, AdminTastingTagIconResponse::class.java)
		val empty = extractData(emptyResult, AdminTastingTagIconResponse::class.java)

		assertThat(icon.icon).isEqualTo("QUJD")
		assertThat(empty.icon).isNull()
	}

	@Test
	@DisplayName("없는 태그의 아이콘을 조회할 때 404를 반환한다")
	fun rejectsMissingTagIcon() {
		assertThat(getAs(accessToken, "/v1/tasting/tags/999999/icon")).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test
	@DisplayName("기존 목록에서 아이콘을 빼고 상세에는 아이콘을 유지한다")
	fun omitsIconFromLegacyList() {
		val tag = tastingTagTestFactory.persistTastingTag("아이콘태그", "Icon Tag")
		tag.update("아이콘태그", "Icon Tag", "QUJD", tag.description, null)
		tastingTagRepository.save(tag)

		val listResult = getAs(accessToken, "/v1/tasting-tags?keyword=아이콘태그")
		assertThat(listResult).hasStatusOk()
		assertThat(listResult.response.contentAsString).doesNotContain("QUJD")
		val rows = extractData(listResult, List::class.java)
		assertThat(rows).hasSize(1)
		val row = rows[0] as Map<*, *>
		assertThat(row["icon"]).isNull()
		assertThat(row["korName"]).isEqualTo("아이콘태그")

		val detail = getAs(accessToken, "/v1/tasting-tags/${tag.id}")
		assertThat(detail).hasStatusOk()
		assertThat(parseResponse(detail).success).isTrue()
		val detailTag = extractData(detail, Map::class.java)["tag"] as Map<*, *>
		assertThat(detailTag["icon"]).isEqualTo("QUJD")
	}

	@Test
	@DisplayName("인증 없이 경량 조회를 호출할 때 거절한다")
	fun rejectsUnauthenticatedCatalogRequest() {
		// admin은 AuthenticationEntryPoint가 없어 무토큰 요청이 403이다
		val result: MvcTestResult = mockMvcTester.get().uri("/v1/tasting/tags/all").exchange()
		assertThat(result).hasStatus(HttpStatus.FORBIDDEN)
	}
}

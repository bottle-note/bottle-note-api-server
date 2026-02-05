package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.alcohols.fixture.TastingTagTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin TastingTag API 통합 테스트")
class AdminTastingTagIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var tastingTagTestFactory: TastingTagTestFactory

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("테이스팅 태그 단건 조회 API")
	inner class GetTagDetail {

		@Test
		@DisplayName("테이스팅 태그 상세 정보를 조회할 수 있다")
		fun getTagDetailSuccess() {
			// given
			val tag = tastingTagTestFactory.persistTastingTag("허니", "Honey")

			// when & then
			assertThat(
				mockMvcTester.get().uri("/tasting-tags/${tag.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)

			assertThat(
				mockMvcTester.get().uri("/tasting-tags/${tag.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.tag.korName").isEqualTo("허니")
		}

		@Test
		@DisplayName("부모 태그가 있는 경우 마트료시카 구조로 조상 정보가 포함된다")
		fun getTagDetailWithParentChain() {
			// given - 3depth 트리 생성 (root -> middle -> leaf)
			val tree = tastingTagTestFactory.persistTastingTagTree()
			val leafTag = tree[2]

			// when & then - leaf 태그 조회 시 parent.parent 존재 확인
			assertThat(
				mockMvcTester.get().uri("/tasting-tags/${leafTag.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.tag.parent").isNotNull()

			assertThat(
				mockMvcTester.get().uri("/tasting-tags/${leafTag.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.tag.parent.parent").isNotNull()
		}

		@Test
		@DisplayName("연결된 위스키 목록이 포함된다")
		fun getTagDetailWithAlcohols() {
			// given
			val tag = tastingTagTestFactory.persistTastingTag()
			val alcohol = alcoholTestFactory.persistAlcohol()
			tastingTagTestFactory.linkAlcoholToTag(alcohol, tag)

			// when & then
			assertThat(
				mockMvcTester.get().uri("/tasting-tags/${tag.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.alcohols.length()").isEqualTo(1)
		}

		@Test
		@DisplayName("존재하지 않는 태그 조회 시 실패한다")
		fun getTagDetailNotFound() {
			// when & then
			assertThat(
				mockMvcTester.get().uri("/tasting-tags/999999")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("테이스팅 태그 생성 API")
	inner class CreateTag {

		@Test
		@DisplayName("테이스팅 태그를 생성할 수 있다")
		fun createTagSuccess() {
			// given
			val request = mapOf(
				"korName" to "새로운 태그",
				"engName" to "New Tag",
				"description" to "테스트 설명"
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/tasting-tags")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("TASTING_TAG_CREATED")
		}

		@Test
		@DisplayName("부모 태그를 지정하여 생성할 수 있다")
		fun createTagWithParent() {
			// given
			val parent = tastingTagTestFactory.persistTastingTag("부모 태그", "Parent Tag")
			val request = mapOf(
				"korName" to "자식 태그",
				"engName" to "Child Tag",
				"parentId" to parent.id
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/tasting-tags")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("TASTING_TAG_CREATED")
		}

		@Test
		@DisplayName("중복된 한글 이름으로 생성 시 실패한다")
		fun createTagDuplicateName() {
			// given
			tastingTagTestFactory.persistTastingTag("중복 태그", "Duplicate Tag")
			val request = mapOf(
				"korName" to "중복 태그",
				"engName" to "Another Tag"
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/tasting-tags")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}

		@Test
		@DisplayName("최대 깊이를 초과하는 태그 생성 시 실패한다")
		fun createTagExceedMaxDepth() {
			// given - 3depth 트리 생성 (root -> middle -> leaf)
			val tree = tastingTagTestFactory.persistTastingTagTree()
			val leafTag = tree[2]

			val request = mapOf(
				"korName" to "4depth 태그",
				"engName" to "4depth Tag",
				"parentId" to leafTag.id
			)

			// when & then - 4depth 생성 시도 시 실패
			assertThat(
				mockMvcTester.post().uri("/tasting-tags")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("테이스팅 태그 수정 API")
	inner class UpdateTag {

		@Test
		@DisplayName("테이스팅 태그를 수정할 수 있다")
		fun updateTagSuccess() {
			// given
			val tag = tastingTagTestFactory.persistTastingTag()
			val request = mapOf(
				"korName" to "수정된 태그",
				"engName" to "Updated Tag",
				"description" to "수정된 설명"
			)

			// when & then
			assertThat(
				mockMvcTester.put().uri("/tasting-tags/${tag.id}")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("TASTING_TAG_UPDATED")
		}

		@Test
		@DisplayName("다른 태그와 중복되는 이름으로 수정 시 실패한다")
		fun updateTagDuplicateName() {
			// given
			tastingTagTestFactory.persistTastingTag("기존 태그", "Existing Tag")
			val targetTag = tastingTagTestFactory.persistTastingTag("수정 대상", "Target Tag")

			val request = mapOf(
				"korName" to "기존 태그",
				"engName" to "Updated Tag"
			)

			// when & then
			assertThat(
				mockMvcTester.put().uri("/tasting-tags/${targetTag.id}")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}

		@Test
		@DisplayName("존재하지 않는 태그 수정 시 실패한다")
		fun updateTagNotFound() {
			// given
			val request = mapOf(
				"korName" to "수정된 태그",
				"engName" to "Updated Tag"
			)

			// when & then
			assertThat(
				mockMvcTester.put().uri("/tasting-tags/999999")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("테이스팅 태그 삭제 API")
	inner class DeleteTag {

		@Test
		@DisplayName("테이스팅 태그를 삭제할 수 있다")
		fun deleteTagSuccess() {
			// given
			val tag = tastingTagTestFactory.persistTastingTag()

			// when & then
			assertThat(
				mockMvcTester.delete().uri("/tasting-tags/${tag.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("TASTING_TAG_DELETED")
		}

		@Test
		@DisplayName("자식 태그가 존재하는 경우 삭제할 수 없다")
		fun deleteTagWithChildren() {
			// given
			val parent = tastingTagTestFactory.persistTastingTag("부모", "Parent")
			tastingTagTestFactory.persistTastingTagWithParent(parent)

			// when & then
			assertThat(
				mockMvcTester.delete().uri("/tasting-tags/${parent.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
		}

		@Test
		@DisplayName("연결된 위스키가 존재하는 경우 삭제할 수 없다")
		fun deleteTagWithAlcohols() {
			// given
			val tag = tastingTagTestFactory.persistTastingTag()
			val alcohol = alcoholTestFactory.persistAlcohol()
			tastingTagTestFactory.linkAlcoholToTag(alcohol, tag)

			// when & then
			assertThat(
				mockMvcTester.delete().uri("/tasting-tags/${tag.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
		}

		@Test
		@DisplayName("존재하지 않는 태그 삭제 시 실패한다")
		fun deleteTagNotFound() {
			// when & then
			assertThat(
				mockMvcTester.delete().uri("/tasting-tags/999999")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("테이스팅 태그 위스키 연결 API")
	inner class ManageAlcohols {

		@Test
		@DisplayName("위스키를 태그에 벌크로 연결할 수 있다")
		fun addAlcoholsToTagSuccess() {
			// given
			val tag = tastingTagTestFactory.persistTastingTag()
			val alcohol1 = alcoholTestFactory.persistAlcohol()
			val alcohol2 = alcoholTestFactory.persistAlcohol()

			val request = mapOf("alcoholIds" to listOf(alcohol1.id, alcohol2.id))

			// when & then
			assertThat(
				mockMvcTester.post().uri("/tasting-tags/${tag.id}/alcohols")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("TASTING_TAG_ALCOHOL_ADDED")
		}

		@Test
		@DisplayName("위스키 연결을 벌크로 해제할 수 있다")
		fun removeAlcoholsFromTagSuccess() {
			// given
			val tag = tastingTagTestFactory.persistTastingTag()
			val alcohol1 = alcoholTestFactory.persistAlcohol()
			val alcohol2 = alcoholTestFactory.persistAlcohol()
			tastingTagTestFactory.linkAlcoholToTag(alcohol1, tag)
			tastingTagTestFactory.linkAlcoholToTag(alcohol2, tag)

			val request = mapOf("alcoholIds" to listOf(alcohol1.id))

			// when & then
			assertThat(
				mockMvcTester.delete().uri("/tasting-tags/${tag.id}/alcohols")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("TASTING_TAG_ALCOHOL_REMOVED")
		}

		@Test
		@DisplayName("존재하지 않는 위스키 연결 시 실패한다")
		fun addAlcoholsNotFound() {
			// given
			val tag = tastingTagTestFactory.persistTastingTag()
			val request = mapOf("alcoholIds" to listOf(999999L))

			// when & then
			assertThat(
				mockMvcTester.post().uri("/tasting-tags/${tag.id}/alcohols")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
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
			assertThat(mockMvcTester.get().uri("/tasting-tags/1"))
				.hasStatus4xxClientError()

			assertThat(
				mockMvcTester.post().uri("/tasting-tags")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(mapOf("korName" to "테스트", "engName" to "Test")))
			)
				.hasStatus4xxClientError()
		}
	}
}

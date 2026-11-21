package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Tag("admin_integration")
class AppAssemblyMergeRolesIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var alcohols: AlcoholTestFactory

	@Autowired
	private lateinit var applicationContext: org.springframework.context.ApplicationContext

	@Test
	@DisplayName("Admin 앱에 병합 handler 여섯 종류를 빠짐없이 조립한다")
	fun assemblesEveryMergeHandlerType() {
		assertThat(
			applicationContext.getBeansOfType(app.bottlenote.alcohols.domain.AlcoholMergeHandler::class.java)
				.values.map { org.springframework.aop.support.AopUtils.getTargetClass(it).name }
		)
			.containsExactlyInAnyOrder(
				"app.bottlenote.alcohols.repository.JdbcAlcoholMergeHandler",
				"app.bottlenote.history.repository.JdbcHistoryAlcoholMergeHandler",
				"app.bottlenote.mfds.repository.JdbcMfdsAlcoholMergeHandler",
				"app.bottlenote.picks.repository.JdbcPicksAlcoholMergeHandler",
				"app.bottlenote.rating.repository.JdbcRatingAlcoholMergeHandler",
				"app.bottlenote.review.repository.JdbcReviewAlcoholMergeHandler"
			)
	}

	@Test
	@DisplayName("관리자가 병합할 때 모든 도메인의 처리 역할과 실행자 이메일을 결과에 남긴다")
	fun merge_reportsEveryDomainRoleAndAuthenticatedAdmin() {
		// given
		val admin = adminUserTestFactory.persistRootAdmin()
		val base = alcohols.persistAlcohol()
		val duplicate = alcohols.persistAlcohol()
		val token = getAccessToken(admin)

		// when
		val result = postAs(token, "/v1/alcohols/merges", mapOf("baseAlcoholId" to base.id, "duplicateAlcoholIds" to listOf(duplicate.id)))
		assertThat(result).hasStatusOk()
		val logs = mockMvcTester.get().uri("/v1/alcohols/merges").header("Authorization", "Bearer $token").exchange()

		// then
		assertThat(logs).hasStatusOk()
		val log = mapper.readTree(logs.response.contentAsString).path("data").single()
		assertThat(log.path("mergedByEmail").asText()).isEqualTo(admin.email)
		assertThat(log.path("counts").map { it.path("name").asText() }).containsExactlyInAnyOrder(
			"tastingTags", "attributes", "curationKeywords", "mfdsDeclarations", "reviews", "ratings", "picks", "userHistories", "viewHistories"
		)
	}
}

package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.history.constant.EventType
import app.bottlenote.history.fixture.AlcoholsViewHistoryTestFactory
import app.bottlenote.history.fixture.HistoryTestFactory
import app.bottlenote.mfds.constant.MfdsNormalizationStatus
import app.bottlenote.mfds.fixture.MfdsTestFactory
import app.bottlenote.picks.constant.PicksStatus
import app.bottlenote.picks.fixture.PicksTestFactory
import app.bottlenote.rating.fixture.RatingTestFactory
import app.bottlenote.review.constant.BestReviewChangeType
import app.bottlenote.review.fixture.BestReviewSelectionLogTestFactory
import app.bottlenote.review.fixture.ReviewTestFactory
import app.bottlenote.user.domain.User
import app.bottlenote.user.fixture.UserTestFactory
import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

@Tag("admin_integration")
@DisplayName("[integration] Admin 중복 위스키 병합 API 통합 테스트")
class AdminAlcoholMergeIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	@Autowired
	private lateinit var reviewTestFactory: ReviewTestFactory

	@Autowired
	private lateinit var ratingTestFactory: RatingTestFactory

	@Autowired
	private lateinit var picksTestFactory: PicksTestFactory

	@Autowired
	private lateinit var historyTestFactory: HistoryTestFactory

	@Autowired
	private lateinit var viewHistoryTestFactory: AlcoholsViewHistoryTestFactory

	@Autowired
	private lateinit var mfdsTestFactory: MfdsTestFactory

	@Autowired
	private lateinit var selectionLogTestFactory: BestReviewSelectionLogTestFactory

	@Autowired
	private lateinit var jdbc: JdbcTemplate

	private lateinit var accessToken: String
	private lateinit var target: Alcohol
	private lateinit var source: Alcohol
	private lateinit var both: User

	/** both는 기준 위스키와 중복 위스키 모두에, sourceOnly는 중복 위스키에만 데이터가 있다. */
	@BeforeEach
	fun setUp() {
		accessToken = getAccessToken(adminUserTestFactory.persistRootAdmin())
		target = alcoholTestFactory.persistAlcohol()
		source = alcoholTestFactory.persistAlcohol()
		both = userTestFactory.persistUser()
		val sourceOnly = userTestFactory.persistUser()
		seed(both, target)
		seed(both, source)
		seed(sourceOnly, source)
	}

	@Test
	@DisplayName("병합할 때 중복 위스키의 데이터를 기준으로 옮기고 겹치는 별점과 찜과 중복 위스키를 삭제한다")
	fun merge_movesReferencesAndDiscardsConflicts() {
		// when
		val result = postAs(accessToken, MERGE_URI, request(target, source))

		// then
		assertThat(result).hasStatusOk()
		REFERENCE_TABLES.forEach { table ->
			assertThat(countOf(table, source)).`as`(table).isZero()
		}
		assertThat(countOf("reviews", target)).isEqualTo(3)
		assertThat(countOf("ratings", target)).isEqualTo(2)
		assertThat(countOf("picks", target)).isEqualTo(2)
		assertThat(countOf("user_histories", target)).isEqualTo(3)
		assertThat(countOf("alcohols_view_histories", target)).isEqualTo(2)
		assertThat(redirectUrlsOf(target)).containsOnly("/search/all/${target.id}")
		assertThat(exists(source)).isFalse()

		val log = getLogs("sourceAlcoholId" to source.id.toString()).get("data").single()
		assertThat(log.get("targetAlcoholId").asLong()).isEqualTo(target.id)
		assertThat(log.get("sourceKorName").asText()).isEqualTo(source.korName)
		assertThat(log.get("mergedByEmail").isNull).isFalse()
		assertThat(countsOf(log.get("counts"), "ratings")).isEqualTo(1 to 1)
		assertThat(countsOf(log.get("counts"), "picks")).isEqualTo(1 to 1)
		assertThat(countsOf(log.get("counts"), "reviews")).isEqualTo(2 to 0)
	}

	@Test
	@DisplayName("미리보기는 데이터를 바꾸지 않고 여러 위스키를 병합한 결과와 같은 건수를 돌려준다")
	fun preview_matchesMergeWithoutChanges() {
		// given
		val secondSource = alcoholTestFactory.persistDeletedAlcohol()
		seed(both, secondSource)
		selectionLogTestFactory.persistLog(reviewTestFactory.persistReview(both, secondSource), LocalDate.of(2026, 11, 1), BestReviewChangeType.SELECTED)
		listOf(target, secondSource).forEach {
			jdbc.update("INSERT INTO alcohol_interest_observations(alcohol_id, bucket_granularity, bucket_at, observed_at) VALUES(?, 'HOUR', '2026-11-01 00:00:00', NOW())", it.id)
		}
		val body = mergeBody(target.id, listOf(source.id, secondSource.id))
		val before = snapshot()

		// when
		val preview = postAs(accessToken, "$MERGE_URI/preview", body)

		// then
		assertThat(preview).hasStatusOk()
		assertThat(snapshot()).isEqualTo(before)
		assertThat(getLogs().get("data")).isEmpty()

		assertThat(postAs(accessToken, MERGE_URI, body)).hasStatusOk()
		val previewCounts = mapper.readTree(preview.response.contentAsString).at("/data/sources").map { it.get("counts") }
		val logs = getLogs().get("data").reversed()
		assertThat(logs.map { it.get("counts") }).isEqualTo(previewCounts)
		assertThat(countsOf(previewCounts.last(), "ratings")).isEqualTo(0 to 1)
		assertThat(logs.map { it.get("mergeGroupId").asText() }.distinct()).hasSize(1)
		assertThat(listOf(source, secondSource).map { exists(it) }).containsOnly(false)
		assertThat(countOf("alcohol_interest_observations", secondSource)).isZero()
		assertThat(countOf("alcohol_interest_observations", target)).isEqualTo(1)
		assertThat(countOf("best_review_selection_logs", target)).isEqualTo(1)
	}

	@Test
	@DisplayName("기준 위스키의 기록이 0점 별점이거나 해제된 찜일 때 중복 위스키의 유효한 기록을 남긴다")
	fun merge_keepsActiveRatingsAndPicks() {
		// given
		val users = (1..4).map { userTestFactory.persistUser() }
		ratingTestFactory.persistRating(users[0], target, 0)
		ratingTestFactory.persistRating(users[0], source, 5)
		ratingTestFactory.persistRating(users[1], target, 3)
		ratingTestFactory.persistRating(users[1], source, 0)
		picksTestFactory.persistPicks(target.id, users[2].id, PicksStatus.UNPICK)
		picksTestFactory.persistPicks(source.id, users[2].id, PicksStatus.PICK)
		picksTestFactory.persistPicks(target.id, users[3].id, PicksStatus.PICK)
		picksTestFactory.persistPicks(source.id, users[3].id, PicksStatus.UNPICK)

		// when
		assertThat(postAs(accessToken, MERGE_URI, request(target, source))).hasStatusOk()

		// then
		val counts = getLogs().get("data").single().get("counts")
		assertThat(countsOf(counts, "ratings")).isEqualTo(2 to 3)
		assertThat(countsOf(counts, "picks")).isEqualTo(2 to 3)
		assertThat(valuesOf("SELECT rating FROM ratings WHERE alcohol_id = ? AND user_id = ?", target, users[0])).containsExactly("5.0")
		assertThat(valuesOf("SELECT rating FROM ratings WHERE alcohol_id = ? AND user_id = ?", target, users[1])).containsExactly("3.0")
		assertThat(valuesOf("SELECT status FROM picks WHERE alcohol_id = ? AND user_id = ?", target, users[2])).containsExactly("PICK")
		assertThat(valuesOf("SELECT status FROM picks WHERE alcohol_id = ? AND user_id = ?", target, users[3])).containsExactly("PICK")
	}

	@Test
	@DisplayName("병합할 때 태그와 복수 값 속성은 합치고 단일 값 속성은 기준값을 유지하며 수입 신고 확정 연결을 옮긴다")
	fun merge_movesMetadataAndConfirmedMfds() {
		// given
		val sharedTag = alcoholTestFactory.appendTastingTag(target, null).first().tastingTag
		jdbc.update("INSERT INTO alcohols_tasting_tags(alcohol_id,tasting_tag_id) VALUES(?,?)", source.id, sharedTag.id)
		alcoholTestFactory.appendTastingTag(source, null)
		jdbc.update(
			"""
			INSERT INTO alcohol_attributes(id, attribute_key, display_name, attribute_type, datatype, allow_multiple_values)
			VALUES (9001, 'merge_single', '단일', 'PRICE', 'STRING', FALSE),
			       (9002, 'merge_multi', '복수', 'PRICE', 'STRING', TRUE)
			""".trimIndent()
		)
		listOf(
			Triple(target, 9001, "기준값"),
			Triple(source, 9001, "중복값"),
			Triple(target, 9002, "공통"),
			Triple(source, 9002, "공통"),
			Triple(source, 9002, "추가")
		).forEach { (alcohol, attributeId, value) ->
			jdbc.update("INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(?,?,?)", alcohol.id, attributeId, value)
		}
		alcoholTestFactory.persistCurationKeyword("공통 키워드", "설명", setOf(target.id, source.id))
		alcoholTestFactory.persistCurationKeyword("중복 전용 키워드", "설명", setOf(source.id))
		val declaration = mfdsTestFactory.persistDeclaration("MERGE-RCNO-1", MfdsNormalizationStatus.NORMALIZED, null, source.id, null)

		// when
		assertThat(postAs(accessToken, MERGE_URI, request(target, source))).hasStatusOk()

		// then
		assertThat(countOf("alcohols_tasting_tags", source)).isZero()
		assertThat(countOf("alcohols_tasting_tags", target)).isEqualTo(2)
		assertThat(countOf("alcohol_attribute_mappings", source)).isZero()
		assertThat(jdbc.queryForList("SELECT attribute_value FROM alcohol_attribute_mappings WHERE alcohol_id = ? ORDER BY attribute_id, attribute_value", String::class.java, target.id))
			.containsExactly("기준값", "공통", "추가")
		assertThat(jdbc.queryForList("SELECT alcohol_ids FROM curation_keyword_alcohol_ids", Long::class.javaObjectType))
			.containsExactly(target.id, target.id)
		assertThat(jdbc.queryForObject("SELECT selected_alcohol_id FROM mfds_declarations WHERE id = ?", Long::class.javaObjectType, declaration.id))
			.isEqualTo(target.id)
	}

	@Test
	@DisplayName("잘못된 병합 요청은 거절하고 함께 요청한 위스키도 병합하지 않는다")
	fun merge_rejectsInvalidRequestsAtomically() {
		// given
		val merged = alcoholTestFactory.persistAlcohol()
		assertThat(postAs(accessToken, MERGE_URI, request(target, merged))).hasStatusOk()
		val deleted = alcoholTestFactory.persistDeletedAlcohol()
		val failing = alcoholTestFactory.persistAlcohol()
		jdbc.update(
			"""
			INSERT INTO alcohol_merge_logs(merge_group_id, target_alcohol_id, target_kor_name, source_alcohol_id, source_kor_name, source_eng_name, counts)
			VALUES ('conflict', ?, '기준', ?, '중복', 'duplicate', '[]')
			""".trimIndent(),
			target.id,
			failing.id
		)

		// when & then
		assertThat(postAs(accessToken, MERGE_URI, request(target, target))).hasStatus(400)
		assertThat(postAs(accessToken, MERGE_URI, request(deleted, source))).hasStatus(400)
		assertThat(postAs(accessToken, MERGE_URI, mergeBody(target.id, listOf(source.id, source.id)))).hasStatus(400)
		val emptySources = postAs(accessToken, MERGE_URI, mergeBody(target.id, emptyList()))
		assertThat(emptySources).hasStatus(400)
		assertThat(mapper.readTree(emptySources.response.contentAsString).at("/errors/0/code").asText()).isEqualTo("ALCOHOL_MERGE_SOURCES_SIZE")
		assertThat(postAs(accessToken, MERGE_URI, mergeBody(target.id, (1L..11L).toList()))).hasStatus(400)
		assertThat(postAs(accessToken, MERGE_URI, mergeBody(target.id, listOf(-1L)))).hasStatus(404)
		assertThat(postAs(accessToken, MERGE_URI, mergeBody(target.id, listOf(source.id, merged.id)))).hasStatus(404)
		// 앞선 위스키를 지운 뒤 다음 위스키의 로그 저장이 실패해도 전부 되돌린다
		assertThat(postAs(accessToken, MERGE_URI, mergeBody(target.id, listOf(source.id, failing.id))).response.status).isGreaterThanOrEqualTo(400)
		assertThat(countOf("reviews", source)).isEqualTo(2)
		assertThat(exists(source)).isTrue()
	}

	@Test
	@DisplayName("병합 로그를 최근 순으로 조회하고 기준 위스키로 좁힌다")
	fun searchLogs_ordersByLatestAndFilters() {
		// given
		val another = alcoholTestFactory.persistAlcohol()
		val anotherSource = alcoholTestFactory.persistAlcohol()
		assertThat(postAs(accessToken, MERGE_URI, request(target, source))).hasStatusOk()
		assertThat(postAs(accessToken, MERGE_URI, request(another, anotherSource))).hasStatusOk()

		// when & then
		assertThat(getLogs().get("data").map { it.get("sourceAlcoholId").asLong() })
			.containsExactly(anotherSource.id, source.id)
		assertThat(getLogs("targetAlcoholId" to target.id.toString()).get("data")).hasSize(1)
		assertThat(getLogs("size" to "1").at("/meta/totalElements").asLong()).isEqualTo(2)
	}

	private fun seed(
		user: User,
		alcohol: Alcohol
	) {
		reviewTestFactory.persistReview(user, alcohol)
		ratingTestFactory.persistRating(user, alcohol, 4)
		picksTestFactory.persistPicks(alcohol.id, user.id, PicksStatus.PICK)
		historyTestFactory.persistUserHistory(user.id, EventType.IS_PICK, alcohol.id, "/search/all/${alcohol.id}", null, null)
		viewHistoryTestFactory.persistAlcoholsViewHistory(user.id, alcohol.id)
	}

	private fun request(
		target: Alcohol,
		source: Alcohol
	) = mergeBody(target.id, listOf(source.id))

	private fun mergeBody(
		targetAlcoholId: Long,
		sourceAlcoholIds: List<Long>
	) = mapOf("targetAlcoholId" to targetAlcoholId, "sourceAlcoholIds" to sourceAlcoholIds)

	private fun getLogs(vararg params: Pair<String, String>): JsonNode {
		val request = mockMvcTester.get().uri(MERGE_URI).header("Authorization", "Bearer $accessToken")
		params.forEach { request.param(it.first, it.second) }

		val result = request.exchange()
		assertThat(result).hasStatusOk()
		return mapper.readTree(result.response.contentAsString)
	}

	private fun countOf(
		table: String,
		alcohol: Alcohol
	): Int = jdbc.queryForObject("SELECT COUNT(*) FROM $table WHERE alcohol_id = ?", Int::class.java, alcohol.id)!!

	private fun redirectUrlsOf(alcohol: Alcohol): List<String> = jdbc.queryForList("SELECT redirect_url FROM user_histories WHERE alcohol_id = ?", String::class.java, alcohol.id)

	private fun exists(alcohol: Alcohol): Boolean = jdbc.queryForObject("SELECT COUNT(*) FROM alcohols WHERE id = ?", Int::class.java, alcohol.id) == 1

	private fun valuesOf(
		sql: String,
		alcohol: Alcohol,
		user: User
	): List<String> = jdbc.queryForList(sql, String::class.java, alcohol.id, user.id)

	/** 미리보기가 건드리는 사용자 데이터의 전체 상태. */
	private fun snapshot(): List<List<Map<String, Any?>>> = REFERENCE_TABLES.map { jdbc.queryForList("SELECT * FROM $it ORDER BY alcohol_id, user_id") } +
		listOf(
			jdbc.queryForList("SELECT id, deleted_at FROM alcohols ORDER BY id"),
			jdbc.queryForList("SELECT * FROM alcohol_interest_observations ORDER BY alcohol_id"),
			jdbc.queryForList("SELECT * FROM best_review_selection_logs ORDER BY id")
		)

	private fun countsOf(
		counts: JsonNode,
		name: String
	): Pair<Int, Int> = counts.single { it.get("name").asText() == name }.let { it.get("moved").asInt() to it.get("discarded").asInt() }

	companion object {
		private const val MERGE_URI = "/v1/alcohols/merges"
		private val REFERENCE_TABLES = listOf("reviews", "ratings", "picks", "user_histories", "alcohols_view_histories")
	}
}

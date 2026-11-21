package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.domain.AlcoholDeleteGuard
import app.bottlenote.alcohols.domain.AlcoholQueryRepository
import app.bottlenote.alcohols.exception.AlcoholException
import app.bottlenote.alcohols.exception.AlcoholExceptionCode
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.alcohols.service.AdminAlcoholCommandService
import app.bottlenote.global.config.jpa.AuditorAwareImpl
import app.bottlenote.picks.fixture.PicksTestFactory
import app.bottlenote.rating.fixture.RatingTestFactory
import app.bottlenote.rating.service.RatingAlcoholDeleteGuard
import app.bottlenote.review.fixture.ReviewTestFactory
import app.bottlenote.review.service.ReviewAlcoholDeleteGuard
import app.bottlenote.user.domain.AdminUser
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.hibernate.Session
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.annotation.OrderUtils
import org.springframework.data.domain.AuditorAware
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.test.util.AopTestUtils
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.sql.Connection

@Tag("admin_integration")
@DisplayName("[integration] Admin 주류 경계 조립과 삭제 거절·상세 효과")
class AppAssemblyAlcoholBoundaryIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var context: ApplicationContext

	@Autowired
	private lateinit var alcoholQueryRepository: AlcoholQueryRepository

	@Autowired
	private lateinit var alcohols: AlcoholTestFactory

	@Autowired
	private lateinit var users: UserTestFactory

	@Autowired
	private lateinit var reviews: ReviewTestFactory

	@Autowired
	private lateinit var ratings: RatingTestFactory

	@Autowired
	private lateinit var picks: PicksTestFactory

	@Autowired
	private lateinit var adminAlcoholCommandService: AdminAlcoholCommandService

	@Autowired
	private lateinit var transactionManager: PlatformTransactionManager

	@Autowired
	private lateinit var jdbc: JdbcTemplate

	private lateinit var admin: AdminUser

	private lateinit var token: String

	private val deleteTransactionName = AdminAlcoholCommandService::class.java.name + ".deleteAlcohol"

	@BeforeEach
	fun setUp() {
		admin = adminUserTestFactory.persistRootAdmin()
		token = getAccessToken(admin)
	}

	@Test
	@DisplayName("관리자 앱을 조립할 때 주류 삭제 가드를 리뷰와 별점 순서로 등록하고 감사자를 하나만 등록한다")
	fun context_registersDeleteGuardsInOrderAndSingleAuditor() {
		// when
		val orderedGuards = context.getBeanProvider(AlcoholDeleteGuard::class.java).orderedStream().toList()
		val guards = orderedGuards.map { AopUtils.getTargetClass(it) }
		val auditors = context.getBeansOfType(AuditorAware::class.java).values.map { AopUtils.getTargetClass(it) }

		// then
		assertThat(guards).containsExactly(ReviewAlcoholDeleteGuard::class.java, RatingAlcoholDeleteGuard::class.java)
		assertThat(auditors).containsExactly(AuditorAwareImpl::class.java)
	}

	@Test
	@DisplayName("리뷰와 별점이 모두 있는 주류를 삭제할 때 리뷰 사유로 거절하고 주류를 남긴다")
	fun delete_withReviewAndRating_rejectsByReviewFirst() {
		// given
		val user = users.persistUser()
		val alcohol = alcohols.persistAlcohol()
		ratings.persistRating(user.id, alcohol.id, 5)
		reviews.persistReview(user.id, alcohol.id)

		// when
		val result = deleteAs(token, "/v1/alcohols/${alcohol.id}")

		// then
		assertThat(result).hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.errors[0].code").isEqualTo("ALCOHOL_HAS_REVIEWS")
		assertThat(alcoholQueryRepository.findById(alcohol.id).orElseThrow().isDeleted).isFalse()
	}

	@Test
	@DisplayName("별점만 있는 주류를 삭제할 때 별점 사유로 거절하고 주류를 남긴다")
	fun delete_withRatingOnly_rejectsByRating() {
		// given
		val user = users.persistUser()
		val alcohol = alcohols.persistAlcohol()
		ratings.persistRating(user.id, alcohol.id, 5)

		// when
		val result = deleteAs(token, "/v1/alcohols/${alcohol.id}")

		// then
		assertThat(result).hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.errors[0].code").isEqualTo("ALCOHOL_HAS_RATINGS")
		assertThat(alcoholQueryRepository.findById(alcohol.id).orElseThrow().isDeleted).isFalse()
	}

	@Test
	@DisplayName("리뷰와 별점이 없는 주류를 삭제할 때 다른 주류의 참조와 무관하게 삭제한다")
	fun delete_withoutReferences_deletesAlcohol() {
		// given
		val user = users.persistUser()
		val alcohol = alcohols.persistAlcohol()
		val other = alcohols.persistAlcohol()
		reviews.persistReview(user.id, other.id)
		ratings.persistRating(user.id, other.id, 5)

		// when
		val result = deleteAs(token, "/v1/alcohols/${alcohol.id}")

		// then
		assertThat(result).hasStatusOk().bodyJson().extractingPath("$.data.code").isEqualTo("ALCOHOL_DELETED")
		assertThat(alcoholQueryRepository.findById(alcohol.id).orElseThrow().isDeleted).isTrue()
		assertThat(alcoholQueryRepository.findById(other.id).orElseThrow().isDeleted).isFalse()
	}

	@Test
	@DisplayName("별점과 리뷰와 찜이 하나씩 있는 주류의 상세를 조회할 때 집계 값을 함께 응답한다")
	fun detail_returnsAggregatesOfRatingReviewAndPick() {
		// given
		val user = users.persistUser()
		val alcohol = alcohols.persistAlcohol()
		ratings.persistRating(user.id, alcohol.id, 4)
		reviews.persistReview(user.id, alcohol.id)
		picks.persistPicks(alcohol.id, user.id)

		// when
		val result = getAs(token, "/v1/alcohols/${alcohol.id}")

		// then
		assertThat(result).hasStatusOk()
		val data = mapper.readTree(result.response.contentAsString).path("data")
		assertThat(data.path("alcoholId").asLong()).isEqualTo(alcohol.id)
		assertThat(data.path("avgRating").asDouble()).isEqualTo(4.0)
		assertThat(data.path("totalRatingsCount").asLong()).isEqualTo(1L)
		assertThat(data.path("reviewCount").asLong()).isEqualTo(1L)
		assertThat(data.path("pickCount").asLong()).isEqualTo(1L)
	}

	@Test
	@DisplayName("삭제된 주류의 상세를 조회할 때 삭제 여부와 관계없이 응답한다")
	fun detail_ofDeletedAlcohol_isStillReturned() {
		// given
		val alcohol = alcohols.persistDeletedAlcohol()

		// when
		val result = getAs(token, "/v1/alcohols/${alcohol.id}")

		// then
		assertThat(result).hasStatusOk()
		val data = mapper.readTree(result.response.contentAsString).path("data")
		assertThat(data.path("alcoholId").asLong()).isEqualTo(alcohol.id)
	}

	@Test
	@DisplayName("관리자가 증류소를 생성할 때 생성과 수정 감사 컬럼에 관리자 주체를 기록한다")
	fun create_recordsAdminAuditorInAuditColumns() {
		// when
		val result = postAs(token, "/v1/distilleries", mapOf("korName" to "감사 증류소", "engName" to "Audit Distillery"))

		// then
		assertThat(result).hasStatusOk()
		val distilleryId = mapper.readTree(result.response.contentAsString).path("data").path("targetId").asLong()
		val audit =
			jdbc.queryForMap(
				"SELECT create_principal_id, create_principal_type, create_principal_email, " +
					"last_modify_principal_id, last_modify_principal_type, last_modify_principal_email " +
					"FROM distilleries WHERE id = ?",
				distilleryId
			)
		assertThat(audit["create_principal_type"]).isEqualTo("ADMIN")
		assertThat((audit["create_principal_id"] as Number).toLong()).isEqualTo(admin.id)
		assertThat(audit["create_principal_email"]).isEqualTo(admin.email)
		assertThat(audit["last_modify_principal_type"]).isEqualTo("ADMIN")
		assertThat((audit["last_modify_principal_id"] as Number).toLong()).isEqualTo(admin.id)
		assertThat(audit["last_modify_principal_email"]).isEqualTo(admin.email)
	}

	@Test
	@DisplayName("별점이 있는 주류를 삭제할 때 두 가드가 서비스가 연 같은 트랜잭션에서 삭제 전에 실행되고 거절과 함께 롤백한다")
	fun delete_rejectedByRating_runsBothGuardsInCallerTransactionAndRollsBack() {
		// given
		val user = users.persistUser()
		val alcohol = alcohols.persistAlcohol()
		ratings.persistRating(user.id, alcohol.id, 5)
		val probe = GuardTransactionProbe(transactionResourceKey())

		// when
		val thrown = withRecordingGuards(probe) { catchThrowable { adminAlcoholCommandService.deleteAlcohol(alcohol.id) } }

		// then
		assertThat(thrown).isInstanceOf(AlcoholException::class.java)
		assertThat((thrown as AlcoholException).exceptionCode).isEqualTo(AlcoholExceptionCode.ALCOHOL_HAS_RATINGS)
		assertGuardsSharedCallerTransaction(probe)
		assertThat(probe.completions)
			.containsExactly(TransactionSynchronization.STATUS_ROLLED_BACK, TransactionSynchronization.STATUS_ROLLED_BACK)
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
		assertThat(alcoholQueryRepository.findById(alcohol.id).orElseThrow().isDeleted).isFalse()
	}

	@Test
	@DisplayName("참조가 없는 주류를 삭제할 때 두 가드가 실행된 서비스 트랜잭션이 삭제와 함께 커밋된다")
	fun delete_withoutReferences_commitsCallerTransactionSharedWithGuards() {
		// given
		val alcohol = alcohols.persistAlcohol()
		val probe = GuardTransactionProbe(transactionResourceKey())

		// when
		val result = withRecordingGuards(probe) { adminAlcoholCommandService.deleteAlcohol(alcohol.id) }

		// then
		assertThat(result.targetId()).isEqualTo(alcohol.id)
		assertGuardsSharedCallerTransaction(probe)
		assertThat(probe.completions)
			.containsExactly(TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_COMMITTED)
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
		assertThat(alcoholQueryRepository.findById(alcohol.id).orElseThrow().isDeleted).isTrue()
	}

	/** JpaTransactionManager가 트랜잭션 자원을 묶는 키(EntityManagerFactory)를 돌려준다. */
	private fun transactionResourceKey(): Any {
		assertThat(transactionManager).isInstanceOf(JpaTransactionManager::class.java)
		return requireNotNull((transactionManager as JpaTransactionManager).entityManagerFactory)
	}

	/** 실제 서비스 bean이 가진 실제 가드를 위임형 기록 가드로 잠시 감싸고, 실제 Spring proxy 호출이 끝나면 되돌린다. */
	private fun <T> withRecordingGuards(
		probe: GuardTransactionProbe,
		action: () -> T
	): T {
		assertThat(AopUtils.isAopProxy(adminAlcoholCommandService)).isTrue()
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
		val service = AopTestUtils.getTargetObject<AdminAlcoholCommandService>(adminAlcoholCommandService)

		@Suppress("UNCHECKED_CAST")
		val realGuards = ReflectionTestUtils.getField(service, "deleteGuards") as List<AlcoholDeleteGuard>
		ReflectionTestUtils.setField(service, "deleteGuards", realGuards.map { probe.recording(it) })
		try {
			return action()
		} finally {
			ReflectionTestUtils.setField(service, "deleteGuards", realGuards)
		}
	}

	private fun assertGuardsSharedCallerTransaction(probe: GuardTransactionProbe) {
		val calls = probe.calls
		assertThat(calls.map { it.guard }).containsExactly(ReviewAlcoholDeleteGuard::class.java, RatingAlcoholDeleteGuard::class.java)
		calls.forEach { call ->
			assertThat(call.transactionActive).isTrue()
			assertThat(call.transactionName).isEqualTo(deleteTransactionName)
			assertThat(call.entityManager).isNotNull()
			assertThat(call.connection).isNotNull()
			assertThat(call.alcoholDeleted).isFalse()
		}
		assertThat(calls[0].entityManager).isSameAs(calls[1].entityManager)
		assertThat(calls[0].connection).isSameAs(calls[1].connection)
	}

	/** 가드 진입 시점의 트랜잭션 상태와 트랜잭션에 묶인 EntityManager, JDBC 연결을 기록한다. */
	private data class GuardCall(
		val guard: Class<*>,
		val transactionActive: Boolean,
		val transactionName: String?,
		val entityManager: Any?,
		val connection: Any?,
		val alcoholDeleted: Boolean?
	)

	private class GuardTransactionProbe(
		private val resourceKey: Any
	) {
		val calls = mutableListOf<GuardCall>()
		val completions = mutableListOf<Int>()

		/** 실제 가드의 선언 순서를 그대로 따르고 호출을 기록한 뒤 실제 가드에 위임한다. */
		fun recording(real: AlcoholDeleteGuard): AlcoholDeleteGuard = object : AlcoholDeleteGuard, Ordered {
			override fun getOrder(): Int = OrderUtils.getOrder(AopUtils.getTargetClass(real)) ?: Ordered.LOWEST_PRECEDENCE

			override fun verifyDeletable(alcoholId: Long) {
				record(real, alcoholId)
				real.verifyDeletable(alcoholId)
			}
		}

		private fun record(
			real: AlcoholDeleteGuard,
			alcoholId: Long
		) {
			val holder = TransactionSynchronizationManager.getResource(resourceKey) as? EntityManagerHolder
			val entityManager = holder?.entityManager
			calls +=
				GuardCall(
					guard = AopUtils.getTargetClass(real),
					transactionActive = TransactionSynchronizationManager.isActualTransactionActive(),
					transactionName = TransactionSynchronizationManager.getCurrentTransactionName(),
					entityManager = entityManager,
					connection = entityManager?.unwrap(Session::class.java)?.doReturningWork<Connection> { it },
					alcoholDeleted = entityManager?.find(Alcohol::class.java, alcoholId)?.isDeleted
				)
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.registerSynchronization(
					object : TransactionSynchronization {
						override fun afterCompletion(status: Int) {
							completions += status
						}
					}
				)
			}
		}
	}
}

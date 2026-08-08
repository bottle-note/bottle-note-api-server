package app.bottlenote.accesscontrol.presentation

import app.bottlenote.accesscontrol.constant.IpBanStatus
import app.bottlenote.accesscontrol.constant.ProjectionStatus
import app.bottlenote.accesscontrol.constant.SignalVerdict
import app.bottlenote.accesscontrol.dto.request.IpSecuritySignalReportRequest
import app.bottlenote.accesscontrol.dto.response.IpBanCommandResponse
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse
import app.bottlenote.accesscontrol.dto.response.IpBanEventResponse
import app.bottlenote.accesscontrol.dto.response.IpBanSummaryResponse
import app.bottlenote.accesscontrol.exception.IpBanException
import app.bottlenote.accesscontrol.exception.IpBanExceptionCode
import app.bottlenote.accesscontrol.facade.IpBanFacade
import app.bottlenote.accesscontrol.facade.IpSecuritySignalFacade
import app.bottlenote.accesscontrol.presentation.docs.AdminAccessControlApiDocs
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.global.security.accesscontrol.ClientIpResolver
import app.bottlenote.user.exception.UserException
import app.bottlenote.user.exception.UserExceptionCode
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDateTime

@RestController
// AdminApiVersionConfig가 app.bottlenote.*.presentation 컨트롤러에 /v1 prefix를 붙인다
@RequestMapping("/access-control/ip-bans")
@AdminAccessControlApiDocs.ApiTag
@ConditionalOnProperty(
	prefix = "bottlenote.access-control",
	name = ["enabled"],
	havingValue = "true",
	matchIfMissing = true
)
class AdminAccessControlController(
	private val ipBanFacade: IpBanFacade,
	private val ipSecuritySignalFacade: IpSecuritySignalFacade
) {
	@AdminAccessControlApiDocs.Ban
	@PostMapping
	fun ban(@Valid @RequestBody request: IpBanRequest): ResponseEntity<GlobalResponse> {
		val result = ipBanFacade.ban(
			request.ip,
			Duration.ofSeconds(request.ttlSeconds.toLong()),
			request.reason?.takeIf { it.isNotBlank() } ?: "manual",
			requiredAdminId()
		)
		return commandResponse(result)
	}

	/** `ip`이 있으면 단건, 없으면 DB의 활성 차단 목록을 조회한다. */
	@AdminAccessControlApiDocs.GetOrList
	@GetMapping
	fun getOrList(
		@RequestParam(required = false) ip: String?,
		@RequestParam(required = false, defaultValue = "100") max: Int
	): ResponseEntity<GlobalResponse> {
		if (!ip.isNullOrBlank()) {
			val normalizedIp = normalizeIp(ip)
			val detail = ipBanFacade.findByIp(normalizedIp)
			return GlobalResponse.ok(detail.map(::toResponse).orElseGet { IpBanResponse(null, normalizedIp, "", 0, false, null) })
		}
		val items = ipBanFacade.list(IpBanStatus.ACTIVE, max.coerceIn(1, 500)).map(::toResponse)
		return GlobalResponse.ok(IpBanListResponse(total = items.size, items = items))
	}

	@AdminAccessControlApiDocs.GetHistory
	@GetMapping("/{ipBanId}")
	fun getHistory(@PathVariable ipBanId: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(
		ipBanFacade.findById(ipBanId)
			.map(::toHistoryResponse)
			.orElseThrow { IpBanException(IpBanExceptionCode.IP_BAN_NOT_FOUND) }
	)

	/** IPv6 호환을 위해 path variable 대신 query param 사용 */
	@AdminAccessControlApiDocs.Unban
	@DeleteMapping
	fun unban(@RequestParam ip: String): ResponseEntity<GlobalResponse> = commandResponse(ipBanFacade.unban(ip, "manual", requiredAdminId()))

	@AdminAccessControlApiDocs.ReportSignal
	@PostMapping("/signals")
	fun reportSignal(@Valid @RequestBody request: IpSecuritySignalRequest): ResponseEntity<GlobalResponse> = GlobalResponse.ok(
		ipSecuritySignalFacade.report(
			IpSecuritySignalReportRequest(
				request.ipBanId,
				request.ip,
				request.endpointPath,
				request.httpMethod,
				request.ruleCode,
				request.observedFrom,
				request.observedUntil,
				request.observationCount,
				request.agentVersion
			),
			requiredAdminId()
		)
	)

	@AdminAccessControlApiDocs.GetSignal
	@GetMapping("/signals/{signalId}")
	fun getSignal(@PathVariable signalId: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(
		ipSecuritySignalFacade.findById(signalId)
			.orElseThrow { IpBanException(IpBanExceptionCode.IP_SECURITY_SIGNAL_NOT_FOUND) }
	)

	@AdminAccessControlApiDocs.ListSignals
	@GetMapping("/signals")
	fun listSignals(
		@RequestParam ip: String,
		@RequestParam(required = false, defaultValue = "100") max: Int
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(ipSecuritySignalFacade.findByIp(ip, max.coerceIn(1, 500)))

	@AdminAccessControlApiDocs.ReviewSignal
	@PostMapping("/signals/{signalId}/verdict")
	fun reviewSignal(
		@PathVariable signalId: Long,
		@Valid @RequestBody request: IpSecuritySignalVerdictRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(
		ipSecuritySignalFacade.review(signalId, request.verdict, request.reviewNote, requiredAdminId())
	)

	private fun commandResponse(result: IpBanCommandResponse): ResponseEntity<GlobalResponse> {
		val response = toResponse(result.detail(), result.projectionStatus())
		return if (result.projectionStatus() == ProjectionStatus.PENDING_RECONCILE) {
			ResponseEntity.status(HttpStatus.ACCEPTED).body(GlobalResponse.ok(response).body)
		} else {
			GlobalResponse.ok(response)
		}
	}

	private fun requiredAdminId(): Long = SecurityContextUtil.getAdminUserIdByContext()
		.orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }

	private fun normalizeIp(rawIp: String): String = ClientIpResolver.normalize(rawIp)
		?: throw IpBanException(IpBanExceptionCode.INVALID_IP)

	private fun toResponse(detail: IpBanDetailResponse, projectionStatus: ProjectionStatus? = null): IpBanResponse = IpBanResponse(
		id = detail.id(),
		ip = detail.normalizedIp(),
		reason = detail.reason(),
		ttlSeconds = if (detail.status() == IpBanStatus.ACTIVE) {
			Duration.between(LocalDateTime.now(), detail.expiresAt()).seconds.coerceAtLeast(0)
		} else {
			0
		},
		banned = detail.status() == IpBanStatus.ACTIVE,
		projectionStatus = projectionStatus
	)

	private fun toResponse(summary: IpBanSummaryResponse): IpBanResponse = IpBanResponse(
		id = summary.id(),
		ip = summary.normalizedIp(),
		reason = summary.reason(),
		ttlSeconds = if (summary.status() == IpBanStatus.ACTIVE) {
			Duration.between(LocalDateTime.now(), summary.expiresAt()).seconds.coerceAtLeast(0)
		} else {
			0
		},
		banned = summary.status() == IpBanStatus.ACTIVE,
		projectionStatus = null
	)

	private fun toHistoryResponse(detail: IpBanDetailResponse): IpBanHistoryResponse = IpBanHistoryResponse(
		id = detail.id(),
		ip = detail.normalizedIp(),
		status = detail.status(),
		reason = detail.reason(),
		effectiveFrom = detail.effectiveFrom(),
		expiresAt = detail.expiresAt(),
		stateChangedAt = detail.stateChangedAt(),
		events = detail.events()
	)
}

data class IpBanRequest(
	@field:NotBlank(message = "ACCESS_CONTROL_IP_REQUIRED")
	val ip: String,
	@field:Min(value = 1, message = "ACCESS_CONTROL_BAN_TTL_MINIMUM")
	@field:Max(value = 2_592_000, message = "ACCESS_CONTROL_BAN_TTL_MAXIMUM")
	val ttlSeconds: Int = 600,
	@field:Size(max = 200, message = "ACCESS_CONTROL_BAN_REASON_MAX_SIZE")
	val reason: String? = null
)

data class IpBanResponse(
	val id: Long?,
	val ip: String,
	val reason: String,
	val ttlSeconds: Long,
	val banned: Boolean,
	val projectionStatus: ProjectionStatus?
)

data class IpBanListResponse(
	val total: Int,
	val items: List<IpBanResponse>
)

data class IpBanHistoryResponse(
	val id: Long,
	val ip: String,
	val status: IpBanStatus,
	val reason: String,
	val effectiveFrom: LocalDateTime,
	val expiresAt: LocalDateTime,
	val stateChangedAt: LocalDateTime,
	val events: List<IpBanEventResponse>
)

data class IpSecuritySignalRequest(
	val ipBanId: Long? = null,
	@field:NotBlank
	val ip: String,
	@field:NotBlank
	@field:Size(max = 1024)
	@field:Pattern(regexp = "^[^?]+$", message = "ACCESS_CONTROL_SIGNAL_ENDPOINT_QUERY_FORBIDDEN")
	val endpointPath: String,
	@field:NotBlank
	@field:Size(max = 10)
	val httpMethod: String,
	@field:NotBlank
	@field:Size(max = 100)
	val ruleCode: String,
	@field:NotNull
	val observedFrom: LocalDateTime,
	@field:NotNull
	val observedUntil: LocalDateTime,
	@field:Min(1)
	val observationCount: Int,
	@field:Size(max = 100)
	val agentVersion: String? = null
)

data class IpSecuritySignalVerdictRequest(
	@field:NotNull
	val verdict: SignalVerdict,
	@field:Size(max = 500)
	val reviewNote: String? = null
)

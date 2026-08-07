package app.bottlenote.accesscontrol.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.global.security.accesscontrol.AccessControlException
import app.bottlenote.global.security.accesscontrol.AccessControlExceptionCode
import app.bottlenote.global.security.accesscontrol.AccessControlService
import app.bottlenote.global.security.accesscontrol.ClientIpResolver
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/v1/access-control/ip-bans")
@ConditionalOnBean(AccessControlService::class)
class AdminAccessControlController(
	private val accessControlService: AccessControlService
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@PostMapping
	fun ban(@Valid @RequestBody request: IpBanRequest): ResponseEntity<GlobalResponse> {
		val ip = normalizeIp(request.ip)
		val reason = request.reason?.takeIf { it.isNotBlank() } ?: "manual"
		accessControlService.banIp(ip, Duration.ofSeconds(request.ttlSeconds.toLong()), reason)
		val ban = accessControlService.getBan(ip)
		audit("BAN", ip, request.ttlSeconds.toLong(), reason)
		return GlobalResponse.ok(
			IpBanResponse(
				ip = ip,
				reason = ban?.reason.orEmpty(),
				ttlSeconds = ban?.ttlSeconds ?: request.ttlSeconds.toLong(),
				banned = true
			)
		)
	}

	/**
	 * - `ip` 있음: 단건 조회
	 * - `ip` 없음: 활성 ban 목록 (max 기본 100, 상한 500)
	 */
	@GetMapping
	fun getOrList(
		@RequestParam(required = false) ip: String?,
		@RequestParam(required = false, defaultValue = "100") max: Int
	): ResponseEntity<GlobalResponse> {
		if (!ip.isNullOrBlank()) {
			val normalized = normalizeIp(ip)
			val ban = accessControlService.getBan(normalized)
			return GlobalResponse.ok(
				IpBanResponse(
					ip = normalized,
					reason = ban?.reason.orEmpty(),
					ttlSeconds = ban?.ttlSeconds ?: 0,
					banned = ban != null
				)
			)
		}
		val limit = max.coerceIn(1, 500)
		val items =
			accessControlService.listBans(limit).map {
				IpBanResponse(
					ip = it.ip(),
					reason = it.reason(),
					ttlSeconds = it.ttlSeconds(),
					banned = true
				)
			}
		return GlobalResponse.ok(IpBanListResponse(total = items.size, items = items))
	}

	/** IPv6 호환을 위해 path variable 대신 query param 사용 */
	@DeleteMapping
	fun unban(@RequestParam ip: String): ResponseEntity<GlobalResponse> {
		val normalized = normalizeIp(ip)
		accessControlService.unbanIp(normalized)
		audit("UNBAN", normalized, 0, "")
		return GlobalResponse.ok(IpBanResponse(ip = normalized, reason = "", ttlSeconds = 0, banned = false))
	}

	private fun normalizeIp(raw: String): String {
		return ClientIpResolver.normalize(raw)
			?: throw AccessControlException(AccessControlExceptionCode.INVALID_IP)
	}

	private fun audit(action: String, ip: String, ttlSeconds: Long, reason: String) {
		val adminId = SecurityContextUtil.getAdminUserIdByContext().orElse(null)
		log.info(
			"access-control audit action={} adminId={} ip={} ttlSeconds={} reason={}",
			action,
			adminId,
			ip,
			ttlSeconds,
			reason
		)
	}
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
	val ip: String,
	val reason: String,
	val ttlSeconds: Long,
	val banned: Boolean
)

data class IpBanListResponse(
	val total: Int,
	val items: List<IpBanResponse>
)

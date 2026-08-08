package app.bottlenote.accesscontrol.presentation.docs

import app.bottlenote.accesscontrol.dto.response.IpSecuritySignalResponse
import app.bottlenote.accesscontrol.presentation.IpBanHistoryResponse
import app.bottlenote.accesscontrol.presentation.IpBanListResponse
import app.bottlenote.accesscontrol.presentation.IpBanResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** IP 차단과 탐지 signal 관리 API 문서. */
object AdminAccessControlApiDocs {
	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "IP 접근 제어", description = "IP 차단 상태, 감사 이력, 보안 signal 판정을 관리한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "IP를 차단한다", responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = IpBanResponse::class))]), ApiResponse(responseCode = "202", description = "DB 저장 후 Redis 재조정을 대기한다", content = [Content(schema = Schema(implementation = IpBanResponse::class))])])
	annotation class Ban

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "IP 차단 상태 또는 목록을 조회한다", responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = IpBanListResponse::class))])])
	annotation class GetOrList

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "IP 차단 감사 이력을 조회한다", responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = IpBanHistoryResponse::class))])])
	annotation class GetHistory

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "IP 차단을 해제한다", responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = IpBanResponse::class))]), ApiResponse(responseCode = "202", description = "DB 저장 후 Redis 재조정을 대기한다", content = [Content(schema = Schema(implementation = IpBanResponse::class))])])
	annotation class Unban

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "IP 보안 signal을 등록한다", description = "query string, header, body, key 등 민감 원문은 저장하지 않는다", responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = IpSecuritySignalResponse::class))])])
	annotation class ReportSignal

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "IP 보안 signal 상세를 조회한다", responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = IpSecuritySignalResponse::class))])])
	annotation class GetSignal

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "IP별 보안 signal을 조회한다", responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = IpSecuritySignalResponse::class))])])
	annotation class ListSignals

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "IP 보안 signal을 확정 판정한다", responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = IpSecuritySignalResponse::class))])])
	annotation class ReviewSignal
}

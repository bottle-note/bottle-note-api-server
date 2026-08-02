package app.bottlenote.auth.presentation.docs

import app.bottlenote.user.dto.response.AdminSignupResponse
import app.bottlenote.user.dto.response.TokenItem
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 관리자 로그인과 계정 등록·탈퇴 엔드포인트의 문서 설명. */
object AuthApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "인증", description = "관리자 로그인, 토큰 재발급, 관리자 계정 등록·탈퇴를 처리한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "이메일과 비밀번호로 로그인한다",
		description = "등록된 관리자 이메일과 비밀번호로 로그인해 액세스 토큰과 리프레시 토큰을 발급받습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "발급된 토큰",
				content = [Content(schema = Schema(implementation = TokenItem::class))]
			)
		]
	)
	annotation class Login

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "리프레시 토큰으로 액세스 토큰을 재발급한다",
		description = "액세스 토큰이 만료됐을 때 리프레시 토큰으로 새 액세스 토큰과 리프레시 토큰을 다시 발급받습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "재발급된 토큰",
				content = [Content(schema = Schema(implementation = TokenItem::class))]
			)
		]
	)
	annotation class Refresh

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "에이전트 키로 로그인한다",
		description = "발급받은 에이전트 키로 매핑된 관리자 계정에 로그인해 액세스 토큰과 리프레시 토큰을 발급받습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "발급된 토큰",
				content = [Content(schema = Schema(implementation = TokenItem::class))]
			)
		]
	)
	annotation class LoginWithAgent

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "관리자 계정을 등록한다",
		description = """
			이메일, 비밀번호, 이름, 역할로 새 관리자 계정을 등록합니다.

			등록은 이미 로그인한 관리자만 요청할 수 있습니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록된 관리자 계정 정보",
				content = [Content(schema = Schema(implementation = AdminSignupResponse::class))]
			)
		]
	)
	annotation class Signup

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "관리자 계정을 탈퇴한다",
		description = "요청자 본인의 관리자 계정을 탈퇴 처리합니다. 탈퇴 후에는 해당 계정으로 다시 로그인할 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "탈퇴 처리 결과 메시지",
				content = [Content(schema = Schema(implementation = AdminWithdrawResponseSchema::class))]
			)
		]
	)
	annotation class Withdraw
}

@Schema(name = "AdminWithdrawResponse", description = "관리자 탈퇴 처리 결과")
internal data class AdminWithdrawResponseSchema(
	@field:Schema(example = "탈퇴 처리되었습니다.")
	val message: String
)

package app.bottlenote.support.help.presentation.docs

import app.bottlenote.support.help.dto.response.AdminHelpAnswerResponse
import app.bottlenote.support.help.dto.response.AdminHelpDetailResponse
import app.bottlenote.support.help.dto.response.AdminHelpListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 문의(Help) 엔드포인트의 문서 설명. */
object AdminHelpApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "문의", description = "사용자가 남긴 문의를 조회하고 답변을 등록한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "문의 목록을 조회한다",
		description = """
			처리 상태, 문의 유형, 커서, 페이지 크기로 문의 목록을 커서 방식으로 조회합니다.

			cursor를 지정하지 않으면 처음부터, size를 지정하지 않으면 20건씩 조회합니다.
			다음 페이지 정보는 meta.pagination에 담깁니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "문의 목록",
				content = [Content(schema = Schema(implementation = AdminHelpListResponse::class))]
			)
		]
	)
	annotation class GetHelpList

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "문의 상세 정보를 조회한다",
		description = "문의 ID로 단일 문의의 상세 정보를 조회합니다. 첨부 이미지 목록과 기존 답변 내용을 함께 반환합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "문의 상세 정보",
				content = [Content(schema = Schema(implementation = AdminHelpDetailResponse::class))]
			)
		]
	)
	annotation class GetHelpDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "문의에 답변을 등록한다",
		description = "문의에 답변 내용과 처리 상태를 등록합니다. 답변한 관리자 계정은 인증 컨텍스트에서 추출합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "답변 등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminHelpAnswerResponse::class))]
			)
		]
	)
	annotation class AnswerHelp
}

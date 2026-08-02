package app.bottlenote.user.presentation.docs

import app.bottlenote.user.dto.response.AdminUserListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 회원 관리 엔드포인트의 문서 설명. */
object AdminUsersApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "회원 관리", description = "가입한 회원 목록을 검색하고 조회한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "회원 목록을 조회한다",
		description = """
			검색어, 상태, 정렬 기준·방향, 페이지 번호·크기로 회원 목록을 페이지 단위로 조회합니다.

			keyword를 비우면 전체 회원을 반환하며, 각 회원의 리뷰·별점·찜 수와 가입일·최종 로그인일을 함께 내려줍니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "회원 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminUserListResponse::class))
					)
				]
			)
		]
	)
	annotation class ListUsers
}

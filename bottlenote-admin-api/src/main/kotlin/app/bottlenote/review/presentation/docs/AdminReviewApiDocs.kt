package app.bottlenote.review.presentation.docs

import app.bottlenote.review.dto.response.AdminReviewListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 리뷰 관리 엔드포인트의 문서 설명. */
object AdminReviewApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "리뷰 관리", description = "작성된 리뷰 목록을 검색하고 조회한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "리뷰 목록을 조회한다",
		description = """
			위스키, 작성자, 활성·공개 상태, 검색어, 작성일 범위, 정렬 기준·방향, 페이지 번호·크기로 리뷰 목록을 페이지 단위로 조회합니다.

			필터 값을 비우면 전체 리뷰를 대상으로 조회합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "리뷰 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminReviewListResponse::class))
					)
				]
			)
		]
	)
	annotation class ListReviews
}

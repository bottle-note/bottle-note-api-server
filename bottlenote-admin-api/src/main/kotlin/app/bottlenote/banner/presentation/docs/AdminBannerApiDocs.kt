package app.bottlenote.banner.presentation.docs

import app.bottlenote.banner.dto.response.AdminBannerDetailResponse
import app.bottlenote.banner.dto.response.AdminBannerListResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 배너 엔드포인트의 문서 설명. */
object AdminBannerApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "배너", description = "앱에 노출되는 배너를 등록·수정·삭제하고 노출 상태와 정렬 순서를 관리한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "배너 목록을 조회한다",
		description = """
			검색어, 활성 여부, 배너 타입, 페이지 번호·크기로 배너 목록을 페이지 단위로 조회합니다.

			keyword를 비우면 전체 배너를 반환합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "배너 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminBannerListResponse::class))
					)
				]
			)
		]
	)
	annotation class GetAllBanners

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "배너 상세 정보를 조회한다",
		description = "배너 ID로 단일 배너의 상세 정보를 조회합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "배너 상세 정보",
				content = [Content(schema = Schema(implementation = AdminBannerDetailResponse::class))]
			)
		]
	)
	annotation class GetBannerDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "배너를 등록한다",
		description = """
			새 배너를 등록합니다.

			이름이 이미 등록된 배너와 중복되면 실패합니다. isExternalUrl이 true이면 targetUrl이 반드시 있어야 하며,
			startDate가 endDate보다 늦으면 실패합니다. endDate가 이미 지난 시점이면 등록과 동시에 비활성 상태가 됩니다.
			정렬 순서를 지정하면 기존에 같은 순서 이상을 쓰던 배너들은 뒤로 한 칸씩 밀립니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class CreateBanner

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "배너 정보를 수정한다",
		description = """
			기존 배너의 문구, 이미지, 노출 기간, 정렬 순서, 활성 여부를 수정합니다.

			isExternalUrl이 true이면 targetUrl이 반드시 있어야 하며, startDate가 endDate보다 늦으면 실패합니다.
			endDate가 이미 지난 시점이면 요청한 isActive 값과 무관하게 비활성 상태로 저장됩니다.
			정렬 순서를 변경하면 기존에 같은 순서 이상을 쓰던 배너들은 뒤로 한 칸씩 밀립니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateBanner

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "배너를 삭제한다",
		description = "배너를 삭제합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class DeleteBanner

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "배너 활성 상태를 변경한다",
		description = "단일 배너의 활성/비활성 상태만 변경합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "상태 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateBannerStatus

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "배너 정렬 순서를 변경한다",
		description = "단일 배너의 정렬 순서 값을 변경합니다. 기존에 같은 순서 이상을 쓰던 배너들은 뒤로 한 칸씩 밀립니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "정렬 순서 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateBannerSortOrder

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "배너 목록을 일괄 재정렬한다",
		description = """
			배너 ID 목록을 원하는 순서대로 보내면 그 순서대로 정렬 순서를 다시 매깁니다.

			목록에 포함되지 않은 나머지 배너는 기존 순서를 유지한 채 뒤로 이어 붙습니다. ID가 중복되면 실패합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "재정렬 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class ReorderBanners
}

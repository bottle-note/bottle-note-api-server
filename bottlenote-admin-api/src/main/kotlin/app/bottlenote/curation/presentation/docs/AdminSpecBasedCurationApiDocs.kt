package app.bottlenote.curation.presentation.docs

import app.bottlenote.curation.dto.response.AdminSpecBasedCurationDetailResponse
import app.bottlenote.curation.dto.response.AdminSpecBasedCurationListResponse
import app.bottlenote.curation.dto.response.CurationFeedItemResponse
import app.bottlenote.curation.dto.request.CurationSortType
import app.bottlenote.global.dto.response.AdminResultResponse
import app.bottlenote.global.service.cursor.SortOrder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 스펙 기반 큐레이션 엔드포인트의 문서 설명. */
object AdminSpecBasedCurationApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "스펙 기반 큐레이션", description = "큐레이션 스펙을 기반으로 만든 큐레이션을 등록·수정하고 목록·피드·상세를 조회한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "스펙 기반 큐레이션 목록을 조회한다",
		description = """
			검색어, 스펙 코드, 활성화 상태, 페이지 번호·크기와 sortType/sortOrder로 큐레이션 목록을 페이지 단위로 조회합니다.
			기본값은 EXPOSURE_START_DATE + DESC입니다. 날짜 정렬은 ASC/DESC 모두 null-last이며 날짜/null 동률은 요청 방향의 id로 정렬합니다.
			DISPLAY_ORDER는 요청 방향의 displayOrder, id로 정렬합니다.

			code에 해당하는 스펙이 없으면 오류가 아니라 빈 목록을 200으로 반환합니다.
			""",
		parameters = [
			Parameter(name = "sortType", description = "정렬 기준 (기본 EXPOSURE_START_DATE)", schema = Schema(implementation = CurationSortType::class, defaultValue = "EXPOSURE_START_DATE")),
			Parameter(name = "sortOrder", description = "정렬 방향 (기본 DESC)", schema = Schema(implementation = SortOrder::class, defaultValue = "DESC"))
		],
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "스펙 기반 큐레이션 목록",
				content = [
					Content(
						array =
						ArraySchema(schema = Schema(implementation = AdminSpecBasedCurationListResponse::class))
					)
				]
			)
		]
	)
	annotation class GetAllSpecBasedCurations

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "스펙 기반 큐레이션 피드를 조회한다",
		description = """
			각 큐레이션을 스펙의 응답 정의(responseSpec)에 맞춰 완성된 피드 항목으로 구체화해 반환합니다.
			sortType(EXPOSURE_START_DATE, DISPLAY_ORDER)와 sortOrder(ASC, DESC)를 선택하며 기본값은 EXPOSURE_START_DATE + DESC입니다.
			날짜 정렬은 ASC/DESC 모두 null-last이고 동률은 요청 방향의 id로, DISPLAY_ORDER는 요청 방향의 displayOrder와 id로 정렬합니다.

			size는 최대 10개로 제한되며, code에 해당하는 스펙이 없으면 오류가 아니라 빈 목록을 200으로 반환합니다.
			""",
		parameters = [
			Parameter(name = "sortType", description = "정렬 기준 (기본 EXPOSURE_START_DATE)", schema = Schema(implementation = CurationSortType::class, defaultValue = "EXPOSURE_START_DATE")),
			Parameter(name = "sortOrder", description = "정렬 방향 (기본 DESC)", schema = Schema(implementation = SortOrder::class, defaultValue = "DESC"))
		],
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "스펙 기반 큐레이션 피드 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = CurationFeedItemResponse::class))
					)
				]
			)
		]
	)
	annotation class GetSpecBasedCurationFeed

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "스펙 기반 큐레이션 상세 정보를 조회한다",
		description = "큐레이션 ID로 단일 큐레이션의 상세 정보를 조회합니다. 사용한 스펙 정보와 등록 시 저장한 원본 payload를 함께 반환합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "스펙 기반 큐레이션 상세 정보",
				content = [Content(schema = Schema(implementation = AdminSpecBasedCurationDetailResponse::class))]
			)
		]
	)
	annotation class GetSpecBasedCurationDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "스펙 기반 큐레이션을 등록한다",
		description = """
			새 큐레이션을 등록합니다.

			specId로 지정한 스펙의 requestSpec 정의에 맞춰 payload를 검증하며, 형식이 맞지 않으면 실패합니다.
			노출 시작일이 종료일보다 늦거나, 활성화 상태로 등록하면서 노출 종료일이 이미 지난 경우에도 실패합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class CreateSpecBasedCuration

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "스펙 기반 큐레이션 정보를 수정한다",
		description = """
			기존 큐레이션의 스펙, 이름, 설명, 이미지, 노출 기간, 노출 순서, 활성화 상태, payload를 한 번에 수정합니다.

			payload는 지정한 스펙의 requestSpec 정의로 다시 검증됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateSpecBasedCuration
}

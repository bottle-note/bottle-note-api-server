package app.bottlenote.curation.presentation.docs

import app.bottlenote.curation.dto.response.CurationSpecListResponse
import app.bottlenote.curation.dto.response.CurationSpecResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 큐레이션 스펙 엔드포인트의 문서 설명. */
object AdminCurationSpecApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "큐레이션 스펙", description = "스펙 기반 큐레이션 작성에 쓰는 스펙(필드 정의)을 조회한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "활성화된 큐레이션 스펙 목록을 조회한다",
		description = """
			현재 활성화된 큐레이션 스펙 목록을 코드, 이름, 버전과 함께 조회합니다.

			새 큐레이션을 등록할 때 어떤 스펙을 쓸지 고르는 데 사용합니다. 응답은 로컬 캐시(local_cache_curation_spec_list)로 캐싱됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "활성 큐레이션 스펙 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = CurationSpecListResponse::class))
					)
				]
			)
		]
	)
	annotation class GetAllCurationSpecs

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션 스펙 상세 정보를 조회한다",
		description = """
			스펙 ID로 단일 스펙의 상세 정보를 조회합니다.

			등록·수정 요청 payload가 따라야 하는 필드 정의는 requestSpec에, 피드 응답을 구체화할 때 쓰는 필드 정의는 responseSpec에 담깁니다.
			스펙별로 로컬 캐시(local_cache_curation_spec_detail)로 캐싱됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "큐레이션 스펙 상세 정보",
				content = [Content(schema = Schema(implementation = CurationSpecResponse::class))]
			)
		]
	)
	annotation class GetCurationSpecDetail
}

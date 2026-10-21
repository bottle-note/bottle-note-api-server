package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest
import app.bottlenote.alcohols.dto.response.AlcoholCandidateResponse
import app.bottlenote.alcohols.service.AlcoholCandidateService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.service.meta.MetaService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/attribute/groups/{groupId}/candidates")
@Tag(name = "속성 그룹 추천 후보")
class AdminAlcoholCandidateController(private val service: AlcoholCandidateService) {
	@PostMapping
	@Operation(summary = "속성 그룹의 추천 후보를 조회한다", description = "그룹에 연결된 미삭제 술을 ID 오름차순으로 반환합니다. include는 모두 만족, exclude는 하나라도 만족하면 술 전체 제외. attributeId는 해당 그룹 소속만 허용합니다. 같은 숫자 속성의 비교 조건은 하나의 연결 값에 함께 적용합니다. NE는 값이 존재하고 동일 값이 전혀 없는 술입니다. 도수 범위는 양끝 포함, 맛 그룹은 대표(CORE) 태그 기준입니다. size 기본 25·최대 100. meta.pagination.nextCursor를 같은 필터와 함께 전달하며 필터 변경 시 cursor를 생략합니다. totalCount·nextQuestion은 페이지가 아닌 전체 결과 기준이고, nextQuestion 생략 시 집계하지 않습니다. 다른 그룹 속성·태그 아이콘·감사 정보는 반환하지 않습니다.")
	@ApiResponse(responseCode = "200", description = "후보 목록과 집계", content = [Content(schema = Schema(implementation = AlcoholCandidateResponse::class))])
	fun candidates(@PathVariable groupId: Long, @RequestBody @Valid request: AlcoholCandidateRequest): ResponseEntity<GlobalResponse> {
		val result = service.findCandidates(groupId, request)
		return GlobalResponse.ok(result.content(), MetaService.createMetaInfo().add("pagination", result.pagination()))
	}
}

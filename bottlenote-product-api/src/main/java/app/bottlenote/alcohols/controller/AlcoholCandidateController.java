package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;

import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest;
import app.bottlenote.alcohols.dto.response.AlcoholCandidateResponse;
import app.bottlenote.alcohols.service.AlcoholCandidateService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.meta.MetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attribute/groups/{groupId}/candidates")
@RequiredArgsConstructor
@SecurityPolicy(auth = OPTIONAL_AUTH)
@Tag(name = "둘러보기")
public class AlcoholCandidateController {
  private final AlcoholCandidateService service;

  @PostMapping
  @Operation(
      summary = "속성 그룹의 추천 후보를 조회한다",
      description =
          "그룹 속성이 하나 이상 연결된 미삭제 술을 ID 오름차순으로 반환합니다. include는 AND, exclude는 하나라도 만족하면 술 전체를 제외합니다. 조건의 attributeId는 해당 그룹 소속이어야 합니다. 같은 숫자 속성의 비교 조건은 하나의 연결 값에 함께 적용합니다. NE는 값이 존재하고 같은 값이 전혀 없는 술입니다. 맛 그룹은 CORE 태그 중 하나와 연결된 술입니다. 도수 양끝은 포함하며 변환할 수 없는 도수는 범위 검색에서 제외합니다. size 기본 25·최대 100, meta.pagination의 nextCursor로 같은 조건의 다음 청크를 요청합니다. 조건 변경 시 cursor를 생략합니다. totalCount와 nextQuestion 집계는 현재 페이지가 아닌 전체 교집합 기준입니다. 다음 청크에서 nextQuestion을 생략하면 집계를 생략합니다. 태그 아이콘·감사 정보·다른 그룹 속성은 노출하지 않습니다. 데이터 변경을 고정하는 스냅샷은 아닙니다.")
  @ApiResponse(
      responseCode = "200",
      description = "후보 목록과 집계",
      content = @Content(schema = @Schema(implementation = AlcoholCandidateResponse.class)))
  public ResponseEntity<GlobalResponse> getCandidates(
      @PathVariable Long groupId, @RequestBody @Valid AlcoholCandidateRequest request) {
    var result = service.findCandidates(groupId, request);
    return GlobalResponse.ok(
        result.content(), MetaService.createMetaInfo().add("pagination", result.pagination()));
  }
}

package app.bottlenote.alcohols.controller.docs;

import app.bottlenote.alcohols.dto.response.ScenarioEvaluationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.*;

public final class ScenarioApiDocs {
  private ScenarioApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "사용자 시나리오")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "게시된 시나리오와 첫 질문을 조회한다",
      description = "활성 게시본만 제공합니다. 후보는 매 요청 현재 데이터로 계산합니다. 로그인 없이 사용할 수 있으며 참여 이벤트는 기록하지 않습니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "시나리오와 첫 질문",
            content =
                @Content(schema = @Schema(implementation = ScenarioEvaluationResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "없거나 비활성 또는 게시본 없는 시나리오",
            content = @Content)
      })
  public @interface Get {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "전체 답변으로 질문 또는 최신 추천 결과를 평가한다",
      description =
          """
      조회한 versionId와 순서대로 선택한 {questionId, optionId} 전체를 전달합니다.
      QUESTION은 다음 질문, RESULT는 현재 답변만 적용한 결과입니다. QUESTION에서 END에 도달해도 결과를 반환합니다.
      과거 질문은 viewQuestionId로 조회하며 그 질문 직전까지의 답변만 후보에 적용합니다.
      답변을 바꾸면 변경한 질문까지의 답변만 제출하고 이후 답변은 브라우저에서 폐기합니다.
      optionId는 서버가 발급한 값을 그대로 보관합니다. 분포가 바뀌어도 숫자 구간 의미는 유지하며 후보만 다시 계산합니다.
      답변이나 결과를 서버에 저장하지 않고 캠페인 참여 이벤트도 기록하지 않습니다.
      결과는 alcoholId 오름차순 keyset이며 candidates.pagination.nextCursor로 다음 페이지를 요청합니다.
      페이징 중 관리 데이터가 변경되면 현재 수와 목록이 달라질 수 있습니다. 커서는 1시간 후 만료됩니다.
      버전 충돌은 재조회하고, 선택지 검증 실패 또는 서명 키 폐기는 해당 질문부터 다시 선택해야 합니다.
      """,
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "질문 또는 추천 결과",
            content =
                @Content(schema = @Schema(implementation = ScenarioEvaluationResponse.class))),
        @ApiResponse(responseCode = "400", description = "답변·선택지·경로·커서 검증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "제공할 수 없는 시나리오", content = @Content),
        @ApiResponse(
            responseCode = "409",
            description = "현재 활성 게시 버전과 요청 버전 불일치",
            content = @Content),
        @ApiResponse(responseCode = "410", description = "결과 커서 만료", content = @Content)
      })
  public @interface Evaluate {}
}

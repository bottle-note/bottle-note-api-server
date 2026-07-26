package app.bottlenote.support.help.controller.docs;

import app.bottlenote.support.help.dto.response.HelpDetailItem;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 고객 문의 엔드포인트의 문서 설명. */
public final class HelpApiDocs {

  private HelpApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "문의", description = "서비스 이용 중 생긴 문의를 남기고 답변을 확인한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "문의를 등록한다",
      description =
          """
          서비스 이용 중 생긴 문의를 남깁니다.

          문의 유형과 내용을 함께 보내며, 이미지를 첨부할 수 있습니다. 운영자가 확인 후 답변을 등록합니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "등록 처리 결과",
              content = @Content(schema = @Schema(implementation = HelpResultResponse.class))))
  public @interface RegisterHelp {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "내가 등록한 문의 목록을 조회한다",
      description =
          """
          로그인한 사용자가 남긴 문의를 커서 방식으로 가져옵니다.

          각 항목에 답변 완료 여부가 표시됩니다. 다음 페이지 정보는 meta.pageable에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "문의 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(schema = @Schema(implementation = HelpListResponse.class)))))
  public @interface GetHelpList {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "문의 상세를 조회한다",
      description = "문의 내용과 운영자 답변을 함께 반환합니다. 본인이 등록한 문의만 조회할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "문의 상세 정보",
              content = @Content(schema = @Schema(implementation = HelpDetailItem.class))))
  public @interface GetHelpDetail {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "문의를 수정한다",
      description = "본인이 등록한 문의만 수정할 수 있습니다. 답변이 등록된 뒤에는 수정할 수 없습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "수정 처리 결과",
              content = @Content(schema = @Schema(implementation = HelpResultResponse.class))))
  public @interface ModifyHelp {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "문의를 삭제한다",
      description = "본인이 등록한 문의만 삭제할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "삭제 처리 결과",
              content = @Content(schema = @Schema(implementation = HelpResultResponse.class))))
  public @interface DeleteHelp {}
}

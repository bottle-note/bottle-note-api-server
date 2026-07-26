package app.bottlenote.support.business.controller.docs;

import app.bottlenote.support.business.dto.response.BusinessInfoResponse;
import app.bottlenote.support.business.dto.response.BusinessSupportDetailItem;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
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
import java.util.List;
import org.springframework.http.MediaType;

/** 비즈니스 제휴 문의 엔드포인트의 문서 설명. */
public final class BusinessSupportApiDocs {

  private BusinessSupportApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "비즈니스 문의", description = "제휴와 협업 문의를 등록하고 관리한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "비즈니스 문의를 등록한다",
      description =
          """
          제휴나 협업을 제안하는 문의를 남깁니다.

          업체 정보와 문의 내용을 함께 보내며, 등록 후에는 운영자가 확인하고 답변합니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "등록 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = BusinessSupportResultResponse.class))))
  public @interface RegisterBusinessSupport {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "내가 등록한 비즈니스 문의 목록을 조회한다",
      description = "로그인한 사용자가 남긴 문의를 전체 건수와 함께 가져옵니다. 다른 사용자의 문의는 보이지 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "전체 건수와 문의 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = BusinessSupportCollection.class))))
  public @interface GetBusinessSupportList {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "비즈니스 문의 상세를 조회한다",
      description = "문의 내용과 운영자 답변을 함께 반환합니다. 본인이 등록한 문의만 조회할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "문의 상세 정보",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = BusinessSupportDetailItem.class))))
  public @interface GetBusinessSupportDetail {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "비즈니스 문의를 수정한다",
      description = "본인이 등록한 문의만 수정할 수 있습니다. 운영자가 답변을 남긴 뒤에는 수정이 제한될 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "수정 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = BusinessSupportResultResponse.class))))
  public @interface ModifyBusinessSupport {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "비즈니스 문의를 삭제한다",
      description = "본인이 등록한 문의만 삭제할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "삭제 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = BusinessSupportResultResponse.class))))
  public @interface DeleteBusinessSupport {}

  /** 실제로는 {@code CollectionResponse<BusinessInfoResponse>}다. */
  @Schema(name = "비즈니스 문의 목록", description = "전체 건수와 문의 목록")
  private record BusinessSupportCollection(
      @Schema(description = "문의 전체 건수", example = "3") long totalCount,
      @ArraySchema(schema = @Schema(implementation = BusinessInfoResponse.class))
          List<BusinessInfoResponse> items) {}
}

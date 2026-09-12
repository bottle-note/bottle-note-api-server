package app.bottlenote.mfds.controller.docs;

import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholDetail;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholListItem;
import app.bottlenote.mfds.dto.response.MfdsPublicCountryItem;
import app.bottlenote.mfds.dto.response.MfdsPublicImporterItem;
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

/** Product 수입 정보 공개 조회 문서. */
public final class MfdsPublicApiDocs {

  private MfdsPublicApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "수입 정보", description = "식약처 수입 주류와 수입사를 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "수입 주류 목록을 조회한다",
      description =
          """
          식약처 수입 원장의 공개 필드를 최신 처리일자 순으로 내려줍니다.

          keyword는 공백 토큰 AND입니다. 제품명·SKU·주류명·카테고리·rcno·제조사명·수입사명을 부분 일치합니다.
          특정 술의 과거 수입은 alcoholNameKo 정확 일치로 묶고, 매칭된 행만 보려면 alcoholId를 씁니다.
          exportCountry는 ISO Alpha-2입니다. processedDate는 YYYY-MM-DD이며 없으면 null입니다.
          다음 페이지는 meta.pagination.nextCursor입니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "수입 주류 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = MfdsPublicAlcoholListItem.class)))))
  public @interface SearchAlcohols {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "수입 주류 상세를 조회한다",
      description =
          """
          경로 id는 수입 주류 레코드 ID이며 BottleNote 주류 ID가 아닙니다.

          노출 대상 수입사가 있으면 importer를 채우고, 없거나 비노출이면 신고는 남기고 importer만 생략합니다.
          검토 메모·원문·매칭 후보·내부 상태는 포함하지 않습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "수입 주류 상세",
              content = @Content(schema = @Schema(implementation = MfdsPublicAlcoholDetail.class))))
  public @interface GetAlcohol {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "수입사 목록을 조회한다",
      description =
          """
          노출 대상 수입사만 최신 id 순으로 내려줍니다.

          keyword는 공백 토큰 AND이며 상호·인허가 번호·업소 코드·대표자명을 부분 일치합니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "수입사 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = MfdsPublicImporterItem.class)))))
  public @interface SearchImporters {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "수입사 상세를 조회한다",
      description = "노출 대상이 아닌 수입사는 찾을 수 없습니다. 관리 메모와 검토 이력은 포함하지 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "수입사 상세",
              content = @Content(schema = @Schema(implementation = MfdsPublicImporterItem.class))))
  public @interface GetImporter {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "수입 주류 검색용 국가 목록을 조회한다",
      description =
          """
          원장에 등장한 수출국 ISO Alpha-2와 한글·영문명을 내립니다.

          검색 파라미터 exportCountry에는 이 목록의 alpha2를 넣습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "수출국 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = MfdsPublicCountryItem.class)))))
  public @interface ListCountries {}
}

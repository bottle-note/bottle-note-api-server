package app.bottlenote.alcohols.controller.docs;

import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
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
import org.springframework.http.MediaType;

/** 위스키 조회 엔드포인트의 문서 설명. */
public final class AlcoholQueryApiDocs {

  private AlcoholQueryApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "위스키 조회", description = "위스키를 검색하고 상세 정보를 확인한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "위스키 이름을 자동 완성용으로 조회한다",
      description =
          """
          입력 중인 글자로 시작하거나 그 글자를 포함하는 위스키 이름을 가볍게 가져옵니다.

          검색창 자동 완성을 위한 용도라 상세 정보 없이 식별자와 이름 위주로 응답합니다.
          다음 페이지 정보는 meta.pageable, 조회 조건은 meta.searchParameters에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "자동 완성 후보 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = AlcoholLookupItem.class)))))
  public @interface GetLookups {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "조건을 걸어 위스키를 검색한다",
      description =
          """
          키워드, 종류, 지역, 도수 등의 조건을 조합해 위스키를 찾습니다.

          로그인하지 않아도 검색할 수 있고, 로그인한 경우 각 위스키에 본인의 찜 여부가 함께 표시됩니다.
          일부 키워드는 서버가 기획된 큐레이션 검색으로 바꿔 처리합니다.
          다음 페이지 정보는 meta.pageable, 검색 조건은 meta.searchParameters에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "검색된 위스키 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = AlcoholSearchResponse.class)))))
  public @interface SearchAlcohols {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "위스키 상세 정보를 조회한다",
      description =
          """
          위스키의 기본 정보와 함께 평균 별점, 리뷰 수, 대표 테이스팅 태그를 반환합니다.

          로그인한 경우 본인이 남긴 별점과 찜 여부가 함께 담깁니다. 조회 이력은 별도로 집계됩니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "위스키 상세 정보",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = AlcoholDetailResponse.class))))
  public @interface GetAlcoholDetail {}
}

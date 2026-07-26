package app.bottlenote.support.report.controller.docs;

import app.bottlenote.support.report.dto.response.ReviewReportResponse;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;

/** 신고 엔드포인트의 문서 설명. */
public final class ReportApiDocs {

  private ReportApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "신고", description = "부적절한 사용자나 리뷰를 신고한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "사용자를 신고한다",
      description =
          """
          부적절한 활동을 하는 사용자를 신고합니다.

          자기 자신은 신고할 수 없습니다. 같은 사용자를 반복 신고하면 횟수가 누적되며, 일정 기준을 넘으면 운영자가 확인합니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "신고 접수 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = UserReportResponse.class))))
  public @interface ReportUser {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰를 신고한다",
      description = "부적절한 내용의 리뷰를 신고합니다. 본인이 쓴 리뷰는 신고할 수 없고, 같은 리뷰를 중복 신고할 수 없습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "신고 접수 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ReviewReportResponse.class))))
  public @interface ReportReview {}
}

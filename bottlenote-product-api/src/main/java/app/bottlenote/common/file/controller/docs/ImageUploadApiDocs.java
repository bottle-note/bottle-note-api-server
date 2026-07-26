package app.bottlenote.common.file.controller.docs;

import app.bottlenote.common.file.dto.response.ImageUploadResponse;
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

/** 이미지 업로드 엔드포인트의 문서 설명. */
public final class ImageUploadApiDocs {

  private ImageUploadApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "이미지 업로드", description = "이미지를 직접 올릴 수 있는 임시 주소를 발급한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "이미지 업로드용 임시 주소를 발급한다",
      description =
          """
          이미지를 저장소에 직접 올릴 수 있는 임시 주소를 발급합니다.

          서버를 거치지 않고 클라이언트가 이 주소로 파일을 올린 뒤, 함께 받은 조회 주소를 리뷰나 프로필 등록 요청에 넣습니다.
          임시 주소는 일정 시간이 지나면 만료됩니다. 한 번에 여러 장이 필요하면 개수를 지정해 요청합니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "업로드 주소와 조회 주소",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ImageUploadResponse.class))))
  public @interface GetPreSignUrl {}
}

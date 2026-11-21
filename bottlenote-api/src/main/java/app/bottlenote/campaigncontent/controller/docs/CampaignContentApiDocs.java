package app.bottlenote.campaigncontent.controller.docs;

import app.bottlenote.campaigncontent.dto.response.CampaignContentEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 캠페인 콘텐츠 참여 이벤트 엔드포인트의 문서 설명. */
public final class CampaignContentApiDocs {

  private CampaignContentApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "캠페인 콘텐츠")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "캠페인 콘텐츠 참여 이벤트를 기록한다",
      description =
          """
          어드민에 등록한 코드로 참여 이벤트를 한 건 기록합니다. 누가·언제는 서버가 채웁니다.

          - VIEW: 페이지 첫 진입, START: 시작, FINISH: 완료(로그인 요구 직전), RESULT: 로그인 상태에서 결과 노출
          - 브라우저에서 같은 origin으로 직접 호출해야 방문자 쿠키가 함께 전달됩니다. SSR이나 서버 프록시로 보내면 방문자와 IP가 달라져 지표에서 빠집니다.
          - 처음 방문한 사용자는 다른 API 응답으로 방문자 쿠키를 받은 뒤 보내야 이후 이벤트와 같은 방문자로 이어집니다.
          - RESULT를 로그인 없이 보내면 REQUIRED_USER_ID(400)로 실패합니다. 만료된 토큰을 붙이면 유형과 관계없이 401입니다.
          - 등록되지 않았거나 비활성인 코드는 404입니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "기록한 이벤트",
              content =
                  @Content(schema = @Schema(implementation = CampaignContentEventResponse.class))))
  public @interface RegisterParticipation {}
}

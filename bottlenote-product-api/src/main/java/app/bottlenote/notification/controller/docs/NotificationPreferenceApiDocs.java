package app.bottlenote.notification.controller.docs;

import app.bottlenote.notification.dto.response.NotificationPreferenceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class NotificationPreferenceApiDocs {
  private NotificationPreferenceApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "알림 수신 설정", description = "인증 사용자 본인의 인앱 알림 생성 여부를 설정한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(summary = "내 인앱 알림 수신 설정을 조회한다",
      description = "REVIEW_COMMENT, REVIEW_REPLY, REVIEW_LIKE, FOLLOW, BEST_REVIEW, HELP_ANSWER 여섯 유형을 반환하며 미설정 값은 true입니다. 푸시 전달 채널 설정과 별개입니다.",
      responses = @ApiResponse(responseCode = "200", description = "유형별 수신 설정",
          content = @Content(schema = @Schema(implementation = NotificationPreferenceResponse.class))))
  public @interface GetPreferences {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(summary = "내 인앱 알림 수신 설정을 변경한다",
      description = "settings에 보낸 유형만 변경하고 전체 설정을 반환합니다. false는 향후 해당 알림 생성을 생략하며 기존 알림과 활동 기록은 유지합니다. true로 바꿔도 과거 알림은 소급 생성하지 않습니다. 빈 settings, null 값 또는 알 수 없는 유형은 400입니다.",
      responses = {
        @ApiResponse(responseCode = "200", description = "변경 후 전체 수신 설정",
            content = @Content(schema = @Schema(implementation = NotificationPreferenceResponse.class))),
        @ApiResponse(responseCode = "400", description = "유형 또는 수신 여부가 올바르지 않음")
      })
  public @interface UpdatePreferences {}
}

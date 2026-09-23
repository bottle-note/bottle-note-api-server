package app.bottlenote.notification.dto.response;

import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.constant.NotificationSettingGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** 사용자 본인의 알림 수신 설정. 서버가 지원하는 모든 그룹과 발생 액션을 선언 순서로 담는다. */
@Schema(name = "NotificationSettingsResponse", description = "알림 수신 설정")
public record NotificationSettingsResponse(@Schema(description = "설정 그룹 목록") List<Group> groups) {

  public static NotificationSettingsResponse from(Map<NotificationEventAction, Boolean> settings) {
    return new NotificationSettingsResponse(
        Arrays.stream(NotificationSettingGroup.values())
            .map(group -> Group.of(group, settings))
            .toList());
  }

  /** 화면 표시용 설정 그룹. 그룹 자체의 설정은 저장하지 않는다. */
  @Schema(name = "NotificationSettingGroupResponse", description = "알림 설정 그룹")
  public record Group(
      @Schema(description = "설정 그룹", example = "REVIEW_AND_FOLLOW") NotificationSettingGroup group,
      @Schema(description = "그룹 표시명", example = "리뷰와 팔로우") String displayName,
      @Schema(description = "그룹에 속한 발생 액션 설정") List<Item> settings) {

    private static Group of(
        NotificationSettingGroup group, Map<NotificationEventAction, Boolean> settings) {
      return new Group(
          group,
          group.getDescription(),
          NotificationEventAction.findByGroup(group).stream()
              .map(
                  action ->
                      Item.of(action, settings.getOrDefault(action, action.isDefaultEnabled())))
              .toList());
    }
  }

  /** 발생 액션 하나의 수신 설정. */
  @Schema(name = "NotificationSettingItemResponse", description = "발생 액션별 알림 수신 설정")
  public record Item(
      @Schema(description = "알림 발생 액션", example = "REVIEW_LIKE_ADD")
          NotificationEventAction eventAction,
      @Schema(description = "표시명", example = "내 리뷰의 좋아요") String displayName,
      @Schema(description = "설명") String description,
      @Schema(description = "설정하지 않았을 때의 수신 여부", example = "true") boolean defaultEnabled,
      @Schema(description = "현재 수신 여부", example = "false") boolean enabled) {

    private static Item of(NotificationEventAction action, boolean enabled) {
      return new Item(
          action,
          action.getDisplayName(),
          action.getDescription(),
          action.isDefaultEnabled(),
          enabled);
    }
  }
}

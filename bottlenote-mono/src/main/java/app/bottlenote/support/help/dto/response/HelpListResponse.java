package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.constant.StatusType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public record HelpListResponse(Long totalCount, List<HelpInfo> helpList) {

  public static HelpListResponse of(Long totalCount, List<HelpInfo> helpList) {
    return new HelpListResponse(totalCount, helpList);
  }

  @Builder
  public record HelpInfo(
      Long helpId, String title, String content, LocalDateTime createAt, StatusType helpStatus) {

    public static HelpInfo of(
        Long helpId, String title, String content, LocalDateTime createAt, StatusType statusType) {
      return new HelpInfo(helpId, title, content, createAt, statusType);
    }
  }
}

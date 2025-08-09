package app.bottlenote.support.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusType {
  WAITING("대기중"),
  SUCCESS("처리 완료"),
  REJECT("반려"),
  DELETED("삭제");

  private final String status;
}

package app.bottlenote.support.business.constant;

import lombok.Getter;

@Getter
public enum BusinessResultMessage {
  REGISTER_SUCCESS("비지니스 문의가 등록되었습니다"),
  MODIFY_SUCCESS("비지니스 문의가 수정되었습니다"),
  DELETE_SUCCESS("비지니스 문의가 삭제되었습니다");

  private final String description;

  BusinessResultMessage(String description) {
    this.description = description;
  }
}

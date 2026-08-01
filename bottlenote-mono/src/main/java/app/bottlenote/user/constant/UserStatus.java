package app.bottlenote.user.constant;

import lombok.Getter;

@Getter
public enum UserStatus {
  ACTIVE("활성상태"),
  SIGNUP_PENDING("가입대기상태"),
  DELETED("탈퇴상태");

  private final String description;

  UserStatus(String description) {
    this.description = description;
  }
}

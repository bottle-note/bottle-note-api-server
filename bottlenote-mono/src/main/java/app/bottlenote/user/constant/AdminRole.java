package app.bottlenote.user.constant;

import lombok.Getter;

@Getter
public enum AdminRole {
  ROOT_ADMIN("최고 관리자"),
  PARTNER("파트너사"),
  CLIENT("고객사"),
  BAR_OWNER("바/매장 사장님"),
  COMMUNITY_MANAGER("커뮤니티 매니저");

  private final String description;

  AdminRole(String description) {
    this.description = description;
  }
}

package app.bottlenote.like.domain;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
public class LikeUserInfo {
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "user_nick_name", nullable = false)
  private String userNickName;

  public static LikeUserInfo create(Long userId, String userNickName) {
    return new LikeUserInfo(userId, userNickName);
  }
}

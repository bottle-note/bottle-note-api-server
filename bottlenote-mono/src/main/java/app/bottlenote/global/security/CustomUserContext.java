package app.bottlenote.global.security;

import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class CustomUserContext extends User {
  private final Long id; // 유저 아이디
  private final String imageUrl; // 유저 이미지 URL

  public CustomUserContext(
      app.bottlenote.user.domain.User user, List<GrantedAuthority> authorities) {
    super(user.getEmail(), "", authorities);
    this.id = user.getId();
    this.imageUrl = user.getImageUrl();
  }
}

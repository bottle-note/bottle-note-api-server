package app.bottlenote.global.security;

import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.domain.AdminUser;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class CustomAdminUserContext extends User {

  private final Long id;
  private final String name;
  private final List<AdminRole> roles;

  public CustomAdminUserContext(AdminUser adminUser, List<GrantedAuthority> authorities) {
    super(adminUser.getEmail(), "", authorities);
    this.id = adminUser.getId();
    this.name = adminUser.getName();
    this.roles = adminUser.getRoles();
  }

  public boolean hasRole(AdminRole role) {
    return roles.contains(role);
  }

  public boolean isRootAdmin() {
    return hasRole(AdminRole.ROOT_ADMIN);
  }
}

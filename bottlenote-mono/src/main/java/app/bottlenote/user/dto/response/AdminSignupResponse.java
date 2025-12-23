package app.bottlenote.user.dto.response;

import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.domain.AdminUser;
import java.util.List;

public record AdminSignupResponse(Long adminId, String email, String name, List<AdminRole> roles) {
  public static AdminSignupResponse from(AdminUser admin) {
    return new AdminSignupResponse(
        admin.getId(), admin.getEmail(), admin.getName(), admin.getRoles());
  }
}

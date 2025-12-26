package app.bottlenote.user.dto.response;

import app.bottlenote.user.constant.AdminRole;
import java.util.List;

public record AdminSignupResponse(Long adminId, String email, String name, List<AdminRole> roles) {}

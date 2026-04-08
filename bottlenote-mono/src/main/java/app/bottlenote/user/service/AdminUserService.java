package app.bottlenote.user.service;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.request.AdminUserSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public GlobalResponse searchUsers(AdminUserSearchRequest request) {
    return GlobalResponse.fromPage(userRepository.searchAdminUsers(request));
  }
}

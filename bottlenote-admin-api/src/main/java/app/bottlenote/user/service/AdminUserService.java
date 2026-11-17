package app.bottlenote.user.service;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.dto.request.AdminUserSearchRequest;
import app.bottlenote.user.repository.QueryDslAdminUserQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

  private final QueryDslAdminUserQueryRepository queryRepository;

  public AdminUserService(QueryDslAdminUserQueryRepository queryRepository) {
    this.queryRepository = queryRepository;
  }

  @Transactional(readOnly = true)
  public GlobalResponse searchUsers(AdminUserSearchRequest request) {
    return GlobalResponse.fromPage(queryRepository.searchAdminUsers(request));
  }
}

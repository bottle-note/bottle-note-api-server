package app.bottlenote.user.service;

import app.bottlenote.user.repository.RootAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final RootAdminRepository rootAdminRepository;

  @Transactional(readOnly = true)
  public boolean checkAdminStatus(Long userId) {
    return rootAdminRepository.existsByUserId(userId);
  }
}

package app.bottlenote.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	@Transactional(readOnly = true)
	public boolean checkAdminStatus(Long userId) {

		return false;
	}
}

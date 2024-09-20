package app.bottlenote.history.service;

import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryDomainSupport {

	private final UserHistoryRepository userHistoryRepository;

	@Transactional
	public UserHistory saveHistory(UserHistory userHistory){
		log.info("히스토리 저장 성공!!!!");
		return userHistoryRepository.save(userHistory);
	}
}

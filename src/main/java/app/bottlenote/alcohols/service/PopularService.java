package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.Populars;
import app.bottlenote.alcohols.repository.PopularQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularService {

	private final PopularQueryRepository popularQueryRepository;

	@Transactional(readOnly = true)
	public List<Populars> getPopularOfWeek(Integer top, Long userId) {
		log.info("service point getPopularOfWeek - top: {}, userId: {}", top, userId);
		return popularQueryRepository.getPopularOfWeeks(userId, top);
	}
}

package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.alcohols.repository.PopularQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularService {

	private final PopularQueryRepository popularQueryRepository;

	@Transactional(readOnly = true)
	public List<PopularItem> getPopularOfWeek(Integer top, Long userId) {
		log.info("service point getPopularOfWeek - top: {}, userId: {}", top, userId);
		PageRequest pageRequest = PageRequest.of(0, top);
		List<PopularItem> popularItemList = popularQueryRepository.getPopularOfWeeks(userId, pageRequest);
		Collections.shuffle(popularItemList);
		return popularItemList;
	}
}

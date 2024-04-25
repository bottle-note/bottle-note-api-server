package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.Populars;
import app.bottlenote.alcohols.repository.PopularQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularService {

	private final PopularQueryRepository popularQueryRepository;

	@Transactional
	public List<Populars> getPopularOfWeek(Integer top) {
		return popularQueryRepository.getPopularOfWeek(PageRequest.of(0, top));
	}
}

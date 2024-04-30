package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.RegionsResponse;
import app.bottlenote.alcohols.repository.RegionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RegionService {
	private final RegionQueryRepository regionQueryRepository;

	@Transactional(readOnly = true)
	public List<RegionsResponse> findAll() {
		return regionQueryRepository.findAll()
			.stream()
			.map(region -> RegionsResponse.of(region.getId(), region.getKorName(), region.getEngName(), region.getDescription()))
			.collect(Collectors.toList());
	}
}

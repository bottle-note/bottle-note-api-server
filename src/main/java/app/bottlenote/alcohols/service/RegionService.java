package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.RegionsResponse;
import app.bottlenote.alcohols.repository.RegionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RegionService {
	private final RegionQueryRepository regionQueryRepository;

	@Transactional(readOnly = true)
	public List<RegionsResponse> findAll() {
		return regionQueryRepository.findAllRegionsResponse();
	}
}

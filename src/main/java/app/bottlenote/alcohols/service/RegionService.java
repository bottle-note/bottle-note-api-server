package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.RegionsResponse;
import app.bottlenote.alcohols.repository.RegionQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.time.LocalDateTime.now;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegionService {
	private final RegionQueryRepository regionQueryRepository;

	@Cacheable(value = "LC-Region")
	@Transactional(readOnly = true)
	public List<RegionsResponse> findAll() {
		log.info("RegionService.findAll() called , {}", now());
		return regionQueryRepository.findAllRegionsResponse();
	}
}

package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.constant.AlcoholType;
import app.bottlenote.alcohols.dto.response.CategoryResponse;
import app.bottlenote.alcohols.dto.response.RegionsResponse;
import app.bottlenote.alcohols.repository.RegionQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.time.LocalDateTime.now;

@RequiredArgsConstructor
@Slf4j
@Service
public class AlcoholReferenceService {
	private final RegionQueryRepository regionQueryRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;

	@Cacheable(value = "local_cache_alcohol_region_information")
	@Transactional(readOnly = true)
	public List<RegionsResponse> findAllRegion() {
		log.info("RegionService.findAll() called , {}", now());
		return regionQueryRepository.findAllRegionsResponse();
	}

	@Cacheable(value = "local_cache_alcohol_category_information")
	@Transactional(readOnly = true)
	public List<CategoryResponse> getAlcoholCategory(AlcoholType type) {
		return alcoholQueryRepository.findAllCategories(type);
	}
}

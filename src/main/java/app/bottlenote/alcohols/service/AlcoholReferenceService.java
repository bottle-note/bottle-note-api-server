package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import app.bottlenote.alcohols.repository.RegionQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.time.LocalDateTime.now;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholReferenceService {
	private final RegionQueryRepository regionQueryRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;

	@Cacheable(value = "local_cache_alcohol_region_information")
	@Transactional(readOnly = true)
	public List<RegionsItem> findAllRegion() {
		log.info("RegionService.findAll() called , {}", now());
		return regionQueryRepository.findAllRegionsResponse();
	}

	@Cacheable(value = "local_cache_alcohol_category_information")
	@Transactional(readOnly = true)
	public List<CategoryItem> getAlcoholCategory(AlcoholType type) {
		return alcoholQueryRepository.findAllCategories(type);
	}
}

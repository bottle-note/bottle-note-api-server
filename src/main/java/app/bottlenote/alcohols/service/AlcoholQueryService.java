package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.global.service.cursor.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholQueryService {

	private final AlcoholQueryRepository alcoholQueryRepository;

	public PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchRequest request, Long userId) {

		AlcoholSearchCriteria criteria = AlcoholSearchCriteria.of(request, userId);

		return alcoholQueryRepository.searchAlcohols(criteria);
	}
}

package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static app.bottlenote.alcohols.dto.response.AlcoholSearchResponse.SearchDetail;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholQueryService {

	public AlcoholSearchResponse searchAlcohols(AlcoholSearchRequest request, Long id) {
		List<SearchDetail> searchDetails = List.of(
			new SearchDetail("imageUrl", 1L, "korName1", "engName1", "categoryName1", null, 1L, false),
			new SearchDetail("imageUrl", 2L, "korName2", "engName2", "categoryName2", null, 1L, false),
			new SearchDetail("imageUrl", 3L, "korName3", "engName3", "categoryName3", null, 1L, false)
		);

		return AlcoholSearchResponse.of(3L, searchDetails);
	}
}

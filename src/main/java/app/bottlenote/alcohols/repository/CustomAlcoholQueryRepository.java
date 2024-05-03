package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.global.service.cursor.PageResponse;

public interface CustomAlcoholQueryRepository {

	PageResponse<?> searchAlcohols(AlcoholSearchCriteria criteriaDto);
}

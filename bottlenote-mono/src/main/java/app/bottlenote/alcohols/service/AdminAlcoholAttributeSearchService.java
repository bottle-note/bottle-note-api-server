package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeFacetsResponse;
import app.bottlenote.alcohols.dto.response.AdminAttributedAlcoholItem;
import org.springframework.data.domain.Page;

public interface AdminAlcoholAttributeSearchService {
  AdminAlcoholAttributeFacetsResponse facets(AdminAlcoholCompoundSearchRequest request);

  Page<AdminAttributedAlcoholItem> searchAlcohols(AdminAttributedAlcoholSearchRequest request);

  Page<AdminAttributedAlcoholItem> searchAlcohols(AdminAlcoholCompoundSearchRequest request);
}

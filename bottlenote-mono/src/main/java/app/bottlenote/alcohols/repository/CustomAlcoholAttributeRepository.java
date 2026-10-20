package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeFacetCount;
import app.bottlenote.global.pagination.OffsetPage;
import java.util.List;

public interface CustomAlcoholAttributeRepository {
  List<AlcoholAttributeFacetCount> findFacets(
      AdminAlcoholCompoundSearchRequest request, java.util.Map<String, Long> attributeIds);

  OffsetPage<AlcoholAttribute> search(AdminAlcoholAttributeSearchRequest request);

  OffsetPage<Long> findAlcoholIds(AdminAttributedAlcoholSearchRequest request);

  OffsetPage<Long> findAlcoholIds(
      AdminAlcoholCompoundSearchRequest request, java.util.Map<String, Long> attributeIds);
}

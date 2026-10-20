package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.global.pagination.OffsetPage;

public interface CustomAlcoholAttributeRepository {
  OffsetPage<AlcoholAttribute> search(AdminAlcoholAttributeSearchRequest request);

  OffsetPage<Long> findAlcoholIds(AdminAttributedAlcoholSearchRequest request);

  OffsetPage<Long> findAlcoholIds(
      AdminAlcoholCompoundSearchRequest request, java.util.Map<String, Long> attributeIds);
}

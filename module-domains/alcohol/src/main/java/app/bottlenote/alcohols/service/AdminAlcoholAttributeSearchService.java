package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.dto.dsl.AlcoholCandidateCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeFacetsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse;
import app.bottlenote.alcohols.dto.response.AdminAttributedAlcoholItem;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public interface AdminAlcoholAttributeSearchService {
  AdminAlcoholCandidateResponse findCandidates(Long groupId, AdminAlcoholCandidateRequest request);

  AdminAlcoholCandidateResponse findCandidates(
      AlcoholCandidateCriteria query,
      Map<Long, AlcoholAttribute> definitions,
      List<TastingTagGroup> groups);

  AdminAlcoholAttributeFacetsResponse facets(AdminAlcoholCompoundSearchRequest request);

  Page<AdminAttributedAlcoholItem> searchAlcohols(AdminAttributedAlcoholSearchRequest request);

  Page<AdminAttributedAlcoholItem> searchAlcohols(AdminAlcoholCompoundSearchRequest request);
}

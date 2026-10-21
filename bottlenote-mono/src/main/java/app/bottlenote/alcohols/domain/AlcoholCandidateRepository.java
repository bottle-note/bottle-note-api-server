package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.dsl.AlcoholCandidateCriteria;
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse.CandidateGroup;
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse.CandidateTag;
import java.util.List;
import java.util.Optional;

public interface AlcoholCandidateRepository {
  Optional<CandidateGroup> findGroup(Long id);

  List<AlcoholAttribute> findAttributes(Long groupId);

  long count(AlcoholCandidateCriteria query);

  List<Alcohol> findPage(AlcoholCandidateCriteria query);

  List<AlcoholAttributeMapping> findMappings(Long groupId, List<Long> alcoholIds);

  List<TagRow> findTags(List<Long> alcoholIds);

  List<FacetRow> findFacetRows(AlcoholCandidateCriteria query);

  List<String> findAttributeOptions(Long groupId, Long attributeId, int limit);

  record TagRow(Long alcoholId, CandidateTag tag) {}

  record FacetRow(Long alcoholId, String value) {}
}

package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.candidate.AlcoholCandidateQuery;
import app.bottlenote.alcohols.dto.response.AlcoholCandidateResponse.CandidateGroup;
import app.bottlenote.alcohols.dto.response.AlcoholCandidateResponse.CandidateTag;
import java.util.List;
import java.util.Optional;

public interface AlcoholCandidateRepository {
  Optional<CandidateGroup> findGroup(Long id);

  List<AlcoholAttribute> findAttributes(Long groupId);

  long count(AlcoholCandidateQuery query);

  List<Alcohol> findPage(AlcoholCandidateQuery query, Long afterId, int limit);

  List<AlcoholAttributeMapping> findMappings(Long groupId, List<Long> alcoholIds);

  List<TagRow> findTags(List<Long> alcoholIds);

  List<FacetRow> findFacetRows(AlcoholCandidateQuery query);

  List<String> findAttributeOptions(Long groupId, Long attributeId, int limit);

  record TagRow(Long alcoholId, CandidateTag tag) {}

  record FacetRow(Long alcoholId, String value) {}
}

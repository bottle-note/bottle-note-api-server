package app.bottlenote.curation.service;

import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_SPEC_NOT_FOUND;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.response.CurationSpecListResponse;
import app.bottlenote.curation.dto.response.CurationSpecResponse;
import app.bottlenote.curation.exception.CurationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurationSpecQueryService {

  private final CurationSpecRepository curationSpecRepository;

  @Cacheable(value = "local_cache_curation_spec_list")
  @Transactional(readOnly = true)
  public List<CurationSpecListResponse> listActiveSpecs() {
    return curationSpecRepository.findAllActiveSpecSummaries();
  }

  @Cacheable(value = "local_cache_curation_spec_detail", key = "#specId")
  @Transactional(readOnly = true)
  public CurationSpecResponse getSpecDetail(Long specId) {
    return toSpecResponse(getSpec(specId));
  }

  @Cacheable(value = "local_cache_curation_spec_detail", key = "'active:' + #specId")
  @Transactional(readOnly = true)
  public CurationSpecResponse getActiveSpecDetail(Long specId) {
    return toSpecResponse(getActiveSpec(specId));
  }

  private CurationSpec getSpec(Long specId) {
    return curationSpecRepository
        .findById(specId)
        .orElseThrow(() -> new CurationException(CURATION_SPEC_NOT_FOUND));
  }

  private CurationSpec getActiveSpec(Long specId) {
    return curationSpecRepository
        .findByIdAndIsActiveTrue(specId)
        .orElseThrow(() -> new CurationException(CURATION_SPEC_NOT_FOUND));
  }

  private CurationSpecResponse toSpecResponse(CurationSpec spec) {
    return new CurationSpecResponse(
        spec.getId(),
        spec.getCode(),
        spec.getName(),
        spec.getDescription(),
        spec.getHydratorKey(),
        spec.getVersion(),
        spec.getIsActive(),
        spec.getRequestSpec(),
        spec.getResponseSpec());
  }
}

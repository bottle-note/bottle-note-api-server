package app.bottlenote.curation.service;

import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_NOT_FOUND;
import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_SPEC_DUPLICATE_CODE;
import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_SPEC_NOT_FOUND;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import app.bottlenote.curation.domain.CurationRepository;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import app.bottlenote.curation.dto.request.CurationSpecCreateRequest;
import app.bottlenote.curation.exception.CurationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurationV2Service {

  private final CurationSpecRepository curationSpecRepository;
  private final CurationRepository curationRepository;
  private final CurationExtensionRepository curationExtensionRepository;

  @Transactional
  public CurationSpec createSpec(CurationSpecCreateRequest command) {
    if (curationSpecRepository.existsByCode(command.code())) {
      throw new CurationException(CURATION_SPEC_DUPLICATE_CODE);
    }

    CurationSpec curationSpec =
        CurationSpec.builder()
            .code(command.code())
            .name(command.name())
            .description(command.description())
            .requestSpec(command.requestSpec())
            .responseSpec(command.responseSpec())
            .hydratorKey(command.hydratorKey())
            .version(command.version() != null ? command.version() : 1)
            .isActive(true)
            .build();
    return curationSpecRepository.save(curationSpec);
  }

  @Transactional
  public Curation createCuration(CurationCreateRequest command) {
    CurationSpec curationSpec = getSpec(command.specId());
    Curation curation =
        Curation.builder()
            .specId(curationSpec.getId())
            .name(command.name())
            .description(command.description())
            .coverImageUrl(command.imageUrls().get(0))
            .imageUrl2(command.imageUrls().size() > 1 ? command.imageUrls().get(1) : null)
            .imageUrl3(command.imageUrls().size() > 2 ? command.imageUrls().get(2) : null)
            .exposureStartDate(command.exposureStartDate())
            .exposureEndDate(command.exposureEndDate())
            .displayOrder(command.displayOrder() != null ? command.displayOrder() : 0)
            .isActive(command.isActive())
            .build();

    Curation saved = curationRepository.save(curation);
    curationExtensionRepository.save(
        CurationExtension.builder()
            .curationId(saved.getId())
            .specId(curationSpec.getId())
            .payload(command.payload())
            .build());
    return saved;
  }

  @Transactional(readOnly = true)
  public CurationSpec getSpec(Long specId) {
    return curationSpecRepository
        .findById(specId)
        .orElseThrow(() -> new CurationException(CURATION_SPEC_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public Curation getCuration(Long curationId) {
    return curationRepository
        .findById(curationId)
        .orElseThrow(() -> new CurationException(CURATION_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public CurationExtension getExtension(Long curationId) {
    return curationExtensionRepository
        .findByCurationId(curationId)
        .orElseThrow(() -> new CurationException(CURATION_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<Curation> findActiveCurations() {
    return curationRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();
  }
}

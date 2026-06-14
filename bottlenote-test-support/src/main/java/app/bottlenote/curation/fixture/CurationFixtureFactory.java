package app.bottlenote.curation.fixture;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import app.bottlenote.curation.domain.CurationRepository;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import java.util.LinkedHashMap;
import java.util.Map;

public class CurationFixtureFactory {

  private final CurationSpecRepository curationSpecRepository;
  private final CurationRepository curationRepository;
  private final CurationExtensionRepository curationExtensionRepository;

  public CurationFixtureFactory(
      CurationSpecRepository curationSpecRepository,
      CurationRepository curationRepository,
      CurationExtensionRepository curationExtensionRepository) {
    this.curationSpecRepository = curationSpecRepository;
    this.curationRepository = curationRepository;
    this.curationExtensionRepository = curationExtensionRepository;
  }

  public CurationSpec saveSpec(
      String code,
      String name,
      String description,
      Map<String, Object> requestSpec,
      Map<String, Object> responseSpec,
      String hydratorKey,
      Integer version) {
    return curationSpecRepository.save(
        CurationSpec.builder()
            .code(code)
            .name(name)
            .description(description)
            .requestSpec(copyOf(requestSpec))
            .responseSpec(copyOf(responseSpec))
            .hydratorKey(hydratorKey)
            .version(version != null ? version : 1)
            .isActive(true)
            .build());
  }

  public Curation saveCuration(CurationCreateRequest request) {
    Curation saved =
        curationRepository.save(
            Curation.builder()
                .specId(request.specId())
                .name(request.name())
                .description(request.description())
                .coverImageUrl(request.imageUrls().get(0))
                .imageUrl2(request.imageUrls().size() > 1 ? request.imageUrls().get(1) : null)
                .imageUrl3(request.imageUrls().size() > 2 ? request.imageUrls().get(2) : null)
                .exposureStartDate(request.exposureStartDate())
                .exposureEndDate(request.exposureEndDate())
                .displayOrder(request.displayOrder())
                .isActive(request.isActive())
                .build());
    curationExtensionRepository.save(
        CurationExtension.builder()
            .curationId(saved.getId())
            .specId(request.specId())
            .payload(request.payload())
            .build());
    return saved;
  }

  private static Map<String, Object> copyOf(Map<String, Object> value) {
    return value != null ? new LinkedHashMap<>(value) : new LinkedHashMap<>();
  }
}

package app.bottlenote.curation.service;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.response.CurationSpecSyncResponse;
import app.bottlenote.curation.service.CurationPayloadValidator.MapBackedSchema;
import app.bottlenote.curation.support.CurationSpecResourceReader;
import app.bottlenote.curation.support.CurationSpecResourceReader.CurationSpecResourceDocument;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurationSpecResourceSyncService {

  private final CurationSpecRepository curationSpecRepository;
  private final CurationSpecResourceReader curationSpecResourceReader;
  private final CurationPayloadValidator curationPayloadValidator;

  @CacheEvict(
      value = {"local_cache_curation_spec_list", "local_cache_curation_spec_detail"},
      allEntries = true)
  @Transactional
  public CurationSpecSyncResponse sync() {
    int createdCount = 0;
    int updatedCount = 0;

    for (CurationSpecResourceDocument specDocument : curationSpecResourceReader.readAll()) {
      validateSpecDocument(specDocument);
      Optional<CurationSpec> existingSpec = curationSpecRepository.findByCode(specDocument.code());
      if (existingSpec.isPresent()) {
        curationSpecRepository.save(update(existingSpec.get(), specDocument));
        updatedCount++;
      } else {
        curationSpecRepository.save(create(specDocument));
        createdCount++;
      }
    }

    return new CurationSpecSyncResponse(createdCount, updatedCount);
  }

  private void validateSpecDocument(CurationSpecResourceDocument specDocument) {
    var errors =
        new ArrayList<>(
            curationPayloadValidator.validateSpec(
                specDocument.code() + "#Request", new MapBackedSchema(specDocument.requestSpec())));
    errors.addAll(
        curationPayloadValidator.validateSpec(
            specDocument.code() + "#Response", new MapBackedSchema(specDocument.responseSpec())));
    if (!errors.isEmpty()) {
      throw new IllegalStateException(
          specDocument.code() + " 큐레이션 스펙이 유효하지 않습니다.\n" + String.join("\n", errors));
    }
  }

  private CurationSpec update(
      CurationSpec curationSpec, CurationSpecResourceDocument specDocument) {
    curationSpec.update(
        specDocument.name(),
        specDocument.description(),
        specDocument.requestSpec(),
        specDocument.responseSpec(),
        specDocument.hydratorKey(),
        specDocument.version(),
        true);
    return curationSpec;
  }

  private CurationSpec create(CurationSpecResourceDocument specDocument) {
    return CurationSpec.builder()
        .code(specDocument.code())
        .name(specDocument.name())
        .description(specDocument.description())
        .requestSpec(specDocument.requestSpec())
        .responseSpec(specDocument.responseSpec())
        .hydratorKey(specDocument.hydratorKey())
        .version(specDocument.version())
        .isActive(true)
        .build();
  }
}

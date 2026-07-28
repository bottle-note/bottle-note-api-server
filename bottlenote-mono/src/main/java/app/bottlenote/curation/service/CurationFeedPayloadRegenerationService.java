package app.bottlenote.curation.service;

import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 스펙의 x-feed가 바뀌면 저장된 feed_payload가 낡는다. 바뀐 스펙의 큐레이션만 다시 만든다.
@Service
@RequiredArgsConstructor
@Slf4j
public class CurationFeedPayloadRegenerationService {

  private final CurationSpecRepository curationSpecRepository;
  private final CurationExtensionRepository curationExtensionRepository;
  private final CurationFeedProjector feedProjector;

  @Transactional
  public int regenerate(Collection<Long> specIds) {
    if (specIds == null || specIds.isEmpty()) {
      return 0;
    }
    Map<Long, CurationSpec> specs =
        curationSpecRepository.findAllByIdIn(Set.copyOf(specIds)).stream()
            .collect(Collectors.toMap(CurationSpec::getId, Function.identity()));

    List<CurationExtension> extensions =
        curationExtensionRepository.findAllBySpecIdIn(specs.keySet());
    int regenerated = 0;
    for (CurationExtension extension : extensions) {
      CurationSpec spec = specs.get(extension.getSpecId());
      if (spec == null) {
        continue;
      }
      // feed_payload가 NULL인 레거시 행도 대상이다. 재생성이 backfill을 겸한다.
      extension.updateFeedPayload(
          feedProjector.extractFeedPayload(spec.getResponseSpec(), extension.getPayload()));
      curationExtensionRepository.save(extension);
      regenerated++;
    }
    log.info("큐레이션 feed_payload 재생성 완료: specIds={}, curations={}", specs.keySet(), regenerated);
    return regenerated;
  }
}

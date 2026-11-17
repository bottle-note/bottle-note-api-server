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

  // 재생성 전에 먼저 비운다. 이후 단계가 실패해도 NULL fallback으로 정확한 응답이 나가기 때문이다.
  // sync가 DB 스펙을 이미 덮어써서 다음 기동에는 "변경 없음"으로 보이므로, 낡은 값을 남기면 영구히 굳는다.
  @Transactional
  public int invalidate(Collection<Long> specIds) {
    List<CurationExtension> extensions =
        curationExtensionRepository.findAllBySpecIdIn(Set.copyOf(specIds));
    extensions.forEach(extension -> extension.updateFeedPayload(null));
    log.info("큐레이션 feed_payload 무효화: specIds={}, curations={}", specIds, extensions.size());
    return extensions.size();
  }

  @Transactional
  public int regenerate(Collection<Long> specIds) {
    Map<Long, CurationSpec> specs =
        curationSpecRepository.findAllByIdIn(Set.copyOf(specIds)).stream()
            .collect(Collectors.toMap(CurationSpec::getId, Function.identity()));

    // feed_payload가 NULL인 레거시 행도 대상이다. 재생성이 backfill을 겸한다.
    List<CurationExtension> extensions =
        curationExtensionRepository.findAllBySpecIdIn(specs.keySet());
    extensions.forEach(
        extension ->
            extension.updateFeedPayload(
                feedProjector.extractFeedPayload(
                    specs.get(extension.getSpecId()).getResponseSpec(), extension.getPayload())));
    log.info(
        "큐레이션 feed_payload 재생성 완료: specIds={}, curations={}", specs.keySet(), extensions.size());
    return extensions.size();
  }
}

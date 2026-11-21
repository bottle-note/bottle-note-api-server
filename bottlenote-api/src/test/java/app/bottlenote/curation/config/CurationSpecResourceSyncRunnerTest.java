package app.bottlenote.curation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import app.bottlenote.curation.service.CurationFeedPayloadRefreshService;
import app.bottlenote.curation.service.CurationPayloadValidator;
import app.bottlenote.curation.service.CurationSpecResourceSyncService;
import app.bottlenote.curation.support.CurationSpecFingerprint;
import app.bottlenote.curation.support.CurationSpecResourceReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 동시 기동 충돌을 결정적으로 만들어 runner가 실제로 다시 시도하는지 확인한다.
 *
 * <p>통합 테스트(6스레드)는 충돌이 나지 않는 실행에서도 통과할 수 있다. 여기서는 다른 인스턴스가 먼저 같은 code를 커밋한 상황을 저장소 대역으로 만든다.
 */
@Tag("unit")
@DisplayName("[unit] 큐레이션 스펙 동기화 runner의 동시 기동 충돌 처리")
class CurationSpecResourceSyncRunnerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("다른 인스턴스가 먼저 같은 code를 저장해 충돌할 때 다시 시도해 update 경로로 수렴한다")
  void 충돌_뒤_재시도로_수렴할_수_있다() {
    // given
    ConflictingCurationSpecRepository repository = new ConflictingCurationSpecRepository(1);
    RecordingRefreshService refreshService = new RecordingRefreshService();
    CurationSpecResourceSyncRunner runner =
        new CurationSpecResourceSyncRunner(syncService(repository), refreshService);

    // when
    runner.sync();

    // then
    assertThat(repository.conflicts).isEqualTo(1);
    List<String> codes =
        repository.findAllByIsActiveTrueOrderByIdAsc().stream().map(CurationSpec::getCode).toList();
    assertThat(codes).doesNotHaveDuplicates().contains("RECOMMENDED_WHISKY");
    assertThat(codes).hasSize(resourceCount());
    assertThat(refreshService.calls).hasSize(1);
  }

  @Test
  @DisplayName("충돌이 최대 시도 횟수까지 이어질 때 기동을 실패시키고 Read Model 갱신을 하지 않는다")
  void 충돌이_계속되면_기동을_실패시킬_수_있다() {
    // given
    ConflictingCurationSpecRepository repository =
        new ConflictingCurationSpecRepository(Integer.MAX_VALUE);
    RecordingRefreshService refreshService = new RecordingRefreshService();
    CurationSpecResourceSyncRunner runner =
        new CurationSpecResourceSyncRunner(syncService(repository), refreshService);

    // when & then
    assertThatThrownBy(runner::sync).isInstanceOf(DataIntegrityViolationException.class);
    assertThat(repository.conflicts).isEqualTo(3);
    assertThat(refreshService.calls).isEmpty();
  }

  private CurationSpecResourceSyncService syncService(InMemoryCurationSpecRepository repository) {
    return new CurationSpecResourceSyncService(
        repository,
        resourceReader(),
        new CurationPayloadValidator(objectMapper),
        new CurationSpecFingerprint(objectMapper));
  }

  private CurationSpecResourceReader resourceReader() {
    return new CurationSpecResourceReader(new PathMatchingResourcePatternResolver(), objectMapper);
  }

  private int resourceCount() {
    return resourceReader().readAll().size();
  }

  // 새 스펙 저장 시 다른 인스턴스가 같은 code를 먼저 커밋한 상황을 정해진 횟수만큼 만든다
  private static final class ConflictingCurationSpecRepository
      extends InMemoryCurationSpecRepository {

    private final int maxConflicts;
    private int conflicts;

    private ConflictingCurationSpecRepository(int maxConflicts) {
      this.maxConflicts = maxConflicts;
    }

    @Override
    public CurationSpec save(CurationSpec curationSpec) {
      if (curationSpec.getId() == null && conflicts < maxConflicts) {
        conflicts++;
        if (findByCode(curationSpec.getCode()).isEmpty()) {
          super.save(curationSpec);
        }
        throw new DataIntegrityViolationException(
            "Duplicate entry '" + curationSpec.getCode() + "' for key 'uk_curation_spec_code'");
      }
      return super.save(curationSpec);
    }
  }

  private static final class RecordingRefreshService extends CurationFeedPayloadRefreshService {

    private final List<Collection<Long>> calls = new ArrayList<>();

    private RecordingRefreshService() {
      super(null, null);
    }

    @Override
    public void refresh(Collection<Long> specIds) {
      calls.add(specIds);
    }
  }
}

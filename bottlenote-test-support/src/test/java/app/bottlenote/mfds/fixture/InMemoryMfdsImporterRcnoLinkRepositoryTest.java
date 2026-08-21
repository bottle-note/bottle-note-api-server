package app.bottlenote.mfds.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLink;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("InMemoryMfdsImporterRcnoLinkRepository 단위 테스트")
class InMemoryMfdsImporterRcnoLinkRepositoryTest {

  private InMemoryMfdsImporterRcnoLinkRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryMfdsImporterRcnoLinkRepository();
  }

  @Test
  @DisplayName("연결 근거를 저장할 때 rcno를 키로 조회할 수 있다")
  void 연결_근거를_저장하고_rcno로_조회할_수_있다() {
    MfdsImporterRcnoLink link =
        MfdsTestData.rcnoLink("RCNO-001", 5L, "보틀상사", MfdsImporterLinkSource.PAGE_RCNO);

    repository.save(link);

    assertThat(repository.findByRcno("RCNO-001")).contains(link);
    assertThat(repository.findByRcno("RCNO-999")).isEmpty();
  }

  @Test
  @DisplayName("같은 rcno를 다시 저장할 때 기존 연결을 대체한다")
  void 같은_rcno_저장_시_기존_연결을_대체할_수_있다() {
    repository.save(MfdsTestData.rcnoLink("RCNO-001", 5L, "보틀상사", MfdsImporterLinkSource.PAGE_RCNO));
    MfdsImporterRcnoLink replaced =
        MfdsTestData.rcnoLink("RCNO-001", 7L, "노트무역", MfdsImporterLinkSource.PAGE_NAME);

    repository.save(replaced);

    assertThat(repository.findByRcno("RCNO-001")).contains(replaced);
    assertThat(repository.countByImporterId(5L)).isZero();
    assertThat(repository.countByImporterId(7L)).isEqualTo(1L);
  }

  @Test
  @DisplayName("수입사 ID로 조회할 때 연결된 rcno 목록을 오름차순으로 반환한다")
  void 수입사_ID로_연결_목록을_조회할_수_있다() {
    repository.save(MfdsTestData.rcnoLink("RCNO-002", 5L, "보틀상사", MfdsImporterLinkSource.PAGE_RCNO));
    repository.save(MfdsTestData.rcnoLink("RCNO-001", 5L, "보틀상사", MfdsImporterLinkSource.PAGE_NAME));
    repository.save(MfdsTestData.rcnoLink("RCNO-003", 9L, "노트무역", MfdsImporterLinkSource.PAGE_RCNO));

    List<MfdsImporterRcnoLink> result = repository.findAllByImporterId(5L);

    assertThat(result)
        .extracting(MfdsImporterRcnoLink::getRcno)
        .containsExactly("RCNO-001", "RCNO-002");
    assertThat(repository.countByImporterId(5L)).isEqualTo(2L);
  }
}

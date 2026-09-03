package app.bottlenote.mfds.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.dto.dsl.MfdsImporterSearchCriteria;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("InMemoryMfdsImporterRepository 단위 테스트")
class InMemoryMfdsImporterRepositoryTest {

  private InMemoryMfdsImporterRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryMfdsImporterRepository();
  }

  @Test
  @DisplayName("수입사를 저장할 때 ID를 부여하고 공식 업소 코드로 조회할 수 있다")
  void 수입사를_저장하고_공식_업소_코드로_조회할_수_있다() {
    MfdsImporter importer =
        MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE);

    repository.save(importer);

    assertThat(importer.getId()).isEqualTo(1L);
    assertThat(repository.findByOfficialBusinessCode("BIZ-001")).contains(importer);
    assertThat(repository.findById(1L)).contains(importer);
  }

  @Test
  @DisplayName("관리 상태로 필터링할 때 해당 상태의 수입사만 반환한다")
  void 관리_상태로_필터링할_수_있다() {
    repository.save(MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    repository.save(MfdsTestData.importer("BIZ-002", "노트무역", MfdsImporterAdminStatus.INACTIVE));

    MfdsImporterSearchCriteria criteria =
        new MfdsImporterSearchCriteria(MfdsImporterAdminStatus.ACTIVE, null, 0L, 20L);

    List<MfdsImporter> result = repository.searchByCriteria(criteria);

    assertThat(result).extracting(MfdsImporter::getBusinessName).containsExactly("보틀상사");
    assertThat(repository.countByCriteria(criteria)).isEqualTo(1L);
  }

  @Test
  @DisplayName("식별자와 관리 상태로 일괄 조회할 때 해당 상태의 수입사만 반환한다")
  void 식별자와_관리_상태로_일괄_조회할_수_있다() {
    MfdsImporter active =
        repository.save(MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    MfdsImporter inactive =
        repository.save(MfdsTestData.importer("BIZ-002", "노트무역", MfdsImporterAdminStatus.INACTIVE));

    List<MfdsImporter> result =
        repository.findAllByIdInAndAdminStatus(
            List.of(active.getId(), inactive.getId()), MfdsImporterAdminStatus.ACTIVE);

    assertThat(result).containsExactly(active);
  }

  @Test
  @DisplayName("식별자 목록이 비어 있으면 일괄 조회는 빈 결과를 반환한다")
  void 빈_식별자_목록은_빈_결과를_반환한다() {
    repository.save(MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));

    assertThat(repository.findAllByIdInAndAdminStatus(List.of(), MfdsImporterAdminStatus.ACTIVE))
        .isEmpty();
  }

  @Test
  @DisplayName("키워드로 검색할 때 수입사명·인허가 번호·업소 코드를 부분 일치로 조회한다")
  void 키워드로_검색할_수_있다() {
    MfdsImporter nameMatch =
        MfdsTestData.importer("BIZ-001", "글렌 임포터", MfdsImporterAdminStatus.ACTIVE);
    MfdsImporter codeMatch =
        MfdsTestData.importer("BIZ-GLEN", "노트무역", MfdsImporterAdminStatus.ACTIVE);
    repository.save(nameMatch);
    repository.save(codeMatch);
    repository.save(MfdsTestData.importer("BIZ-003", "보틀상사", MfdsImporterAdminStatus.ACTIVE));

    List<MfdsImporter> result =
        repository.searchByCriteria(new MfdsImporterSearchCriteria(null, "glen", 0L, 20L));

    assertThat(result).containsExactly(codeMatch);

    List<MfdsImporter> koResult =
        repository.searchByCriteria(new MfdsImporterSearchCriteria(null, "글렌", 0L, 20L));
    assertThat(koResult).containsExactly(nameMatch);
  }

  @Test
  @DisplayName("커서 페이징으로 조회할 때 id 내림차순과 pageSize+1 조회를 지킨다")
  void 커서_페이징으로_조회할_수_있다() {
    for (int index = 1; index <= 4; index++) {
      repository.save(
          MfdsTestData.importer("BIZ-00" + index, "수입사" + index, MfdsImporterAdminStatus.ACTIVE));
    }

    List<MfdsImporter> firstPage =
        repository.searchByCriteria(MfdsImporterSearchCriteria.of(0L, 2L));
    assertThat(firstPage).extracting(MfdsImporter::getId).containsExactly(4L, 3L, 2L);

    List<MfdsImporter> secondPage =
        repository.searchByCriteria(MfdsImporterSearchCriteria.of(3L, 2L));
    assertThat(secondPage).extracting(MfdsImporter::getId).containsExactly(2L, 1L);
  }
}

package app.bottlenote.mfds.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("InMemoryMfdsDeclarationRepository 단위 테스트")
class InMemoryMfdsDeclarationRepositoryTest {

  private InMemoryMfdsDeclarationRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryMfdsDeclarationRepository();
  }

  @Test
  @DisplayName("신고 데이터를 저장할 때 ID를 부여하고 rcno로 조회할 수 있다")
  void 신고_데이터를_저장하고_rcno로_조회할_수_있다() {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            "RCNO-001", MfdsNormalizationStatus.PENDING, null, null, null, "글렌피딕", "glenfiddich");

    repository.save(declaration);

    assertThat(declaration.getId()).isEqualTo(1L);
    assertThat(repository.findByRcno("RCNO-001")).contains(declaration);
    assertThat(repository.findById(1L)).contains(declaration);
  }

  @Test
  @DisplayName("정규화 상태로 필터링할 때 해당 상태의 신고 데이터만 반환한다")
  void 정규화_상태로_필터링할_수_있다() {
    repository.save(declaration("RCNO-001", MfdsNormalizationStatus.PENDING));
    repository.save(declaration("RCNO-002", MfdsNormalizationStatus.NORMALIZED));
    repository.save(declaration("RCNO-003", MfdsNormalizationStatus.NORMALIZED));

    MfdsDeclarationSearchCriteria criteria =
        new MfdsDeclarationSearchCriteria(
            MfdsNormalizationStatus.NORMALIZED, null, null, null, null, 0L, 20L);

    List<MfdsDeclaration> result = repository.searchByCriteria(criteria);

    assertThat(result).extracting(MfdsDeclaration::getRcno).containsExactly("RCNO-003", "RCNO-002");
    assertThat(repository.countByCriteria(criteria)).isEqualTo(2L);
  }

  @Test
  @DisplayName("매칭 여부로 필터링할 때 selectedAlcoholId 존재 기준으로 구분한다")
  void 매칭_여부로_필터링할_수_있다() {
    MfdsDeclaration matched =
        MfdsTestData.declaration(
            "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, 77L, "AUTO_ACCEPT", null, null);
    repository.save(matched);
    repository.save(declaration("RCNO-002", MfdsNormalizationStatus.NORMALIZED));

    List<MfdsDeclaration> matchedResult =
        repository.searchByCriteria(
            new MfdsDeclarationSearchCriteria(null, true, null, null, null, 0L, 20L));
    List<MfdsDeclaration> unmatchedResult =
        repository.searchByCriteria(
            new MfdsDeclarationSearchCriteria(null, false, null, null, null, 0L, 20L));

    assertThat(matchedResult).containsExactly(matched);
    assertThat(unmatchedResult).extracting(MfdsDeclaration::getRcno).containsExactly("RCNO-002");
  }

  @Test
  @DisplayName("매칭 결정 값으로 필터링할 때 정확히 일치하는 행만 반환한다")
  void 매칭_결정으로_필터링할_수_있다() {
    MfdsDeclaration autoAccepted =
        MfdsTestData.declaration(
            "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, 77L, "AUTO_ACCEPT", null, null);
    MfdsDeclaration reviewNeeded =
        MfdsTestData.declaration(
            "RCNO-002", MfdsNormalizationStatus.NORMALIZED, null, null, "REVIEW", null, null);
    repository.save(autoAccepted);
    repository.save(reviewNeeded);

    List<MfdsDeclaration> result =
        repository.searchByCriteria(
            new MfdsDeclarationSearchCriteria(null, null, "REVIEW", null, null, 0L, 20L));

    assertThat(result).containsExactly(reviewNeeded);
  }

  @Test
  @DisplayName("수입사 ID로 필터링할 때 연결된 신고 데이터만 반환한다")
  void 수입사_ID로_필터링할_수_있다() {
    MfdsDeclaration linked =
        MfdsTestData.declaration(
            "RCNO-001", MfdsNormalizationStatus.NORMALIZED, 5L, null, null, null, null);
    repository.save(linked);
    repository.save(declaration("RCNO-002", MfdsNormalizationStatus.NORMALIZED));

    List<MfdsDeclaration> result =
        repository.searchByCriteria(
            new MfdsDeclarationSearchCriteria(null, null, null, 5L, null, 0L, 20L));

    assertThat(result).containsExactly(linked);
  }

  @Test
  @DisplayName("키워드로 검색할 때 한글/영문 검색 키와 rcno를 부분 일치로 조회한다")
  void 키워드로_검색할_수_있다() {
    MfdsDeclaration koMatch =
        MfdsTestData.declaration(
            "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null, "글렌피딕 12년", null);
    MfdsDeclaration enMatch =
        MfdsTestData.declaration(
            "RCNO-002", MfdsNormalizationStatus.NORMALIZED, null, null, null, null, "GLENFIDDICH 12");
    MfdsDeclaration rcnoMatch =
        MfdsTestData.declaration(
            "RCNO-GLEN", MfdsNormalizationStatus.NORMALIZED, null, null, null, null, null);
    MfdsDeclaration noMatch =
        MfdsTestData.declaration(
            "RCNO-004", MfdsNormalizationStatus.NORMALIZED, null, null, null, "맥캘란", "macallan");
    repository.save(koMatch);
    repository.save(enMatch);
    repository.save(rcnoMatch);
    repository.save(noMatch);

    List<MfdsDeclaration> koResult =
        repository.searchByCriteria(
            new MfdsDeclarationSearchCriteria(null, null, null, null, "글렌피딕", 0L, 20L));
    List<MfdsDeclaration> enResult =
        repository.searchByCriteria(
            new MfdsDeclarationSearchCriteria(null, null, null, null, "glen", 0L, 20L));

    assertThat(koResult).containsExactly(koMatch);
    assertThat(enResult).containsExactly(rcnoMatch, enMatch);
  }

  @Test
  @DisplayName("커서 페이징으로 조회할 때 id 내림차순과 pageSize+1 조회를 지킨다")
  void 커서_페이징으로_조회할_수_있다() {
    for (int index = 1; index <= 5; index++) {
      repository.save(declaration("RCNO-00" + index, MfdsNormalizationStatus.NORMALIZED));
    }

    List<MfdsDeclaration> firstPage =
        repository.searchByCriteria(MfdsDeclarationSearchCriteria.of(0L, 2L));
    assertThat(firstPage).extracting(MfdsDeclaration::getId).containsExactly(5L, 4L, 3L);

    List<MfdsDeclaration> secondPage =
        repository.searchByCriteria(MfdsDeclarationSearchCriteria.of(4L, 2L));
    assertThat(secondPage).extracting(MfdsDeclaration::getId).containsExactly(3L, 2L, 1L);

    assertThat(repository.countByCriteria(MfdsDeclarationSearchCriteria.of(4L, 2L))).isEqualTo(5L);
  }

  private MfdsDeclaration declaration(String rcno, MfdsNormalizationStatus status) {
    return MfdsTestData.declaration(rcno, status, null, null, null, null, null);
  }
}

package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.facade.payload.MfdsPublicDeclarationItem;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsImporterRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("DefaultMfdsFacade 단위 테스트")
class DefaultMfdsFacadeTest {

  private InMemoryMfdsDeclarationRepository declarationRepository;
  private InMemoryMfdsImporterRepository importerRepository;
  private DefaultMfdsFacade facade;

  @BeforeEach
  void setUp() {
    declarationRepository = new InMemoryMfdsDeclarationRepository();
    importerRepository = new InMemoryMfdsImporterRepository();
    facade = new DefaultMfdsFacade(declarationRepository, importerRepository);
  }

  @Test
  @DisplayName("주류 매칭이 확정되고 정규화가 완료된 신고만 공개 필드와 수입사 중첩으로 반환한다")
  void 검증_완료된_신고만_공개한다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    MfdsTestData.set(importer, "adminNote", "내부 메모");
    MfdsTestData.set(importer, "reviewedBy", "admin");

    MfdsDeclaration verified =
        MfdsTestData.declaration(
            "RCNO-001",
            MfdsNormalizationStatus.NORMALIZED,
            importer.getId(),
            42L,
            "CANDIDATE",
            "글렌피딕",
            "glenfiddich");
    MfdsTestData.set(verified, "baseProductNameKo", "글렌피딕 12년");
    MfdsTestData.set(verified, "volumeMl", 700);
    MfdsTestData.set(verified, "abvPercent", new BigDecimal("40.000"));
    MfdsTestData.set(verified, "volumeRaw", "700ml 원문");
    MfdsTestData.set(verified, "abvRaw", "40도 원문");
    MfdsTestData.set(verified, "reviewNote", "검토 메모 비공개");
    MfdsTestData.set(verified, "unparsedFragments", List.of("미해석 조각"));
    MfdsTestData.set(verified, "alcoholCandidate1Score", new BigDecimal("99.123456"));
    declarationRepository.save(verified);

    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-REVIEW",
            MfdsNormalizationStatus.REVIEW_REQUIRED,
            null,
            null,
            "REVIEW",
            null,
            null));
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-OTHER",
            MfdsNormalizationStatus.NORMALIZED,
            importer.getId(),
            99L,
            "MANUAL",
            null,
            null));

    List<MfdsPublicDeclarationItem> result = facade.findVerifiedDeclarationsByAlcoholId(42L);

    assertThat(result).hasSize(1);
    MfdsPublicDeclarationItem item = result.getFirst();
    assertThat(item.id()).isEqualTo(verified.getId());
    assertThat(item.rcno()).isEqualTo("RCNO-001");
    assertThat(item.baseProductNameKo()).isEqualTo("글렌피딕 12년");
    assertThat(item.volumeMl()).isEqualTo(700);
    assertThat(item.abvPercent()).isEqualByComparingTo("40.000");
    assertThat(item.importer()).isNotNull();
    assertThat(item.importer().businessName()).isEqualTo("보틀상사");
    assertThat(item.importer().id()).isEqualTo(importer.getId());

    String jsonLike = item.toString();
    assertThat(jsonLike)
        .doesNotContain("reviewNote")
        .doesNotContain("unparsedFragments")
        .doesNotContain("alcoholCandidate")
        .doesNotContain("normalizationStatus")
        .doesNotContain("adminNote")
        .doesNotContain("volumeRaw")
        .doesNotContain("CANDIDATE")
        .doesNotContain("99.123456");
  }

  @Test
  @DisplayName("selectedAlcoholId가 있어도 REVIEW_REQUIRED 등 상태 강등이면 공개하지 않는다")
  void selectedAlcoholId만_있고_상태_강등이면_공개하지_않는다() {
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-REVIEW",
            MfdsNormalizationStatus.REVIEW_REQUIRED,
            null,
            42L,
            "REVIEW",
            null,
            null));
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-PARTIAL",
            MfdsNormalizationStatus.PARTIAL,
            null,
            42L,
            "CANDIDATE",
            null,
            null));
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-STALE", MfdsNormalizationStatus.STALE, null, 42L, "MANUAL", null, null));

    assertThat(facade.findVerifiedDeclarationsByAlcoholId(42L)).isEmpty();
  }

  @Test
  @DisplayName("연결이 없거나 검토중이면 빈 목록을 반환한다")
  void 연결_없음과_검토중은_공개하지_않는다() {
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-NONE", MfdsNormalizationStatus.NORMALIZED, null, null, null, null, null));
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-REVIEW",
            MfdsNormalizationStatus.REVIEW_REQUIRED,
            null,
            null,
            "REVIEW",
            null,
            null));

    assertThat(facade.findVerifiedDeclarationsByAlcoholId(42L)).isEmpty();
  }

  @Test
  @DisplayName("동일 주류에 확정된 신고가 여러 개면 id 내림차순으로 반환하고 수입사는 일괄 조회한다")
  void 복수_확정_신고는_id_내림차순이고_수입사는_일괄_조회한다() {
    MfdsImporter importerA =
        importerRepository.save(
            MfdsTestData.importer("BIZ-A", "수입사A", MfdsImporterAdminStatus.ACTIVE));
    MfdsImporter importerB =
        importerRepository.save(
            MfdsTestData.importer("BIZ-B", "수입사B", MfdsImporterAdminStatus.ACTIVE));

    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-A",
            MfdsNormalizationStatus.NORMALIZED,
            importerA.getId(),
            7L,
            "MANUAL",
            null,
            null));
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-B",
            MfdsNormalizationStatus.NORMALIZED,
            importerB.getId(),
            7L,
            "CANDIDATE",
            null,
            null));
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-C", MfdsNormalizationStatus.NORMALIZED, null, 7L, "MANUAL", null, null));

    List<MfdsPublicDeclarationItem> result = facade.findVerifiedDeclarationsByAlcoholId(7L);

    assertThat(result)
        .extracting(MfdsPublicDeclarationItem::rcno)
        .containsExactly("RCNO-C", "RCNO-B", "RCNO-A");
    assertThat(result.get(0).importer()).isNull();
    assertThat(result.get(1).importer().businessName()).isEqualTo("수입사B");
    assertThat(result.get(2).importer().businessName()).isEqualTo("수입사A");
  }
}

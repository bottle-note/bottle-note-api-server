package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.dto.request.MfdsDeclarationImporterLinkRequest;
import app.bottlenote.mfds.dto.request.MfdsDeclarationSearchRequest;
import app.bottlenote.mfds.dto.request.MfdsDeclarationStatusRequest;
import app.bottlenote.mfds.dto.response.MfdsDeclarationDetailResponse;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.exception.MfdsExceptionCode;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsImporterRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("MfdsDeclarationService 단위 테스트")
class MfdsDeclarationServiceTest {

  private InMemoryMfdsDeclarationRepository declarationRepository;
  private InMemoryMfdsImporterRepository importerRepository;
  private MfdsDeclarationService service;

  @BeforeEach
  void setUp() {
    declarationRepository = new InMemoryMfdsDeclarationRepository();
    importerRepository = new InMemoryMfdsImporterRepository();
    service = new MfdsDeclarationService(declarationRepository, importerRepository);
  }

  @Test
  @DisplayName("수입 신고 목록을 조회할 때 커서 페이징 메타를 함께 반환한다")
  void 수입_신고_목록을_조회할_수_있다() {
    for (int index = 1; index <= 3; index++) {
      declarationRepository.save(
          MfdsTestData.declaration(
              "RCNO-00" + index, MfdsNormalizationStatus.NORMALIZED, null, null, null, null, null));
    }

    GlobalResponse response =
        service.search(new MfdsDeclarationSearchRequest(null, null, null, null, null, null, 2L));

    assertThat((List<?>) response.getData()).hasSize(2);
    assertThat(response.getMeta())
        .containsEntry("totalElements", 3L)
        .containsEntry("hasNext", true)
        .containsEntry("nextCursor", 2L);
  }

  @Test
  @DisplayName("상세 조회할 때 연결된 수입사 정보와 매칭 후보를 포함한다")
  void 상세를_조회할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            "RCNO-001",
            MfdsNormalizationStatus.NORMALIZED,
            importer.getId(),
            77L,
            MfdsMatchSelectionSource.AUTO,
            null,
            null);
    MfdsTestData.set(declaration, "alcoholCandidate1Id", 77L);
    MfdsTestData.set(declaration, "alcoholCandidate2Id", 88L);
    declarationRepository.save(declaration);

    MfdsDeclarationDetailResponse detail = service.getDetail(declaration.getId());

    assertThat(detail.importer()).isNotNull();
    assertThat(detail.importer().businessName()).isEqualTo("보틀상사");
    assertThat(detail.alcoholCandidates()).hasSize(2);
    assertThat(detail.alcoholCandidates().get(0).candidateId()).isEqualTo(77L);
  }

  @Test
  @DisplayName("정규화 상태를 변경할 때 검토 이력을 남긴다")
  void 정규화_상태를_변경할_수_있다() {
    MfdsDeclaration declaration =
        declarationRepository.save(
            MfdsTestData.declaration(
                "RCNO-001", MfdsNormalizationStatus.REVIEW_REQUIRED, null, null, null, null, null));

    AdminResultResponse result =
        service.changeNormalizationStatus(
            declaration.getId(),
            new MfdsDeclarationStatusRequest(MfdsNormalizationStatus.NORMALIZED, "admin", "검토 완료"));

    assertThat(result.code()).isEqualTo("MFDS_DECLARATION_STATUS_UPDATED");
    assertThat(declaration.getNormalizationStatus()).isEqualTo(MfdsNormalizationStatus.NORMALIZED);
    assertThat(declaration.getReviewedBy()).isEqualTo("admin");
    assertThat(declaration.getReviewNote()).isEqualTo("검토 완료");
    assertThat(declaration.getReviewedAt()).isNotNull();
  }

  @Test
  @DisplayName("수입사를 수동 연결할 때 신고의 수입사와 MANUAL 근거만 갱신한다")
  void 수입사를_수동_연결할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    MfdsDeclaration declaration =
        declarationRepository.save(
            MfdsTestData.declaration(
                "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null, null, null));

    service.linkImporter(
        declaration.getId(), new MfdsDeclarationImporterLinkRequest(importer.getId()));

    assertThat(declaration.getImporterId()).isEqualTo(importer.getId());
    assertThat(declaration.getImporterLinkSource()).isEqualTo(MfdsImporterLinkSource.MANUAL);
    assertThat(declaration.getImporterLinkedAt()).isNotNull();
  }

  @Test
  @DisplayName("이미 수입사가 연결된 신고에 연결을 시도할 때 거부한다")
  void 중복_연결을_거부할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    MfdsDeclaration declaration =
        declarationRepository.save(
            MfdsTestData.declaration(
                "RCNO-001", MfdsNormalizationStatus.NORMALIZED, 999L, null, null, null, null));

    assertThatThrownBy(
            () ->
                service.linkImporter(
                    declaration.getId(), new MfdsDeclarationImporterLinkRequest(importer.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_DECLARATION_ALREADY_LINKED.getMessage());
  }

  @Test
  @DisplayName("존재하지 않는 수입사로 연결을 시도할 때 거부한다")
  void 없는_수입사_연결을_거부할_수_있다() {
    MfdsDeclaration declaration =
        declarationRepository.save(
            MfdsTestData.declaration(
                "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null, null, null));

    assertThatThrownBy(
            () ->
                service.linkImporter(
                    declaration.getId(), new MfdsDeclarationImporterLinkRequest(404L)))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_IMPORTER_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("수입사 연결을 해제할 때 신고의 수입사와 연결 근거만 비운다")
  void 수입사_연결을_해제할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    MfdsDeclaration declaration =
        declarationRepository.save(
            MfdsTestData.declaration(
                "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null, null, null));
    service.linkImporter(
        declaration.getId(), new MfdsDeclarationImporterLinkRequest(importer.getId()));

    AdminResultResponse result = service.unlinkImporter(declaration.getId());

    assertThat(result.code()).isEqualTo("MFDS_DECLARATION_IMPORTER_UNLINKED");
    assertThat(declaration.getImporterId()).isNull();
    assertThat(declaration.getImporterLinkSource()).isNull();
    assertThat(declaration.getImporterLinkedAt()).isNull();
  }

  @Test
  @DisplayName("연결되지 않은 신고의 해제를 시도할 때 거부한다")
  void 미연결_해제를_거부할_수_있다() {
    MfdsDeclaration declaration =
        declarationRepository.save(
            MfdsTestData.declaration(
                "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null, null, null));

    assertThatThrownBy(() -> service.unlinkImporter(declaration.getId()))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_DECLARATION_NOT_LINKED.getMessage());
  }
}

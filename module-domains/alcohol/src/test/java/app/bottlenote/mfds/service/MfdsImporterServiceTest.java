package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.dto.request.MfdsImporterCreateRequest;
import app.bottlenote.mfds.dto.request.MfdsImporterSearchRequest;
import app.bottlenote.mfds.dto.request.MfdsImporterUpdateRequest;
import app.bottlenote.mfds.dto.response.MfdsImporterItem;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.exception.MfdsExceptionCode;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsImporterRcnoLinkRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsImporterRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("MfdsImporterService 단위 테스트")
class MfdsImporterServiceTest {

  private InMemoryMfdsImporterRepository importerRepository;
  private InMemoryMfdsDeclarationRepository declarationRepository;
  private InMemoryMfdsImporterRcnoLinkRepository rcnoLinkRepository;
  private MfdsImporterService service;

  @BeforeEach
  void setUp() {
    importerRepository = new InMemoryMfdsImporterRepository();
    declarationRepository = new InMemoryMfdsDeclarationRepository();
    rcnoLinkRepository = new InMemoryMfdsImporterRcnoLinkRepository();
    service =
        new MfdsImporterService(importerRepository, declarationRepository, rcnoLinkRepository);
  }

  @Test
  @DisplayName("수입사 목록을 조회할 때 커서 페이징 메타를 함께 반환한다")
  void 수입사_목록을_조회할_수_있다() {
    for (int index = 1; index <= 3; index++) {
      importerRepository.save(
          MfdsTestData.importer("BIZ-00" + index, "수입사" + index, MfdsImporterAdminStatus.ACTIVE));
    }

    GlobalResponse response = service.search(new MfdsImporterSearchRequest(null, null, null, 2L));

    assertThat((List<?>) response.getData()).hasSize(2);
    assertThat(response.getMeta())
        .containsEntry("totalElements", 3L)
        .containsEntry("hasNext", true)
        .containsEntry("nextCursor", 2L);
  }

  @Test
  @DisplayName("수입사를 등록할 때 자동 매칭 키와 기본 관리 상태를 채운다")
  void 수입사를_등록할_수_있다() {
    AdminResultResponse result =
        service.create(
            new MfdsImporterCreateRequest(
                "BIZ-001",
                "제0001호",
                " 보틀상사 ",
                "김대표",
                "https://impfood.mfds.go.kr/list",
                "설명",
                "메모",
                null));

    assertThat(result.code()).isEqualTo("MFDS_IMPORTER_CREATED");
    MfdsImporter saved = importerRepository.findById(result.targetId()).orElseThrow();
    assertThat(saved.getBusinessName()).isEqualTo("보틀상사");
    assertThat(saved.getBusinessNameKeySha256()).hasSize(32);
    assertThat(saved.getAdminStatus()).isEqualTo(MfdsImporterAdminStatus.ACTIVE);
    assertThat(saved.getOperatingStatus()).isEqualTo("UNKNOWN");
  }

  @Test
  @DisplayName("공식 업소 코드가 중복될 때 등록을 거부한다")
  void 중복_코드_등록을_거부할_수_있다() {
    importerRepository.save(
        MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));

    assertThatThrownBy(
            () ->
                service.create(
                    new MfdsImporterCreateRequest(
                        "BIZ-001",
                        "제0002호",
                        "노트무역",
                        null,
                        "https://example.com",
                        null,
                        null,
                        null)))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_IMPORTER_DUPLICATE_CODE.getMessage());
  }

  @Test
  @DisplayName("수입사 관리 항목을 수정할 때 이름이 바뀌면 매칭 키를 갱신한다")
  void 수입사를_수정할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    byte[] beforeKey = importer.getBusinessNameKeySha256();

    service.update(
        importer.getId(),
        new MfdsImporterUpdateRequest("보틀상사코리아", "새 설명", "새 메모", MfdsImporterAdminStatus.INACTIVE));

    MfdsImporterItem detail = service.getDetail(importer.getId());
    assertThat(detail.businessName()).isEqualTo("보틀상사코리아");
    assertThat(detail.description()).isEqualTo("새 설명");
    assertThat(detail.adminStatus()).isEqualTo(MfdsImporterAdminStatus.INACTIVE);
    assertThat(importer.getBusinessNameKeySha256()).isNotEqualTo(beforeKey);
  }

  @Test
  @DisplayName("연결된 수입 신고가 있을 때 수입사 삭제를 거부한다")
  void 신고가_연결된_수입사_삭제를_거부할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    declarationRepository.save(
        MfdsTestData.declaration(
            "RCNO-001",
            MfdsNormalizationStatus.NORMALIZED,
            importer.getId(),
            null,
            null,
            null,
            null));

    assertThatThrownBy(() -> service.delete(importer.getId()))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_IMPORTER_HAS_DECLARATIONS.getMessage());
  }

  @Test
  @DisplayName("연결된 RCNO 근거가 있을 때 수입사 삭제를 거부한다")
  void 근거가_연결된_수입사_삭제를_거부할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    rcnoLinkRepository.save(
        MfdsTestData.rcnoLink(
            "RCNO-001", importer.getId(), "보틀상사", MfdsImporterLinkSource.PAGE_RCNO));

    assertThatThrownBy(() -> service.delete(importer.getId()))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_IMPORTER_HAS_RCNO_LINKS.getMessage());
  }

  @Test
  @DisplayName("연결이 없는 수입사를 삭제할 수 있다")
  void 수입사를_삭제할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));

    AdminResultResponse result = service.delete(importer.getId());

    assertThat(result.code()).isEqualTo("MFDS_IMPORTER_DELETED");
    assertThat(importerRepository.findById(importer.getId())).isEmpty();
  }

  @Test
  @DisplayName("존재하지 않는 수입사를 조회할 때 예외를 던진다")
  void 없는_수입사_조회_시_예외를_던진다() {
    assertThatThrownBy(() -> service.getDetail(999L))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_IMPORTER_NOT_FOUND.getMessage());
  }
}

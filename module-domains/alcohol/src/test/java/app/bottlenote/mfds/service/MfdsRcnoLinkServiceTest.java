package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.dto.request.MfdsRcnoLinkCreateRequest;
import app.bottlenote.mfds.dto.response.MfdsRcnoLinkItem;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.exception.MfdsExceptionCode;
import app.bottlenote.mfds.fixture.InMemoryMfdsImporterRcnoLinkRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsImporterRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("MfdsRcnoLinkService 단위 테스트")
class MfdsRcnoLinkServiceTest {

  private InMemoryMfdsImporterRcnoLinkRepository rcnoLinkRepository;
  private InMemoryMfdsImporterRepository importerRepository;
  private MfdsRcnoLinkService service;

  @BeforeEach
  void setUp() {
    rcnoLinkRepository = new InMemoryMfdsImporterRcnoLinkRepository();
    importerRepository = new InMemoryMfdsImporterRepository();
    service = new MfdsRcnoLinkService(rcnoLinkRepository, importerRepository);
  }

  @Test
  @DisplayName("rcno로 조회할 때 해당 근거 1건을 반환한다")
  void rcno로_조회할_수_있다() {
    rcnoLinkRepository.save(
        MfdsTestData.rcnoLink("RCNO-001", 5L, "보틀상사", MfdsImporterLinkSource.PAGE_RCNO));

    List<MfdsRcnoLinkItem> result = service.search("RCNO-001", null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).importerId()).isEqualTo(5L);
  }

  @Test
  @DisplayName("수입사 ID로 조회할 때 연결 근거 전체를 반환한다")
  void 수입사_ID로_조회할_수_있다() {
    rcnoLinkRepository.save(
        MfdsTestData.rcnoLink("RCNO-001", 5L, "보틀상사", MfdsImporterLinkSource.PAGE_RCNO));
    rcnoLinkRepository.save(
        MfdsTestData.rcnoLink("RCNO-002", 5L, "보틀상사", MfdsImporterLinkSource.PAGE_NAME));
    rcnoLinkRepository.save(
        MfdsTestData.rcnoLink("RCNO-003", 9L, "노트무역", MfdsImporterLinkSource.PAGE_RCNO));

    List<MfdsRcnoLinkItem> result = service.search(null, 5L);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("연결 근거를 등록할 때 수입사명과 MANUAL 근거를 채운다")
  void 연결_근거를_등록할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));

    AdminResultResponse result =
        service.create(new MfdsRcnoLinkCreateRequest("RCNO-001", importer.getId()));

    assertThat(result.code()).isEqualTo("MFDS_RCNO_LINK_CREATED");
    assertThat(rcnoLinkRepository.findByRcno("RCNO-001"))
        .hasValueSatisfying(
            link -> {
              assertThat(link.getSourceImporterName()).isEqualTo("보틀상사");
              assertThat(link.getLinkSource()).isEqualTo(MfdsImporterLinkSource.MANUAL);
            });
  }

  @Test
  @DisplayName("이미 근거가 있는 rcno로 등록할 때 거부한다")
  void 중복_근거_등록을_거부할_수_있다() {
    MfdsImporter importer =
        importerRepository.save(
            MfdsTestData.importer("BIZ-001", "보틀상사", MfdsImporterAdminStatus.ACTIVE));
    rcnoLinkRepository.save(
        MfdsTestData.rcnoLink(
            "RCNO-001", importer.getId(), "보틀상사", MfdsImporterLinkSource.PAGE_RCNO));

    assertThatThrownBy(
            () -> service.create(new MfdsRcnoLinkCreateRequest("RCNO-001", importer.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_RCNO_LINK_DUPLICATE.getMessage());
  }

  @Test
  @DisplayName("연결 근거를 삭제할 수 있다")
  void 연결_근거를_삭제할_수_있다() {
    rcnoLinkRepository.save(
        MfdsTestData.rcnoLink("RCNO-001", 5L, "보틀상사", MfdsImporterLinkSource.PAGE_RCNO));

    AdminResultResponse result = service.delete("RCNO-001");

    assertThat(result.code()).isEqualTo("MFDS_RCNO_LINK_DELETED");
    assertThat(rcnoLinkRepository.findByRcno("RCNO-001")).isEmpty();
  }

  @Test
  @DisplayName("없는 근거를 삭제할 때 예외를 던진다")
  void 없는_근거_삭제_시_예외를_던진다() {
    assertThatThrownBy(() -> service.delete("RCNO-404"))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_RCNO_LINK_NOT_FOUND.getMessage());
  }
}

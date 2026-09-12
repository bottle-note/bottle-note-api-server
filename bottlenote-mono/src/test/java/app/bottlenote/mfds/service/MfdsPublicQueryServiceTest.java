package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.constant.MfdsImporterAdminStatus.ACTIVE;
import static app.bottlenote.mfds.constant.MfdsImporterAdminStatus.INACTIVE;
import static app.bottlenote.mfds.constant.MfdsNormalizationStatus.NORMALIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.global.pagination.CursorProperties;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.dto.request.MfdsPublicAlcoholSearchRequest;
import app.bottlenote.mfds.dto.request.MfdsPublicImporterSearchRequest;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholListItem;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsImporterRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("MfdsPublicQueryService 단위 테스트")
class MfdsPublicQueryServiceTest {

  private InMemoryMfdsDeclarationRepository declarationRepository;
  private InMemoryMfdsImporterRepository importerRepository;
  private MfdsPublicQueryService service;

  @BeforeEach
  void setUp() {
    declarationRepository = new InMemoryMfdsDeclarationRepository();
    importerRepository = new InMemoryMfdsImporterRepository();
    CursorProperties properties = new CursorProperties();
    properties.setCurrentKeyId("v1");
    properties.setCurrentSecret("test-pagination-cursor-secret");
    service =
        new MfdsPublicQueryService(
            declarationRepository,
            importerRepository,
            new HmacCursorCodec(properties, Clock.systemUTC()));
  }

  @Test
  @DisplayName("한글명이 같으면 수입사가 달라도 과거 수입을 같이 반환한다")
  void 한글명으로_병행_수입을_묶는다() {
    saveAlcohol("RCNO-1", "글렌피딕 15년", "보틀상사", LocalDate.of(2026, 8, 31), "GB");
    saveAlcohol("RCNO-2", "글렌피딕 15년", "병행상사", LocalDate.of(2026, 7, 1), "GB");
    saveAlcohol("RCNO-3", "글렌피딕 18년", "보틀상사", LocalDate.of(2026, 6, 1), "GB");

    List<MfdsPublicAlcoholListItem> items =
        service.searchAlcohols(request("글렌피딕 15년", null, null, null, null, null)).content();

    assertThat(items)
        .extracting(MfdsPublicAlcoholListItem::rcno)
        .containsExactly("RCNO-1", "RCNO-2");
  }

  @Test
  @DisplayName("keyword 토큰은 AND로 결합하고 제조사명에도 맞는다")
  void keyword_토큰은_AND이다() {
    MfdsDeclaration matched =
        saveAlcohol("RCNO-1", "글렌피딕 15년", "보틀상사", LocalDate.of(2026, 8, 1), "GB");
    MfdsTestData.set(matched, "manufacturerName", "WILLIAM GRANT");
    saveAlcohol("RCNO-2", "글렌피딕 15년", "보틀상사", LocalDate.of(2026, 7, 1), "GB");

    List<MfdsPublicAlcoholListItem> items =
        service.searchAlcohols(request(null, null, null, null, null, "글렌피딕 GRANT")).content();

    assertThat(items).extracting(MfdsPublicAlcoholListItem::rcno).containsExactly("RCNO-1");
  }

  @Test
  @DisplayName("수출국 Alpha-2와 품목 이넘으로 걸러낸다")
  void 수출국과_품목으로_걸러낸다() {
    MfdsDeclaration gb = saveAlcohol("RCNO-GB", "맥캘란 12년", "보틀상사", LocalDate.of(2026, 8, 1), "GB");
    MfdsTestData.set(gb, "alcoholCategoryKo", "위스키");
    MfdsDeclaration jp = saveAlcohol("RCNO-JP", "야마자키 12년", "보틀상사", LocalDate.of(2026, 8, 2), "JP");
    MfdsTestData.set(jp, "alcoholCategoryKo", "위스키");
    MfdsDeclaration wine = saveAlcohol("RCNO-WINE", "와인", "보틀상사", LocalDate.of(2026, 8, 3), "FR");
    MfdsTestData.set(wine, "alcoholCategoryKo", "와인");

    List<MfdsPublicAlcoholListItem> items =
        service.searchAlcohols(request(null, null, "GB", AlcoholType.WHISKY, null, null)).content();

    assertThat(items).extracting(MfdsPublicAlcoholListItem::rcno).containsExactly("RCNO-GB");
  }

  @Test
  @DisplayName("처리일자 구간은 날짜가 없는 행을 제외한다")
  void 처리일자_구간은_null을_제외한다() {
    saveAlcohol("RCNO-IN", "글렌피딕", "보틀상사", LocalDate.of(2026, 8, 10), "GB");
    saveAlcohol("RCNO-OUT", "글렌피딕", "보틀상사", LocalDate.of(2026, 7, 1), "GB");
    saveAlcohol("RCNO-NULL", "글렌피딕", "보틀상사", null, "GB");

    List<MfdsPublicAlcoholListItem> items =
        service
            .searchAlcohols(request(null, null, null, null, LocalDate.of(2026, 8, 1), null))
            .content();

    assertThat(items).extracting(MfdsPublicAlcoholListItem::rcno).containsExactly("RCNO-IN");
  }

  @Test
  @DisplayName("비노출 수입사는 상세 importer를 비운다")
  void 비노출_수입사는_상세에서_생략한다() {
    MfdsImporter inactive =
        importerRepository.save(MfdsTestData.importer("BIZ-IN", "숨은상사", INACTIVE));
    MfdsDeclaration declaration =
        saveAlcohol("RCNO-1", "글렌피딕", "숨은상사", LocalDate.of(2026, 8, 1), "GB");
    MfdsTestData.set(declaration, "importerId", inactive.getId());

    assertThat(service.getAlcohol(declaration.getId()).importer()).isNull();
    assertThatThrownBy(() -> service.getImporter(inactive.getId()))
        .isInstanceOf(MfdsException.class);
  }

  @Test
  @DisplayName("수출국 목록은 Alpha-2를 중복 없이 내린다")
  void 수출국_목록은_중복이_없다() {
    saveAlcohol("RCNO-1", "글렌피딕", "보틀상사", LocalDate.of(2026, 8, 1), "GB");
    saveAlcohol("RCNO-2", "맥캘란", "보틀상사", LocalDate.of(2026, 8, 2), "GB");
    MfdsDeclaration jp = saveAlcohol("RCNO-3", "야마자키", "보틀상사", LocalDate.of(2026, 8, 3), "JP");
    MfdsTestData.set(jp, "exportCountryNameKo", "일본");

    assertThat(service.listCountries())
        .extracting(item -> item.alpha2())
        .containsExactlyInAnyOrder("GB", "JP");
  }

  @Test
  @DisplayName("수입사 keyword는 대표자명에도 맞는다")
  void 수입사_keyword는_대표자명에도_맞는다() {
    MfdsImporter importer = importerRepository.save(MfdsTestData.importer("BIZ-1", "보틀상사", ACTIVE));
    MfdsTestData.set(importer, "representativeName", "홍길동");
    importerRepository.save(MfdsTestData.importer("BIZ-2", "다른상사", ACTIVE));

    assertThat(
            service.searchImporters(new MfdsPublicImporterSearchRequest("홍길동", null, 20)).content())
        .extracting(item -> item.businessName())
        .containsExactly("보틀상사");
  }

  private MfdsPublicAlcoholSearchRequest request(
      String alcoholNameKo,
      Long alcoholId,
      String exportCountry,
      AlcoholType alcoholType,
      LocalDate from,
      String keyword) {
    return new MfdsPublicAlcoholSearchRequest(
        alcoholNameKo, alcoholId, null, exportCountry, alcoholType, from, null, keyword, null, 20);
  }

  private MfdsDeclaration saveAlcohol(
      String rcno,
      String alcoholNameKo,
      String importerName,
      LocalDate processedDate,
      String country) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            rcno, NORMALIZED, null, null, null, alcoholNameKo, null, processedDate);
    MfdsTestData.set(declaration, "alcoholNameKo", alcoholNameKo);
    MfdsTestData.set(declaration, "importerBaseName", importerName);
    MfdsTestData.set(declaration, "exportCountryAlpha2", country);
    MfdsTestData.set(declaration, "exportCountryNameKo", "GB".equals(country) ? "영국" : country);
    return declarationRepository.save(declaration);
  }
}

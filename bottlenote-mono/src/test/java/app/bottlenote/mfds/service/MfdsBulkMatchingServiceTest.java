package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.fixture.FakeAlcoholMatchTargetFacade;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsMatchingCandidate;
import app.bottlenote.mfds.domain.MfdsMatchingRun;
import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingConfirmRequest;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingPreviewRequest;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewItem;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewResponse;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingReasonItem;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.exception.MfdsExceptionCode;
import app.bottlenote.mfds.fixture.InMemoryMfdsBulkPreviewIssuanceStore;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingSelectionRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("같은 제품 일괄 매칭")
class MfdsBulkMatchingServiceTest {

  private static final long ALCOHOL_ID = 10L;
  private static final long DISTILLERY_ID = 3L;
  private static final long REGION_ID = 4L;
  private static final long ADMIN_ID = 42L;
  private static final Clock BASE_CLOCK =
      Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneOffset.UTC);

  private InMemoryMfdsDeclarationRepository declarationRepository;
  private InMemoryMfdsMatchingSelectionRepository selectionRepository;
  private InMemoryMfdsMatchingRepository matchingRepository;
  private FakeAlcoholMatchTargetFacade alcoholFacade;
  private InMemoryMfdsBulkPreviewIssuanceStore issuanceStore;
  private MfdsBulkMatchingService service;

  @BeforeEach
  void setUp() {
    declarationRepository = new InMemoryMfdsDeclarationRepository();
    selectionRepository = new InMemoryMfdsMatchingSelectionRepository();
    matchingRepository = new InMemoryMfdsMatchingRepository();
    alcoholFacade = new FakeAlcoholMatchTargetFacade();
    alcoholFacade.addAlcohol(
        alcohol(ALCOHOL_ID, DISTILLERY_ID, REGION_ID, "몽키숄더", "Monkey Shoulder"));
    issuanceStore = new InMemoryMfdsBulkPreviewIssuanceStore();
    service = service(declarationRepository, BASE_CLOCK);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("classificationCases")
  @DisplayName("식별 속성과 현재 연결에 따라 서로 다른 분류를 반환한다")
  void 분류한다(ClassificationCase scenario) {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsDeclaration target = row("TGT", key(1));
    scenario.prepare(source, target, alcoholFacade);
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    MfdsBulkMatchingPreviewItem item = item(preview, target.getId());
    assertThat(item.classification()).as(scenario.name()).isEqualTo(scenario.classification());
    if (scenario.reasonCode() != null) {
      assertThat(item.reasons())
          .extracting(MfdsBulkMatchingReasonItem::code)
          .as(scenario.name())
          .contains(scenario.reasonCode());
    }
  }

  static Stream<ClassificationCase> classificationCases() {
    return Stream.of(
        classification(
            "용량이 달라도 적용할 수 있다",
            "APPLICABLE",
            null,
            (source, target, facade) -> {
              MfdsTestData.set(source, "volumeMl", 200);
              MfdsTestData.set(target, "volumeMl", 700);
            }),
        classification(
            "수입사가 달라도 적용할 수 있다",
            "APPLICABLE",
            null,
            (source, target, facade) -> {
              MfdsTestData.set(source, "importerBaseName", "수입사A");
              MfdsTestData.set(target, "importerBaseName", "수입사B");
            }),
        classification(
            "통관일이 달라도 적용할 수 있다",
            "APPLICABLE",
            null,
            (source, target, facade) -> {
              MfdsTestData.set(source, "processedDate", LocalDate.of(2024, 1, 1));
              MfdsTestData.set(target, "processedDate", LocalDate.of(2025, 6, 1));
            }),
        classification(
            "숙성이 같으면 적용할 수 있다",
            "APPLICABLE",
            null,
            (source, target, facade) -> {
              MfdsTestData.set(source, "ageYears", (short) 12);
              MfdsTestData.set(target, "ageYears", (short) 12);
            }),
        classification(
            "숙성이 양쪽 모두 없어도 키만으로 적용을 막지 않는다", "APPLICABLE", null, (source, target, facade) -> {}),
        classification(
            "숙성 연수가 다르면 확인이 필요하다",
            "NEEDS_REVIEW",
            "AGE_DIFFERS",
            (source, target, facade) -> {
              MfdsTestData.set(source, "ageYears", (short) 12);
              MfdsTestData.set(target, "ageYears", (short) 15);
            }),
        classification(
            "숙성 정보가 한쪽에만 있으면 확인이 필요하다",
            "NEEDS_REVIEW",
            "AGE_MISSING_ON_SOURCE",
            (source, target, facade) -> MfdsTestData.set(target, "ageYears", (short) 18)),
        classification(
            "배치 표기가 달라도 같은 번호면 적용할 수 있다",
            "APPLICABLE",
            null,
            (source, target, facade) -> {
              MfdsTestData.set(source, "batchNumber", "001");
              MfdsTestData.set(target, "batchNumber", "1");
            }),
        classification(
            "배치 번호가 다르면 확인이 필요하다",
            "NEEDS_REVIEW",
            "BATCH_DIFFERS",
            (source, target, facade) -> {
              MfdsTestData.set(source, "batchNumber", "12");
              MfdsTestData.set(target, "batchNumber", "13");
            }),
        classification(
            "배치 번호가 한쪽에만 있으면 확인이 필요하다",
            "NEEDS_REVIEW",
            "BATCH_MISSING_ON_TARGET",
            (source, target, facade) -> MfdsTestData.set(source, "batchNumber", "4")),
        classification(
            "캐스크 번호가 다르면 확인이 필요하다",
            "NEEDS_REVIEW",
            "CASK_DIFFERS",
            (source, target, facade) -> {
              MfdsTestData.set(source, "caskNumber", "100");
              MfdsTestData.set(target, "caskNumber", "200");
            }),
        classification(
            "캐스크 번호가 한쪽에만 있으면 확인이 필요하다",
            "NEEDS_REVIEW",
            "CASK_MISSING_ON_SOURCE",
            (source, target, facade) -> MfdsTestData.set(target, "caskNumber", "55")),
        classification(
            "빈티지 연도가 다르면 확인이 필요하다",
            "NEEDS_REVIEW",
            "VINTAGE_DIFFERS",
            (source, target, facade) -> {
              MfdsTestData.set(source, "vintageYear", (short) 2016);
              MfdsTestData.set(target, "vintageYear", (short) 2018);
            }),
        classification(
            "빈티지 연도가 한쪽에만 있으면 확인이 필요하다",
            "NEEDS_REVIEW",
            "VINTAGE_MISSING_ON_TARGET",
            (source, target, facade) -> MfdsTestData.set(source, "vintageYear", (short) 2008)),
        classification(
            "숫자 에디션이 다르면 확인이 필요하다",
            "NEEDS_REVIEW",
            "EDITION_DIFFERS",
            (source, target, facade) -> {
              MfdsTestData.set(source, "editionName", "edition 2");
              MfdsTestData.set(target, "editionName", "edition 3");
            }),
        classification(
            "숫자 에디션이 한쪽에만 있으면 확인이 필요하다",
            "NEEDS_REVIEW",
            "EDITION_MISSING_ON_SOURCE",
            (source, target, facade) -> MfdsTestData.set(target, "editionName", "edition 1")),
        classification(
            "캐스크 스트렝스 표기가 한쪽에만 있으면 확인이 필요하다",
            "NEEDS_REVIEW",
            "STRENGTH_DIFFERS",
            (source, target, facade) ->
                MfdsTestData.set(target, "skuDisplayNameEn", "monkey shoulder cask strength")),
        classification(
            "도수 유형이 다르면 확인이 필요하다",
            "NEEDS_REVIEW",
            "STRENGTH_DIFFERS",
            (source, target, facade) -> {
              MfdsTestData.set(source, "strengthType", "STANDARD");
              MfdsTestData.set(target, "strengthType", "CASK STRENGTH");
            }),
        classification(
            "도수가 한쪽에만 있으면 확인이 필요하다",
            "NEEDS_REVIEW",
            "ABV_MISSING_ON_SOURCE",
            (source, target, facade) ->
                MfdsTestData.set(target, "abvPercent", new BigDecimal("40.000"))),
        classification(
            "도수가 다르면 확인이 필요하다",
            "NEEDS_REVIEW",
            "ABV_DIFFERS",
            (source, target, facade) -> {
              MfdsTestData.set(source, "abvPercent", new BigDecimal("40.000"));
              MfdsTestData.set(target, "abvPercent", new BigDecimal("46.000"));
            }),
        classification(
            "이미 같은 주류·증류소·지역이면 변경이 필요 없다",
            "NO_CHANGE",
            null,
            (source, target, facade) -> link(target, ALCOHOL_ID, DISTILLERY_ID, REGION_ID)),
        classification(
            "다른 주류가 연결되어 있으면 충돌이다",
            "CONFLICT",
            "EXISTING_SELECTION_DIFFERS",
            (source, target, facade) -> link(target, 99L, null, null)),
        classification(
            "다른 증류소가 연결되어 있으면 충돌이다",
            "CONFLICT",
            "EXISTING_REFERENCE_DIFFERS",
            (source, target, facade) -> link(target, ALCOHOL_ID, 8L, REGION_ID)),
        classification(
            "다른 지역이 연결되어 있으면 충돌이다",
            "CONFLICT",
            "EXISTING_REFERENCE_DIFFERS",
            (source, target, facade) -> link(target, ALCOHOL_ID, DISTILLERY_ID, 9L)),
        classification(
            "비어 있는 증류소만 채울 수 있다",
            "APPLICABLE",
            null,
            (source, target, facade) -> link(target, ALCOHOL_ID, null, REGION_ID)),
        classification(
            "0 이하인 기존 연결은 비어 있는 값으로 본다",
            "APPLICABLE",
            null,
            (source, target, facade) -> link(target, 0L, -1L, 0L)));
  }

  @Test
  @DisplayName("적용 값이 비어 있고 기존 증류소가 있으면 비우는 충돌로 본다")
  void 기존_증류소를_비우면_충돌이다() {
    alcoholFacade.clear();
    alcoholFacade.addAlcohol(alcohol(ALCOHOL_ID, null, null, "몽키숄더", "Monkey Shoulder"));
    MfdsDeclaration source = row("SRC", key(1));
    MfdsDeclaration target = row("TGT", key(1));
    link(target, null, 8L, null);

    MfdsBulkMatchingPreviewItem item = item(preview(source.getId(), null, null), target.getId());

    assertThat(item.classification()).isEqualTo("CONFLICT");
    assertThat(item.reasons())
        .extracting(MfdsBulkMatchingReasonItem::code)
        .contains("EXISTING_REFERENCE_WOULD_CLEAR");
  }

  @Test
  @DisplayName("글렌피딕 12년과 15년처럼 동일성 키가 다르면 미리보기에 넣지 않는다")
  void 다른_동일성_키는_대상이_아니다() {
    MfdsDeclaration source = row("GF12", key(1));
    MfdsTestData.set(source, "nameSearchKeyKo", "글렌피딕 12");
    MfdsTestData.set(source, "ageYears", (short) 12);
    MfdsDeclaration other = row("GF15", key(2));
    MfdsTestData.set(other, "nameSearchKeyKo", "글렌피딕 15");
    MfdsTestData.set(other, "ageYears", (short) 15);

    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    assertThat(preview.items())
        .extracting(MfdsBulkMatchingPreviewItem::declarationId)
        .containsExactly(source.getId());
    assertThat(preview.applicableCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("기준 신고에 제품 식별 키가 없으면 미리보기를 거절한다")
  void 식별_키가_없으면_미리보기를_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsTestData.set(source, "productIdentityKeySha256", null);

    assertThatThrownBy(() -> preview(source.getId(), null, null))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_PRODUCT_IDENTITY_UNAVAILABLE.getMessage());
  }

  @Test
  @DisplayName("같은 키의 용량이 다른 18건은 모두 적용 가능하다")
  void 용량이_다른_같은_제품_18건을_함께_보여_준다() {
    int[] volumes = {200, 500, 700};
    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < 18; i++) {
      MfdsDeclaration declaration = row("MS-" + i, key(1));
      MfdsTestData.set(declaration, "volumeMl", volumes[i % volumes.length]);
      ids.add(declaration.getId());
    }

    MfdsBulkMatchingPreviewResponse preview = preview(ids.get(0), null, null);

    assertThat(preview.items()).hasSize(18);
    assertThat(preview.applicableCount()).isEqualTo(18);
    assertThat(preview.reviewCount()).isZero();
    assertThat(preview.conflictCount()).isZero();
    assertThat(preview.items())
        .extracting(MfdsBulkMatchingPreviewItem::volumeMl)
        .contains(200, 500, 700);
  }

  @Test
  @DisplayName("미리보기에 없는 신고를 넣으면 아무것도 바꾸지 않는다")
  void 미리보기에_없는_신고는_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    assertThatThrownBy(() -> confirm(preview, List.of(999L)))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_TARGET_INVALID.getMessage());
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("확인이 필요한 대상을 선택하면 적용 가능한 대상도 함께 거절한다")
  void 확인_필요_대상이_있으면_전체_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsDeclaration applicable = row("OK", key(1));
    MfdsDeclaration review = row("REVIEW", key(1));
    MfdsTestData.set(review, "batchNumber", "9");
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    assertThatThrownBy(() -> confirm(preview, List.of(applicable.getId(), review.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_TARGET_INVALID.getMessage());
    assertThat(applicable.getSelectedAlcoholId()).isNull();
    assertThat(review.getBatchNumber()).isEqualTo("9");
  }

  @Test
  @DisplayName("충돌 대상을 선택하면 거절한다")
  void 충돌_대상은_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsDeclaration conflict = row("CONFLICT", key(1));
    link(conflict, 99L, null, null);
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    assertThatThrownBy(() -> confirm(preview, List.of(conflict.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_TARGET_INVALID.getMessage());
    assertThat(conflict.getSelectedAlcoholId()).isEqualTo(99L);
  }

  @Test
  @DisplayName("중복된 신고 ID는 거절한다")
  void 중복_대상은_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    assertThatThrownBy(() -> confirm(preview, List.of(source.getId(), source.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_DUPLICATE_TARGET.getMessage());
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("선택한 신고가 없으면 거절한다")
  void 빈_선택은_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    assertThatThrownBy(() -> confirm(preview, List.of()))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_EMPTY_SELECTION.getMessage());
  }

  @Test
  @DisplayName("검증값을 변조하면 거절한다")
  void 변조된_검증값은_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);
    char last = preview.previewToken().charAt(preview.previewToken().length() - 1);
    String tampered =
        preview.previewToken().substring(0, preview.previewToken().length() - 1)
            + (last == 'a' ? 'b' : 'a');

    assertThatThrownBy(
            () ->
                service.confirm(
                    new MfdsBulkMatchingConfirmRequest(
                        source.getId(),
                        ALCOHOL_ID,
                        null,
                        null,
                        List.of(source.getId()),
                        tampered,
                        preview.previewExpiresAt()),
                    ADMIN_ID))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_NOT_ISSUED.getMessage());
  }

  @Test
  @DisplayName("미리보기 없이 미래 만료 해시로 확정하지 못하고 다른 관리자도 거절한다")
  void 발급되지_않은_미리보기와_다른_관리자는_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);
    assertThatThrownBy(
            () ->
                service.confirm(
                    new MfdsBulkMatchingConfirmRequest(
                        source.getId(),
                        ALCOHOL_ID,
                        null,
                        null,
                        List.of(source.getId()),
                        "a".repeat(64),
                        preview.previewExpiresAt().plusHours(5)),
                    ADMIN_ID))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_NOT_ISSUED.getMessage());
    assertThatThrownBy(() -> confirm(preview, List.of(source.getId()), null, null, 99L))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_ADMIN_MISMATCH.getMessage());
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("미리보기 유효 시간이 지나면 거절한다")
  void 만료된_미리보기는_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);
    service = service(declarationRepository, Clock.offset(BASE_CLOCK, Duration.ofMinutes(11)));

    assertThatThrownBy(() -> confirm(preview, List.of(source.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_EXPIRED.getMessage());
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("클라이언트가 유효 시간만 늘리면 거절한다")
  void 유효시간_변조는_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    assertThatThrownBy(
            () ->
                service.confirm(
                    new MfdsBulkMatchingConfirmRequest(
                        source.getId(),
                        ALCOHOL_ID,
                        null,
                        null,
                        List.of(source.getId()),
                        preview.previewToken(),
                        preview.previewExpiresAt().plusMinutes(30)),
                    ADMIN_ID))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_MISMATCH.getMessage());
  }

  @Test
  @DisplayName("미리보기 이후 배치가 바뀌면 선택 전체를 거절한다")
  void 미리보기_이후_변경은_전체_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsDeclaration other = row("OTHER", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);
    MfdsTestData.set(other, "batchNumber", "77");

    assertThatThrownBy(() -> confirm(preview, List.of(source.getId(), other.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_MISMATCH.getMessage());
    assertThat(source.getSelectedAlcoholId()).isNull();
    assertThat(other.getSelectedAlcoholId()).isNull();
    assertThat(other.getBatchNumber()).isEqualTo("77");
  }

  @Test
  @DisplayName("적용 가능한 대상 중 고른 신고만 확정한다")
  void 선택하지_않은_적용_대상은_유지한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsDeclaration chosen = row("CHOSEN", key(1));
    MfdsDeclaration skipped = row("SKIP", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    var response = confirm(preview, List.of(chosen.getId()));

    assertThat(response.appliedCount()).isEqualTo(1);
    assertThat(chosen.getSelectedAlcoholId()).isEqualTo(ALCOHOL_ID);
    assertThat(skipped.getSelectedAlcoholId()).isNull();
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("변경이 필요 없는 신고는 이름과 감사 이력을 바꾸지 않는다")
  void 변경_불필요는_이력을_남기지_않는다() {
    MfdsDeclaration source = row("SRC", key(1));
    link(source, ALCOHOL_ID, DISTILLERY_ID, REGION_ID);
    MfdsTestData.set(source, "alcoholNameKo", "기존 이름");
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    var response = confirm(preview, List.of(source.getId()));

    assertThat(response.unchangedCount()).isEqualTo(1);
    assertThat(response.appliedCount()).isZero();
    assertThat(source.getAlcoholNameKo()).isEqualTo("기존 이름");
    assertThat(selectionRepository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("확정은 주류명과 기준 신고·관리자 이력만 남기고 원문 속성은 유지한다")
  void 확정은_선택값과_이력만_저장한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsTestData.set(source, "volumeMl", 700);
    MfdsTestData.set(source, "abvPercent", new BigDecimal("40.000"));
    MfdsTestData.set(source, "ageYears", (short) 12);
    MfdsTestData.set(source, "batchNumber", "8");
    MfdsDeclaration target = row("TGT", key(1));
    MfdsTestData.set(target, "volumeMl", 200);
    MfdsTestData.set(target, "abvPercent", new BigDecimal("40.000"));
    MfdsTestData.set(target, "ageYears", (short) 12);
    MfdsTestData.set(target, "batchNumber", "8");
    MfdsTestData.set(target, "matchingRunId", 5L);
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    confirm(preview, List.of(target.getId()));

    assertThat(target.getSelectedAlcoholId()).isEqualTo(ALCOHOL_ID);
    assertThat(target.getSelectedDistilleryId()).isEqualTo(DISTILLERY_ID);
    assertThat(target.getSelectedRegionId()).isEqualTo(REGION_ID);
    assertThat(target.getAlcoholMatchDecision()).isEqualTo("MANUAL");
    assertThat(target.getDistilleryMatchSource()).isEqualTo("ALCOHOL_PROPAGATED");
    assertThat(target.getRegionMatchSource()).isEqualTo("ALCOHOL_PROPAGATED");
    assertThat(target.getAlcoholNameKo()).isEqualTo("몽키숄더");
    assertThat(target.getAlcoholNameEn()).isEqualTo("Monkey Shoulder");
    assertThat(target.getVolumeMl()).isEqualTo(200);
    assertThat(target.getAbvPercent()).isEqualByComparingTo("40.000");
    assertThat(target.getAgeYears()).isEqualTo((short) 12);
    assertThat(target.getBatchNumber()).isEqualTo("8");
    assertThat(target.getMatchingRunId()).isEqualTo(5L);
    assertThat(target.getInheritedFromDeclarationId()).isNull();
    assertThat(selectionRepository.findAll())
        .extracting(MfdsMatchingSelection::getReasonCode)
        .containsExactly(
            "BULK_MANUAL:" + source.getId(),
            "BULK_ALCOHOL_PROPAGATED:" + source.getId(),
            "BULK_ALCOHOL_PROPAGATED:" + source.getId());
    assertThat(selectionRepository.findAll())
        .extracting(MfdsMatchingSelection::getSelectedBy)
        .containsOnly("42");
    assertThat(source.getProductIdentityKeySha256()).isEqualTo(key(1));
  }

  @Test
  @DisplayName("후보에 있는 주류는 CANDIDATE로 기록한다")
  void 후보_선택이면_CANDIDATE다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsMatchingRun run =
        matchingRepository.saveRun(
            MfdsMatchingRun.builder().matcherVersion("mfds-matching-v2").status("DONE").build());
    MfdsTestData.set(source, "matchingRunId", run.getId());
    MfdsTestData.set(source, "matchingVersion", "mfds-matching-v2");
    matchingRepository.saveCandidate(
        MfdsMatchingCandidate.builder()
            .runId(run.getId())
            .declarationId(source.getId())
            .stage("A")
            .targetType("ALCOHOL")
            .targetId(ALCOHOL_ID)
            .rankNo(1)
            .rawScore(BigDecimal.ONE)
            .evidenceStrength(0)
            .createdAt(LocalDateTime.of(2026, 9, 24, 0, 0))
            .build());
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    confirm(preview, List.of(source.getId()));

    assertThat(source.getAlcoholMatchDecision()).isEqualTo("CANDIDATE");
    assertThat(selectionRepository.findAll().get(0).getReasonCode())
        .isEqualTo("BULK_CANDIDATE:" + source.getId());
  }

  @Test
  @DisplayName("요청에 증류소를 지정하면 주류에 등록된 값 대신 그 값을 쓴다")
  void 명시한_증류소를_적용한다() {
    alcoholFacade.addDistillery(new DistilleryMatchTargetItem(9L, "다른 증류소", "Other"));
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), 9L, null);

    confirm(preview, List.of(source.getId()), 9L, null);

    assertThat(source.getSelectedDistilleryId()).isEqualTo(9L);
    assertThat(source.getDistilleryMatchSource()).isEqualTo("MANUAL");
    assertThat(preview.distilleryId()).isEqualTo(9L);
  }

  @Test
  @DisplayName("없는 주류·증류소·지역은 거절한다")
  void 없는_기준_정보는_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    assertThatThrownBy(() -> preview(999L, null, null))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND.getMessage());
    assertThatThrownBy(
            () ->
                service.preview(
                    new MfdsBulkMatchingPreviewRequest(source.getId(), 404L, null, null), ADMIN_ID))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_SELECTED_ALCOHOL_NOT_FOUND.getMessage());
    assertThatThrownBy(() -> preview(source.getId(), 404L, null))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_SELECTED_DISTILLERY_NOT_FOUND.getMessage());
    assertThatThrownBy(() -> preview(source.getId(), null, 404L))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_SELECTED_REGION_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("한 번 확정한 검증값으로 다시 확정하지 못한다")
  void 같은_검증값의_재요청은_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);
    confirm(preview, List.of(source.getId()));

    assertThatThrownBy(() -> confirm(preview, List.of(source.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_NOT_ISSUED.getMessage());
  }

  @Test
  @DisplayName("미리보기 이후 같은 제품 신고가 추가되면 거절한다")
  void 그룹에_신고가_추가되면_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);
    row("NEW", key(1));

    assertThatThrownBy(() -> confirm(preview, List.of(source.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_MISMATCH.getMessage());
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("같은 제품 그룹은 신고 ID가 오름차순이 되도록 잠근다")
  void 잠금_순서는_신고_ID_오름차순이다() {
    RecordingDeclarationRepository recording = new RecordingDeclarationRepository();
    declarationRepository = recording;
    service = service(recording, BASE_CLOCK);
    MfdsDeclaration third = savedWithId("C", 30L);
    MfdsDeclaration first = savedWithId("A", 10L);
    savedWithId("B", 20L);
    MfdsBulkMatchingPreviewResponse preview = preview(first.getId(), null, null);

    confirm(preview, List.of(third.getId(), first.getId()));

    assertThat(recording.lockedIds).containsExactly(10L, 20L, 30L);
  }

  @Test
  @DisplayName("주류 이름이 미리보기 뒤에 바뀌면 거절한다")
  void 기준_주류가_바뀌면_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);
    alcoholFacade.clear();
    alcoholFacade.addAlcohol(alcohol(ALCOHOL_ID, DISTILLERY_ID, REGION_ID, "바뀐 이름", "Changed"));

    assertThatThrownBy(() -> confirm(preview, List.of(source.getId())))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_PREVIEW_MISMATCH.getMessage());
    assertThat(source.getAlcoholNameKo()).isNull();
  }

  @Test
  @DisplayName("배치가 달라도 기존 주류가 다르면 충돌이 우선하고 두 사유를 함께 보여 준다")
  void 충돌이_확인필요보다_우선한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsTestData.set(source, "batchNumber", "8");
    MfdsDeclaration target = row("TGT", key(1));
    MfdsTestData.set(target, "batchNumber", "9");
    link(target, 99L, null, null);

    MfdsBulkMatchingPreviewItem item = item(preview(source.getId(), null, null), target.getId());

    assertThat(item.classification()).isEqualTo("CONFLICT");
    assertThat(item.reasons())
        .extracting(MfdsBulkMatchingReasonItem::code)
        .contains("EXISTING_SELECTION_DIFFERS", "BATCH_DIFFERS");
  }

  @Test
  @DisplayName("버전·변이·정제 상태·일반명·제조국·관리자 해제는 확인 필요가 된다")
  void 추가_확인필요_사유를_구분한다() {
    MfdsDeclaration versionSource = row("VER-S", key(2));
    MfdsDeclaration versionTarget = row("VER-T", key(2));
    MfdsTestData.set(versionTarget, "versionMarker", "구형");
    assertThat(item(preview(versionSource.getId(), null, null), versionTarget.getId()).reasons())
        .extracting(MfdsBulkMatchingReasonItem::code)
        .contains("VERSION_MISSING_ON_SOURCE");

    MfdsDeclaration variantSource = row("VAR-S", key(3));
    MfdsDeclaration variantTarget = row("VAR-T", key(3));
    MfdsTestData.set(variantSource, "variantMarkerRaw", "#1");
    MfdsTestData.set(variantTarget, "variantMarkerRaw", "#2");
    assertThat(item(preview(variantSource.getId(), null, null), variantTarget.getId()).reasons())
        .extracting(MfdsBulkMatchingReasonItem::code)
        .contains("VARIANT_DIFFERS");

    MfdsDeclaration reviewSource = row("REV-S", key(4));
    MfdsDeclaration reviewTarget = row("REV-T", key(4));
    MfdsTestData.set(reviewTarget, "normalizationStatus", MfdsNormalizationStatus.REVIEW_REQUIRED);
    MfdsTestData.set(
        reviewTarget, "normalizationReasons", List.of("GENERIC_PRODUCT_NAME_REVIEW_REQUIRED"));
    assertThat(item(preview(reviewSource.getId(), null, null), reviewTarget.getId()).reasons())
        .extracting(MfdsBulkMatchingReasonItem::code)
        .contains("NORMALIZATION_REVIEW_REQUIRED", "GENERIC_PRODUCT_NAME");

    MfdsDeclaration countrySource = row("CTY-S", key(5));
    MfdsDeclaration countryTarget = row("CTY-T", key(5));
    MfdsTestData.set(countrySource, "manufactureCountryAlpha2", "IE");
    MfdsTestData.set(countryTarget, "manufactureCountryAlpha2", "gb");
    assertThat(item(preview(countrySource.getId(), null, null), countryTarget.getId()).reasons())
        .extracting(MfdsBulkMatchingReasonItem::code)
        .contains("COUNTRY_DIFFERS");

    MfdsDeclaration releaseSource = row("REL-S", key(6));
    MfdsDeclaration releaseTarget = row("REL-T", key(6));
    selectionRepository.save(
        MfdsMatchingSelection.adminRevoke(
            releaseTarget.getId(),
            "ALCOHOL",
            1L,
            "ADMIN_RELEASE",
            ADMIN_ID,
            LocalDateTime.of(2026, 9, 24, 0, 0)));
    assertThat(
            item(preview(releaseSource.getId(), null, null), releaseTarget.getId())
                .classification())
        .isEqualTo("NEEDS_REVIEW");
    assertThat(item(preview(releaseSource.getId(), null, null), releaseTarget.getId()).reasons())
        .extracting(MfdsBulkMatchingReasonItem::code)
        .contains("ADMIN_RELEASED");
  }

  @Test
  @DisplayName("0 이하이거나 500건을 넘는 선택은 거절한다")
  void 선택_형식과_한도를_거절한다() {
    MfdsDeclaration source = row("SRC", key(1));
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);
    assertThatThrownBy(() -> confirm(preview, List.of(0L)))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_TARGET_INVALID.getMessage());
    assertThatThrownBy(() -> confirm(preview, List.of(-1L)))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_TARGET_INVALID.getMessage());
    List<Long> tooMany = new ArrayList<>();
    for (long id = 1; id <= 501; id++) {
      tooMany.add(id);
    }
    assertThatThrownBy(() -> confirm(preview, tooMany))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_BULK_SELECTION_LIMIT.getMessage());
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  private MfdsBulkMatchingPreviewResponse preview(Long sourceId, Long distilleryId, Long regionId) {
    return service.preview(
        new MfdsBulkMatchingPreviewRequest(sourceId, ALCOHOL_ID, distilleryId, regionId), ADMIN_ID);
  }

  private app.bottlenote.mfds.dto.response.MfdsBulkMatchingConfirmResponse confirm(
      MfdsBulkMatchingPreviewResponse preview, List<Long> ids) {
    return confirm(preview, ids, null, null);
  }

  private app.bottlenote.mfds.dto.response.MfdsBulkMatchingConfirmResponse confirm(
      MfdsBulkMatchingPreviewResponse preview, List<Long> ids, Long distilleryId, Long regionId) {
    return confirm(preview, ids, distilleryId, regionId, ADMIN_ID);
  }

  private app.bottlenote.mfds.dto.response.MfdsBulkMatchingConfirmResponse confirm(
      MfdsBulkMatchingPreviewResponse preview,
      List<Long> ids,
      Long distilleryId,
      Long regionId,
      Long adminId) {
    return service.confirm(
        new MfdsBulkMatchingConfirmRequest(
            preview.sourceDeclarationId(),
            preview.alcoholId(),
            distilleryId,
            regionId,
            ids,
            preview.previewToken(),
            preview.previewExpiresAt()),
        adminId);
  }

  private MfdsDeclaration row(String rcno, byte[] identityKey) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            rcno, MfdsNormalizationStatus.NORMALIZED, null, null, null, "몽키숄더", "monkey shoulder");
    MfdsTestData.set(declaration, "productIdentityKeySha256", identityKey);
    return declarationRepository.save(declaration);
  }

  private MfdsDeclaration savedWithId(String rcno, long id) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            rcno, MfdsNormalizationStatus.NORMALIZED, null, null, null, "몽키숄더", "monkey shoulder");
    MfdsTestData.set(declaration, "productIdentityKeySha256", key(1));
    ReflectionTestUtils.setField(declaration, "id", id);
    return declarationRepository.save(declaration);
  }

  private static void link(
      MfdsDeclaration declaration, Long alcoholId, Long distilleryId, Long regionId) {
    MfdsTestData.set(declaration, "selectedAlcoholId", alcoholId);
    MfdsTestData.set(declaration, "selectedDistilleryId", distilleryId);
    MfdsTestData.set(declaration, "selectedRegionId", regionId);
  }

  private static MfdsBulkMatchingPreviewItem item(
      MfdsBulkMatchingPreviewResponse preview, Long id) {
    return preview.items().stream()
        .filter(item -> id.equals(item.declarationId()))
        .findFirst()
        .orElseThrow();
  }

  private MfdsBulkMatchingService service(
      InMemoryMfdsDeclarationRepository repository, Clock clock) {
    return new MfdsBulkMatchingService(
        repository,
        alcoholFacade,
        selectionRepository,
        new MfdsMatchingHistoryService(
            matchingRepository, new MfdsMatchingEvidenceCodec(new ObjectMapper())),
        issuanceStore,
        new NoopMfdsBulkLockGate(),
        new NoopMfdsBulkSaveGuard(),
        clock);
  }

  private static byte[] key(int marker) {
    byte[] value = new byte[32];
    value[31] = (byte) marker;
    return value;
  }

  private static AlcoholMatchTargetItem alcohol(
      long id, Long distilleryId, Long regionId, String korName, String engName) {
    return new AlcoholMatchTargetItem(
        id,
        korName,
        engName,
        null,
        null,
        null,
        null,
        regionId,
        null,
        null,
        distilleryId,
        null,
        null,
        null,
        null);
  }

  private static ClassificationCase classification(
      String name, String classification, String reasonCode, Preparer preparer) {
    return new ClassificationCase(name, classification, reasonCode, preparer);
  }

  record ClassificationCase(
      String name, String classification, String reasonCode, Preparer preparer) {
    @Override
    public String toString() {
      return name;
    }

    void prepare(
        MfdsDeclaration source, MfdsDeclaration target, FakeAlcoholMatchTargetFacade facade) {
      preparer.prepare(source, target, facade);
    }
  }

  @FunctionalInterface
  interface Preparer {
    void prepare(
        MfdsDeclaration source, MfdsDeclaration target, FakeAlcoholMatchTargetFacade facade);
  }

  private static final class RecordingDeclarationRepository
      extends InMemoryMfdsDeclarationRepository {
    private List<Long> lockedIds = List.of();

    @Override
    public List<MfdsDeclaration> findByProductIdentityKeySha256ForUpdate(
        byte[] productIdentityKeySha256) {
      List<MfdsDeclaration> rows =
          super.findByProductIdentityKeySha256ForUpdate(productIdentityKeySha256);
      lockedIds = rows.stream().map(MfdsDeclaration::getId).toList();
      return rows;
    }
  }
}

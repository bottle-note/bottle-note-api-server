package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.service.MfdsBulkTestFixture.ADMIN_ID;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.BASE_CLOCK;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.alcohol;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.item;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.key;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsMatchingCandidate;
import app.bottlenote.mfds.domain.MfdsMatchingRun;
import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingConfirmRequest;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingConfirmResponse;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewItem;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewResponse;
import app.bottlenote.mfds.dto.response.MfdsMatchingConfirmResponse;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.exception.MfdsExceptionCode;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingSelectionRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("같은 제품 일괄 매칭")
class MfdsBulkMatchingServiceTest {

  private static final long ALCOHOL_ID = 10L;
  private static final long DISTILLERY_ID = 3L;
  private static final long REGION_ID = 4L;

  private MfdsBulkTestFixture fixture;
  private MfdsBulkMatchingService service;

  @BeforeEach
  void setUp() {
    use(new InMemoryMfdsDeclarationRepository());
  }

  private void use(InMemoryMfdsDeclarationRepository declarations) {
    fixture = new MfdsBulkTestFixture(declarations);
    fixture.alcohols.addAlcohol(
        alcohol(ALCOHOL_ID, "몽키숄더", "Monkey Shoulder", DISTILLERY_ID, REGION_ID));
    service = fixture.service(BASE_CLOCK);
  }

  @Test
  @DisplayName("기준 신고에 제품 식별 키가 없을 때 미리보기를 거절한다")
  void 식별_키가_없으면_미리보기를_거절한다() {
    MfdsDeclaration source = fixture.row("SRC", null);

    assertRejected(() -> preview(source.getId(), null, null), "MFDS_PRODUCT_IDENTITY_UNAVAILABLE");
  }

  @Test
  @DisplayName("없는 신고·주류·증류소·지역을 요청할 때 거절한다")
  void 없는_기준_정보는_거절한다() {
    MfdsDeclaration source = fixture.row("SRC", key(1));

    assertRejected(() -> preview(999L, null, null), "MFDS_DECLARATION_NOT_FOUND");
    assertRejected(
        () -> MfdsBulkTestFixture.preview(service, source.getId(), 404L, null, null),
        "MFDS_SELECTED_ALCOHOL_NOT_FOUND");
    assertRejected(() -> preview(source.getId(), 404L, null), "MFDS_SELECTED_DISTILLERY_NOT_FOUND");
    assertRejected(() -> preview(source.getId(), null, 404L), "MFDS_SELECTED_REGION_NOT_FOUND");
  }

  @Test
  @DisplayName("같은 키의 용량이 다른 18건을 미리볼 때 다른 키 신고를 빼고 모두 적용 가능으로 본다")
  void 같은_제품_18건을_함께_보여_준다() {
    int[] volumes = {200, 500, 700};
    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < 18; i++) {
      MfdsDeclaration declaration = fixture.row("MS-" + i, key(1));
      MfdsTestData.set(declaration, "volumeMl", volumes[i % volumes.length]);
      ids.add(declaration.getId());
    }
    fixture.row("OTHER-KEY", key(2));

    MfdsBulkMatchingPreviewResponse preview = preview(ids.get(0), null, null);

    assertThat(preview.items())
        .extracting(MfdsBulkMatchingPreviewItem::declarationId)
        .isEqualTo(ids);
    assertThat(preview.items())
        .extracting(MfdsBulkMatchingPreviewItem::classification)
        .containsOnly("APPLICABLE");
    assertThat(preview.items())
        .extracting(MfdsBulkMatchingPreviewItem::volumeMl)
        .contains(200, 500, 700);
  }

  @Test
  @DisplayName("그룹의 관리자 해제 이력을 판단할 때 선택 이력을 한 번만 조회한다")
  void 관리자_해제_이력을_한_번에_조회한다() {
    MfdsDeclaration source = fixture.row("SRC", key(1));
    MfdsDeclaration released = fixture.row("REL", key(1));
    MfdsDeclaration reselected = fixture.row("RESEL", key(1));
    LocalDateTime at = LocalDateTime.of(2026, 9, 24, 0, 0);
    fixture.selections.save(revoke(released.getId(), at));
    fixture.selections.save(revoke(reselected.getId(), at));
    fixture.selections.save(
        MfdsMatchingSelection.adminSelect(
            reselected.getId(), "ALCOHOL", 1L, "ADMIN_SELECT", ADMIN_ID, at.plusSeconds(1)));
    List<Collection<Long>> lookups = new ArrayList<>();
    service =
        new MfdsBulkMatchingService(
            fixture.declarations,
            new InMemoryMfdsMatchingSelectionRepository() {
              @Override
              public List<MfdsMatchingSelection> findByDeclarationIdInOrderBySelectedAtDescIdDesc(
                  Collection<Long> ids) {
                lookups.add(List.copyOf(ids));
                return fixture.selections.findByDeclarationIdInOrderBySelectedAtDescIdDesc(ids);
              }
            },
            fixture.single(),
            BASE_CLOCK);

    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    assertThat(lookups)
        .containsExactly(List.of(source.getId(), released.getId(), reselected.getId()));
    assertThat(item(preview, released.getId()).orElseThrow().classification())
        .isEqualTo("NEEDS_REVIEW");
    assertThat(item(preview, reselected.getId()).orElseThrow().classification())
        .isEqualTo("APPLICABLE");
  }

  static Stream<Arguments> invalidSelections() {
    return Stream.of(
        selection("없는 신고", g -> List.of(g.get(0), 999L), "MFDS_DECLARATION_NOT_FOUND"),
        selection("중복 ID", g -> List.of(g.get(0), g.get(0)), "MFDS_BULK_DUPLICATE_TARGET"),
        selection("빈 선택", g -> List.of(), "MFDS_BULK_EMPTY_SELECTION"),
        selection("0 ID", g -> List.of(0L), "MFDS_BULK_TARGET_INVALID"),
        selection("음수 ID", g -> List.of(-1L), "MFDS_BULK_TARGET_INVALID"),
        selection(
            "500건 초과",
            g -> LongStream.rangeClosed(1, 501).boxed().toList(),
            "MFDS_BULK_SELECTION_LIMIT"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidSelections")
  @DisplayName("요청한 신고 목록이 올바르지 않을 때 전체를 거절하고 아무것도 쓰지 않는다")
  void 잘못된_선택은_전체_거절한다(Function<List<Long>, List<Long>> pick, String code) {
    MfdsDeclaration source = fixture.row("SRC", key(1));
    MfdsDeclaration other = fixture.row("OTHER", key(1));

    assertRejected(
        () -> confirm(source.getId(), pick.apply(List.of(source.getId(), other.getId()))), code);
    assertThat(Stream.of(source, other))
        .extracting(MfdsDeclaration::getSelectedAlcoholId)
        .containsOnlyNulls();
    assertThat(fixture.selections.findAll()).isEmpty();
  }

  @Test
  @DisplayName("없는 주류로 확정할 때 거절하고 아무것도 쓰지 않는다")
  void 없는_주류는_거절한다() {
    MfdsDeclaration source = fixture.row("SRC", key(1));

    assertRejected(
        () ->
            service.confirm(
                source.getId(),
                new MfdsBulkMatchingConfirmRequest(404L, null, null, List.of(source.getId())),
                ADMIN_ID),
        "MFDS_SELECTED_ALCOHOL_NOT_FOUND");
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("미리보기 없이도 확정할 수 있고 충돌·확인 필요로 분류된 신고도 덮어쓴다")
  void 충돌과_확인_필요도_덮어쓴다() {
    MfdsDeclaration source = fixture.row("SRC", key(1));
    MfdsDeclaration conflict = fixture.row("CONFLICT", key(1));
    link(conflict, 99L, 7L, 8L);
    MfdsDeclaration review = fixture.row("REVIEW", key(1));
    MfdsTestData.set(review, "batchNumber", "9");
    MfdsBulkMatchingPreviewResponse preview = preview(source.getId(), null, null);

    MfdsBulkMatchingConfirmResponse response =
        confirm(source.getId(), List.of(conflict.getId(), review.getId()));

    assertThat(item(preview, conflict.getId()).orElseThrow().classification())
        .isEqualTo("CONFLICT");
    assertThat(item(preview, review.getId()).orElseThrow().classification())
        .isEqualTo("NEEDS_REVIEW");
    assertThat(response.applied())
        .extracting(MfdsMatchingConfirmResponse::declarationId)
        .containsExactly(conflict.getId(), review.getId());
    assertThat(Stream.of(conflict, review))
        .extracting(
            MfdsDeclaration::getSelectedAlcoholId,
            MfdsDeclaration::getSelectedDistilleryId,
            MfdsDeclaration::getSelectedRegionId)
        .containsOnly(tuple(ALCOHOL_ID, DISTILLERY_ID, REGION_ID));
    assertThat(review.getBatchNumber()).isEqualTo("9");
  }

  @Test
  @DisplayName("기준 신고와 다른 제품 그룹의 신고를 골랐을 때도 그대로 반영한다")
  void 그룹_밖_신고도_반영한다() {
    MfdsDeclaration source = fixture.row("SRC", key(1));
    MfdsDeclaration otherKey = fixture.row("OTHER-KEY", key(2));
    MfdsDeclaration noKey = fixture.row("NO-KEY", null);

    confirm(source.getId(), List.of(otherKey.getId(), noKey.getId()));

    assertThat(Stream.of(otherKey, noKey))
        .extracting(MfdsDeclaration::getSelectedAlcoholId)
        .containsOnly(ALCOHOL_ID);
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("고른 신고만 확정할 때 나머지는 그대로 둔다")
  void 선택하지_않은_신고는_유지한다() {
    MfdsDeclaration source = fixture.row("SRC", key(1));
    MfdsDeclaration chosen = fixture.row("CHOSEN", key(1));
    MfdsDeclaration skipped = fixture.row("SKIP", key(1));

    MfdsBulkMatchingConfirmResponse response = confirm(source.getId(), List.of(chosen.getId()));

    assertThat(response.applied())
        .extracting(MfdsMatchingConfirmResponse::declarationId)
        .containsExactly(chosen.getId());
    assertThat(response.unchangedDeclarationIds()).isEmpty();
    assertThat(chosen.getSelectedAlcoholId()).isEqualTo(ALCOHOL_ID);
    assertThat(skipped.getSelectedAlcoholId()).isNull();
    assertThat(source.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("이미 같은 주류·증류소·지역이 연결된 신고를 확정할 때 저장과 감사 이력을 건너뛴다")
  void 같은_값은_건너뛴다() {
    List<Integer> saves = new ArrayList<>();
    use(
        new InMemoryMfdsDeclarationRepository() {
          @Override
          public MfdsDeclaration save(MfdsDeclaration declaration) {
            saves.add(1);
            return super.save(declaration);
          }
        });
    MfdsDeclaration source = fixture.row("SRC", key(1));
    link(source, ALCOHOL_ID, DISTILLERY_ID, REGION_ID);
    MfdsTestData.set(source, "alcoholNameKo", "기존 이름");
    saves.clear();

    MfdsBulkMatchingConfirmResponse response = confirm(source.getId(), List.of(source.getId()));

    assertThat(response.unchangedDeclarationIds()).containsExactly(source.getId());
    assertThat(response.applied()).isEmpty();
    assertThat(source.getAlcoholNameKo()).isEqualTo("기존 이름");
    assertThat(saves).isEmpty();
    assertThat(fixture.selections.findAll()).isEmpty();
  }

  @Test
  @DisplayName("확정할 때 선택값·주류명과 기준 신고·관리자 이력만 남기고 원문 속성은 유지한다")
  void 확정은_선택값과_이력만_저장한다() {
    MfdsDeclaration source = fixture.row("SRC", key(1));
    MfdsDeclaration target = fixture.row("TGT", key(1));
    MfdsTestData.set(target, "abvPercent", new BigDecimal("40.000"));
    MfdsTestData.set(target, "ageYears", (short) 12);
    MfdsTestData.set(target, "batchNumber", "8");
    MfdsTestData.set(target, "volumeMl", 200);
    MfdsTestData.set(target, "matchingRunId", 5L);

    confirm(source.getId(), List.of(target.getId()));

    assertThat(target)
        .extracting(
            MfdsDeclaration::getSelectedAlcoholId,
            MfdsDeclaration::getSelectedDistilleryId,
            MfdsDeclaration::getSelectedRegionId,
            MfdsDeclaration::getAlcoholMatchDecision,
            MfdsDeclaration::getDistilleryMatchSource,
            MfdsDeclaration::getRegionMatchSource,
            MfdsDeclaration::getAlcoholNameKo,
            MfdsDeclaration::getAlcoholNameEn)
        .containsExactly(
            ALCOHOL_ID,
            DISTILLERY_ID,
            REGION_ID,
            "MANUAL",
            "ALCOHOL_PROPAGATED",
            "ALCOHOL_PROPAGATED",
            "몽키숄더",
            "Monkey Shoulder");
    assertThat(target.getVolumeMl()).isEqualTo(200);
    assertThat(target.getAbvPercent()).isEqualByComparingTo("40.000");
    assertThat(target.getAgeYears()).isEqualTo((short) 12);
    assertThat(target.getBatchNumber()).isEqualTo("8");
    assertThat(target.getMatchingRunId()).isEqualTo(5L);
    assertThat(target.getInheritedFromDeclarationId()).isNull();
    assertThat(fixture.selections.findAll())
        .extracting(MfdsMatchingSelection::getReasonCode)
        .containsExactly(
            "BULK_MANUAL:" + source.getId(),
            "BULK_ALCOHOL_PROPAGATED:" + source.getId(),
            "BULK_ALCOHOL_PROPAGATED:" + source.getId());
    assertThat(fixture.selections.findAll())
        .extracting(MfdsMatchingSelection::getSelectedBy)
        .containsOnly("42");
  }

  @Test
  @DisplayName("증류소가 없는 주류로 덮어쓸 때 기존 증류소를 비우고 해제 이력을 남긴다")
  void 비워진_참조는_해제_이력을_남긴다() {
    fixture.alcohols.addAlcohol(alcohol(11L, "증류소 없는 주류", "No Distillery", null, null));
    MfdsDeclaration source = fixture.row("SRC", key(1));
    link(source, 99L, 7L, null);

    service.confirm(
        source.getId(),
        new MfdsBulkMatchingConfirmRequest(11L, null, null, List.of(source.getId())),
        ADMIN_ID);

    assertThat(source.getSelectedAlcoholId()).isEqualTo(11L);
    assertThat(source.getSelectedDistilleryId()).isNull();
    assertThat(fixture.selections.findAll())
        .extracting(
            MfdsMatchingSelection::getAction,
            MfdsMatchingSelection::getTargetType,
            MfdsMatchingSelection::getTargetId)
        .containsExactly(tuple("SELECT", "ALCOHOL", 11L), tuple("REVOKE", "DISTILLERY", 7L));
  }

  @Test
  @DisplayName("후보에 있는 주류를 확정할 때 CANDIDATE로 기록한다")
  void 후보_선택이면_CANDIDATE다() {
    MfdsDeclaration source = fixture.row("SRC", key(1));
    MfdsMatchingRun run =
        fixture.matching.saveRun(
            MfdsMatchingRun.builder().matcherVersion("mfds-matching-v2").status("DONE").build());
    MfdsTestData.set(source, "matchingRunId", run.getId());
    MfdsTestData.set(source, "matchingVersion", "mfds-matching-v2");
    fixture.matching.saveCandidate(
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

    confirm(source.getId(), List.of(source.getId()));

    assertThat(source.getAlcoholMatchDecision()).isEqualTo("CANDIDATE");
    assertThat(fixture.selections.findAll().get(0).getReasonCode())
        .isEqualTo("BULK_CANDIDATE:" + source.getId());
  }

  @Test
  @DisplayName("요청에 증류소를 지정할 때 주류에 등록된 값 대신 그 값을 쓴다")
  void 명시한_증류소를_적용한다() {
    fixture.alcohols.addDistillery(new DistilleryMatchTargetItem(9L, "다른 증류소", "Other"));
    MfdsDeclaration source = fixture.row("SRC", key(1));

    service.confirm(
        source.getId(),
        new MfdsBulkMatchingConfirmRequest(ALCOHOL_ID, 9L, null, List.of(source.getId())),
        ADMIN_ID);

    assertThat(preview(source.getId(), 9L, null).distilleryId()).isEqualTo(9L);
    assertThat(source.getSelectedDistilleryId()).isEqualTo(9L);
    assertThat(source.getDistilleryMatchSource()).isEqualTo("MANUAL");
  }

  @Test
  @DisplayName("여러 신고를 확정할 때 요청 순서와 관계없이 신고 ID 오름차순으로 잠근다")
  void 잠금_순서는_신고_ID_오름차순이다() {
    List<Collection<Long>> lockRequests = new ArrayList<>();
    List<Long> lockedIds = new ArrayList<>();
    use(
        new InMemoryMfdsDeclarationRepository() {
          @Override
          public List<MfdsDeclaration> findByIdInForUpdate(Collection<Long> ids) {
            lockRequests.add(List.copyOf(ids));
            List<MfdsDeclaration> rows = super.findByIdInForUpdate(ids);
            rows.forEach(row -> lockedIds.add(row.getId()));
            return rows;
          }
        });
    MfdsDeclaration third = savedWithId("C", 30L);
    MfdsDeclaration first = savedWithId("A", 10L);
    savedWithId("B", 20L);

    confirm(first.getId(), List.of(third.getId(), first.getId()));

    assertThat(lockRequests).containsExactly(List.of(10L, 30L));
    assertThat(lockedIds).containsExactly(10L, 30L);
  }

  private MfdsBulkMatchingPreviewResponse preview(Long sourceId, Long distilleryId, Long regionId) {
    return MfdsBulkTestFixture.preview(service, sourceId, ALCOHOL_ID, distilleryId, regionId);
  }

  private MfdsBulkMatchingConfirmResponse confirm(Long sourceId, List<Long> ids) {
    return service.confirm(
        sourceId, new MfdsBulkMatchingConfirmRequest(ALCOHOL_ID, null, null, ids), ADMIN_ID);
  }

  private static void assertRejected(ThrowingCallable call, String code) {
    assertThatThrownBy(call)
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.valueOf(code).getMessage());
  }

  private MfdsDeclaration savedWithId(String rcno, long id) {
    MfdsDeclaration declaration = MfdsBulkTestFixture.declaration(rcno, key(1));
    ReflectionTestUtils.setField(declaration, "id", id);
    return fixture.declarations.save(declaration);
  }

  private static MfdsMatchingSelection revoke(Long declarationId, LocalDateTime at) {
    return MfdsMatchingSelection.adminRevoke(
        declarationId, "ALCOHOL", 1L, "ADMIN_RELEASE", ADMIN_ID, at);
  }

  private static void link(
      MfdsDeclaration declaration, Long alcoholId, Long distilleryId, Long regionId) {
    MfdsTestData.set(declaration, "selectedAlcoholId", alcoholId);
    MfdsTestData.set(declaration, "selectedDistilleryId", distilleryId);
    MfdsTestData.set(declaration, "selectedRegionId", regionId);
  }

  private static Arguments selection(
      String name, Function<List<Long>, List<Long>> pick, String code) {
    return Arguments.of(Named.of(name, pick), code);
  }
}

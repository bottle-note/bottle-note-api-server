package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.alcohols.fixture.FakeAlcoholMatchTargetFacade;
import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.request.MfdsMatchingConfirmRequest;
import app.bottlenote.mfds.dto.response.MfdsAlcoholCandidateItem;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetailItem;
import app.bottlenote.mfds.dto.response.MfdsMatchingCandidatesResponse;
import app.bottlenote.mfds.dto.response.MfdsMatchingConfirmResponse;
import app.bottlenote.mfds.dto.response.MfdsMatchingRunResponse;
import app.bottlenote.mfds.dto.response.MfdsReferenceCandidateItem;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.exception.MfdsExceptionCode;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("MfdsMatchingService 단위 테스트")
class MfdsMatchingServiceTest {

  private RecordingDeclarationRepository declarationRepository;
  private RecordingMatchTargetFacade alcoholMatchTargetFacade;
  private MfdsMatchingService matchingService;

  @BeforeEach
  void setUp() {
    declarationRepository = new RecordingDeclarationRepository();
    alcoholMatchTargetFacade = new RecordingMatchTargetFacade();
    matchingService =
        new MfdsMatchingService(
            declarationRepository, alcoholMatchTargetFacade, new MfdsMatchingScoreCalculator());
  }

  @Test
  @DisplayName("매칭을 실행할 때 점수 상위 3개 후보만 저장한다")
  void 후보를_3개까지만_저장한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    alcoholMatchTargetFacade.addAlcohol(alcohol(2L, "글렌피딕 12 리저브", "Glenfiddich 12 Reserve"));
    alcoholMatchTargetFacade.addAlcohol(
        alcohol(3L, "글렌피딕 12 스페셜 에디션", "Glenfiddich 12 Special Edition"));
    alcoholMatchTargetFacade.addAlcohol(
        alcohol(4L, "글렌피딕 12 캐스크 스트렝스 리미티드", "Glenfiddich 12 Cask Strength Limited"));

    MfdsMatchingRunResponse response = matchingService.runMatching(declaration.getId());

    assertThat(response.alcoholCandidates()).hasSize(3);
    assertThat(response.alcoholCandidates().get(0).alcoholId()).isEqualTo(1L);
    assertThat(response.alcoholCandidates().get(0).scoreDetail()).isNotNull();
    assertThat(declaration.getAlcoholCandidates()).hasSize(3);
    assertThat(declaration.getAlcoholCandidate1Id()).isEqualTo(1L);
    assertThat(declaration.getAlcoholCandidate1Score())
        .isGreaterThanOrEqualTo(declaration.getAlcoholCandidate2Score());
    assertThat(declaration.getAlcoholCandidate2Score())
        .isGreaterThanOrEqualTo(declaration.getAlcoholCandidate3Score());
    assertThat(declaration.getMatchingVersion()).isEqualTo(MfdsMatchingService.MATCHING_VERSION);
    assertThat(declaration.getMatchedAt()).isNotNull();
  }

  @Test
  @DisplayName("비교 집합이 비어 있을 때 후보 없이 매칭 이력만 기록한다")
  void 빈_비교_집합이면_후보없이_기록한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");

    MfdsMatchingRunResponse response = matchingService.runMatching(declaration.getId());

    assertThat(response.alcoholCandidates()).isEmpty();
    assertThat(response.distilleryCandidates()).isEmpty();
    assertThat(response.regionCandidates()).isEmpty();
    assertThat(declaration.getAlcoholCandidate1Id()).isNull();
    assertThat(declaration.getMatchingVersion()).isEqualTo(MfdsMatchingService.MATCHING_VERSION);
    assertThat(declaration.getMatchedAt()).isNotNull();
  }

  @Test
  @DisplayName("점수가 기준 미달인 대상은 후보에서 제외한다")
  void 기준_미달_대상은_후보에서_제외한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "산토리 가쿠빈", "Suntory Kakubin"));

    MfdsMatchingRunResponse response = matchingService.runMatching(declaration.getId());

    assertThat(response.alcoholCandidates()).isEmpty();
    assertThat(declaration.getAlcoholCandidate1Id()).isNull();
  }

  @Test
  @DisplayName("다시 실행할 때 기존 후보를 덮어쓴다")
  void 재실행시_기존_후보를_덮어쓴다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());
    assertThat(declaration.getAlcoholCandidate1Id()).isEqualTo(1L);

    alcoholMatchTargetFacade.clear();
    matchingService.runMatching(declaration.getId());

    assertThat(declaration.getAlcoholCandidate1Id()).isNull();
    assertThat(declaration.getAlcoholCandidate1Score()).isNull();
  }

  @Test
  @DisplayName("증류소·지역 이름 후보가 있을 때 함께 매칭한다")
  void 증류소와_지역도_함께_매칭한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    MfdsTestData.set(declaration, "distilleryNameEnCandidate", "Glenfiddich");
    MfdsTestData.set(declaration, "manufactureCountryNameEn", "United Kingdom");
    alcoholMatchTargetFacade.addDistillery(
        new DistilleryMatchTargetItem(11L, "글렌피딕", "Glenfiddich"));
    alcoholMatchTargetFacade.addDistillery(new DistilleryMatchTargetItem(12L, "야마자키", "Yamazaki"));
    alcoholMatchTargetFacade.addRegion(new RegionMatchTargetItem(21L, "영국", "United Kingdom"));
    alcoholMatchTargetFacade.addRegion(new RegionMatchTargetItem(22L, "일본", "Japan"));

    MfdsMatchingRunResponse response = matchingService.runMatching(declaration.getId());

    assertThat(response.distilleryCandidates()).hasSize(1);
    assertThat(response.distilleryCandidates().get(0).id()).isEqualTo(11L);
    assertThat(response.regionCandidates()).hasSize(1);
    assertThat(response.regionCandidates().get(0).id()).isEqualTo(21L);
    assertThat(declaration.getDistilleryCandidate1Id()).isEqualTo(11L);
    assertThat(declaration.getRegionCandidate1Id()).isEqualTo(21L);
  }

  @Test
  @DisplayName("저장된 후보를 요약 정보와 함께 조회할 수 있다")
  void 저장된_후보를_조회할_수_있다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());

    MfdsMatchingCandidatesResponse response = matchingService.getCandidates(declaration.getId());

    assertThat(response.alcoholCandidates()).hasSize(1);
    assertThat(response.alcoholCandidates().get(0).alcoholId()).isEqualTo(1L);
    assertThat(response.alcoholCandidates().get(0).korName()).isEqualTo("글렌피딕 12");
    assertThat(response.matchingVersion()).isEqualTo(MfdsMatchingService.MATCHING_VERSION);
    assertThat(response.selection().alcoholId()).isNull();
  }

  @Test
  @DisplayName("후보 안의 ID로 확정할 때 CANDIDATE 결정으로 기록한다")
  void 후보내_ID_확정시_CANDIDATE로_기록한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null));

    assertThat(response.selectedAlcoholId()).isEqualTo(1L);
    assertThat(response.alcoholMatchDecision()).isEqualTo("CANDIDATE");
    assertThat(declaration.getSelectedAlcoholId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("후보 밖의 ID로 확정할 때 MANUAL 결정으로 기록한다")
  void 후보외_ID_확정시_MANUAL로_기록한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());
    alcoholMatchTargetFacade.addAlcohol(alcohol(99L, "발베니 12", "Balvenie 12"));

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(99L, null, null));

    assertThat(response.selectedAlcoholId()).isEqualTo(99L);
    assertThat(response.alcoholMatchDecision()).isEqualTo("MANUAL");
  }

  @Test
  @DisplayName("존재하지 않는 주류 ID로 확정할 때 예외를 던진다")
  void 존재하지_않는_주류로_확정하면_예외가_발생한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");

    assertThatThrownBy(
            () ->
                matchingService.confirmMatching(
                    declaration.getId(), new MfdsMatchingConfirmRequest(404L, null, null)))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_SELECTED_ALCOHOL_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("확정을 해제할 때 선택 상태만 비우고 후보는 유지한다")
  void 확정_해제시_선택만_비운다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());
    matchingService.confirmMatching(
        declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null));

    MfdsMatchingConfirmResponse response = matchingService.clearMatching(declaration.getId());

    assertThat(response.selectedAlcoholId()).isNull();
    assertThat(response.alcoholMatchDecision()).isNull();
    assertThat(declaration.getSelectedAlcoholId()).isNull();
    assertThat(declaration.getAlcoholCandidate1Id()).isEqualTo(1L);
    assertThat(declaration.getMatchedAt()).isNotNull();
  }

  @Test
  @DisplayName("존재하지 않는 신고 데이터로 실행할 때 예외를 던진다")
  void 신고_데이터가_없으면_예외가_발생한다() {
    assertThatThrownBy(() -> matchingService.runMatching(404L))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("매칭을 확정·해제할 때 잠금 조회로 신고를 읽는다")
  void 확정과_해제는_잠금_조회를_사용한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    declarationRepository.resetCounts();

    matchingService.confirmMatching(
        declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null));
    matchingService.clearMatching(declaration.getId());

    assertThat(declarationRepository.lockedReads).isEqualTo(2);
    assertThat(declarationRepository.plainReads).isZero();
  }

  @Test
  @DisplayName("저장된 후보를 조회할 때는 잠금 없이 읽는다")
  void 후보_조회는_잠금을_걸지_않는다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    declarationRepository.resetCounts();

    matchingService.getCandidates(declaration.getId());

    assertThat(declarationRepository.lockedReads).isZero();
    assertThat(declarationRepository.plainReads).isEqualTo(1);
  }

  @Test
  @DisplayName("저장된 후보를 조회할 때 증류소·지역을 전체 조회하지 않고 후보 ID로만 읽는다")
  void 후보_조회는_참조_테이블을_전체_조회하지_않는다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    MfdsTestData.set(declaration, "distilleryNameEnCandidate", "Glenfiddich");
    MfdsTestData.set(declaration, "manufactureCountryNameEn", "United Kingdom");
    alcoholMatchTargetFacade.addDistillery(
        new DistilleryMatchTargetItem(11L, "글렌피딕", "Glenfiddich"));
    alcoholMatchTargetFacade.addRegion(new RegionMatchTargetItem(21L, "영국", "United Kingdom"));
    matchingService.runMatching(declaration.getId());
    alcoholMatchTargetFacade.resetCounts();

    MfdsMatchingCandidatesResponse response = matchingService.getCandidates(declaration.getId());

    assertThat(response.distilleryCandidates().get(0).korName()).isEqualTo("글렌피딕");
    assertThat(response.regionCandidates().get(0).korName()).isEqualTo("영국");
    assertThat(alcoholMatchTargetFacade.fullScans).isZero();
    assertThat(alcoholMatchTargetFacade.distilleryIdLookups).isEqualTo(1);
    assertThat(alcoholMatchTargetFacade.regionIdLookups).isEqualTo(1);
  }

  @Test
  @DisplayName("저장된 증류소·지역 후보가 없을 때 참조 테이블을 조회하지 않는다")
  void 후보가_없으면_참조_테이블을_조회하지_않는다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.resetCounts();

    MfdsMatchingCandidatesResponse response = matchingService.getCandidates(declaration.getId());

    assertThat(response.distilleryCandidates()).isEmpty();
    assertThat(response.regionCandidates()).isEmpty();
    assertThat(alcoholMatchTargetFacade.fullScans).isZero();
    assertThat(alcoholMatchTargetFacade.distilleryIdLookups).isZero();
    assertThat(alcoholMatchTargetFacade.regionIdLookups).isZero();
  }

  @Test
  @DisplayName("총점이 임계값 0.4와 같은 대상은 후보에 포함하고 0.399는 제외한다")
  void 총점이_임계값과_같으면_후보에_포함한다() {
    // 필터가 >= 에서 > 로 바뀌면 0.4 대상이 사라진다. 실제 산식으로는 이 경계를 정확히 만들 수 없어 점수를 고정한다
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    FixedScoreCalculator calculator = new FixedScoreCalculator();
    calculator.putAlcoholScore(1L, "0.400");
    calculator.putAlcoholScore(2L, "0.399");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "임계값 동일", "Exactly Threshold"));
    alcoholMatchTargetFacade.addAlcohol(alcohol(2L, "임계값 미만", "Below Threshold"));

    MfdsMatchingRunResponse response = serviceWith(calculator).runMatching(declaration.getId());

    assertThat(response.alcoholCandidates())
        .extracting(MfdsAlcoholCandidateItem::alcoholId)
        .containsExactly(1L);
    assertThat(declaration.getAlcoholCandidate1Id()).isEqualTo(1L);
    assertThat(declaration.getAlcoholCandidate2Id()).isNull();
  }

  @Test
  @DisplayName("증류소·지역 후보도 점수가 임계값 0.4와 같으면 포함하고 0.399는 제외한다")
  void 참조_후보도_임계값_경계를_포함한다() {
    // 참조 후보는 알코올과 별도의 필터를 쓴다. 같은 경계 규칙이 적용되는지 따로 확인한다
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    FixedScoreCalculator calculator = new FixedScoreCalculator();
    calculator.putDistilleryScore(11L, "0.400");
    calculator.putDistilleryScore(12L, "0.399");
    calculator.putRegionScore(21L, "0.400");
    calculator.putRegionScore(22L, "0.399");
    alcoholMatchTargetFacade.addDistillery(new DistilleryMatchTargetItem(11L, "포함", "Included"));
    alcoholMatchTargetFacade.addDistillery(new DistilleryMatchTargetItem(12L, "제외", "Excluded"));
    alcoholMatchTargetFacade.addRegion(new RegionMatchTargetItem(21L, "포함", "Included"));
    alcoholMatchTargetFacade.addRegion(new RegionMatchTargetItem(22L, "제외", "Excluded"));

    MfdsMatchingRunResponse response = serviceWith(calculator).runMatching(declaration.getId());

    assertThat(response.distilleryCandidates())
        .extracting(MfdsReferenceCandidateItem::id)
        .containsExactly(11L);
    assertThat(response.regionCandidates())
        .extracting(MfdsReferenceCandidateItem::id)
        .containsExactly(21L);
  }

  @Test
  @DisplayName("점수가 모두 같을 때 alcoholId 오름차순으로 상위 3개를 뽑는다")
  void 동점_후보는_alcoholId_오름차순으로_자른다() {
    // 동점이면 정렬 결과가 입력 순서에 좌우될 수 있다. id 오름차순 고정이 계약이다
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    FixedScoreCalculator calculator = new FixedScoreCalculator();
    List.of(7L, 3L, 9L, 1L, 5L)
        .forEach(
            id -> {
              calculator.putAlcoholScore(id, "0.900");
              alcoholMatchTargetFacade.addAlcohol(alcohol(id, "동점 " + id, "Tied " + id));
            });
    MfdsMatchingService service = serviceWith(calculator);

    MfdsMatchingRunResponse first = service.runMatching(declaration.getId());
    MfdsMatchingRunResponse second = service.runMatching(declaration.getId());

    assertThat(first.alcoholCandidates())
        .extracting(MfdsAlcoholCandidateItem::alcoholId)
        .containsExactly(1L, 3L, 5L);
    assertThat(second.alcoholCandidates())
        .extracting(MfdsAlcoholCandidateItem::alcoholId)
        .containsExactly(1L, 3L, 5L);
  }

  @Test
  @DisplayName("점수가 높은 대상을 먼저 두고 동점 구간만 alcoholId 오름차순으로 정렬한다")
  void 점수가_우선이고_동점만_id로_정렬한다() {
    // 상위 3개 컷이 동점 구간을 가로지를 때도 결과가 결정적이어야 한다
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    FixedScoreCalculator calculator = new FixedScoreCalculator();
    calculator.putAlcoholScore(9L, "0.950");
    alcoholMatchTargetFacade.addAlcohol(alcohol(9L, "최고점", "Top"));
    List.of(5L, 1L, 3L)
        .forEach(
            id -> {
              calculator.putAlcoholScore(id, "0.900");
              alcoholMatchTargetFacade.addAlcohol(alcohol(id, "동점 " + id, "Tied " + id));
            });

    MfdsMatchingRunResponse response = serviceWith(calculator).runMatching(declaration.getId());

    assertThat(response.alcoholCandidates())
        .extracting(MfdsAlcoholCandidateItem::alcoholId)
        .containsExactly(9L, 1L, 3L);
  }

  @Test
  @DisplayName("저장된 후보가 주류 목록에서 사라졌을 때 ID와 점수만 반환한다")
  void 사라진_후보는_요약_없이_반환한다() {
    // 매칭 실행 이후 주류가 삭제될 수 있다. 요약 조회 실패로 후보 자체가 사라지면 안 된다
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());
    alcoholMatchTargetFacade.clear();

    MfdsMatchingCandidatesResponse response = matchingService.getCandidates(declaration.getId());

    assertThat(response.alcoholCandidates()).hasSize(1);
    MfdsAlcoholCandidateItem candidate = response.alcoholCandidates().get(0);
    assertThat(candidate.alcoholId()).isEqualTo(1L);
    assertThat(candidate.score()).isNotNull();
    assertThat(candidate.korName()).isNull();
    assertThat(candidate.engName()).isNull();
    assertThat(candidate.imageUrl()).isNull();
    assertThat(candidate.scoreDetail()).isNull();
  }

  @Test
  @DisplayName("존재하지 않는 증류소 ID로 확정할 때 예외를 던진다")
  void 존재하지_않는_증류소로_확정하면_예외가_발생한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));

    assertThatThrownBy(
            () ->
                matchingService.confirmMatching(
                    declaration.getId(), new MfdsMatchingConfirmRequest(1L, 404L, null)))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_SELECTED_DISTILLERY_NOT_FOUND.getMessage());
    assertThat(declaration.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("존재하지 않는 지역 ID로 확정할 때 예외를 던진다")
  void 존재하지_않는_지역으로_확정하면_예외가_발생한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    alcoholMatchTargetFacade.addDistillery(
        new DistilleryMatchTargetItem(11L, "글렌피딕", "Glenfiddich"));

    assertThatThrownBy(
            () ->
                matchingService.confirmMatching(
                    declaration.getId(), new MfdsMatchingConfirmRequest(1L, 11L, 404L)))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_SELECTED_REGION_NOT_FOUND.getMessage());
    assertThat(declaration.getSelectedAlcoholId()).isNull();
  }

  @Test
  @DisplayName("후보 안의 증류소·지역 ID로 확정할 때 CANDIDATE 근거로 기록한다")
  void 후보내_증류소와_지역_확정시_CANDIDATE로_기록한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    MfdsTestData.set(declaration, "distilleryNameEnCandidate", "Glenfiddich");
    MfdsTestData.set(declaration, "manufactureCountryNameEn", "United Kingdom");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    alcoholMatchTargetFacade.addDistillery(
        new DistilleryMatchTargetItem(11L, "글렌피딕", "Glenfiddich"));
    alcoholMatchTargetFacade.addRegion(new RegionMatchTargetItem(21L, "영국", "United Kingdom"));
    matchingService.runMatching(declaration.getId());

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, 11L, 21L));

    assertThat(response.selectedDistilleryId()).isEqualTo(11L);
    assertThat(response.distilleryMatchSource())
        .isEqualTo(MfdsMatchSelectionSource.CANDIDATE.name());
    assertThat(response.selectedRegionId()).isEqualTo(21L);
    assertThat(response.regionMatchSource()).isEqualTo(MfdsMatchSelectionSource.CANDIDATE.name());
    assertThat(declaration.getSelectedDistilleryId()).isEqualTo(11L);
    assertThat(declaration.getSelectedRegionId()).isEqualTo(21L);
  }

  @Test
  @DisplayName("후보 밖의 증류소·지역 ID로 확정할 때 MANUAL 근거로 기록한다")
  void 후보외_증류소와_지역_확정시_MANUAL로_기록한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());
    alcoholMatchTargetFacade.addDistillery(new DistilleryMatchTargetItem(99L, "야마자키", "Yamazaki"));
    alcoholMatchTargetFacade.addRegion(new RegionMatchTargetItem(98L, "일본", "Japan"));

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, 99L, 98L));

    assertThat(response.distilleryMatchSource()).isEqualTo(MfdsMatchSelectionSource.MANUAL.name());
    assertThat(response.regionMatchSource()).isEqualTo(MfdsMatchSelectionSource.MANUAL.name());
  }

  @Test
  @DisplayName("증류소·지역을 지정하지 않고 확정할 때 선택 근거를 남기지 않는다")
  void 증류소와_지역_미지정시_선택_근거가_없다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null));

    assertThat(response.selectedDistilleryId()).isNull();
    assertThat(response.distilleryMatchSource()).isNull();
    assertThat(response.selectedRegionId()).isNull();
    assertThat(response.regionMatchSource()).isNull();
  }

  private MfdsMatchingService serviceWith(MfdsMatchingScoreCalculator calculator) {
    return new MfdsMatchingService(declarationRepository, alcoholMatchTargetFacade, calculator);
  }

  private MfdsDeclaration savedDeclaration(String nameKo, String nameEn) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null, nameKo, nameEn);
    return declarationRepository.save(declaration);
  }

  private AlcoholMatchTargetItem alcohol(Long id, String korName, String engName) {
    return new AlcoholMatchTargetItem(
        id,
        korName,
        engName,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "https://bottlenote.app/alcohol/" + id);
  }

  /** 잠금 조회 경로가 실제로 쓰이는지 확인하기 위한 호출 기록용 더블. */
  private static class RecordingDeclarationRepository extends InMemoryMfdsDeclarationRepository {

    private int plainReads;
    private int lockedReads;

    void resetCounts() {
      plainReads = 0;
      lockedReads = 0;
    }

    @Override
    public Optional<MfdsDeclaration> findById(Long id) {
      plainReads++;
      return super.findById(id);
    }

    @Override
    public Optional<MfdsDeclaration> findByIdForUpdate(Long id) {
      lockedReads++;
      return super.findById(id);
    }
  }

  /** 참조 테이블 전체 조회 여부를 확인하기 위한 호출 기록용 더블. */
  private static class RecordingMatchTargetFacade extends FakeAlcoholMatchTargetFacade {

    private int fullScans;
    private int distilleryIdLookups;
    private int regionIdLookups;

    void resetCounts() {
      fullScans = 0;
      distilleryIdLookups = 0;
      regionIdLookups = 0;
    }

    @Override
    public List<DistilleryMatchTargetItem> findAllDistilleryTargets() {
      fullScans++;
      return super.findAllDistilleryTargets();
    }

    @Override
    public List<RegionMatchTargetItem> findAllRegionTargets() {
      fullScans++;
      return super.findAllRegionTargets();
    }

    @Override
    public List<DistilleryMatchTargetItem> findDistilleryTargetsByIds(List<Long> distilleryIds) {
      distilleryIdLookups++;
      return super.findDistilleryTargetsByIds(distilleryIds);
    }

    @Override
    public List<RegionMatchTargetItem> findRegionTargetsByIds(List<Long> regionIds) {
      regionIdLookups++;
      return super.findRegionTargetsByIds(regionIds);
    }
  }

  /** 임계값·정렬 경계를 정확히 만들기 위해 총점을 고정하는 스텁. 실제 산식은 계산기 테스트가 검증한다. */
  private static class FixedScoreCalculator extends MfdsMatchingScoreCalculator {

    private final Map<Long, BigDecimal> alcoholScores = new HashMap<>();
    private final Map<Long, BigDecimal> distilleryScores = new HashMap<>();
    private final Map<Long, BigDecimal> regionScores = new HashMap<>();

    void putAlcoholScore(Long alcoholId, String score) {
      alcoholScores.put(alcoholId, new BigDecimal(score));
    }

    void putDistilleryScore(Long distilleryId, String score) {
      distilleryScores.put(distilleryId, new BigDecimal(score));
    }

    void putRegionScore(Long regionId, String score) {
      regionScores.put(regionId, new BigDecimal(score));
    }

    @Override
    public MfdsMatchScoreDetailItem scoreAlcohol(
        MfdsDeclaration declaration, AlcoholMatchTargetItem target) {
      BigDecimal total = alcoholScores.getOrDefault(target.alcoholId(), BigDecimal.ZERO);
      return new MfdsMatchScoreDetailItem(total, null, null, null, null, total);
    }

    @Override
    public BigDecimal scoreDistillery(
        MfdsDeclaration declaration, DistilleryMatchTargetItem target) {
      return distilleryScores.get(target.id());
    }

    @Override
    public BigDecimal scoreRegion(MfdsDeclaration declaration, RegionMatchTargetItem target) {
      return regionScores.get(target.id());
    }
  }
}

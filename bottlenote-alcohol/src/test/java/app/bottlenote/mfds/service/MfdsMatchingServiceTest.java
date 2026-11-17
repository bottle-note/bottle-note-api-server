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
import app.bottlenote.mfds.domain.MfdsMatchCandidate;
import app.bottlenote.mfds.domain.MfdsMatchingSelection;
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
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingSelectionRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private InMemoryMfdsMatchingSelectionRepository selectionRepository;
  private MfdsMatchingHistoryService historyService;
  private InMemoryMfdsMatchingRepository matchingRepository;
  private static final Long ADMIN_ID = 42L;

  @BeforeEach
  void setUp() {
    declarationRepository = new RecordingDeclarationRepository();
    selectionRepository = new InMemoryMfdsMatchingSelectionRepository();
    alcoholMatchTargetFacade = new RecordingMatchTargetFacade();
    matchingRepository = new InMemoryMfdsMatchingRepository();
    historyService =
        new MfdsMatchingHistoryService(
            matchingRepository, new MfdsMatchingEvidenceCodec(new ObjectMapper()));
    matchingService =
        new MfdsMatchingService(
            declarationRepository,
            alcoholMatchTargetFacade,
            new MfdsMatchingScoreCalculator(),
            selectionRepository,
            historyService);
  }

  @Test
  @DisplayName("매칭을 실행할 때 점수 상위 10개 후보만 저장한다")
  void 후보를_10개까지만_저장한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    for (long id = 1; id <= 12; id++) {
      alcoholMatchTargetFacade.addAlcohol(alcohol(id, "글렌피딕 12", "Glenfiddich 12"));
    }

    MfdsMatchingRunResponse response = matchingService.runMatching(declaration.getId());

    assertThat(response.alcoholCandidates()).hasSize(10);
    assertThat(response.alcoholCandidates().get(0).alcoholId()).isEqualTo(1L);
    assertThat(response.alcoholCandidates().get(0).scoreDetail()).isNotNull();
    assertThat(storedCandidates(declaration, "ALCOHOL")).hasSize(10);
    assertThat(candidateId(declaration, "ALCOHOL", 0)).isEqualTo(1L);
    assertThat(candidateScore(declaration, "ALCOHOL", 0))
        .isGreaterThanOrEqualTo(candidateScore(declaration, "ALCOHOL", 1));
    assertThat(candidateScore(declaration, "ALCOHOL", 1))
        .isGreaterThanOrEqualTo(candidateScore(declaration, "ALCOHOL", 2));
    assertThat(matchingService.getCandidates(declaration.getId()).alcoholCandidates())
        .extracting(MfdsAlcoholCandidateItem::alcoholId)
        .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    assertThat(
            matchingService
                .confirmMatching(
                    declaration.getId(), new MfdsMatchingConfirmRequest(10L, null, null), ADMIN_ID)
                .alcoholMatchDecision())
        .isEqualTo("CANDIDATE");
    alcoholMatchTargetFacade.clear();
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());
    assertThat(storedCandidates(declaration, "ALCOHOL"))
        .extracting(candidate -> candidate.id())
        .containsExactly(1L);
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
    assertThat(candidateId(declaration, "ALCOHOL", 0)).isNull();
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
    assertThat(candidateId(declaration, "ALCOHOL", 0)).isNull();
  }

  @Test
  @DisplayName("다시 실행할 때 기존 후보를 덮어쓴다")
  void 재실행시_기존_후보를_덮어쓴다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());
    assertThat(candidateId(declaration, "ALCOHOL", 0)).isEqualTo(1L);

    alcoholMatchTargetFacade.clear();
    matchingService.runMatching(declaration.getId());

    assertThat(candidateId(declaration, "ALCOHOL", 0)).isNull();
    assertThat(candidateScore(declaration, "ALCOHOL", 0)).isNull();
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
    assertThat(candidateId(declaration, "DISTILLERY", 0)).isEqualTo(11L);
    assertThat(candidateId(declaration, "REGION", 0)).isEqualTo(21L);
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
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);

    assertThat(response.selectedAlcoholId()).isEqualTo(1L);
    assertThat(response.alcoholMatchDecision()).isEqualTo("CANDIDATE");
    assertThat(declaration.getSelectedAlcoholId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("확정하면 선택한 주류 이름으로 알코올명을 바로 바꾼다")
  void 확정시_알코올명을_선택한_주류_이름으로_바꾼다() {
    MfdsDeclaration declaration =
        savedDeclaration("글렌모렌지 프라이빗에디션 스피오스", "GLENMORANGIE PRIVATE EDITION SPIOS");
    alcoholMatchTargetFacade.addAlcohol(alcohol(5582L, "글렌모렌지 스피오스", "Glenmorangie Spios"));

    matchingService.confirmMatching(
        declaration.getId(), new MfdsMatchingConfirmRequest(5582L, null, null), ADMIN_ID);

    assertThat(declaration.getAlcoholNameKo()).isEqualTo("글렌모렌지 스피오스");
    assertThat(declaration.getAlcoholNameEn()).isEqualTo("Glenmorangie Spios");
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
            declaration.getId(), new MfdsMatchingConfirmRequest(99L, null, null), ADMIN_ID);

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
                    declaration.getId(),
                    new MfdsMatchingConfirmRequest(404L, null, null),
                    ADMIN_ID))
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
        declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);

    MfdsMatchingConfirmResponse response =
        matchingService.clearMatching(declaration.getId(), ADMIN_ID);

    assertThat(response.selectedAlcoholId()).isNull();
    assertThat(response.alcoholMatchDecision()).isNull();
    assertThat(declaration.getSelectedAlcoholId()).isNull();
    assertThat(candidateId(declaration, "ALCOHOL", 0)).isEqualTo(1L);
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
        declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);
    matchingService.clearMatching(declaration.getId(), ADMIN_ID);

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
  @DisplayName("저장된 후보를 조회할 때 저장 당시 증류소·지역 이름을 반환한다")
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
    assertThat(alcoholMatchTargetFacade.distilleryIdLookups).isZero();
    assertThat(alcoholMatchTargetFacade.regionIdLookups).isZero();
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
    assertThat(candidateId(declaration, "ALCOHOL", 0)).isEqualTo(1L);
    assertThat(candidateId(declaration, "ALCOHOL", 1)).isNull();
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
  @DisplayName("점수가 모두 같을 때 alcoholId 오름차순으로 상위 10개를 뽑는다")
  void 동점_후보는_alcoholId_오름차순으로_자른다() {
    // 동점이면 정렬 결과가 입력 순서에 좌우될 수 있다. id 오름차순 고정이 계약이다
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    FixedScoreCalculator calculator = new FixedScoreCalculator();
    List.of(12L, 7L, 3L, 9L, 1L, 5L, 11L, 2L, 10L, 4L, 8L, 6L)
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
        .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    assertThat(second.alcoholCandidates())
        .extracting(MfdsAlcoholCandidateItem::alcoholId)
        .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
  }

  @Test
  @DisplayName("점수가 높은 대상을 먼저 두고 동점 구간만 alcoholId 오름차순으로 정렬한다")
  void 점수가_우선이고_동점만_id로_정렬한다() {
    // 상위 10개 컷이 동점 구간을 가로지를 때도 결과가 결정적이어야 한다
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    FixedScoreCalculator calculator = new FixedScoreCalculator();
    calculator.putAlcoholScore(9L, "0.950");
    alcoholMatchTargetFacade.addAlcohol(alcohol(9L, "최고점", "Top"));
    List.of(12L, 11L, 10L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L)
        .forEach(
            id -> {
              calculator.putAlcoholScore(id, "0.900");
              alcoholMatchTargetFacade.addAlcohol(alcohol(id, "동점 " + id, "Tied " + id));
            });

    MfdsMatchingRunResponse response = serviceWith(calculator).runMatching(declaration.getId());

    assertThat(response.alcoholCandidates())
        .extracting(MfdsAlcoholCandidateItem::alcoholId)
        .containsExactly(9L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 10L);
  }

  @Test
  @DisplayName("저장된 후보가 주류 목록에서 사라졌을 때 저장 당시 이름과 점수 근거를 반환한다")
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
    assertThat(candidate.korName()).isEqualTo("글렌피딕 12");
    assertThat(candidate.engName()).isEqualTo("Glenfiddich 12");
    assertThat(candidate.imageUrl()).isNull();
    assertThat(candidate.scoreDetail()).isNotNull();
  }

  @Test
  @DisplayName("존재하지 않는 증류소 ID로 확정할 때 예외를 던진다")
  void 존재하지_않는_증류소로_확정하면_예외가_발생한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));

    assertThatThrownBy(
            () ->
                matchingService.confirmMatching(
                    declaration.getId(), new MfdsMatchingConfirmRequest(1L, 404L, null), ADMIN_ID))
        .isInstanceOf(MfdsException.class)
        .hasMessage(MfdsExceptionCode.MFDS_SELECTED_DISTILLERY_NOT_FOUND.getMessage());
    assertThat(declaration.getSelectedAlcoholId()).isNull();
    assertThat(selectionRepository.findAll()).isEmpty();
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
                    declaration.getId(), new MfdsMatchingConfirmRequest(1L, 11L, 404L), ADMIN_ID))
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
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, 11L, 21L), ADMIN_ID);

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
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, 99L, 98L), ADMIN_ID);

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
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);

    assertThat(response.selectedDistilleryId()).isNull();
    assertThat(response.distilleryMatchSource()).isNull();
    assertThat(response.selectedRegionId()).isNull();
    assertThat(response.regionMatchSource()).isNull();
  }

  @Test
  @DisplayName("증류소와 지역을 생략할 때 확정한 주류의 값과 전파 근거를 저장한다")
  void 생략한_증류소와_지역을_주류에서_전파한다() {
    MfdsDeclaration declaration = savedDeclaration("주류", "Alcohol");
    alcoholMatchTargetFacade.addAlcohol(alcoholWithReferences(1L, 11L, 21L));

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);

    assertThat(response.selectedDistilleryId()).isEqualTo(11L);
    assertThat(response.distilleryMatchSource()).isEqualTo("ALCOHOL_PROPAGATED");
    assertThat(response.selectedRegionId()).isEqualTo(21L);
    assertThat(response.regionMatchSource()).isEqualTo("ALCOHOL_PROPAGATED");
    assertThat(selectionRepository.findAll())
        .extracting(MfdsMatchingSelection::getTargetType)
        .containsExactly("ALCOHOL", "DISTILLERY", "REGION");
    assertThat(selectionRepository.findAll())
        .extracting(MfdsMatchingSelection::getTargetId)
        .containsExactly(1L, 11L, 21L);
    assertThat(selectionRepository.findAll())
        .extracting(MfdsMatchingSelection::getReasonCode)
        .containsExactly("MANUAL", "ALCOHOL_PROPAGATED", "ALCOHOL_PROPAGATED");
    assertThat(selectionRepository.findAll())
        .allSatisfy(
            selection -> {
              assertThat(selection.getDeclarationId()).isEqualTo(declaration.getId());
              assertThat(selection.getRunId()).isNull();
              assertThat(selection.getAction()).isEqualTo("SELECT");
              assertThat(selection.getSelectionSource()).isEqualTo("ADMIN");
              assertThat(selection.getSelectedBy()).isEqualTo("42");
              assertThat(selection.getSelectedAt()).isNotNull();
            });
  }

  @Test
  @DisplayName("주류의 증류소와 지역이 0일 때 자리표시 값을 전파하지 않는다")
  void 자리표시_증류소와_지역을_전파하지_않는다() {
    MfdsDeclaration declaration = savedDeclaration("주류", "Alcohol");
    alcoholMatchTargetFacade.addAlcohol(alcoholWithReferences(1L, 0L, 0L));

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);

    assertThat(response.selectedDistilleryId()).isNull();
    assertThat(response.distilleryMatchSource()).isNull();
    assertThat(response.selectedRegionId()).isNull();
    assertThat(response.regionMatchSource()).isNull();
    assertThat(selectionRepository.findAll())
        .extracting(MfdsMatchingSelection::getTargetType)
        .containsExactly("ALCOHOL");
  }

  @Test
  @DisplayName("증류소를 명시하고 지역을 생략할 때 요청한 증류소와 주류의 지역을 저장한다")
  void 명시한_증류소를_우선하고_생략한_지역만_전파한다() {
    MfdsDeclaration declaration = savedDeclaration("주류", "Alcohol");
    alcoholMatchTargetFacade.addAlcohol(alcoholWithReferences(1L, 11L, 21L));
    alcoholMatchTargetFacade.addDistillery(new DistilleryMatchTargetItem(99L, "수동", "Manual"));

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, 99L, null), ADMIN_ID);

    assertThat(response.selectedDistilleryId()).isEqualTo(99L);
    assertThat(response.distilleryMatchSource()).isEqualTo("MANUAL");
    assertThat(response.selectedRegionId()).isEqualTo(21L);
    assertThat(response.regionMatchSource()).isEqualTo("ALCOHOL_PROPAGATED");
  }

  @Test
  @DisplayName("지역을 명시하고 증류소를 생략할 때 요청한 지역과 주류의 증류소를 저장한다")
  void 명시한_지역을_우선하고_생략한_증류소만_전파한다() {
    MfdsDeclaration declaration = savedDeclaration("주류", "Alcohol");
    alcoholMatchTargetFacade.addAlcohol(alcoholWithReferences(1L, 11L, 21L));
    alcoholMatchTargetFacade.addRegion(new RegionMatchTargetItem(99L, "수동", "Manual"));

    MfdsMatchingConfirmResponse response =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, 99L), ADMIN_ID);

    assertThat(response.selectedDistilleryId()).isEqualTo(11L);
    assertThat(response.distilleryMatchSource()).isEqualTo("ALCOHOL_PROPAGATED");
    assertThat(response.selectedRegionId()).isEqualTo(99L);
    assertThat(response.regionMatchSource()).isEqualTo("MANUAL");
  }

  @Test
  @DisplayName("확정을 해제할 때 여섯 선택 컬럼을 비우고 이전 대상별 REVOKE 이력을 저장한다")
  void 해제시_모든_선택을_비우고_감사_이력을_저장한다() {
    MfdsDeclaration declaration = savedDeclaration("주류", "Alcohol");
    alcoholMatchTargetFacade.addAlcohol(alcoholWithReferences(1L, 11L, 21L));
    matchingService.confirmMatching(
        declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);

    MfdsMatchingConfirmResponse response = matchingService.clearMatching(declaration.getId(), 43L);

    assertThat(response.selectedAlcoholId()).isNull();
    assertThat(response.alcoholMatchDecision()).isNull();
    assertThat(response.selectedDistilleryId()).isNull();
    assertThat(response.distilleryMatchSource()).isNull();
    assertThat(response.selectedRegionId()).isNull();
    assertThat(response.regionMatchSource()).isNull();
    assertThat(selectionRepository.findAll()).hasSize(6);
    assertThat(selectionRepository.findAll().subList(3, 6))
        .extracting(MfdsMatchingSelection::getTargetId)
        .containsExactly(1L, 11L, 21L);
    assertThat(selectionRepository.findAll().subList(3, 6))
        .allSatisfy(
            selection -> {
              assertThat(selection.getAction()).isEqualTo("REVOKE");
              assertThat(selection.getSelectionSource()).isEqualTo("ADMIN");
              assertThat(selection.getReasonCode()).isEqualTo("ADMIN_RELEASE");
              assertThat(selection.getSelectedBy()).isEqualTo("43");
              assertThat(selection.getRunId()).isNull();
            });
  }

  @Test
  @DisplayName("참조가 없는 주류로 재확정할 때 사라진 증류소와 지역의 해제 이력을 저장한다")
  void 재확정으로_사라진_참조는_해제로_기록한다() {
    MfdsDeclaration declaration = savedDeclaration("주류", "Alcohol");
    alcoholMatchTargetFacade.addAlcohol(alcoholWithReferences(1L, 11L, 21L));
    alcoholMatchTargetFacade.addAlcohol(alcoholWithReferences(2L, null, null));
    matchingService.confirmMatching(
        declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);

    matchingService.confirmMatching(
        declaration.getId(), new MfdsMatchingConfirmRequest(2L, null, null), ADMIN_ID);

    assertThat(selectionRepository.findAll().subList(3, 6))
        .extracting(MfdsMatchingSelection::getAction)
        .containsExactly("SELECT", "REVOKE", "REVOKE");
    assertThat(selectionRepository.findAll().subList(4, 6))
        .extracting(MfdsMatchingSelection::getReasonCode)
        .containsOnly("ADMIN_SELECTION_CLEARED");
    assertThat(declaration.getSelectedDistilleryId()).isNull();
    assertThat(declaration.getSelectedRegionId()).isNull();
  }

  @Test
  @DisplayName("이미 해제된 신고를 다시 해제할 때 존재하지 않는 대상의 이력을 만들지 않는다")
  void 빈_선택의_해제는_감사_행을_추가하지_않는다() {
    MfdsDeclaration declaration = savedDeclaration("주류", "Alcohol");

    matchingService.clearMatching(declaration.getId(), ADMIN_ID);

    assertThat(selectionRepository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("상속한 선택을 조회하고 후보를 재확정할 때 관리자 판정으로 변경한다")
  void INHERITED_조회와_후보_재확정을_지원한다() {
    MfdsDeclaration declaration = savedDeclaration("글렌피딕 12", "glenfiddich 12");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, "글렌피딕 12", "Glenfiddich 12"));
    matchingService.runMatching(declaration.getId());
    declaration.confirmMatching(
        1L,
        MfdsMatchSelectionSource.INHERITED,
        11L,
        MfdsMatchSelectionSource.INHERITED,
        21L,
        MfdsMatchSelectionSource.INHERITED);

    MfdsTestData.set(declaration, "inheritedFromDeclarationId", 100L);
    MfdsMatchingCandidatesResponse candidates = matchingService.getCandidates(declaration.getId());
    assertThat(candidates.selection().alcoholMatchDecision()).isEqualTo("INHERITED");
    assertThat(candidates.selection().distilleryMatchSource()).isEqualTo("INHERITED");
    assertThat(candidates.selection().regionMatchSource()).isEqualTo("INHERITED");

    MfdsMatchingConfirmResponse confirmed =
        matchingService.confirmMatching(
            declaration.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID);
    assertThat(confirmed.alcoholMatchDecision()).isEqualTo("CANDIDATE");
    assertThat(declaration.getInheritedFromDeclarationId()).isNull();
  }

  @Test
  @DisplayName("재계산할 때 새 실행을 연결하고 이전 후보와 근거는 보존한다")
  void 재계산_이력을_분리한다() {
    var d = savedDeclaration(null, "Glenmorangie Original 12yo");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, null, "Glenmorangie Original 12yo"));
    var first = matchingService.runMatching(d.getId());
    Long firstRun = d.getMatchingRunId();
    assertThat(
            matchingService.getCandidates(d.getId()).alcoholCandidates().getFirst().scoreDetail())
        .isEqualTo(first.alcoholCandidates().getFirst().scoreDetail());
    alcoholMatchTargetFacade.clear();
    matchingService.runMatching(d.getId());
    assertThat(d.getMatchingRunId()).isNotEqualTo(firstRun);
    assertThat(matchingService.getCandidates(d.getId()).alcoholCandidates()).isEmpty();
    assertThat(matchingRepository.findCandidates(firstRun, d.getId())).hasSize(1);
  }

  @Test
  @DisplayName("신고와 실행 버전이 다를 때 과거 후보를 반환하거나 후보 선택으로 판정하지 않는다")
  void 불일치한_실행은_참조하지_않는다() {
    var d = savedDeclaration(null, "Glenmorangie Original 12yo");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, null, "Glenmorangie Original 12yo"));
    matchingService.runMatching(d.getId());
    MfdsTestData.set(d, "matchingVersion", "older-version");
    assertThat(matchingService.getCandidates(d.getId()).alcoholCandidates()).isEmpty();
    assertThat(
            matchingService
                .confirmMatching(
                    d.getId(), new MfdsMatchingConfirmRequest(1L, null, null), ADMIN_ID)
                .alcoholMatchDecision())
        .isEqualTo("MANUAL");
  }

  @Test
  @DisplayName("다른 신고의 실행을 가리켜도 후보를 섞어서 반환하지 않는다")
  void 신고별_후보를_격리한다() {
    var first = savedDeclaration(null, "Glenmorangie Original 12yo");
    alcoholMatchTargetFacade.addAlcohol(alcohol(1L, null, "Glenmorangie Original 12yo"));
    matchingService.runMatching(first.getId());
    var second = savedDeclaration(null, "Glenmorangie Original 12yo");
    second.applyMatchingRun(
        first.getMatchingRunId(), first.getMatchingVersion(), first.getMatchedAt());
    assertThat(matchingService.getCandidates(second.getId()).alcoholCandidates()).isEmpty();
  }

  private List<MfdsMatchCandidate> storedCandidates(MfdsDeclaration declaration, String type) {
    return historyService.findCandidates(declaration).stream()
        .filter(c -> type.equals(c.getTargetType()))
        .map(c -> new MfdsMatchCandidate(c.getTargetId(), c.getRawScore()))
        .toList();
  }

  private Long candidateId(MfdsDeclaration declaration, String type, int index) {
    var candidates = storedCandidates(declaration, type);
    return candidates.size() > index ? candidates.get(index).id() : null;
  }

  private BigDecimal candidateScore(MfdsDeclaration declaration, String type, int index) {
    var candidates = storedCandidates(declaration, type);
    return candidates.size() > index ? candidates.get(index).score() : null;
  }

  private AlcoholMatchTargetItem alcoholWithReferences(Long id, Long distilleryId, Long regionId) {
    return new AlcoholMatchTargetItem(
        id,
        "주류",
        "Alcohol",
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

  private MfdsMatchingService serviceWith(MfdsMatchingScoreCalculator calculator) {
    return new MfdsMatchingService(
        declarationRepository,
        alcoholMatchTargetFacade,
        calculator,
        selectionRepository,
        historyService);
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
        "https://bottlenote.app/alcohol/" + id,
        null);
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
      return new MfdsMatchScoreDetailItem(
          total, null, null, null, null, total, null, true, List.of());
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

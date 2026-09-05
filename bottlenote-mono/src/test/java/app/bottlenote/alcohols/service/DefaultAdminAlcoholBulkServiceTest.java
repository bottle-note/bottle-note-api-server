package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRowRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkIssue;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkRowResult;
import app.bottlenote.alcohols.dto.response.AlcoholBulkReferenceItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.fixture.InMemoryDistilleryRepository;
import app.bottlenote.alcohols.fixture.InMemoryRegionRepository;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository;
import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
@DisplayName("알코올 벌크 검증 및 생성")
class DefaultAdminAlcoholBulkServiceTest {
  private CountingAlcoholRepository alcohols;
  private CountingRegionRepository regions;
  private CountingDistilleryRepository distilleries;
  private CountingTagRepository tags;
  private InMemoryAlcoholsTastingTagsRepository mappings;
  private List<Object> events;
  private DefaultAdminAlcoholBulkService service;

  @BeforeEach
  void setUp() {
    alcohols = new CountingAlcoholRepository();
    regions = new CountingRegionRepository();
    distilleries = new CountingDistilleryRepository();
    tags = new CountingTagRepository();
    mappings = new InMemoryAlcoholsTastingTagsRepository();
    events = new ArrayList<>();
    service =
        new DefaultAdminAlcoholBulkService(
            alcohols, regions, distilleries, tags, mappings, events::add);
    regions.save(Region.builder().korName("지역").engName("Region").sortOrder(1).build());
    distilleries.save(
        Distillery.builder().korName("증류소").engName("Distillery").sortOrder(1).build());
    tags.save(TastingTag.builder().korName("과일").engName("Fruit").build());
  }

  @Test
  @DisplayName("선택값이 비어 있을 때 null로 정규화하고 JSON 재입력을 저장한다")
  void 정규화한_행을_저장한다() {
    Row row = new Row();
    row.abv = " 40.00 % ";
    row.volume = "70cl";
    row.age = " ";
    row.type = "위스키";
    row.group = "싱글몰트 위스키";
    var normalized = validate(row).normalized();
    var created = service.create(new AdminAlcoholBulkRequest(List.of(normalized)));
    assertThat(created.createdRows()).isEqualTo(1);
    assertThat(created.rows().getFirst().clientRowId()).isEqualTo("row1");
    Alcohol saved = alcohols.findById(created.rows().getFirst().alcoholId()).orElseThrow();
    assertThat(saved.getKorName()).isEqualTo("새 위스키");
    assertThat(saved.getAbv()).isEqualTo("40%");
    assertThat(saved.getVolume()).isEqualTo("700ml");
    assertThat(saved.getAge()).isNull();
    assertThat(saved.getCask()).isNull();
    assertThat(saved.getDescription()).isNull();
    assertThat(saved.getImageUrl()).isNull();
    assertThat(events).isEmpty();
  }

  @Test
  @DisplayName("유효한 이미지와 중복 태그가 있을 때 태그는 한 번 저장하고 이미지 이벤트를 발행한다")
  void 태그와_이미지_이벤트를_저장한다() {
    Row row = new Row();
    row.image = "https://cdn.example.com/alcohols/new.png";
    row.tagIds = List.of(1L, 1L);
    var response = service.create(request(row));
    assertThat(response.createdRows()).isEqualTo(1);
    assertThat(mappings.count()).isEqualTo(1);
    assertThat(response.validation().rows().getFirst().warnings())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("DUPLICATE_TAG_REMOVED");
    assertThat(events)
        .containsExactly(
            ImageResourceActivatedEvent.of(
                "alcohols/new.png", response.rows().getFirst().alcoholId(), "ALCOHOL"));
  }

  @Test
  @DisplayName("한 행에 오류가 있을 때 유효한 앞 행도 저장하지 않는다")
  void 오류가_있으면_모두_저장하지_않는다() {
    Row valid = new Row();
    valid.image = "https://cdn.example.com/a.png";
    valid.tagIds = List.of(1L);
    Row invalid = new Row();
    invalid.clientId = "row2";
    invalid.type = "WHIKSY";
    var result = service.create(request(valid, invalid));
    assertThat(result.createdRows()).isZero();
    assertThat(result.rows()).isEmpty();
    assertThat(result.validation().invalidRows()).isEqualTo(1);
    assertThat(alcohols.findAll()).isEmpty();
    assertThat(mappings.count()).isZero();
    assertThat(events).isEmpty();
  }

  @Test
  @DisplayName("검증 이후 참조가 삭제됐을 때 생성에서 재검증하여 저장하지 않는다")
  void 생성에서_참조를_다시_검증한다() {
    Row row = new Row();
    row.tagIds = List.of(1L);
    var validated = validate(row);
    tags.clear();
    var result = service.create(new AdminAlcoholBulkRequest(List.of(validated.normalized())));
    assertThat(result.validation().invalidRows()).isEqualTo(1);
    assertThat(result.createdRows()).isZero();
    assertThat(alcohols.findAll()).isEmpty();
  }

  @Test
  @DisplayName("clientRowId가 중복될 때 해당 행 모두에 오류를 반환한다")
  void 클라이언트_식별자_중복을_거절한다() {
    var result = service.validate(request(new Row(), new Row()));
    assertThat(result.invalidRows()).isEqualTo(2);
    assertThat(result.rows())
        .allSatisfy(
            row -> {
              assertThat(row.normalized()).isNull();
              assertThat(row.errors())
                  .extracting(AdminAlcoholBulkIssue::code)
                  .contains("DUPLICATE_CLIENT_ROW_ID");
            });
  }

  @Test
  @DisplayName("요청과 DB에 중복 후보가 있을 때 경고와 후보 ID를 반환하고 저장을 허용한다")
  void 중복_후보는_경고한다() {
    Alcohol existing = existing(AlcoholCategoryGroup.SINGLE_MALT);
    Row first = new Row();
    first.korName = existing.getKorName();
    Row second = new Row();
    second.clientId = "row2";
    second.korName = first.korName;
    var result = service.create(request(first, second));
    assertThat(result.createdRows()).isEqualTo(2);
    assertThat(result.validation().warningRows()).isEqualTo(2);
    assertThat(result.validation().rows().getFirst().candidateAlcoholIds())
        .containsExactly(existing.getId());
    assertThat(result.validation().rows().getFirst().warnings())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("DUPLICATE_REQUEST_ROW", "DUPLICATE_DB_CANDIDATE");
  }

  @Test
  @DisplayName("카테고리 참조가 유일할 때 생략한 그룹을 도출한다")
  void 유일한_그룹을_도출한다() {
    existing(AlcoholCategoryGroup.SINGLE_MALT);
    Row row = new Row();
    row.group = null;
    assertThat(validate(row).normalized().categoryGroup()).isEqualTo("SINGLE_MALT");
  }

  @Test
  @DisplayName("위스키 카테고리 참조가 없거나 모호할 때 그룹 생략을 거절한다")
  void 위스키_그룹_추론이_모호하면_거절한다() {
    Row row = new Row();
    row.group = null;
    assertThat(validate(row).errors())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("CATEGORY_GROUP_REQUIRED");
    existing(AlcoholCategoryGroup.SINGLE_MALT);
    existing(AlcoholCategoryGroup.BLEND);
    assertThat(validate(row).errors())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("CATEGORY_GROUP_REQUIRED");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"RUM", "VODKA", "GIN", "TEQUILA", "BRANDY", "BEER", "WINE", "ETC", "기타", "Others"})
  @DisplayName("위스키 이외 타입의 그룹이 없을 때 OTHER를 기본값으로 쓴다")
  void 모든_비위스키_타입을_허용한다(String type) {
    Row row = new Row();
    row.type = type;
    row.group = null;
    assertThat(validate(row).normalized().categoryGroup()).isEqualTo("OTHER");
  }

  @Test
  @DisplayName("카테고리와 타입 및 그룹이 의미상 다를 때 경고를 반환하고 보존한다")
  void 의미상_불일치를_보존한다() {
    existing(AlcoholCategoryGroup.SINGLE_MALT);
    Row row = new Row();
    row.type = "RUM";
    row.group = "BLEND";
    var result = validate(row);
    assertThat(result.valid()).isTrue();
    assertThat(result.normalized().categoryGroup()).isEqualTo("BLEND");
    assertThat(result.warnings())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("CATEGORY_GROUP_MISMATCH", "TYPE_GROUP_MISMATCH", "TYPE_CATEGORY_MISMATCH");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "blob:https://example.com/a",
        "data:image/png;base64,AA",
        "javascript:alert(1)",
        "ftp://example.com/a",
        "//example.com/a",
        "https:///a",
        "https://a b/a",
        "https://user:pass@example.com/a",
        "https://example.com:99999/a",
        "https://example.com/a?X-Amz-Signature=test",
        "https://example.com/a?X%2DAmz%2DSignature=test"
      })
  @DisplayName("이미지 URL이 유효하지 않을 때 오류를 반환한다")
  void 잘못된_URL을_거절한다(String image) {
    Row row = new Row();
    row.image = image;
    assertThat(validate(row).errors())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("INVALID_URL");
  }

  @Test
  @DisplayName("이름과 선택 문자열이 길거나 필수값이 없을 때 필드별 오류를 반환한다")
  void 문자열_길이와_필수값을_검증한다() {
    Row row = new Row();
    row.korName = "가".repeat(256);
    row.engName = " ";
    row.age = "a".repeat(256);
    row.cask = "c".repeat(256);
    row.description = "한".repeat(21846);
    row.korCategory = null;
    row.group = "invalid";
    row.regionId = null;
    row.distilleryId = -1L;
    assertThat(validate(row).errors())
        .extracting(AdminAlcoholBulkIssue::field)
        .contains(
            "korName",
            "engName",
            "age",
            "cask",
            "description",
            "korCategory",
            "categoryGroup",
            "regionId",
            "distilleryId");
  }

  @Test
  @DisplayName("태그에 null이나 존재하지 않는 ID가 있을 때 오류를 반환한다")
  void 잘못된_태그를_거절한다() {
    Row row = new Row();
    row.tagIds = Arrays.asList(1L, null, 999L);
    assertThat(validate(row).errors())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("INVALID_REFERENCE");
    row.tagIds = Collections.nCopies(1001, 1L);
    assertThat(validate(row).errors())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("TOO_MANY_TAGS");
  }

  @Test
  @DisplayName("기존 325개 이상의 태그를 요청할 때 모두 보존한다")
  void 많은_태그를_보존한다() {
    for (int i = 2; i <= 400; i++)
      tags.save(TastingTag.builder().korName("태그" + i).engName("tag" + i).build());
    Row row = new Row();
    row.tagIds = IntStream.rangeClosed(1, 400).mapToObj(i -> (long) i).toList();
    var created = service.create(request(row));
    assertThat(created.createdRows()).isEqualTo(1);
    assertThat(mappings.count()).isEqualTo(400);
  }

  @Test
  @DisplayName("요청이 비거나 한도를 넘을 때 도메인 오류를 반환한다")
  void 요청_행_한도를_검증한다() {
    assertThatThrownBy(() -> service.validate(null)).isInstanceOf(AlcoholException.class);
    assertThatThrownBy(() -> service.validate(new AdminAlcoholBulkRequest(null)))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(() -> service.create(new AdminAlcoholBulkRequest(List.of())))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(
            () ->
                service.validate(
                    new AdminAlcoholBulkRequest(Collections.nCopies(1001, new Row().build()))))
        .isInstanceOf(AlcoholException.class);
    assertThat(
            service
                .validate(
                    new AdminAlcoholBulkRequest(Arrays.asList((AdminAlcoholBulkRowRequest) null)))
                .invalidRows())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("1000개 행이 같은 참조를 쓸 때 참조를 한번씩 일괄 조회한다")
  void 참조를_반복_조회하지_않는다() {
    var rows =
        IntStream.range(0, 1000)
            .mapToObj(
                i -> {
                  Row row = new Row();
                  row.clientId = "row" + i;
                  row.tagIds = List.of(1L);
                  return row.build();
                })
            .toList();
    var result = service.validate(new AdminAlcoholBulkRequest(rows));
    assertThat(result.validRows()).isEqualTo(1000);
    assertThat(regions.batchReads).isEqualTo(1);
    assertThat(distilleries.batchReads).isEqualTo(1);
    assertThat(tags.reads).isEqualTo(1);
    assertThat(alcohols.bulkReads).isEqualTo(1);
  }

  @Test
  @DisplayName("비위스키 기존 데이터가 있을 때 그룹 도출과 중복 후보에 반영한다")
  void 비위스키_기존_데이터도_참조한다() {
    Alcohol existing =
        alcohols.save(
            Alcohol.builder()
                .korName("새 Ｗｈｉｓｋｙ")
                .engName("New Whisky")
                .type(AlcoholType.RUM)
                .korCategory("싱글몰트")
                .engCategory("Single Malts")
                .categoryGroup(AlcoholCategoryGroup.BLEND)
                .distillery(distilleries.findById(1L).orElseThrow())
                .abv("40")
                .volume("0.7L")
                .build());
    Row row = new Row();
    row.type = "RUM";
    row.korName = "새 Whisky";
    row.group = null;
    var result = validate(row);
    assertThat(result.normalized().categoryGroup()).isEqualTo("BLEND");
    assertThat(result.candidateAlcoholIds()).containsExactly(existing.getId());
  }

  @Test
  @DisplayName("DB 후보가 100개를 넘을 때 목록을 제한하고 경고한다")
  void 후보_응답_크기를_제한한다() {
    for (int i = 0; i < 110; i++) existing(AlcoholCategoryGroup.SINGLE_MALT);
    Row row = new Row();
    row.korName = "기존 위스키";
    var result = validate(row);
    assertThat(result.candidateAlcoholIds()).hasSize(100);
    assertThat(result.warnings())
        .extracting(AdminAlcoholBulkIssue::code)
        .contains("CANDIDATES_TRUNCATED");
  }

  @Test
  @DisplayName("같은 이름이라도 용량이나 도수 및 증류소가 다를 때 중복으로 경고하지 않는다")
  void 다른_제품_규격은_중복이_아니다() {
    existing(AlcoholCategoryGroup.SINGLE_MALT);
    Row row = new Row();
    row.korName = "기존 위스키";
    row.volume = "1000ml";
    Row second = new Row();
    second.korName = row.korName;
    second.clientId = "row2";
    second.abv = "43%";
    var result = service.validate(request(row, second));
    assertThat(result.rows())
        .allSatisfy(
            value -> {
              assertThat(value.candidateAlcoholIds()).isEmpty();
              assertThat(value.warnings())
                  .extracting(AdminAlcoholBulkIssue::code)
                  .doesNotContain("DUPLICATE_REQUEST_ROW", "DUPLICATE_DB_CANDIDATE");
            });
  }

  private AdminAlcoholBulkRowResult validate(Row row) {
    return service.validate(request(row)).rows().getFirst();
  }

  private AdminAlcoholBulkRequest request(Row... rows) {
    return new AdminAlcoholBulkRequest(Arrays.stream(rows).map(Row::build).toList());
  }

  private Alcohol existing(AlcoholCategoryGroup group) {
    return alcohols.save(
        Alcohol.builder()
            .korName("기존 위스키")
            .engName("Existing whisky")
            .type(AlcoholType.WHISKY)
            .korCategory("싱글몰트")
            .engCategory("Single Malts")
            .categoryGroup(group)
            .distillery(distilleries.findById(1L).orElseThrow())
            .abv("40")
            .volume("700")
            .build());
  }

  private static final class Row {
    String clientId = "row1";
    String korName = " 새 위스키 ";
    String engName = "New Whisky";
    String abv = "40%";
    String type = "WHISKY";
    String korCategory = "싱글몰트";
    String engCategory = "Single Malts";
    String group = "SINGLE_MALT";
    Long regionId = 1L;
    Long distilleryId = 1L;
    String age;
    String cask;
    String description;
    String volume = "700ml";
    List<Long> tagIds;
    String image;

    AdminAlcoholBulkRowRequest build() {
      return new AdminAlcoholBulkRowRequest(
          clientId,
          korName,
          engName,
          abv,
          type,
          korCategory,
          engCategory,
          group,
          regionId,
          distilleryId,
          age,
          cask,
          description,
          volume,
          tagIds,
          image);
    }
  }

  private static final class CountingAlcoholRepository extends InMemoryAlcoholQueryRepository {
    int bulkReads;

    @Override
    public List<AlcoholBulkReferenceItem> findAllBulkReferenceItems() {
      bulkReads++;
      return super.findAllBulkReferenceItems();
    }
  }

  private static final class CountingRegionRepository extends InMemoryRegionRepository {
    int batchReads;

    @Override
    public List<Region> findAllByIdInOrderBySortOrderAsc(Collection<Long> ids) {
      batchReads++;
      return super.findAllByIdInOrderBySortOrderAsc(ids);
    }
  }

  private static final class CountingDistilleryRepository extends InMemoryDistilleryRepository {
    int batchReads;

    @Override
    public List<Distillery> findAllByIdInOrderBySortOrderAsc(Collection<Long> ids) {
      batchReads++;
      return super.findAllByIdInOrderBySortOrderAsc(ids);
    }
  }

  private static final class CountingTagRepository extends InMemoryTastingTagRepository {
    int reads;

    @Override
    public List<TastingTag> findAll() {
      reads++;
      return super.findAll();
    }
  }
}

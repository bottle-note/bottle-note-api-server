package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_VALUE;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.constant.ValueProvider;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeCreateResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeKeyItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeKeywordItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholAttributeRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("위스키 속성 어드민 서비스 단위 테스트")
@SuppressWarnings("NonAsciiCharacters")
class DefaultAdminAlcoholAttributeServiceTest {

  InMemoryAlcoholAttributeRepository attributeRepository;
  InMemoryAlcoholQueryRepository alcoholQueryRepository;
  DefaultAdminAlcoholAttributeService service;
  Alcohol first;
  Alcohol second;
  Alcohol third;

  @BeforeEach
  void setUp() {
    attributeRepository = new InMemoryAlcoholAttributeRepository();
    alcoholQueryRepository = new InMemoryAlcoholQueryRepository();
    service = new DefaultAdminAlcoholAttributeService(attributeRepository, alcoholQueryRepository);
    first = saveAlcohol("첫 병");
    second = saveAlcohol("둘째 병");
    third = saveAlcohol("셋째 병");
  }

  @Test
  @DisplayName("위스키를 고르면 그 병들의 속성만 반환한다")
  void 선택한_위스키의_속성을_조회할_수_있다() {
    saveAttribute(first.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");
    saveAttribute(
        second.getId(), "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "58000");
    saveAttribute(third.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "과일");

    List<AdminAlcoholAttributeItem> items =
        service.findAll(
            new AdminAlcoholAttributeSearchRequest(List.of(first.getId(), second.getId())));

    assertThat(items)
        .extracting(AdminAlcoholAttributeItem::alcoholId)
        .containsExactly(first.getId(), second.getId());
    assertThat(items.get(0).key()).isEqualTo("keyword");
    assertThat(items.get(0).value()).isEqualTo("피트");
    assertThat(items.get(0).type()).isEqualTo(AttributeType.KEYWORD);
    assertThat(items.get(0).createPrincipal()).isNull();
  }

  @Test
  @DisplayName("숫자 속성은 항목별로 묶고 그 외는 값까지 나눠 집계한다")
  void 선택한_위스키_속성을_묶어_조회할_수_있다() {
    saveAttribute(
        first.getId(), "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000");
    saveAttribute(
        second.getId(), "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "30000");
    saveAttribute(
        third.getId(), "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000");
    saveAttribute(first.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");
    saveAttribute(second.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");
    saveAttribute(third.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "과일");

    List<AdminAlcoholAttributeSummaryItem> summary =
        service.summarize(
            new AdminAlcoholAttributeSearchRequest(
                List.of(first.getId(), second.getId(), third.getId(), first.getId())));

    AdminAlcoholAttributeSummaryItem price =
        summary.stream()
            .filter(item -> item.key().equals("price_bottle"))
            .findFirst()
            .orElseThrow();
    assertThat(price.alcoholIds()).containsExactly(first.getId(), second.getId(), third.getId());
    assertThat(price.registeredCount()).isEqualTo(3);
    assertThat(price.selectedCount()).isEqualTo(3);
    assertThat(price.min()).isEqualByComparingTo(new BigDecimal("10000"));
    assertThat(price.max()).isEqualByComparingTo(new BigDecimal("30000"));
    assertThat(price.allSameValue()).isFalse();
    assertThat(price.value()).isNull();

    AdminAlcoholAttributeSummaryItem peat =
        summary.stream().filter(item -> "피트".equals(item.value())).findFirst().orElseThrow();
    assertThat(peat.alcoholIds()).containsExactly(first.getId(), second.getId());
    assertThat(peat.registeredCount()).isEqualTo(2);
    assertThat(peat.allSameValue()).isTrue();
    assertThat(peat.min()).isNull();
  }

  @Test
  @DisplayName("같은 항목과 값이 이미 있으면 그 위스키는 건너뛴다")
  void 같은_항목과_값이_있으면_추가를_건너뛸_수_있다() {
    saveAttribute(first.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");

    AdminAlcoholAttributeCreateResponse response =
        service.create(
            new AdminAlcoholAttributeCreateRequest(
                List.of(first.getId(), second.getId(), first.getId()),
                " keyword ",
                AttributeType.KEYWORD,
                AttributeDatatype.STRING,
                " 피트 ",
                " 추천 ",
                LocalDateTime.of(2026, 10, 12, 0, 0)));

    assertThat(response.added()).isEqualTo(1);
    assertThat(response.skipped()).isEqualTo(2);
    assertThat(response.createdIds()).hasSize(1);
    assertThat(attributeRepository.findAll()).hasSize(2);
    AlcoholAttribute created =
        attributeRepository.findById(response.createdIds().get(0)).orElseThrow();
    assertThat(created.getAttributeKey()).isEqualTo("keyword");
    assertThat(created.getAttributeValue()).isEqualTo("피트");
    assertThat(created.getDescription()).isEqualTo("추천");
  }

  @Test
  @DisplayName("없는 위스키가 있으면 속성을 추가하지 않는다")
  void 없는_위스키에는_속성을_추가할_수_없다() {
    assertThatThrownBy(
            () ->
                service.create(
                    new AdminAlcoholAttributeCreateRequest(
                        List.of(first.getId(), 999L),
                        "keyword",
                        AttributeType.KEYWORD,
                        AttributeDatatype.STRING,
                        "피트",
                        null,
                        null)))
        .isInstanceOf(AlcoholException.class)
        .extracting(exception -> ((AlcoholException) exception).getExceptionCode())
        .isEqualTo(ALCOHOL_NOT_FOUND);
    assertThat(attributeRepository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("값 형식이 맞지 않으면 추가를 거부한다")
  void 잘못된_값은_거부할_수_있다() {
    assertThatThrownBy(
            () ->
                service.create(
                    new AdminAlcoholAttributeCreateRequest(
                        List.of(first.getId()),
                        "price_bottle",
                        AttributeType.PRICE,
                        AttributeDatatype.INT,
                        "58000.5",
                        null,
                        null)))
        .isInstanceOf(AlcoholException.class)
        .extracting(exception -> ((AlcoholException) exception).getExceptionCode())
        .isEqualTo(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
  }

  @Test
  @DisplayName("속성 한 건의 값과 분류를 수정한다")
  void 속성을_수정할_수_있다() {
    AlcoholAttribute saved =
        saveAttribute(
            first.getId(), "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000");

    AdminResultResponse response =
        service.update(
            saved.getId(),
            new AdminAlcoholAttributeUpdateRequest(
                AttributeType.PRICE, AttributeDatatype.INT, " 20000 ", "시세", null));

    assertThat(response.code()).isEqualTo("ALCOHOL_ATTRIBUTE_UPDATED");
    assertThat(response.targetId()).isEqualTo(saved.getId());
    assertThat(attributeRepository.findById(saved.getId()).orElseThrow().getAttributeValue())
        .isEqualTo("20000");
    assertThat(attributeRepository.findById(saved.getId()).orElseThrow().getAttributeKey())
        .isEqualTo("price_bottle");
  }

  @Test
  @DisplayName("없는 속성은 수정할 수 없다")
  void 없는_속성을_수정할_수_없다() {
    assertThatThrownBy(
            () ->
                service.update(
                    404L,
                    new AdminAlcoholAttributeUpdateRequest(
                        AttributeType.NOTE, AttributeDatatype.STRING, "메모", null, null)))
        .isInstanceOf(AlcoholException.class)
        .extracting(exception -> ((AlcoholException) exception).getExceptionCode())
        .isEqualTo(ALCOHOL_ATTRIBUTE_NOT_FOUND);
  }

  @Test
  @DisplayName("속성 한 건을 삭제한다")
  void 속성을_삭제할_수_있다() {
    AlcoholAttribute saved =
        saveAttribute(
            first.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");

    AdminResultResponse response = service.delete(saved.getId());

    assertThat(response.code()).isEqualTo("ALCOHOL_ATTRIBUTE_DELETED");
    assertThat(attributeRepository.findById(saved.getId())).isEmpty();
  }

  @Test
  @DisplayName("없는 속성은 삭제할 수 없다")
  void 없는_속성을_삭제할_수_없다() {
    assertThatThrownBy(() -> service.delete(404L))
        .isInstanceOf(AlcoholException.class)
        .extracting(exception -> ((AlcoholException) exception).getExceptionCode())
        .isEqualTo(ALCOHOL_ATTRIBUTE_NOT_FOUND);
  }

  @Test
  @DisplayName("여러 속성을 지우고 지운 행을 그대로 돌려준다")
  void 여러_속성을_지우고_지운_행을_돌려줄_수_있다() {
    AlcoholAttribute kept =
        saveAttribute(
            first.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");
    AlcoholAttribute removed =
        saveAttribute(
            second.getId(), "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000");

    List<AdminAlcoholAttributeItem> deleted =
        service.deleteBatch(
            new AdminAlcoholAttributeBatchDeleteRequest(List.of(removed.getId(), 999L)));

    assertThat(deleted).extracting(AdminAlcoholAttributeItem::id).containsExactly(removed.getId());
    assertThat(deleted.get(0).value()).isEqualTo("10000");
    assertThat(attributeRepository.findById(removed.getId())).isEmpty();
    assertThat(attributeRepository.findById(kept.getId())).isPresent();
  }

  @Test
  @DisplayName("없는 ID만 지우면 빈 목록을 돌려준다")
  void 없는_속성만_지우면_빈_목록을_돌려줄_수_있다() {
    List<AdminAlcoholAttributeItem> deleted =
        service.deleteBatch(new AdminAlcoholAttributeBatchDeleteRequest(List.of()));

    assertThat(deleted).isEmpty();
  }

  @Test
  @DisplayName("전체 속성에서 항목별 건수와 위스키 수를 센다")
  void 키별_집계를_조회할_수_있다() {
    saveAttribute(first.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");
    saveAttribute(second.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "과일");
    saveAttribute(
        first.getId(), "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000");

    List<AdminAlcoholAttributeKeyItem> keys = service.findKeys();

    AdminAlcoholAttributeKeyItem keyword =
        keys.stream().filter(item -> item.key().equals("keyword")).findFirst().orElseThrow();
    assertThat(keyword.type()).isEqualTo(AttributeType.KEYWORD);
    assertThat(keyword.datatype()).isEqualTo(AttributeDatatype.STRING);
    assertThat(keyword.count()).isEqualTo(2);
    assertThat(keyword.alcoholCount()).isEqualTo(2);
    assertThat(keys).extracting(AdminAlcoholAttributeKeyItem::key).contains("price_bottle");
  }

  @Test
  @DisplayName("키워드 값은 위스키가 많은 순으로 반환한다")
  void 키워드를_많은_순으로_조회할_수_있다() {
    saveAttribute(first.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");
    saveAttribute(second.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트");
    saveAttribute(third.getId(), "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "과일");
    saveAttribute(
        first.getId(), "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000");

    List<AdminAlcoholAttributeKeywordItem> keywords = service.findKeywords();

    assertThat(keywords)
        .extracting(AdminAlcoholAttributeKeywordItem::value)
        .containsExactly("피트", "과일");
    assertThat(keywords.get(0).alcoholCount()).isEqualTo(2);
    assertThat(keywords.get(1).alcoholCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("분류와 값 형식, 값 제공자의 화면 라벨을 반환한다")
  void 화면_라벨용_코드를_조회할_수_있다() {
    AdminAlcoholAttributeEnumsResponse enums = service.findEnums();

    assertThat(enums.types()).hasSize(AttributeType.values().length);
    assertThat(enums.datatypes()).hasSize(AttributeDatatype.values().length);
    assertThat(enums.providers()).hasSize(ValueProvider.values().length);
    assertThat(enums.types().get(0).code()).isEqualTo(AttributeType.KEYWORD.name());
    assertThat(enums.types().get(0).korName()).isEqualTo(AttributeType.KEYWORD.getKorName());
    assertThat(enums.providers())
        .anyMatch(
            item ->
                item.code().equals(ValueProvider.ADMIN.name()) && item.engName().equals("admin"));
  }

  private Alcohol saveAlcohol(String name) {
    return alcoholQueryRepository.save(Alcohol.builder().korName(name).engName(name).build());
  }

  private AlcoholAttribute saveAttribute(
      Long alcoholId, String key, AttributeType type, AttributeDatatype datatype, String value) {
    return attributeRepository.save(
        AlcoholAttribute.create(alcoholId, key, type, datatype, value, null, null));
  }
}

package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] 위스키 속성 영속성")
class AlcoholAttributePersistenceIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private AlcoholAttributeRepository alcoholAttributeRepository;

  @Test
  @DisplayName("여러 위스키의 속성을 한 번에 저장하고 위스키 ID 묶음으로 읽는다")
  void 여러_위스키_속성을_저장하고_묶음으로_읽는다() {
    Alcohol first = alcoholTestFactory.persistAlcohol();
    Alcohol second = alcoholTestFactory.persistAlcohol();
    Alcohol other = alcoholTestFactory.persistAlcohol();
    LocalDateTime basedAt = LocalDateTime.of(2026, 9, 28, 0, 0);

    alcoholAttributeRepository.saveAll(
        List.of(
            AlcoholAttribute.create(
                first.getId(),
                "keyword",
                AttributeType.KEYWORD,
                AttributeDatatype.STRING,
                "입문",
                "추천 필터",
                null),
            AlcoholAttribute.create(
                first.getId(),
                "score",
                AttributeType.POPULARITY,
                AttributeDatatype.DOUBLE,
                "92.00",
                "데일리샷",
                basedAt),
            AlcoholAttribute.create(
                second.getId(),
                "keyword",
                AttributeType.KEYWORD,
                AttributeDatatype.STRING,
                "입문",
                null,
                null),
            AlcoholAttribute.create(
                other.getId(),
                "keyword",
                AttributeType.KEYWORD,
                AttributeDatatype.STRING,
                "선물",
                null,
                null)));

    List<AlcoholAttribute> rows =
        alcoholAttributeRepository.findAllByAlcoholIdIn(List.of(first.getId(), second.getId()));

    assertThat(rows).hasSize(3);
    assertThat(rows)
        .extracting(AlcoholAttribute::getAlcoholId)
        .containsExactly(first.getId(), first.getId(), second.getId());
    assertThat(rows)
        .filteredOn(row -> row.getAttributeKey().equals("score"))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.getDatatype()).isEqualTo(AttributeDatatype.DOUBLE);
              assertThat(row.getBasedAt()).isEqualTo(basedAt);
              assertThat(row.getCreateAt()).isNotNull();
            });
    assertThat(alcoholAttributeRepository.findAllByAttributeType(AttributeType.KEYWORD))
        .extracting(AlcoholAttribute::getAttributeValue)
        .containsExactlyInAnyOrder("입문", "입문", "선물");
  }

  @Test
  @DisplayName("같은 항목·값 존재 여부를 확인하고 ID 묶음으로 삭제한다")
  void 중복을_확인하고_묶음으로_삭제한다() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    AlcoholAttribute keyword =
        alcoholAttributeRepository.save(
            AlcoholAttribute.create(
                alcohol.getId(),
                "keyword",
                AttributeType.KEYWORD,
                AttributeDatatype.STRING,
                "하이볼",
                null,
                null));
    AlcoholAttribute price =
        alcoholAttributeRepository.save(
            AlcoholAttribute.create(
                alcohol.getId(),
                "price_bottle",
                AttributeType.PRICE,
                AttributeDatatype.INT,
                "58000",
                null,
                null));

    assertThat(
            alcoholAttributeRepository.existsByAlcoholIdAndAttributeKeyAndAttributeValue(
                alcohol.getId(), "keyword", "하이볼"))
        .isTrue();
    assertThat(
            alcoholAttributeRepository.existsByAlcoholIdAndAttributeKeyAndAttributeValue(
                alcohol.getId(), "keyword", "니트"))
        .isFalse();

    alcoholAttributeRepository.deleteAllByIdIn(List.of(keyword.getId(), price.getId()));

    assertThat(alcoholAttributeRepository.findAllByAlcoholIdIn(List.of(alcohol.getId()))).isEmpty();
  }
}

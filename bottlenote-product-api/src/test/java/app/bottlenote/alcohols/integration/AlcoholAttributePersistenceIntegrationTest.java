package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.*;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingRepository;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingBulkRequest;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.alcohols.service.AdminAlcoholAttributeService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] 공통 속성·매핑 영속성")
class AlcoholAttributePersistenceIntegrationTest extends IntegrationTestSupport {
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private AlcoholAttributeRepository attributes;
  @Autowired private AlcoholAttributeMappingRepository mappings;
  @Autowired private AdminAlcoholAttributeService service;

  @Test
  @DisplayName("한 속성을 여러 술에 연결할 때 공통 값과 독립된 감사 정보를 저장한다")
  void 공통_속성과_매핑을_저장한다() {
    var first = alcoholTestFactory.persistAlcohol();
    var second = alcoholTestFactory.persistAlcohol();
    var a =
        service.create(
            new AdminAlcoholAttributeCreateRequest(
                "price_gte",
                "15,000원 이상",
                AttributeType.PRICE,
                AttributeDatatype.INT,
                AttributeOperator.GTE,
                "15000",
                "원",
                "등록 기준"));
    service.mapBulk(
        new AdminAlcoholAttributeMappingBulkRequest(
            List.of(
                new AdminAlcoholAttributeMappingBulkRequest.Item(first.getId(), a.id()),
                new AdminAlcoholAttributeMappingBulkRequest.Item(second.getId(), a.id()))));
    assertThat(attributes.findById(a.id()))
        .get()
        .satisfies(
            row -> {
              assertThat(row.getOperator()).isEqualTo(AttributeOperator.GTE);
              assertThat(row.getCreateAt()).isNotNull();
            });
    assertThat(mappings.findAllByAlcoholIdIn(List.of(first.getId(), second.getId())))
        .hasSize(2)
        .allSatisfy(
            row -> {
              assertThat(row.getAttributeId()).isEqualTo(a.id());
              assertThat(row.getCreateAt()).isNotNull();
            });
    service.deleteBatch(new AdminAlcoholAttributeBatchDeleteRequest(List.of(a.id())));
    assertThat(mappings.findAllByAlcoholIdIn(List.of(first.getId(), second.getId()))).isEmpty();
  }
}

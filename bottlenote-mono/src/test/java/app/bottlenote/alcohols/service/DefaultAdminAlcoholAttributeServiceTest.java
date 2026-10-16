package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSummaryRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholAttributeMappingRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholAttributeRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("공통 속성 어드민 서비스")
class DefaultAdminAlcoholAttributeServiceTest {
  InMemoryAlcoholQueryRepository alcohols;
  InMemoryAlcoholAttributeMappingRepository mappings;
  InMemoryAlcoholAttributeRepository attributes;
  DefaultAdminAlcoholAttributeService service;
  Alcohol first;
  Alcohol second;

  @BeforeEach
  void setUp() {
    alcohols = new InMemoryAlcoholQueryRepository();
    mappings = new InMemoryAlcoholAttributeMappingRepository(alcohols);
    attributes = new InMemoryAlcoholAttributeRepository(mappings, alcohols);
    service = new DefaultAdminAlcoholAttributeService(attributes, mappings, alcohols);
    first = alcohols.save(Alcohol.builder().korName("가나다").engName("Alpha").build());
    second = alcohols.save(Alcohol.builder().korName("라마바").engName("Beta").build());
  }

  private AdminAlcoholAttributeCreateRequest request(String key, String name, String value) {
    return new AdminAlcoholAttributeCreateRequest(
        key,
        name,
        AttributeType.PRICE,
        AttributeDatatype.INT,
        AttributeOperator.EQ,
        value,
        "원",
        "설명");
  }

  private void map(Long alcoholId, Long attributeId) {
    service.mapBulk(
        new AdminAlcoholAttributeMappingBulkRequest(
            List.of(new AdminAlcoholAttributeMappingBulkRequest.Item(alcoholId, attributeId))));
  }

  @Test
  @DisplayName("같은 키를 벌크 등록할 때 ID와 매핑을 보존하고 덮어쓴다")
  void 같은_키를_갱신한다() {
    var a = service.create(request("price", "1만원", "10000"));
    map(first.getId(), a.id());
    var response =
        service.upsertBulk(
            new AdminAlcoholAttributeBulkRequest(List.of(request("price", "2만원", "20000"))));
    assertThat(response.updated()).isEqualTo(1);
    assertThat(response.created()).isZero();
    assertThat(response.ids()).containsExactly(a.id());
    assertThat(attributes.findById(a.id()).orElseThrow().getAttributeValue()).isEqualTo("20000");
    assertThat(mappings.findAll())
        .singleElement()
        .satisfies(m -> assertThat(m.getAttributeId()).isEqualTo(a.id()));
  }

  @Test
  @DisplayName("다른 키에서 같은 표시명을 사용할 때 충돌로 처리한다")
  void 이름_중복을_거부한다() {
    service.create(request("one", "Price", "1"));
    assertThatThrownBy(() -> service.create(request("two", "price", "2")))
        .isInstanceOf(AlcoholException.class);
    assertThat(attributes.findAll()).hasSize(1);
  }

  @Test
  @DisplayName("요청 내부에 같은 정규화 키가 있을 때 저장 전에 거부한다")
  void 요청_중복_키를_거부한다() {
    assertThatThrownBy(
            () ->
                service.upsertBulk(
                    new AdminAlcoholAttributeBulkRequest(
                        List.of(request("one", "하나", "1"), request(" one ", "둘", "2")))))
        .isInstanceOf(AlcoholException.class);
    assertThat(attributes.findAll()).isEmpty();
  }

  @Test
  @DisplayName("기존 연결과 요청 중복을 등록할 때 건너뛰고 수정 시각을 유지한다")
  void 중복_매핑은_변경하지_않는다() {
    var a = service.create(request("one", "하나", "1"));
    map(first.getId(), a.id());
    var m = mappings.findAll().getFirst();
    var time = LocalDateTime.of(2026, 10, 16, 0, 0);
    ReflectionTestUtils.setField(m, "lastModifyAt", time);
    var item = new AdminAlcoholAttributeMappingBulkRequest.Item(first.getId(), a.id());
    var result =
        service.mapBulk(
            new AdminAlcoholAttributeMappingBulkRequest(
                List.of(
                    item,
                    item,
                    new AdminAlcoholAttributeMappingBulkRequest.Item(second.getId(), a.id()))));
    assertThat(result.added()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(2);
    assertThat(m.getLastModifyAt()).isEqualTo(time);
    assertThat(mappings.findAll()).hasSize(2);
  }

  @Test
  @DisplayName("없는 술이나 속성을 연결할 때 아무 연결도 만들지 않는다")
  void 없는_대상을_검증한다() {
    var a = service.create(request("one", "하나", "1"));
    assertThatThrownBy(() -> map(9999L, a.id())).isInstanceOf(AlcoholException.class);
    assertThatThrownBy(() -> map(first.getId(), 9999L)).isInstanceOf(AlcoholException.class);
    assertThat(mappings.findAll()).isEmpty();
  }

  @Test
  @DisplayName("삭제된 술을 연결할 때 거부한다")
  void 삭제된_술을_거부한다() {
    var a = service.create(request("one", "하나", "1"));
    first.delete();
    assertThatThrownBy(() -> map(first.getId(), a.id())).isInstanceOf(AlcoholException.class);
  }

  @Test
  @DisplayName("사전을 조회할 때 미연결 속성을 포함하고 이름으로 검색한다")
  void 사전과_연결을_분리해_조회한다() {
    var a = service.create(request("one", "1만원", "10000"));
    service.create(request("two", "2만원", "20000"));
    map(first.getId(), a.id());
    assertThat(
            service
                .findAll(new AdminAlcoholAttributeSearchRequest(null, null, null, 0, 20))
                .getTotalElements())
        .isEqualTo(2);
    assertThat(
            service
                .findAll(
                    new AdminAlcoholAttributeSearchRequest(
                        List.of(first.getId()), null, null, 0, 20))
                .getContent())
        .singleElement()
        .satisfies(i -> assertThat(i.id()).isEqualTo(a.id()));
    assertThat(
            service
                .findAll(new AdminAlcoholAttributeSearchRequest(null, "2만원", null, 0, 20))
                .getContent())
        .singleElement()
        .satisfies(i -> assertThat(i.key()).isEqualTo("two"));
  }

  @Test
  @DisplayName("같은 속성을 여러 술에 연결할 때 속성 ID별로 집계한다")
  void 연결을_집계한다() {
    var a = service.create(request("one", "하나", "1"));
    map(first.getId(), a.id());
    map(second.getId(), a.id());
    var summaries =
        service.summarize(
            new AdminAlcoholAttributeSummaryRequest(List.of(first.getId(), second.getId())));
    assertThat(summaries)
        .singleElement()
        .satisfies(
            s -> {
              assertThat(s.registeredCount()).isEqualTo(2);
              assertThat(s.mappingIds()).hasSize(2);
              assertThat(s.attribute().id()).isEqualTo(a.id());
            });
  }

  @Test
  @DisplayName("연결을 해제할 때 속성은 남기고 속성을 삭제할 때 연결도 삭제한다")
  void 삭제_범위를_구분한다() {
    var a = service.create(request("one", "하나", "1"));
    map(first.getId(), a.id());
    service.deleteMapping(mappings.findAll().getFirst().getId());
    assertThat(attributes.findById(a.id())).isPresent();
    map(second.getId(), a.id());
    service.deleteBatch(new AdminAlcoholAttributeBatchDeleteRequest(List.of(a.id())));
    assertThat(attributes.findAll()).isEmpty();
    assertThat(mappings.findAll()).isEmpty();
  }

  @Test
  @DisplayName("속성으로 술을 조회할 때 매핑 기준으로 검색하고 페이징한다")
  void 매핑으로_술을_검색한다() {
    var a = service.create(request("one", "하나", "1"));
    map(first.getId(), a.id());
    map(second.getId(), a.id());
    var page =
        service.searchAlcohols(
            new AdminAttributedAlcoholSearchRequest(null, null, "one", null, null, 0, 1));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent())
        .singleElement()
        .satisfies(i -> assertThat(i.alcoholId()).isEqualTo(first.getId()));
    assertThat(service.findKeys())
        .singleElement()
        .satisfies(i -> assertThat(i.alcoholCount()).isEqualTo(2));
  }
}

package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeMapping;
import app.bottlenote.alcohols.domain.AlcoholCandidateRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholCandidateCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingUpdateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSummaryRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse.CandidateGroup;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholAttributeGroupRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholAttributeMappingRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholAttributeRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagGroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
  DefaultAdminAlcoholAttributeSearchService searchService;
  Alcohol first;
  Alcohol second;

  @BeforeEach
  void setUp() {
    alcohols = new InMemoryAlcoholQueryRepository();
    mappings = new InMemoryAlcoholAttributeMappingRepository(alcohols);
    attributes = new InMemoryAlcoholAttributeRepository(mappings, alcohols);
    service =
        new DefaultAdminAlcoholAttributeService(
            attributes,
            mappings,
            new InMemoryAlcoholAttributeGroupRepository(),
            new app.bottlenote.alcohols.fixture.InMemoryAlcoholScenarioRepository()
                .referenceGuard(
                    new app.bottlenote.alcohols.fixture.InMemoryTastingTagGroupRepository(),
                    new app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository()));
    searchService =
        new DefaultAdminAlcoholAttributeSearchService(
            attributes,
            mappings,
            alcohols,
            new UnusedCandidateRepository(),
            new InMemoryTastingTagGroupRepository());
    first = alcohols.save(Alcohol.builder().korName("가나다").engName("Alpha").build());
    second = alcohols.save(Alcohol.builder().korName("라마바").engName("Beta").build());
  }

  private AdminAlcoholAttributeCreateRequest request(String key, String name) {
    return new AdminAlcoholAttributeCreateRequest(
        key, name, AttributeType.PRICE, AttributeDatatype.INT, "원", "설명");
  }

  private void map(Long alcoholId, Long attributeId) {
    service.mapBulk(
        new AdminAlcoholAttributeMappingBulkRequest(
            List.of(
                new AdminAlcoholAttributeMappingBulkRequest.Item(
                    alcoholId, attributeId, "10000"))));
  }

  @Test
  @DisplayName("같은 키를 벌크 등록할 때 ID와 매핑을 보존하고 덮어쓴다")
  void 같은_키를_갱신한다() {
    var a = service.create(request("price", "1만원"));
    map(first.getId(), a.id());
    var response =
        service.upsertBulk(new AdminAlcoholAttributeBulkRequest(List.of(request("price", "2만원"))));
    assertThat(response.updated()).isEqualTo(1);
    assertThat(response.created()).isZero();
    assertThat(response.ids()).containsExactly(a.id());
    assertThat(attributes.findById(a.id()).orElseThrow().getDisplayName()).isEqualTo("2만원");
    assertThat(mappings.findAll())
        .singleElement()
        .satisfies(m -> assertThat(m.getAttributeId()).isEqualTo(a.id()));
  }

  @Test
  @DisplayName("다른 키에서 같은 표시명을 사용할 때 충돌로 처리한다")
  void 이름_중복을_거부한다() {
    service.create(request("one", "Price"));
    assertThatThrownBy(() -> service.create(request("two", "price")))
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
                        List.of(request("one", "하나"), request(" one ", "둘")))))
        .isInstanceOf(AlcoholException.class);
    assertThat(attributes.findAll()).isEmpty();
  }

  @Test
  @DisplayName("기존 연결과 요청 중복을 등록할 때 건너뛰고 수정 시각을 유지한다")
  void 중복_매핑은_변경하지_않는다() {
    var a = service.create(request("one", "하나"));
    map(first.getId(), a.id());
    var m = mappings.findAll().getFirst();
    var time = LocalDateTime.of(2026, 10, 16, 0, 0);
    ReflectionTestUtils.setField(m, "lastModifyAt", time);
    var item = new AdminAlcoholAttributeMappingBulkRequest.Item(first.getId(), a.id(), "9999");
    var result =
        service.mapBulk(
            new AdminAlcoholAttributeMappingBulkRequest(
                List.of(
                    item,
                    item,
                    new AdminAlcoholAttributeMappingBulkRequest.Item(
                        second.getId(), a.id(), "20000"))));
    assertThat(result.added()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(2);
    assertThat(m.getLastModifyAt()).isEqualTo(time);
    assertThat(m.getAttributeValue()).isEqualTo("10000");
    assertThat(mappings.findAll()).hasSize(2);
  }

  @Test
  @DisplayName("없는 술이나 속성을 연결할 때 아무 연결도 만들지 않는다")
  void 없는_대상을_검증한다() {
    var a = service.create(request("one", "하나"));
    assertThatThrownBy(() -> map(9999L, a.id())).isInstanceOf(AlcoholException.class);
    assertThatThrownBy(() -> map(first.getId(), 9999L)).isInstanceOf(AlcoholException.class);
    assertThat(mappings.findAll()).isEmpty();
  }

  @Test
  @DisplayName("삭제된 술을 연결할 때 거부한다")
  void 삭제된_술을_거부한다() {
    var a = service.create(request("one", "하나"));
    first.delete();
    assertThatThrownBy(() -> map(first.getId(), a.id())).isInstanceOf(AlcoholException.class);
  }

  @Test
  @DisplayName("사전을 조회할 때 미연결 속성을 포함하고 이름으로 검색한다")
  void 사전과_연결을_분리해_조회한다() {
    var a = service.create(request("one", "1만원"));
    service.create(request("two", "2만원"));
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
    var a = service.create(request("one", "하나"));
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
    var a = service.create(request("one", "하나"));
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
    var a = service.create(request("one", "하나"));
    map(first.getId(), a.id());
    map(second.getId(), a.id());
    var page =
        searchService.searchAlcohols(
            new AdminAttributedAlcoholSearchRequest(null, null, "one", null, null, null, 0, 1));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent())
        .singleElement()
        .satisfies(i -> assertThat(i.alcoholId()).isEqualTo(first.getId()));
    assertThat(service.findKeys())
        .singleElement()
        .satisfies(i -> assertThat(i.alcoholCount()).isEqualTo(2));
  }

  @Test
  void 술별_값을_수정하고_조회한다() {
    var a = service.create(request("price", "병 가격"));
    map(first.getId(), a.id());
    map(second.getId(), a.id());
    var mapping = mappings.findAll().getFirst();
    var result =
        service.updateMapping(
            mapping.getId(), new AdminAlcoholAttributeMappingUpdateRequest("65000"));
    assertThat(result.value()).isEqualTo("65000");
    assertThat(mappings.findAll().get(1).getAttributeValue()).isEqualTo("10000");
    var page =
        searchService.searchAlcohols(
            new AdminAttributedAlcoholSearchRequest(
                null, null, "price", "60000", AttributeOperator.GTE, null, 0, 20));
    assertThat(page.getContent())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.alcoholId()).isEqualTo(first.getId());
              assertThat(row.attributes().getFirst().value()).isEqualTo("65000");
              assertThat(row.attributes().getFirst().mappingId()).isEqualTo(mapping.getId());
            });
    assertThat(
            service
                .summarize(
                    new AdminAlcoholAttributeSummaryRequest(List.of(first.getId(), second.getId())))
                .getFirst()
                .mappings())
        .extracting(m -> m.value())
        .containsExactlyInAnyOrder("65000", "10000");
  }

  @Test
  void 잘못된_벌크_값은_저장_전에_거부한다() {
    var a = service.create(request("price", "병 가격"));
    assertThatThrownBy(
            () ->
                service.mapBulk(
                    new AdminAlcoholAttributeMappingBulkRequest(
                        List.of(
                            new AdminAlcoholAttributeMappingBulkRequest.Item(
                                first.getId(), a.id(), "10000"),
                            new AdminAlcoholAttributeMappingBulkRequest.Item(
                                second.getId(), a.id(), "wrong")))))
        .isInstanceOf(AlcoholException.class);
    assertThat(mappings.findAll()).isEmpty();
  }

  @Test
  void 연결된_자료형_변경을_거부한다() {
    var a = service.create(request("price", "병 가격"));
    map(first.getId(), a.id());
    var updated =
        new AdminAlcoholAttributeUpdateRequest(
            "병 가격", AttributeType.PRICE, AttributeDatatype.STRING, "원", null);
    assertThatThrownBy(() -> service.update(a.id(), updated)).isInstanceOf(AlcoholException.class);
    assertThatThrownBy(
            () ->
                service.create(
                    new AdminAlcoholAttributeCreateRequest(
                        "price", "병 가격", AttributeType.PRICE, AttributeDatatype.STRING, "원", null)))
        .isInstanceOf(AlcoholException.class);
    assertThat(attributes.findById(a.id()).orElseThrow().getDatatype())
        .isEqualTo(AttributeDatatype.INT);
    service.deleteMapping(mappings.findAll().getFirst().getId());
    service.update(a.id(), updated);
    assertThat(attributes.findById(a.id()).orElseThrow().getDatatype())
        .isEqualTo(AttributeDatatype.STRING);
  }

  @Test
  void 잘못된_수정값과_없는_연결을_거부한다() {
    var a = service.create(request("price", "병 가격"));
    map(first.getId(), a.id());
    var mapping = mappings.findAll().getFirst();
    assertThatThrownBy(
            () ->
                service.updateMapping(
                    mapping.getId(),
                    new app.bottlenote.alcohols.dto.request
                        .AdminAlcoholAttributeMappingUpdateRequest("bad")))
        .isInstanceOf(AlcoholException.class);
    assertThat(mapping.getAttributeValue()).isEqualTo("10000");
    assertThatThrownBy(
            () ->
                service.updateMapping(
                    999L,
                    new app.bottlenote.alcohols.dto.request
                        .AdminAlcoholAttributeMappingUpdateRequest("1")))
        .isInstanceOf(AlcoholException.class);
    first.delete();
    assertThatThrownBy(
            () ->
                service.updateMapping(
                    mapping.getId(),
                    new app.bottlenote.alcohols.dto.request
                        .AdminAlcoholAttributeMappingUpdateRequest("1")))
        .isInstanceOf(AlcoholException.class);
  }

  private AdminAlcoholAttributeCreateRequest stringRequest() {
    return new AdminAlcoholAttributeCreateRequest(
        "taste", "맛 평가", AttributeType.KEYWORD, AttributeDatatype.STRING, null, null);
  }

  private void mapString(Long attributeId, String value) {
    service.mapBulk(
        new AdminAlcoholAttributeMappingBulkRequest(
            List.of(
                new AdminAlcoholAttributeMappingBulkRequest.Item(
                    first.getId(), attributeId, value))));
  }

  @Test
  @DisplayName("문자열 규칙을 생성하고 변경할 때 응답에 반영하고 기존 매핑과 충돌하면 거부한다")
  void 문자열_규칙_변경과_충돌을_검증한다() {
    var request = stringRequest();
    request.setAllowedValues(List.of("맛있음", "적당함", "맛없음"));
    request.setPattern(".*(음|함)");
    var attribute = service.create(request);
    assertThat(attribute.allowedValues()).containsExactly("맛있음", "적당함", "맛없음");
    assertThat(attribute.pattern()).isEqualTo(".*(음|함)");
    mapString(attribute.id(), "맛있음");
    var update =
        new AdminAlcoholAttributeUpdateRequest(
            "맛 평가", AttributeType.KEYWORD, AttributeDatatype.STRING, null, null);
    update.setAllowedValues(List.of("적당함"));
    assertThatThrownBy(() -> service.update(attribute.id(), update))
        .isInstanceOfSatisfying(
            AlcoholException.class,
            e ->
                assertThat(e.getExceptionCode())
                    .isEqualTo(
                        app.bottlenote.alcohols.exception.AlcoholExceptionCode
                            .ALCOHOL_ATTRIBUTE_RULE_IN_USE));
    assertThat(attributes.findById(attribute.id()).orElseThrow().getAllowedValues())
        .containsExactly("맛있음", "적당함", "맛없음");
    update.setAllowedValues(List.of("맛있음"));
    service.update(attribute.id(), update);
    assertThat(attributes.findById(attribute.id()).orElseThrow().getAllowedValues())
        .containsExactly("맛있음");
  }

  @Test
  @DisplayName("단건·벌크 갱신에서 규칙을 생략하면 유지하고 null을 전달하면 해제한다")
  void 생략과_null_규칙을_구분한다() {
    var request = stringRequest();
    request.setAllowedValues(List.of("맛있음"));
    request.setPattern(".*음");
    var attribute = service.create(request);
    var update =
        new AdminAlcoholAttributeUpdateRequest(
            "맛 평가", AttributeType.KEYWORD, AttributeDatatype.STRING, null, null);
    service.update(attribute.id(), update);
    var preserved = service.create(stringRequest());
    assertThat(preserved.allowedValues()).containsExactly("맛있음");
    assertThat(preserved.pattern()).isEqualTo(".*음");
    var clear = stringRequest();
    clear.setAllowedValues(null);
    service.create(clear);
    assertThat(attributes.findById(attribute.id()).orElseThrow().getAllowedValues()).isNull();
    assertThat(attributes.findById(attribute.id()).orElseThrow().getPattern()).isEqualTo(".*음");
    update.setPattern(null);
    service.update(attribute.id(), update);
    mapString(attribute.id(), "기타");
  }

  @Test
  @DisplayName("정규식만 강화할 때 기존 매핑과 충돌하면 단건·벌크 모두 거부한다")
  void 기존_값과_충돌하는_정규식을_거부한다() {
    var attribute = service.create(stringRequest());
    mapString(attribute.id(), "맛있음");
    var update =
        new AdminAlcoholAttributeUpdateRequest(
            "맛 평가", AttributeType.KEYWORD, AttributeDatatype.STRING, null, null);
    update.setPattern("[A-Z]+");
    assertThatThrownBy(() -> service.update(attribute.id(), update))
        .isInstanceOf(AlcoholException.class);
    var request = stringRequest();
    request.setPattern("[A-Z]+");
    assertThatThrownBy(() -> service.create(request)).isInstanceOf(AlcoholException.class);
    assertThat(attributes.findById(attribute.id()).orElseThrow().getPattern()).isNull();
  }

  @Test
  @DisplayName("선택값 밖의 매핑을 생성하거나 수정할 때 기존 데이터를 유지한다")
  void 매핑_입력_규칙을_검증한다() {
    var request = stringRequest();
    request.setAllowedValues(List.of("맛있음", "적당함"));
    var attribute = service.create(request);
    assertThatThrownBy(() -> mapString(attribute.id(), "맛없음")).isInstanceOf(AlcoholException.class);
    assertThat(mappings.findAll()).isEmpty();
    mapString(attribute.id(), "맛있음");
    var mapping = mappings.findAll().getFirst();
    assertThatThrownBy(
            () ->
                service.updateMapping(
                    mapping.getId(), new AdminAlcoholAttributeMappingUpdateRequest("기타")))
        .isInstanceOf(AlcoholException.class);
    assertThat(mapping.getAttributeValue()).isEqualTo("맛있음");
    assertThat(
            service
                .updateMapping(
                    mapping.getId(), new AdminAlcoholAttributeMappingUpdateRequest("적당함"))
                .value())
        .isEqualTo("적당함");
  }

  @Test
  @DisplayName("복수 값 속성은 서로 다른 값을 연결하고 동일 값은 건너뛴다")
  void multipleValuesAndSummary() {
    var request = request("multiple", "복수 값");
    request.setAllowMultipleValues(true);
    var a = service.create(request);
    assertThat(a.allowMultipleValues()).isTrue();
    var result =
        service.mapBulk(
            new AdminAlcoholAttributeMappingBulkRequest(
                List.of(
                    new AdminAlcoholAttributeMappingBulkRequest.Item(first.getId(), a.id(), "100"),
                    new AdminAlcoholAttributeMappingBulkRequest.Item(first.getId(), a.id(), "200"),
                    new AdminAlcoholAttributeMappingBulkRequest.Item(
                        first.getId(), a.id(), "0100"))));
    assertThat(result.added()).isEqualTo(2);
    assertThat(result.skipped()).isEqualTo(1);
    var summary =
        service
            .summarize(new AdminAlcoholAttributeSummaryRequest(List.of(first.getId())))
            .getFirst();
    assertThat(summary.registeredCount()).isEqualTo(1);
    assertThat(summary.mappings()).hasSize(2);
  }

  @Test
  @DisplayName("복수 값이 남아 있으면 단일 값 전환과 충돌하는 값 수정을 거부한다")
  void rejectsMultiplicityConflict() {
    var request = request("multiple", "복수 값");
    request.setAllowMultipleValues(true);
    var a = service.create(request);
    var linked =
        service.mapBulk(
            new AdminAlcoholAttributeMappingBulkRequest(
                List.of(
                    new AdminAlcoholAttributeMappingBulkRequest.Item(first.getId(), a.id(), "100"),
                    new AdminAlcoholAttributeMappingBulkRequest.Item(
                        first.getId(), a.id(), "200"))));
    var update =
        new AdminAlcoholAttributeUpdateRequest(
            a.displayName(), a.type(), a.datatype(), a.unit(), null);
    update.setAllowMultipleValues(false);
    assertThatThrownBy(() -> service.update(a.id(), update)).isInstanceOf(AlcoholException.class);
    assertThatThrownBy(
            () ->
                service.updateMapping(
                    linked.createdIds().getFirst(),
                    new AdminAlcoholAttributeMappingUpdateRequest("200")))
        .isInstanceOf(AlcoholException.class);
    assertThat(attributes.findById(a.id()).orElseThrow().isAllowMultipleValues()).isTrue();
    service.deleteMapping(linked.createdIds().getLast());
    service.update(a.id(), update);
    assertThat(attributes.findById(a.id()).orElseThrow().isAllowMultipleValues()).isFalse();
  }

  @Test
  @DisplayName("허용 설정 생략은 생성 시 false이며 수정과 벌크 갱신 시 기존 설정을 유지한다")
  void preservesOmittedMultiplicity() {
    var request = request("multiple", "복수 값");
    assertThat(service.create(request).allowMultipleValues()).isFalse();
    request.setAllowMultipleValues(true);
    var a = service.create(request);
    service.update(
        a.id(),
        new AdminAlcoholAttributeUpdateRequest(
            a.displayName(), a.type(), a.datatype(), a.unit(), null));
    assertThat(attributes.findById(a.id()).orElseThrow().isAllowMultipleValues()).isTrue();
    assertThat(service.create(request("multiple", "복수 값")).allowMultipleValues()).isTrue();
  }

  /** 기존 속성 검색은 후보 전용 저장소를 호출하지 않아야 한다. */
  private static final class UnusedCandidateRepository implements AlcoholCandidateRepository {
    @Override
    public Optional<CandidateGroup> findGroup(Long id) {
      throw unexpectedCall();
    }

    @Override
    public List<AlcoholAttribute> findAttributes(Long groupId) {
      throw unexpectedCall();
    }

    @Override
    public long count(AlcoholCandidateCriteria query) {
      throw unexpectedCall();
    }

    @Override
    public List<Alcohol> findPage(AlcoholCandidateCriteria query) {
      throw unexpectedCall();
    }

    @Override
    public List<AlcoholAttributeMapping> findMappings(Long groupId, List<Long> alcoholIds) {
      throw unexpectedCall();
    }

    @Override
    public List<TagRow> findTags(List<Long> alcoholIds) {
      throw unexpectedCall();
    }

    @Override
    public List<FacetRow> findFacetRows(AlcoholCandidateCriteria query) {
      throw unexpectedCall();
    }

    @Override
    public List<String> findAttributeOptions(Long groupId, Long attributeId, int limit) {
      throw unexpectedCall();
    }

    private AssertionError unexpectedCall() {
      return new AssertionError("기존 속성 검색에서 후보 저장소를 호출했습니다");
    }
  }
}

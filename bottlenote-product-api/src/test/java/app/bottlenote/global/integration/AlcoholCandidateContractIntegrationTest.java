package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("integration")
@DisplayName("프로덕트 후보 API가 공통 필터와 커서 계약을 제공한다")
class AlcoholCandidateContractIntegrationTest extends OpenApiSpecTestSupport {
  @Autowired private JdbcTemplate jdbc;

  @Test
  void 공개_조회는_그룹의_후보만_반환하고_커서를_제공한다() throws Exception {
    jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(700001,'product scenario')");
    jdbc.update(
        "INSERT INTO alcohol_attributes(id,group_id,attribute_key,display_name,attribute_type,datatype) VALUES(700001,700001,'product_scenario','경험','KEYWORD','STRING')");
    jdbc.update(
        "INSERT INTO alcohols(id,kor_name,eng_name,type,kor_category,eng_category,category_group,abv) VALUES(800001,'첫 술','First','WHISKY','위스키','Whisky','SINGLE_MALT','40%'),(800002,'둘째 술','Second','WHISKY','위스키','Whisky','BOURBON','50%')");
    jdbc.update(
        "INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(800001,700001,'예'),(800002,700001,'예')");
    var first =
        mockMvcTester
            .post()
            .uri("/api/v1/attribute/groups/700001/candidates")
            .contentType(APPLICATION_JSON)
            .content("{\"size\":1}")
            .exchange();
    assertThat(first).hasStatusOk();
    var json = mapper.readTree(first.getResponse().getContentAsString());
    assertThat(json.at("/data/totalCount").asLong()).isEqualTo(2);
    assertThat(json.at("/data/items/0/abv").asDouble()).isEqualTo(40);
    String cursor = json.at("/meta/pagination/nextCursor").asText();
    var next =
        mockMvcTester
            .post()
            .uri("/api/v1/attribute/groups/700001/candidates")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(java.util.Map.of("size", 1, "cursor", cursor)))
            .exchange();
    assertThat(next).hasStatusOk();
    assertThat(
            mapper
                .readTree(next.getResponse().getContentAsString())
                .at("/data/items/0/alcoholId")
                .asLong())
        .isEqualTo(800002);
  }

  @Test
  void 실제_OpenAPI에_후보_필터와_응답이_문서화된다() {
    var spec = fetchSpec();
    var operation =
        spec.path("paths").path("/api/v1/attribute/groups/{groupId}/candidates").path("post");
    assertThat(operation.isMissingNode()).isFalse();
    assertThat(
            spec.at("/components/schemas/AlcoholCandidateRequest/properties/cursor")
                .isMissingNode())
        .isFalse();
    assertThat(
            spec.at("/components/schemas/AlcoholCandidateResponse/properties/totalCount")
                .isMissingNode())
        .isFalse();
    assertThat(
            spec.at("/components/schemas/AlcoholCandidateResponse/properties/facets")
                .isMissingNode())
        .isFalse();
  }
}

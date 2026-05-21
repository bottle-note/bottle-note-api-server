package app.bottlenote.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
class GraphQLCurationSchemaTest {

  @Test
  @DisplayName("큐레이션 GraphQL SDL은 Alcohol 통계 조회 필드만 노출한다")
  void schema_whenParsed_exposesAlcoholStatsOnly() throws IOException {
    ClassPathResource resource = new ClassPathResource("graphql/schema.graphqls");
    String schema = resource.getContentAsString(StandardCharsets.UTF_8);

    TypeDefinitionRegistry registry = new SchemaParser().parse(schema);

    assertThat(registry.getType("Query")).isPresent();
    assertThat(registry.getType("Alcohol")).isPresent();
    assertThat(schema).contains("alcohols(ids: [ID!]!): [Alcohol!]!");
    assertThat(schema)
        .contains(
            "alcoholId",
            "korName",
            "engName",
            "imageUrl",
            "regionName",
            "korCategory",
            "cask",
            "abv",
            "volume",
            "rating",
            "totalRatingsCount",
            "reviewCount",
            "totalPickCount");
    assertThat(schema).doesNotContain("picks(", "ratings(", "reviews(");
  }
}

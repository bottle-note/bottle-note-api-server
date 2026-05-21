package app.bottlenote.curation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
class CurationOpenApiSpecResourceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  @DisplayName("큐레이션 OpenAPI 스펙 3종은 리소스에 포함되고 GraphQL 보강 메타를 가진다")
  void curationOpenApiSpecs_whenLoaded_containCurationMetadata() throws IOException {
    List<SpecResource> specs =
        List.of(
            new SpecResource(
                "openapi/curation/recommended_whisky.json", "RECOMMENDED_WHISKY", "array"),
            new SpecResource("openapi/curation/whisky_pairing.json", "WHISKY_PAIRING", "array"),
            new SpecResource(
                "openapi/curation/whisky_tasting_event.json", "WHISKY_TASTING_EVENT", "object"));

    for (SpecResource spec : specs) {
      ClassPathResource resource = new ClassPathResource(spec.path());

      assertThat(resource.exists()).isTrue();

      JsonNode root = OBJECT_MAPPER.readTree(resource.getInputStream());

      assertThat(root.path("openapi").asText()).isEqualTo("3.0.3");
      assertThat(root.path("paths").isObject()).isTrue();
      assertThat(root.path("x-curation").path("code").asText()).isEqualTo(spec.code());
      assertThat(root.path("x-curation").path("hydratorKey").asText()).isEqualTo("alcohol");
      assertThat(root.path("x-curation").path("container").asText()).isEqualTo(spec.container());
      assertThat(root.path("components").path("schemas").isObject()).isTrue();
      assertThat(root.toString())
          .contains(
              "\"source\"",
              "\"BOTTLE_NOTE\"",
              "\"MANUAL\"",
              "\"alcoholId\"",
              "\"stats\"",
              "\"x-graphql\"",
              "\"totalRatingsCount\"",
              "\"reviewCount\"",
              "\"totalPickCount\"");
    }
  }

  private record SpecResource(String path, String code, String container) {}
}

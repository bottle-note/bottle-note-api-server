package app.bottlenote.mfds.controller.docs;

import app.bottlenote.mfds.dto.response.MfdsNameFieldDescriptions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/** 공유 DTO에는 문서 어노테이션이 없으므로 공개 수입 주류 응답 스키마의 이름 필드 설명을 여기서 붙인다. */
@Component
public class MfdsNameFieldSchemaCustomizer implements OpenApiCustomizer {

  private static final List<String> TARGET_SCHEMAS =
      List.of("MfdsPublicAlcoholListItem", "MfdsPublicAlcoholDetailResponse");

  @Override
  @SuppressWarnings("rawtypes")
  public void customise(OpenAPI openApi) {
    if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
      return;
    }
    Map<String, Schema> schemas = openApi.getComponents().getSchemas();
    for (String name : TARGET_SCHEMAS) {
      Schema schema = schemas.get(name);
      if (schema == null || schema.getProperties() == null) {
        continue;
      }
      Map<String, Schema> properties = schema.getProperties();
      MfdsNameFieldDescriptions.BY_PROPERTY.forEach(
          (property, description) -> {
            Schema field = properties.get(property);
            if (field != null) {
              field.setDescription(description);
            }
          });
    }
  }
}

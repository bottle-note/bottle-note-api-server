package app.bottlenote.user.fake;

import app.bottlenote.global.service.converter.JsonArrayConverter;
import app.bottlenote.user.constant.SocialType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class FakeJsonArrayConverter extends JsonArrayConverter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<SocialType> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (Exception e) {
      return "[]";
    }
  }

  @Override
  public List<SocialType> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(dbData, List.class);
    } catch (Exception e) {
      return List.of();
    }
  }
}

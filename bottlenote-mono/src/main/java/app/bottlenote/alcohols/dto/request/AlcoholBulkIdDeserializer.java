package app.bottlenote.alcohols.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class AlcoholBulkIdDeserializer extends JsonDeserializer<Long> {
  @Override
  public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
      return parser.getLongValue();
    }
    if (parser.hasToken(JsonToken.VALUE_STRING)) {
      String value = parser.getText().trim();
      try {
        return Long.valueOf(value);
      } catch (NumberFormatException exception) {
        return (Long) context.handleWeirdStringValue(Long.class, value, "ID는 Long 범위의 정수여야 합니다.");
      }
    }
    return (Long) context.handleUnexpectedToken(Long.class, parser);
  }
}

package app.bottlenote.alcohols.domain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.math.BigDecimal;

public final class ScenarioJson {
  private ScenarioJson() {}

  public static class Decimal extends JsonDeserializer<BigDecimal> {
    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      if (!parser.currentToken().isNumeric())
        throw JsonMappingException.from(parser, "JSON 숫자가 필요합니다.");
      return parser.getDecimalValue();
    }
  }

  public static class Bool extends JsonDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      if (parser.currentToken() != JsonToken.VALUE_TRUE
          && parser.currentToken() != JsonToken.VALUE_FALSE)
        throw JsonMappingException.from(parser, "JSON boolean이 필요합니다.");
      return parser.getBooleanValue();
    }
  }
}

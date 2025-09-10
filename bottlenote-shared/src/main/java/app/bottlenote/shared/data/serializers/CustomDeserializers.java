package app.bottlenote.shared.data.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomDeserializers {
	public static class TagListDeserializer extends JsonDeserializer<String> {
		@Override
		public String deserialize(JsonParser p, DeserializationContext text) throws IOException {
			List<String> tags = List.of(p.getText().split(","));
			return String.join(",", tags);
		}
	}

	public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
		private static final DateTimeFormatter formatter =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		@Override
		public LocalDateTime deserialize(JsonParser p, DeserializationContext text) throws IOException {
			String date = p.getText();
			return LocalDateTime.parse(date, formatter);
		}
	}
}
package app.bottlenote.global.data.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomDeserializers {
	public static class TastingTagListDeserializer extends JsonDeserializer<String> {
		@Override
		public String deserialize(JsonParser p, DeserializationContext text) throws IOException {
			List<String> tags = p.readValueAs(List.class);
			return tags.stream().collect(Collectors.joining(","));
		}
	}

	public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
		private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		@Override
		public LocalDateTime deserialize(JsonParser p, DeserializationContext text) throws IOException {
			String date = p.getText();
			return LocalDateTime.parse(date, formatter);
		}
	}
}

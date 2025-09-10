package app.bottlenote.shared.data.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomSerializers {

	public static class TagListSerializer extends JsonSerializer<String> {
		@Override
		public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {
			List<String> tags = Arrays.stream(value.split(",")).map(String::trim).toList();
			gen.writeObject(tags);
		}
	}

	public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
		private static final DateTimeFormatter formatter =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		@Override
		public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {
			String string = value.atZone(ZoneId.systemDefault()).format(formatter);
			gen.writeString(string);
		}
	}
}
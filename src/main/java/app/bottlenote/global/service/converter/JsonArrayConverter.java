package app.bottlenote.global.service.converter;

import static app.bottlenote.user.exception.UserExceptionCode.JSON_PARSING_EXCEPTION;

import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.exception.UserException;
import com.amazonaws.util.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Converter(autoApply = true)
@Component
public class JsonArrayConverter implements AttributeConverter<List<SocialType>, String> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<SocialType> list) {
		if (CollectionUtils.isNullOrEmpty(list)) {
			return "[]";
		}
		try {
			return objectMapper.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			throw new UserException(JSON_PARSING_EXCEPTION);
		}
	}

	@Override
	public List<SocialType> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(dbData, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			log.error("Failed to parse JSON data: {}", dbData, e);
			throw new UserException(JSON_PARSING_EXCEPTION);
		}
	}
}

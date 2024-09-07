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

@Slf4j
@Converter
public class JsonArrayConverter implements AttributeConverter<List<SocialType>, String> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<SocialType> list) {
		if (CollectionUtils.isNullOrEmpty(list)) {
			return "[" + list.toString() + "]";
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
			// dbData가 JSON 배열 형식인지 확인
			if (dbData.trim().startsWith("[") && dbData.trim().endsWith("]")) {
				// JSON 배열 형식이면 그대로 처리
				return objectMapper.readValue(dbData, new TypeReference<List<SocialType>>() {
				});
			} else {
				// 단순 문자열일 경우 JSON 배열로 감싸서 변환
				String jsonArray = "[" + objectMapper.writeValueAsString(dbData) + "]";
				return objectMapper.readValue(jsonArray, new TypeReference<List<SocialType>>() {
				});
			}
		} catch (JsonProcessingException e) {
			log.error("Failed to parse JSON data: {}", dbData, e);
			throw new UserException(JSON_PARSING_EXCEPTION);
		}
	}
}
package app.bottlenote.global.service.converter;

import static app.bottlenote.user.exception.UserExceptionCode.JSON_PARSING_EXCEPTION;

import app.bottlenote.user.constant.AdminRole;
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
public class AdminRoleConverter implements AttributeConverter<List<AdminRole>, String> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<AdminRole> roles) {
    if (CollectionUtils.isNullOrEmpty(roles)) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(roles);
    } catch (JsonProcessingException e) {
      throw new UserException(JSON_PARSING_EXCEPTION);
    }
  }

  @Override
  public List<AdminRole> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(dbData, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      log.error("Failed to parse AdminRole JSON data: {}", dbData, e);
      throw new UserException(JSON_PARSING_EXCEPTION);
    }
  }
}

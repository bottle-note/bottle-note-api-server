package app.bottlenote.user.constant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FollowStatusConverter implements AttributeConverter<FollowStatus, String> {
  @Override
  public String convertToDatabaseColumn(FollowStatus attribute) {
    return attribute != null ? attribute.name() : null;
  }

  @Override
  public FollowStatus convertToEntityAttribute(String dbData) {
    return dbData != null ? FollowStatus.parsing(dbData) : null;
  }
}

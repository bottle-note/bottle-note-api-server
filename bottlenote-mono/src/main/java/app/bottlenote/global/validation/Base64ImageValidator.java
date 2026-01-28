package app.bottlenote.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Base64ImageValidator implements ConstraintValidator<Base64Image, String> {

  private static final Pattern DATA_URI_PATTERN =
      Pattern.compile("^data:([a-zA-Z0-9]+/[a-zA-Z0-9+.-]+)?;base64,(.*)$");

  private Set<String> allowedTypes;
  private long maxSize;

  @Override
  public void initialize(Base64Image annotation) {
    this.allowedTypes = Set.copyOf(Arrays.asList(annotation.allowedTypes()));
    this.maxSize = annotation.maxSize();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true; // null/blank는 허용 (@NotBlank로 별도 검증)
    }

    String base64Data;
    String declaredMimeType = null;

    Matcher matcher = DATA_URI_PATTERN.matcher(value);
    if (matcher.matches()) {
      declaredMimeType = matcher.group(1);
      base64Data = matcher.group(2);
    } else {
      base64Data = value;
    }

    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(base64Data);
    } catch (IllegalArgumentException e) {
      setMessage(context, "유효한 Base64 인코딩이 아닙니다.");
      return false;
    }

    if (decoded.length > maxSize) {
      setMessage(context, "이미지 크기가 최대 허용 크기를 초과합니다. (최대: " + (maxSize / 1024 / 1024) + "MB)");
      return false;
    }

    String detectedMimeType = detectMimeType(decoded);
    if (detectedMimeType == null) {
      setMessage(context, "이미지 형식을 감지할 수 없습니다.");
      return false;
    }

    if (!allowedTypes.contains(detectedMimeType)) {
      setMessage(context, "허용되지 않는 이미지 형식입니다. (허용: " + String.join(", ", allowedTypes) + ")");
      return false;
    }

    if (declaredMimeType != null && !declaredMimeType.equals(detectedMimeType)) {
      setMessage(context, "선언된 MIME 타입과 실제 이미지 형식이 일치하지 않습니다.");
      return false;
    }

    return true;
  }

  private String detectMimeType(byte[] data) {
    if (data.length < 8) {
      return null;
    }

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
      return "image/png";
    }

    // JPEG: FF D8 FF
    if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
      return "image/jpeg";
    }

    // GIF: 47 49 46 38
    if (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x38) {
      return "image/gif";
    }

    // WEBP: 52 49 46 46 ... 57 45 42 50
    if (data.length >= 12
        && data[0] == 0x52
        && data[1] == 0x49
        && data[2] == 0x46
        && data[3] == 0x46
        && data[8] == 0x57
        && data[9] == 0x45
        && data[10] == 0x42
        && data[11] == 0x50) {
      return "image/webp";
    }

    // SVG: <?xml 또는 <svg
    String prefix = new String(data, 0, Math.min(data.length, 100));
    if (prefix.contains("<svg") || prefix.contains("<?xml")) {
      return "image/svg+xml";
    }

    return null;
  }

  private void setMessage(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}

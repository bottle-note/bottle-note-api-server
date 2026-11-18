package app.bottlenote.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = Base64ImageValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Base64Image {

  String message() default "유효한 Base64 이미지 형식이 아닙니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** 허용할 MIME 타입 목록 (기본: png, jpg, jpeg, gif, webp, svg) */
  String[] allowedTypes() default {
    "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml"
  };

  /** 최대 허용 크기 (bytes, 기본: 5MB) */
  long maxSize() default 5 * 1024 * 1024;
}

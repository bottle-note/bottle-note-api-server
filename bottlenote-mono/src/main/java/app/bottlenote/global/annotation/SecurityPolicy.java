package app.bottlenote.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SecurityPolicy {

  AuthType auth() default AuthType.REQUIRED_AUTH;

  AccessType access() default AccessType.ALL;

  String key() default "userId";

  enum AuthType {
    PUBLIC,
    OPTIONAL_AUTH,
    REQUIRED_AUTH
  }

  enum AccessType {
    ALL,
    OWNER,
    ADMIN
  }
}

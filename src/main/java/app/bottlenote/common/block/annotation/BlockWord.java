package app.bottlenote.common.block.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 차단된 사용자의 컨텐츠를 대체 메시지로 변경하는 어노테이션
 * JSON 직렬화 시점에서 동작합니다.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = app.bottlenote.common.block.serializer.BlockWordSerializer.class)
public @interface BlockWord {

    /**
     * 차단된 사용자 컨텐츠 대체 메시지
     * 기본값: "차단된 사용자의 글입니다"
     */
    String value() default "차단된 사용자의 글입니다";

    /**
     * 차단 판단 기준이 되는 사용자 ID 경로
     * 기본값: "userId"
     * 중첩 객체 예시: "userInfo.userId"
     */
    String userIdPath() default "userId";
}

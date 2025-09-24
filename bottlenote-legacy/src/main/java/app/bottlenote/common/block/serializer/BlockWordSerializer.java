package app.bottlenote.common.block.serializer;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.shared.annotation.BlockWord;
import app.bottlenote.support.block.service.BlockService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import java.io.IOException;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @BlockWord 어노테이션이 적용된 필드의 Jackson 커스텀 시리얼라이저 차단된 사용자의 컨텐츠를 대체 메시지로 변경합니다.
 */
@Slf4j
@Component
public class BlockWordSerializer extends JsonSerializer<String>
    implements ContextualSerializer, ApplicationContextAware {

  private final BlockService blockService;
  private BlockWord annotation;
  private static ApplicationContext applicationContext;

  public BlockWordSerializer(BlockService blockService) {
    this.blockService = blockService;
  }

  // Jackson이 사용하는 기본 생성자
  public BlockWordSerializer() {
    this.blockService = null;
  }

  private BlockWordSerializer(BlockService blockService, BlockWord annotation) {
    this.blockService = blockService;
    this.annotation = annotation;
  }

  @Override
  public void setApplicationContext(@NotNull ApplicationContext context) {
    applicationContext = context;
  }

  private BlockService getBlockService() {
    if (blockService != null) {
      return blockService;
    }
    if (applicationContext != null) {
      return applicationContext.getBean(BlockService.class);
    }
    return null;
  }

  @Override
  public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
      throws JsonMappingException {

    if (property != null) {
      BlockWord ann = property.getAnnotation(BlockWord.class);
      if (ann != null) {
        BlockService actualBlockService = getBlockService();
        if (actualBlockService == null) {
          log.warn("BlockService를 찾을 수 없어 @BlockWord 어노테이션 무시");
          return new StringSerializer();
        }
        return new BlockWordSerializer(actualBlockService, ann);
      }

      if (property.getMember() != null) {
        BlockWord memberAnn = property.getMember().getAnnotation(BlockWord.class);
        if (memberAnn != null) {
          BlockService actualBlockService = getBlockService();
          if (actualBlockService == null) {
            log.warn("BlockService를 찾을 수 없어 @BlockWord 어노테이션 무시");
            return new StringSerializer();
          }
          return new BlockWordSerializer(actualBlockService, memberAnn);
        }
      }
    }
    return new StringSerializer();
  }

  @Override
  public void serialize(String value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    if (annotation == null) {
      gen.writeString(value);
      return;
    }

    BlockService actualBlockService = getBlockService();
    if (actualBlockService == null) {
      gen.writeString(value);
      return;
    }

    try {
      Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(null);
      if (currentUserId == null) {
        gen.writeString(value);
        return;
      }

      Object currentObject = gen.getCurrentValue();
      if (currentObject == null) {
        gen.writeString(value);
        return;
      }

      Long authorId = extractUserIdByPath(currentObject, annotation.userIdPath());
      if (authorId == null) {
        gen.writeString(value);
        return;
      }

      boolean isBlocked = actualBlockService.isBlocked(currentUserId, authorId);
      if (isBlocked) {
        gen.writeString(annotation.value());
      } else {
        gen.writeString(value);
      }

    } catch (Exception e) {
      log.warn("차단 필터링 중 오류 발생, 원본 값 반환: {}", e.getMessage());
      gen.writeString(value);
    }
  }

  /**
   * 지정된 경로로 객체에서 userId 추출
   *
   * @param object 대상 객체
   * @param userIdPath userId 경로 (예: "userId", "userInfo.userId")
   * @return 추출된 userId, 실패시 null
   */
  private Long extractUserIdByPath(Object object, String userIdPath) {
    try {
      String[] pathParts = userIdPath.split("\\.");
      Object currentObject = object;

      // 경로를 따라 객체 탐색
      for (String part : pathParts) {
        if (currentObject == null) {
          return null;
        }

        Field field = findField(currentObject.getClass(), part);
        if (field == null) {
          return null;
        }

        field.setAccessible(true);
        currentObject = field.get(currentObject);
      }

      return currentObject instanceof Long ? (Long) currentObject : null;

    } catch (Exception e) {
      log.warn("userId 추출 중 오류 발생: {}", e.getMessage());
      return null;
    }
  }

  /** 클래스에서 필드 찾기 (상속 계층 포함) */
  private Field findField(Class<?> clazz, String fieldName) {
    Class<?> currentClass = clazz;

    while (currentClass != null) {
      try {
        return currentClass.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        currentClass = currentClass.getSuperclass();
      }
    }

    return null;
  }
}

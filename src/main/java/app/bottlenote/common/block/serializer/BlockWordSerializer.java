package app.bottlenote.common.block.serializer;

import app.bottlenote.common.block.annotation.BlockWord;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.block.service.BlockService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * @BlockWord 어노테이션이 적용된 필드의 Jackson 커스텀 시리얼라이저
 * 차단된 사용자의 컨텐츠를 대체 메시지로 변경합니다.
 */
@Slf4j
@Component
public class BlockWordSerializer extends JsonSerializer<String> implements ContextualSerializer, ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private final BlockService blockService;
    private BlockWord blockWordAnnotation;

    public BlockWordSerializer(BlockService blockService) {
        this.blockService = blockService;
    }
    
    public BlockWordSerializer() {
        this.blockService = null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BlockWordSerializer.applicationContext = applicationContext;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (blockWordAnnotation == null) {
            // 어노테이션이 없는 경우 원본 값 반환
            gen.writeString(value);
            return;
        }

        // BlockService 획득 (의존성 주입된 것이 있으면 사용, 없으면 ApplicationContext에서 가져오기)
        BlockService currentBlockService = blockService;
        if (currentBlockService == null && applicationContext != null) {
            try {
                currentBlockService = applicationContext.getBean(BlockService.class);
            } catch (Exception e) {
                log.warn("ApplicationContext에서 BlockService를 가져오는데 실패: {}", e.getMessage());
            }
        }
        
        if (currentBlockService == null) {
            // BlockService를 찾을 수 없는 경우 원본 값 반환 (테스트 환경 등)
            gen.writeString(value);
            return;
        }

        try {
            // 현재 사용자 ID 획득
            Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(null);
            if (currentUserId == null) {
                // 비로그인 사용자는 필터링 스킵
                gen.writeString(value);
                return;
            }

            // 현재 직렬화 중인 객체 획득
            Object currentObject = gen.getCurrentValue();
            if (currentObject == null) {
                gen.writeString(value);
                return;
            }

            // userIdPath로 작성자 ID 추출
            Long authorId = extractUserIdByPath(currentObject, blockWordAnnotation.userIdPath());
            if (authorId == null) {
                gen.writeString(value);
                return;
            }

            // 차단 관계 확인
            boolean isBlocked = currentBlockService.isBlocked(currentUserId, authorId);
            if (isBlocked) {
                // 차단된 경우 대체 메시지로 변경
                gen.writeString(blockWordAnnotation.value());
            } else {
                // 정상인 경우 원본 값 사용
                gen.writeString(value);
            }

        } catch (Exception e) {
            log.warn("BlockWordSerializer 처리 중 오류 발생. 원본 값 반환: {}", e.getMessage());
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {

        if (property != null) {
            // @BlockWord 어노테이션 찾기
            BlockWord annotation = property.getAnnotation(BlockWord.class);
            if (annotation == null && property.getMember() != null) {
                annotation = property.getMember().getAnnotation(BlockWord.class);
            }

            if (annotation != null) {
                // ApplicationContext에서 BlockService 가져오기
                BlockService contextBlockService = null;
                if (applicationContext != null) {
                    try {
                        contextBlockService = applicationContext.getBean(BlockService.class);
                    } catch (Exception e) {
                        log.warn("ApplicationContext에서 BlockService 가져오기 실패: {}", e.getMessage());
                    }
                }
                
                BlockWordSerializer serializer = new BlockWordSerializer(contextBlockService);
                serializer.blockWordAnnotation = annotation;
                return serializer;
            }
        }

        return this;
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

    /**
     * 클래스에서 필드 찾기 (상속 계층 포함)
     */
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

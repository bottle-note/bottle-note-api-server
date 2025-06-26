package app.bottlenote.global.aspect;

import app.bottlenote.global.annotation.BlockFilter;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.BlockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 차단 필터 AOP 클래스
 *
 * @BlockFilter 어노테이션이 붙은 메서드의 응답을 가로채서
 * 차단된 사용자의 컨텐츠를 필터링합니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class BlockFilterAspect {

	private final BlockService blockService;
	private final ObjectMapper objectMapper;

	/**
	 * @BlockFilter 어노테이션이 붙은 메서드를 Around 어드바이스로 처리
	 */
	@Around("@annotation(blockFilter)")
	public Object applyBlockFilter(ProceedingJoinPoint joinPoint, BlockFilter blockFilter) throws Throwable {

		// 1. 원본 메서드 실행
		Object result = joinPoint.proceed();

		// 2. 현재 사용자 ID 확인
		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

		if (currentUserId == -1L) {
			// 익명 사용자는 차단 로직 적용하지 않음
			return result;
		}

		// 3. 차단된 사용자 목록 조회
		Set<Long> blockedUserIds = blockService.getBlockedUserIds(currentUserId);

		if (blockedUserIds.isEmpty()) {
			return result;
		}

		// 4. 응답 데이터에 차단 로직 적용 (항상 블러 처리)
		return applyBlockFilter(result, blockedUserIds, blockFilter.userField());
	}

	/**
	 * 응답 데이터에 차단 로직을 적용하는 메서드 (단순 블러 처리)
	 */
	private Object applyBlockFilter(Object result, Set<Long> blockedUserIds, String userField) {

		if (!(result instanceof ResponseEntity)) {
			return result;
		}

		ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
		Object body = responseEntity.getBody();

		if (!(body instanceof GlobalResponse)) {
			return result;
		}

		GlobalResponse globalResponse = (GlobalResponse) body;
		Object data = globalResponse.getData();

		if (data == null) {
			return result;
		}

		// 데이터 타입에 따른 블러 처리
		Object filteredData;
		if (data instanceof List) {
			filteredData = blurListData((List<?>) data, blockedUserIds, userField);
		} else {
			filteredData = blurSingleData(data, blockedUserIds, userField);
		}

		// 블러 처리된 데이터로 새로운 응답 생성
		GlobalResponse filteredResponse = GlobalResponse.success(filteredData);
		return ResponseEntity.ok(filteredResponse);
	}

	/**
	 * List 형태의 데이터 블러 처리
	 */
	@SuppressWarnings("unchecked")
	private List<?> blurListData(List<?> dataList, Set<Long> blockedUserIds, String userField) {
		return dataList.stream()
				.map(item -> blurItemIfBlocked(item, blockedUserIds, userField))
				.collect(Collectors.toList());
	}

	/**
	 * 단일 객체 데이터 블러 처리
	 */
	private Object blurSingleData(Object data, Set<Long> blockedUserIds, String userField) {
		return blurItemIfBlocked(data, blockedUserIds, userField);
	}

	/**
	 * 개별 항목 차단 여부 확인 후 블러 처리
	 */
	private Object blurItemIfBlocked(Object item, Set<Long> blockedUserIds, String userField) {
		if (item == null) {
			return item;
		}

		// 사용자 ID 추출
		Long userId = extractUserIdFromObject(item, userField);

		if (userId == null || !blockedUserIds.contains(userId)) {
			return item; // 차단된 사용자가 아니면 원본 반환
		}

		// 차단된 사용자면 블러 처리
		return blurContent(item);
	}

	/**
	 * 객체에서 사용자 ID를 추출하는 메서드
	 */
	private Long extractUserIdFromObject(Object obj, String fieldName) {
		try {
			Field field = findFieldInClass(obj.getClass(), fieldName);
			if (field != null) {
				field.setAccessible(true);
				Object value = field.get(obj);
				if (value instanceof Long) {
					return (Long) value;
				}
			}
		} catch (Exception e) {
			log.warn("사용자 ID 추출 실패 - 객체: {}, 필드: {}, 에러: {}",
					obj.getClass().getSimpleName(), fieldName, e.getMessage());
		}
		return null;
	}

	/**
	 * 클래스 계층구조에서 필드를 찾는 메서드
	 */
	private Field findFieldInClass(Class<?> clazz, String fieldName) {
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

	/**
	 * 차단된 사용자의 컨텐츠를 블러 처리하는 메서드
	 */
	private Object blurContent(Object originalItem) {
		try {
			// Jackson을 사용한 안전한 깊은 복사
			Object clonedItem = deepCloneObject(originalItem);

			// 차단된 사용자 표시로 마스킹
			maskField(clonedItem, "content", "차단된 사용자의 글입니다");
			maskField(clonedItem, "title", "차단된 사용자의 글입니다");
			maskField(clonedItem, "authorNickname", "차단된 사용자");
			maskField(clonedItem, "nickname", "차단된 사용자");
			maskField(clonedItem, "authorProfileImage", null);
			maskField(clonedItem, "profileImage", null);

			return clonedItem;

		} catch (Exception e) {
			log.error("차단 처리 실패: {}", e.getMessage());
			// 실패 시 원본 반환
			return originalItem;
		}
	}

	/**
	 * 객체의 특정 필드를 마스킹하는 메서드
	 */
	private void maskField(Object obj, String fieldName, Object maskValue) {
		try {
			Field field = findFieldInClass(obj.getClass(), fieldName);
			if (field != null) {
				field.setAccessible(true);
				field.set(obj, maskValue);
			}
		} catch (Exception e) {
			// 필드가 없거나 설정 실패 시 무시 (정상적인 상황)
		}
	}

	/**
	 * Jackson ObjectMapper를 사용한 안전한 깊은 복사
	 */
	private Object deepCloneObject(Object original) {
		try {
			if (original == null) {
				return null;
			}
			
			// Jackson을 사용한 깊은 복사
			String jsonString = objectMapper.writeValueAsString(original);
			return objectMapper.readValue(jsonString, original.getClass());
		} catch (Exception e) {
			log.warn("객체 복사 실패, 원본 반환: {}", e.getMessage());
			return original;
		}
	}
}

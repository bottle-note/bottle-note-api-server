package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

/**
 * Admin 큐레이션 목록 응답 항목
 *
 * @param id 큐레이션 ID
 * @param name 큐레이션 이름
 * @param alcoholCount 포함된 위스키 수
 * @param displayOrder 노출 순서
 * @param isActive 활성화 상태
 * @param createdAt 생성일시
 */
public record AdminCurationListResponse(
    Long id,
    String name,
    Integer alcoholCount,
    Integer displayOrder,
    Boolean isActive,
    LocalDateTime createdAt) {}

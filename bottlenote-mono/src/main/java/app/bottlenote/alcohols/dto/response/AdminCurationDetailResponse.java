package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Admin 큐레이션 상세 조회 응답
 *
 * @param id 큐레이션 ID
 * @param name 큐레이션 이름
 * @param description 설명
 * @param coverImageUrl 커버 이미지 URL
 * @param displayOrder 노출 순서
 * @param isActive 활성화 상태
 * @param alcoholIds 포함된 위스키 ID 목록
 * @param createdAt 생성일시
 * @param modifiedAt 수정일시
 */
public record AdminCurationDetailResponse(
    Long id,
    String name,
    String description,
    String coverImageUrl,
    Integer displayOrder,
    Boolean isActive,
    Set<Long> alcoholIds,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {

  public static AdminCurationDetailResponse of(
      Long id,
      String name,
      String description,
      String coverImageUrl,
      Integer displayOrder,
      Boolean isActive,
      Set<Long> alcoholIds,
      LocalDateTime createdAt,
      LocalDateTime modifiedAt) {
    return new AdminCurationDetailResponse(
        id,
        name,
        description,
        coverImageUrl,
        displayOrder,
        isActive,
        alcoholIds,
        createdAt,
        modifiedAt);
  }
}

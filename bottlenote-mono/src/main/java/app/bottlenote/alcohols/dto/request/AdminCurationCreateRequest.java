package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import lombok.Builder;

/**
 * Admin 큐레이션 생성 요청
 *
 * @param name 큐레이션 이름
 * @param description 설명
 * @param coverImageUrl 커버 이미지 URL
 * @param displayOrder 노출 순서
 * @param alcoholIds 포함할 위스키 ID 목록
 */
public record AdminCurationCreateRequest(
    @NotBlank(message = "큐레이션 이름은 필수입니다.") String name,
    String description,
    String coverImageUrl,
    Integer displayOrder,
    Set<Long> alcoholIds) {

  @Builder
  public AdminCurationCreateRequest {
    displayOrder = displayOrder != null ? displayOrder : 0;
    alcoholIds = alcoholIds != null ? alcoholIds : Set.of();
  }
}

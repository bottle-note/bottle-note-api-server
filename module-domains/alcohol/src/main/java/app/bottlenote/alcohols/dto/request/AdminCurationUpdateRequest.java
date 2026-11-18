package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Admin 큐레이션 수정 요청
 *
 * @param name 큐레이션 이름
 * @param description 설명
 * @param coverImageUrl 커버 이미지 URL
 * @param displayOrder 노출 순서
 * @param isActive 활성화 상태
 * @param alcoholIds 포함할 위스키 ID 목록
 */
public record AdminCurationUpdateRequest(
    @NotBlank(message = "큐레이션 이름은 필수입니다.") String name,
    String description,
    String coverImageUrl,
    @NotNull(message = "노출 순서는 필수입니다.") Integer displayOrder,
    @NotNull(message = "활성화 상태는 필수입니다.") Boolean isActive,
    Set<Long> alcoholIds) {

  public AdminCurationUpdateRequest {
    alcoholIds = alcoholIds != null ? alcoholIds : Set.of();
  }
}

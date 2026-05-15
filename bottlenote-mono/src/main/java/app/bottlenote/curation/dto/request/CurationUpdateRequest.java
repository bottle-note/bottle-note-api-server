package app.bottlenote.curation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CurationUpdateRequest(
    @NotNull(message = "큐레이션 스펙 ID는 필수입니다.") Long specId,
    @NotBlank(message = "큐레이션 이름은 필수입니다.") String name,
    String description,
    @NotEmpty(message = "큐레이션 이미지는 최소 1개 이상이어야 합니다.")
        @Size(max = 3, message = "큐레이션 이미지는 최대 3개까지 등록할 수 있습니다.")
        List<String> imageUrls,
    LocalDate exposureStartDate,
    LocalDate exposureEndDate,
    @NotNull(message = "노출 순서는 필수입니다.") Integer displayOrder,
    @NotNull(message = "활성화 상태는 필수입니다.") Boolean isActive,
    @NotNull(message = "payload는 필수입니다.") Object payload) {}

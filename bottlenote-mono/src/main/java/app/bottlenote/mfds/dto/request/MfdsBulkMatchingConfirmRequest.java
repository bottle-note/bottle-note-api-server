package app.bottlenote.mfds.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;

/** 미리보기에서 고른 신고만 한 트랜잭션으로 확정한다. */
public record MfdsBulkMatchingConfirmRequest(
    @NotNull(message = "sourceDeclarationId는 필수입니다.") Long sourceDeclarationId,
    @NotNull(message = "alcoholId는 필수입니다.") Long alcoholId,
    Long distilleryId,
    Long regionId,
    @NotEmpty(message = "declarationIds는 한 건 이상이어야 합니다.") List<@NotNull Long> declarationIds,
    @NotBlank(message = "previewToken은 필수입니다.")
        @Pattern(regexp = "^[0-9a-f]{64}$", message = "previewToken은 서버가 발급한 64자리 식별자여야 합니다.")
        String previewToken,
    @NotNull(message = "previewExpiresAt은 필수입니다.") LocalDateTime previewExpiresAt) {}

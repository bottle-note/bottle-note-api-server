package app.bottlenote.mfds.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 고른 신고에 주류·증류소·지역을 그대로 반영한다. 증류소·지역을 생략하면 주류에 등록된 값을 사용한다. */
public record MfdsBulkMatchingConfirmRequest(
    @NotNull(message = "alcoholId는 필수입니다.") Long alcoholId,
    Long distilleryId,
    Long regionId,
    @NotEmpty(message = "declarationIds는 한 건 이상이어야 합니다.") List<@NotNull Long> declarationIds) {}

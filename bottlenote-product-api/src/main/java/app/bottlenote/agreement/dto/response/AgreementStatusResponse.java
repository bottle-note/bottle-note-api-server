package app.bottlenote.agreement.dto.response;

import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.AgreementEvaluation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 사용자의 현재 약관 동의 상태 응답을 나타낸다. */
@Schema(description = "사용자 동의 충족 상태")
public record AgreementStatusResponse(
    @Schema(description = "모든 필수 동의 충족 여부") boolean eligible,
    @Schema(description = "유형별 동의 상태") List<Item> items) {

  public static AgreementStatusResponse from(AgreementEvaluation evaluation) {
    return new AgreementStatusResponse(
        evaluation.eligible(), evaluation.items().stream().map(Item::from).toList());
  }

  /** 약관 유형별 현재 동의 상태를 나타낸다. */
  @Schema(name = "AgreementStatusItem", description = "유형별 동의 상태")
  public record Item(
      @Schema(description = "동의 유형") AgreementType type,
      @Schema(description = "필수 동의 여부") boolean required,
      @Schema(description = "현재 동의 충족 여부") boolean agreed) {

    private static Item from(AgreementEvaluation.Item item) {
      return new Item(item.type(), item.required(), item.agreed());
    }
  }
}

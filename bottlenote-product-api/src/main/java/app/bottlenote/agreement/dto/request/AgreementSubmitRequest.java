package app.bottlenote.agreement.dto.request;

import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.AgreementSubmission;
import app.bottlenote.agreement.exception.AgreementException;
import app.bottlenote.agreement.exception.AgreementExceptionCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "사용자 동의 제출 요청")
public record AgreementSubmitRequest(
    @NotEmpty(message = "AGREEMENTS_REQUIRED") List<@Valid Item> agreements) {

  public List<AgreementSubmission> toSubmissions() {
    return agreements.stream().map(Item::toSubmission).toList();
  }

  @Schema(name = "AgreementSubmitItem", description = "제출할 동의 항목")
  public record Item(
      @Schema(allowableValues = {"TERMS_OF_SERVICE", "PRIVACY_COLLECTION_USE"})
          @NotBlank(message = "AGREEMENT_TYPE_REQUIRED")
          String type,
      @NotNull(message = "AGREEMENT_ACTION_REQUIRED") AgreementAction action,
      @NotBlank(message = "AGREEMENT_CONTENT_EMPTY") String content,
      @NotNull(message = "AGREEMENT_INPUT_CONTEXT_REQUIRED") AgreementInputContext inputContext) {

    private AgreementSubmission toSubmission() {
      return new AgreementSubmission(agreementType(), action, content, inputContext);
    }

    private AgreementType agreementType() {
      try {
        return AgreementType.valueOf(type);
      } catch (IllegalArgumentException exception) {
        throw new AgreementException(AgreementExceptionCode.INVALID_AGREEMENT_TYPE);
      }
    }
  }
}

package app.bottlenote.campaigncontent.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class CampaignContentException extends AbstractCustomException {

  public CampaignContentException(CampaignContentExceptionCode code) {
    super(code);
  }
}

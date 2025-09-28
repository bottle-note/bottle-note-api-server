package app.bottlenote.history.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RedirectUrlType {
  REVIEW("/review"),
  ALCOHOL("/search/all"),
  ;

  private final String url;
}

package app.bottlenote.mfds.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** 매칭 후보 하나의 대상 ID와 점수(0~1) 쌍. */
public record MfdsMatchCandidate(Long id, BigDecimal score) {

  public MfdsMatchCandidate {
    Objects.requireNonNull(id, "후보 id는 null일 수 없습니다.");
    Objects.requireNonNull(score, "후보 score는 null일 수 없습니다.");
  }
}

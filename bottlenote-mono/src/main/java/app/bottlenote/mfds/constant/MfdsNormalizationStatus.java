package app.bottlenote.mfds.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MfdsNormalizationStatus {
  PENDING("대기", "정규화 처리 대기"),
  STALE("갱신 필요", "원본 변경으로 재정규화 필요"),
  NORMALIZED("완료", "정규화 처리 완료"),
  PARTIAL("부분 완료", "일부 필드만 정규화 완료"),
  REVIEW_REQUIRED("검토 필요", "관리자 검토 필요"),
  UNPARSED("파싱 실패", "원본 값을 정규화하지 못함");

  private final String name;
  private final String description;
}

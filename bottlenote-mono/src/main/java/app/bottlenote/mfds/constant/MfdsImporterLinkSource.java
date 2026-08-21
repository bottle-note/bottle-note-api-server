package app.bottlenote.mfds.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MfdsImporterLinkSource {
  PAGE_NAME("페이지 수입사명", "공식 페이지의 수입사명으로 연결"),
  PAGE_RCNO("페이지 신고번호", "공식 페이지의 수입신고번호로 연결");

  private final String name;
  private final String description;
}

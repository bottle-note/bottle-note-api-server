package app.bottlenote.curation.dto.request;

import lombok.Builder;

public record CurationSearchRequest(
    String keyword, String code, Boolean isActive, Integer page, Integer size) {

  public CurationSearchRequest(String keyword, Boolean isActive, Integer page, Integer size) {
    this(keyword, null, isActive, page, size);
  }

  @Builder
  public CurationSearchRequest {
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
